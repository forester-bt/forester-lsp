package com.github.besok.foresterlsp;

import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.github.besok.foresterlsp.grammar.TreeLexer;


public final class DefinitionService {

    private DefinitionService() {
    }

    public static List<Location> findDefinition(ForesterDocument document, Workspace workspace, String uri,
                                                int line, int character) {
        CommonTokenStream tokens = document.getTokens();
        int offset = SourceUtil.offsetOf(document.getText(), line, character);
        Token token = SourceUtil.tokenAt(tokens, offset);
        if (token == null) {
            token = SourceUtil.previousToken(tokens, offset);
        }
        if (token == null) {
            return List.of();
        }

        // Import path string → the resolved file.
        if (token.getType() == TreeLexer.STRING) {
            Token before = SourceUtil.previousToken(tokens, token.getStartIndex());
            if (before != null && before.getType() == TreeLexer.IMPORT) {
                String importPath = SourceUtil.unquote(token.getText());
                return workspace.resolveImportPath(importPath, uri)
                        .map(path -> List.of(location(path.toUri().toString(), 0, 0)))
                        .orElseGet(List::of);
            }
        }

        // Identifier → local or imported definition.
        if (token.getType() == TreeLexer.ID) {
            String name = token.getText();
            for (ForesterDocument.Definition definition : document.getDefinitions()) {
                if (definition.name().equals(name)) {
                    return List.of(new Location(uri, definition.nameRange()));
                }
            }
            for (Workspace.ImportedDefinition imported : workspace.importedDefinitions(document, uri)) {
                if (imported.name().equals(name) && imported.source() != null) {
                    return List.of(location(imported.source().toUri().toString(),
                            imported.definition().nameRange().getStart().getLine(),
                            imported.definition().nameRange().getStart().getCharacter(),
                            imported.definition().nameRange().getEnd().getLine(),
                            imported.definition().nameRange().getEnd().getCharacter()));
                }
            }
        }

        return List.of();
    }

    private static Location location(String uri, int line, int character) {
        return new Location(uri, new Range(new Position(line, character), new Position(line, character)));
    }

    private static Location location(String uri, int startLine, int startChar, int endLine, int endChar) {
        return new Location(uri, new Range(new Position(startLine, startChar), new Position(endLine, endChar)));
    }
}
