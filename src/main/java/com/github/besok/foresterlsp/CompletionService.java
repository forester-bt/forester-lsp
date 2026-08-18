package com.github.besok.foresterlsp;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.besok.foresterlsp.grammar.TreeLexer;


public final class CompletionService {

    private static final List<String> STATIC_TYPES = List.of(
            "root", "parallel", "sequence", "m_sequence", "r_sequence", "fallback", "r_fallback");

    private static final List<String> MES_TYPES = List.of(
            "num", "array", "object", "string", "bool", "tree", "any");

    private CompletionService() {
    }

    public static CompletionList complete(ForesterDocument document, Workspace workspace, String uri,
                                          int line, int character) {
        CommonTokenStream tokens = document.getTokens();
        String text = document.getText();
        int offset = offsetOf(text, line, character);
        Token prev = previousToken(tokens, offset);

        var items = new ArrayList<CompletionItem>();

        if (prev != null && prev.getType() == TreeLexer.STRING) {
            Token before = previousToken(tokens, prev.getStartIndex());
            if (before != null && before.getType() == TreeLexer.IMPORT) {
                String raw = prev.getText();
                String inner = raw.length() >= 2
                        ? raw.substring(1, raw.length() - 1)
                        : "";
                Path currentDir = Path.of(URI.create(uri)).getParent();
                Path base = currentDir.resolve(inner).normalize();
                if (base.getParent() != null && inner.equals("")){
                    var item = new CompletionItem("../");
                    item.setKind(CompletionItemKind.Folder);
                    items.add(item);
                }

                try (var stream = Files.list(base)) {
                    for (Path entry : stream.sorted().toList()) {
                        String name = entry.getFileName().toString();

                        CompletionItem item = new CompletionItem(name);
                        item.setFilterText(name);
                        if (Files.isDirectory(entry)) {
                            item.setKind(CompletionItemKind.Folder);
                            items.add(item);
                        } else if (name.endsWith(".tree")) {
                            item.setKind(CompletionItemKind.File);
                            items.add(item);
                        }
                    }
                } catch (IOException e) {
                    return new CompletionList(true, items);
                }
                return new CompletionList(true, items);
            }
        }

        // Skip back over a partial word being typed.
        Token position = prev;
        if (prev != null && prev.getType() == TreeLexer.ID) {
            position = previousToken(tokens, prev.getStartIndex());
        }

        // After "id :" (or while typing the type): suggest message types.
        if (isParamColon(tokens, position)) {
            addMesTypes(items);
            return new CompletionList(items);
        }

        // After "(": either definition params (nothing) or call args (param names).
        if (position != null && position.getType() == TreeLexer.LPR) {
            Token target = previousToken(tokens, position.getStartIndex());
            if (target != null && target.getType() == TreeLexer.ID) {
                Token beforeTarget = previousToken(tokens, target.getStartIndex());
                boolean isDefinition = beforeTarget != null
                        && (beforeTarget.getType() == TreeLexer.ID || isStaticType(beforeTarget.getType()));
                if (!isDefinition) {
                    addParamNames(items, target.getText(), document, workspace, uri);
                }
            }
            return new CompletionList(items);
        }

        // Statement start: suggest tree types.
        if (position == null || position.getType() == TreeLexer.SEMI || position.getType() == TreeLexer.RBC) {
            addStaticTypes(items);
            return new CompletionList(items);
        }

        // After a tree type (static keyword or a custom id at statement start),
        // the definition name is free-form: suggest nothing.
        if (isStaticType(position.getType()) || isCustomTreeType(tokens, position)) {
            return new CompletionList(items);
        }

        // Inside a calls block: lambda or invocation.
        if (position.getType() == TreeLexer.LBC) {
            addStaticTypes(items);
            addDefinitions(items, document);
            addImported(items, workspace, document, uri);
        } else {
            addDefinitions(items, document);
            addImported(items, workspace, document, uri);
        }

        return new CompletionList(items);
    }

    private static boolean isCustomTreeType(CommonTokenStream tokens, Token position) {
        if (position == null || position.getType() != TreeLexer.ID) {
            return false;
        }
        Token before = previousToken(tokens, position.getStartIndex());
        return before == null
                || before.getType() == TreeLexer.SEMI
                || before.getType() == TreeLexer.RBC
                || before.getType() == TreeLexer.STRING;
    }

