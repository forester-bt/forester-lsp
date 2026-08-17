package com.github.besok.foresterlsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.github.besok.foresterlsp.grammar.TreeLexer;
import com.github.besok.foresterlsp.grammar.TreeParser;
import com.github.besok.foresterlsp.grammar.TreeParserBaseListener;

/**
 * Produces LSP semantic token data for a Forester `.tree` document by parsing
 * the full text and mapping tokens to the semantic token legend declared by the
 * server. Lexer token types provide a base classification, refined by the
 * syntactic role a token plays in the parse tree.
 */
public final class SemanticTokenizer {

    public static final List<String> TOKEN_TYPES = List.of(
            "keyword",
            "type",
            "string",
            "number",
            "comment",
            "operator",
            "variable",
            "function",
            "parameter",
            "namespace");

    public static final List<String> TOKEN_MODIFIERS = List.of();

    private static final int KEYWORD = 0;
    private static final int TYPE = 1;
    private static final int STRING = 2;
    private static final int NUMBER = 3;
    private static final int COMMENT = 4;
    private static final int OPERATOR = 5;
    private static final int VARIABLE = 6;
    private static final int FUNCTION = 7;
    private static final int PARAMETER = 8;
    private static final int NAMESPACE = 9;

    private SemanticTokenizer() {
    }

    /**
     * Returns the token data in the relative encoding required by the LSP:
     * five integers per token (deltaLine, deltaStartChar, length, tokenType,
     * tokenModifiers). Multi-line tokens (block comments) are split into one
     * entry per line.
     */
    public static List<Integer> tokenize(String text) {
        var lexer = new TreeLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        var tokens = new CommonTokenStream(lexer);

        Map<Integer, Integer> overrides = RoleListener.collect(tokens);

        var data = new ArrayList<Integer>();
        int prevLine = 0;
        int prevChar = 0;

        for (Token token : tokens.getTokens()) {
            if (token.getType() == Token.EOF) {
                continue;
            }
            int semanticType = overrides.getOrDefault(token.getTokenIndex(), lexerType(token.getType()));
            if (semanticType < 0) {
                continue;
            }

            int line = token.getLine() - 1;
            int startChar = token.getCharPositionInLine();
            String[] lines = token.getText().split("\n", -1);

            for (int i = 0; i < lines.length; i++) {
                String segment = lines[i];
                int segLine = line + i;
                int segChar = i == 0 ? startChar : 0;
                int segLength = segment.endsWith("\r") ? segment.length() - 1 : segment.length();
                if (segLength == 0) {
                    continue;
                }

                data.add(segLine - prevLine);
                data.add(segLine == prevLine ? segChar - prevChar : segChar);
                data.add(segLength);
                data.add(semanticType);
                data.add(0);

                prevLine = segLine;
                prevChar = segChar;
            }
        }
        return data;
    }

    private static int lexerType(int antlrType) {
        return switch (antlrType) {
            case TreeLexer.IMPORT, TreeLexer.TRUE, TreeLexer.FALSE -> KEYWORD;
            case TreeLexer.STRING -> STRING;
            case TreeLexer.NUMBER -> NUMBER;
            case TreeLexer.BlockComment,
                 TreeLexer.LineComment -> COMMENT;
            case TreeLexer.EQ,
                 TreeLexer.EQ_A,
                 TreeLexer.DOT_DOT -> OPERATOR;
            default -> -1;
        };
    }

    /**
     * Walks the parse tree and records, per token index, the semantic token type
     * implied by the token's syntactic role. Overrides take precedence over the
     * lexer-based classification. Add one {@code enterX} method per parser rule
     * that should refine highlighting.
     */
    private static final class RoleListener extends TreeParserBaseListener {

        private final Map<Integer, Integer> overrides = new HashMap<>();

        private RoleListener() {
        }

        static Map<Integer, Integer> collect(CommonTokenStream tokens) {
            var parser = new TreeParser(tokens);
            parser.removeErrorListeners();
            var listener = new RoleListener();
            parser.addParseListener(listener);
            try {
                parser.file();
            } catch (RuntimeException ignored) {
                return listener.overrides;
            }
            return listener.overrides;
        }


        @Override
        public void enterTree_type(TreeParser.Tree_typeContext ctx) {
            mark(ctx.getStart(), FUNCTION);
        }

        @Override
        public void enterMes_type(TreeParser.Mes_typeContext ctx) {
            mark(ctx.getStart(), TYPE);
        }

        @Override
        public void exitDefinition(TreeParser.DefinitionContext ctx) {

        }

        @Override
        public void exitParam(TreeParser.ParamContext ctx) {
            if (ctx.id() != null) {
                mark(ctx.id().ID(), PARAMETER);
            }
        }

        @Override
        public void exitInvocation(TreeParser.InvocationContext ctx) {
            if (ctx.id() != null) {
                mark(ctx.id().ID(), FUNCTION);
            }
        }

        @Override
        public void exitArg(TreeParser.ArgContext ctx) {
            for (var id : ctx.id()) {
                mark(id.ID(), VARIABLE);
            }
        }

        @Override
        public void exitImport_name(TreeParser.Import_nameContext ctx) {
            for (var id : ctx.id()) {
                mark(id.ID(), VARIABLE);
            }
        }

        private void mark(TerminalNode node, int role) {
            if (node != null) {
                mark(node.getSymbol(), role);
            }
        }

        private void mark(Token token, int role) {
            if (token != null) {
                overrides.put(token.getTokenIndex(), role);
            }
        }
    }
}
