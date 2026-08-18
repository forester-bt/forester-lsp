package com.github.besok.foresterlsp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;

import com.github.besok.foresterlsp.grammar.TreeLexer;
import com.github.besok.foresterlsp.grammar.TreeParser;
import com.github.besok.foresterlsp.grammar.TreeParserBaseListener;

/**
 * Finds references to the identifier under the cursor by walking the parse
 * tree of the current document and every `.tree` file in the workspace,
 * collecting invocation targets, argument names and import names that match.
 */
public final class ReferenceService {

    private ReferenceService() {
    }

    public static List<Location> findReferences(ForesterDocument document, Workspace workspace, String uri,
                                                int line, int character) {
        CommonTokenStream tokens = document.getTokens();
        int offset = SourceUtil.offsetOf(document.getText(), line, character);
        Token token = SourceUtil.tokenAt(tokens, offset);
        if (token == null) {
            token = SourceUtil.previousToken(tokens, offset);
        }
        if (token == null || token.getType() != TreeLexer.ID) {
            return List.of();
        }
        String name = token.getText();

        List<Location> locations = new ArrayList<>();
        for (Range range : collectReferences(document, name)) {
            locations.add(new Location(uri, range));
        }
        for (Path file : workspace.treeFiles(uri)) {
            String fileUri = file.toUri().toString();
            if (fileUri.equals(uri)) {
                continue;
            }
            for (Range range : collectReferences(workspace.parse(file), name)) {
                locations.add(new Location(fileUri, range));
            }
        }
        return locations;
    }

    private static List<Range> collectReferences(ForesterDocument document, String name) {
        var collector = new ReferenceCollector(name);
        if (document.getParseTree() != null) {
            ParseTreeWalker.DEFAULT.walk(collector, document.getParseTree());
        }
        return collector.ranges;
    }

    private static final class ReferenceCollector extends TreeParserBaseListener {

        private final String name;
        private final List<Range> ranges = new ArrayList<>();

        private ReferenceCollector(String name) {
            this.name = name;
        }

        @Override
        public void exitInvocation(TreeParser.InvocationContext ctx) {
            if (ctx.id() != null && name.equals(ctx.id().getText())) {
                ranges.add(SourceUtil.rangeOf(ctx.id().ID().getSymbol()));
            }
        }

        @Override
        public void exitArg(TreeParser.ArgContext ctx) {
            for (var id : ctx.id()) {
                if (name.equals(id.getText())) {
                    ranges.add(SourceUtil.rangeOf(id.ID().getSymbol()));
                }
            }
        }

        @Override
        public void exitImport_name(TreeParser.Import_nameContext ctx) {
            for (var id : ctx.id()) {
                if (name.equals(id.getText())) {
                    ranges.add(SourceUtil.rangeOf(id.ID().getSymbol()));
                }
            }
        }
    }
}