    private static boolean isStaticType(int type) {
        return switch (type) {
            case TreeLexer.ROOT,
                 TreeLexer.PARALLEL,
                 TreeLexer.SEQUENCE,
                 TreeLexer.MSEQUENCE,
                 TreeLexer.RSEQUENCE,
                 TreeLexer.FALLBACK,
                 TreeLexer.RFALLBACK -> true;
            default -> false;
        };
    }

    private static boolean isParamColon(CommonTokenStream tokens, Token colon) {
        if (colon == null || colon.getType() != TreeLexer.COLON) {
            return false;
        }
        Token before = previousToken(tokens, colon.getStartIndex());
        return before != null && before.getType() == TreeLexer.ID;
    }

    private static void addParamNames(List<CompletionItem> items, String target,
                                      ForesterDocument document, Workspace workspace, String uri) {
        for (ForesterDocument.Definition.Param param : findParams(target, document, workspace, uri)) {
            items.add(keyword(param.name(), CompletionItemKind.Variable));
        }
    }

    private static List<ForesterDocument.Definition.Param> findParams(String name,
                                                                      ForesterDocument document, Workspace workspace, String uri) {
        for (ForesterDocument.Definition definition : document.getDefinitions()) {
            if (definition.name().equals(name)) {
                return definition.params();
            }
        }
        for (Workspace.ImportedDefinition imported : workspace.importedDefinitions(document, uri)) {
            if (imported.name().equals(name)) {
                return imported.definition().params();
            }
        }
        return List.of();
    }

    private static void addStaticTypes(List<CompletionItem> items) {
        for (String type : STATIC_TYPES) {
            items.add(keyword(type, CompletionItemKind.Keyword));
        }
    }

    private static void addMesTypes(List<CompletionItem> items) {
        for (String type : MES_TYPES) {
            items.add(keyword(type, CompletionItemKind.TypeParameter));
        }
    }

    private static void addDefinitions(List<CompletionItem> items, ForesterDocument document) {
        for (ForesterDocument.Definition definition : document.getDefinitions()) {
            items.add(definitionItem(definition.name(), definition, null));
        }
    }

    private static void addImported(List<CompletionItem> items, Workspace workspace,
                                    ForesterDocument document, String uri) {
        for (Workspace.ImportedDefinition imported : workspace.importedDefinitions(document, uri)) {
            items.add(definitionItem(imported.name(), imported.definition(), imported.importPath()));
        }
    }


    private static CompletionItem pathItem(String insert, String filterText, CompletionItemKind kind,
                                           Position startPos, Position endPos) {
        var item = new CompletionItem(insert);
        item.setKind(kind);
        item.setFilterText(filterText);
        item.setTextEdit(Either.forLeft(new TextEdit(new Range(startPos, endPos), insert)));
        return item;
    }

    private static CompletionItem keyword(String label, CompletionItemKind kind) {
        var item = new CompletionItem(label);
        item.setKind(kind);
        return item;
    }

    private static CompletionItem definitionItem(String name, ForesterDocument.Definition definition,
                                                 String importPath) {
        var item = new CompletionItem(name);
        item.setKind(CompletionItemKind.Function);

        StringBuilder detail = new StringBuilder();
        if (importPath != null && !importPath.isEmpty()) {
            detail.append(importPath);
        }
        if (!definition.params().isEmpty()) {
            if (detail.length() > 0) {
                detail.append(' ');
            }
            detail.append(formatParams(definition.params()));
        }
        if (detail.length() > 0) {
            item.setDetail(detail.toString());
        }
        return item;
    }

    private static String formatParams(List<ForesterDocument.Definition.Param> params) {
        var sb = new StringBuilder("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params.get(i).name()).append(": ").append(params.get(i).type());
        }
        return sb.append(')').toString();
    }

    private static Token previousToken(CommonTokenStream tokens, int offset) {
        Token prev = null;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.EOF) {
                break;
            }
            if (token.getStartIndex() >= offset) {
                break;
            }
            if (token.getChannel() == Token.DEFAULT_CHANNEL) {
                prev = token;
            }
        }
        return prev;
    }

    private static int offsetOf(String text, int line, int character) {
        int offset = 0;
        int currentLine = 0;
        while (offset < text.length() && currentLine < line) {
            if (text.charAt(offset) == '\n') {
                currentLine++;
            }
            offset++;
        }
        return offset + character;
    }
}
