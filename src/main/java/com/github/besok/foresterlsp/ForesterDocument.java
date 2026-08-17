package com.github.besok.foresterlsp;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.github.besok.foresterlsp.grammar.TreeLexer;
import com.github.besok.foresterlsp.grammar.TreeParser;
import com.github.besok.foresterlsp.grammar.TreeParserBaseListener;

/**
 * Parsed representation of a single `.tree` document: the raw text, the parse
 * tree, the syntax diagnostics, and a symbol table (definitions and imports)
 * that other language features can query.
 */
public final class ForesterDocument {

    public record Definition(String name, String treeType, List<Param> params, Range nameRange, Range fullRange) {

        public record Param(String name, String type) {
        }
    }

    public record Import(String path, List<Alias> aliases, Range pathRange) {

        public record Alias(String name, String rename) {
        }
    }

    private final String text;
    private final CommonTokenStream tokens;
    private final TreeParser.FileContext parseTree;
    private final List<Diagnostic> diagnostics;
    private final List<Definition> definitions;
    private final List<Import> imports;

    private ForesterDocument(String text, CommonTokenStream tokens, TreeParser.FileContext parseTree,
                             List<Diagnostic> diagnostics, List<Definition> definitions, List<Import> imports) {
        this.text = text;
        this.tokens = tokens;
        this.parseTree = parseTree;
        this.diagnostics = diagnostics;
        this.definitions = definitions;
        this.imports = imports;
    }

    public static ForesterDocument parse(String text) {
        var lexer = new TreeLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        var diagnostics = new ArrayList<Diagnostic>();
        lexer.addErrorListener(new ErrorCollector(diagnostics));

        var tokens = new CommonTokenStream(lexer);
        var parser = new TreeParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ErrorCollector(diagnostics));

        var symbols = new SymbolCollector();
        parser.addParseListener(symbols);

        TreeParser.FileContext tree = null;
        try {
            tree = parser.file();
        } catch (RuntimeException ignored) {
        }

        return new ForesterDocument(text, tokens, tree, diagnostics, symbols.definitions, symbols.imports);
    }

    public String getText() {
        return text;
    }

    public CommonTokenStream getTokens() {
        return tokens;
    }

    public TreeParser.FileContext getParseTree() {
        return parseTree;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public List<Import> getImports() {
        return imports;
    }

    private static final class ErrorCollector extends BaseErrorListener {

        private final List<Diagnostic> diagnostics;

        private ErrorCollector(List<Diagnostic> diagnostics) {
            this.diagnostics = diagnostics;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            var diagnostic = new Diagnostic(
                    new Range(new Position(line - 1, charPositionInLine),
                            offendingSymbol instanceof Token token
                                    ? endOf(token)
                                    : new Position(line - 1, charPositionInLine + 1)),
                    msg);
            diagnostic.setSeverity(DiagnosticSeverity.Error);
            diagnostic.setSource("forester");
            diagnostics.add(diagnostic);
        }
    }

    private static final class SymbolCollector extends TreeParserBaseListener {

        private final List<Definition> definitions = new ArrayList<>();
        private final List<Import> imports = new ArrayList<>();

        @Override
        public void exitDefinition(TreeParser.DefinitionContext ctx) {
            var id = ctx.id();
            if (id == null || id.ID() == null) {
                return;
            }
            String name = id.ID().getText();
            String treeType = ctx.tree_type() == null ? "" : ctx.tree_type().getText();
            List<Definition.Param> params = new ArrayList<>();
            if (ctx.params() != null) {
                for (var param : ctx.params().param()) {
                    String paramName = param.id() != null && param.id().ID() != null ? param.id().ID().getText() : "";
                    String paramType = param.mes_type() != null ? param.mes_type().getText() : "";
                    params.add(new Definition.Param(paramName, paramType));
                }
            }
            definitions.add(new Definition(name, treeType, params, rangeOf(id.ID().getSymbol()), rangeOf(ctx)));
        }

        @Override
        public void exitImportSt(TreeParser.ImportStContext ctx) {
            var string = ctx.string();
            if (string == null || string.STRING() == null) {
                return;
            }
            List<Import.Alias> aliases = new ArrayList<>();
            if (ctx.importCalls() != null) {
                for (var importName : ctx.importCalls().import_name()) {
                    String name = importName.id(0).ID().getText();
                    String rename = importName.id().size() > 1 ? importName.id(1).ID().getText() : null;
                    aliases.add(new Import.Alias(name, rename));
                }
            }
            imports.add(new Import(unquote(string.STRING().getText()), aliases,
                    rangeOf(string.STRING().getSymbol())));
        }
    }

    private static String unquote(String literal) {
        if (literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"")) {
            return literal.substring(1, literal.length() - 1);
        }
        return literal;
    }

    private static Range rangeOf(Token token) {
        return new Range(startOf(token), endOf(token));
    }

    private static Range rangeOf(ParserRuleContext ctx) {
        if (ctx.getStart() == null || ctx.getStop() == null) {
            return new Range(new Position(0, 0), new Position(0, 0));
        }
        return new Range(startOf(ctx.getStart()), endOf(ctx.getStop()));
    }

    private static Position startOf(Token token) {
        return new Position(token.getLine() - 1, token.getCharPositionInLine());
    }

    private static Position endOf(Token token) {
        int line = token.getLine() - 1;
        int startChar = token.getCharPositionInLine();
        String[] segments = token.getText().split("\n", -1);
        int endLine = line + segments.length - 1;
        int endChar = segments.length == 1
                ? startChar + Math.max(1, segments[0].length())
                : Math.max(1, segments[segments.length - 1].length());
        return new Position(endLine, endChar);
    }
}
