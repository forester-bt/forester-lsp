package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesImportAndCollectsDefinitions() throws IOException {
        Files.createDirectories(tempDir.resolve("std"));
        Files.writeString(tempDir.resolve("std/actions.tree"), """
                root success();
                root fail();
                root store(data: any);
                """);

        var workspace = new Workspace();
        workspace.addRoot(tempDir.toUri().toString());

        var doc = ForesterDocument.parse("import \"std::actions\"\nroot main sequence {}\n");
        var definitions = workspace.importedDefinitions(doc, "file:///project/main.tree");
        var names = definitions.stream().map(Workspace.ImportedDefinition::name).toList();

        assertTrue(names.contains("success"));
        assertTrue(names.contains("fail"));
        assertTrue(names.contains("store"));

        var store = definitions.stream()
                .map(Workspace.ImportedDefinition::definition)
                .filter(d -> d.name().equals("store")).findFirst().orElseThrow();
        assertEquals(1, store.params().size());
        assertEquals("data", store.params().get(0).name());
        assertEquals("any", store.params().get(0).type());
    }

    @Test
    void completionIncludesImportedDefinitionParams() throws IOException {
        Files.createDirectories(tempDir.resolve("std"));
        Files.writeString(tempDir.resolve("std/actions.tree"), """
                root success();
                root fail();
                root store(key: string, value: any);
                """);

        var workspace = new Workspace();
        workspace.addRoot(tempDir.toUri().toString());

        var doc = ForesterDocument.parse("import \"std::actions\"\nroot main sequence { store(");
        var labels = CompletionService.complete(doc, workspace, "file:///project/main.tree", 1, 27)
                .getItems().stream().map(CompletionItem::getLabel).toList();

        assertTrue(labels.contains("key"));
        assertTrue(labels.contains("value"));
        assertFalse(labels.contains("store"));
    }

    @Test
    void resolvesBundledStdlibResourceWithoutWorkspaceRoot() {
        var workspace = new Workspace();
        var doc = ForesterDocument.parse("import \"std::actions\"\nroot main sequence {}\n");
        var names = workspace.importedDefinitions(doc, "file:///project/main.tree")
                .stream().map(Workspace.ImportedDefinition::name).toList();

        assertTrue(names.contains("success"));
        assertTrue(names.contains("fail"));
        assertTrue(names.contains("http_get"));
    }

    @Test
    void resolvesRelativeImportFromSameDirectory() throws IOException {
        Files.writeString(tempDir.resolve("utils.tree"), """
                root do_thing();
                root reset();
                """);

        var workspace = new Workspace();
        var doc = ForesterDocument.parse("import \"utils.tree\"\nroot main sequence {}\n");
        String uri = tempDir.resolve("main.tree").toUri().toString();
        var definitions = workspace.importedDefinitions(doc, uri);

        var names = definitions.stream().map(Workspace.ImportedDefinition::name).toList();
        assertTrue(names.contains("do_thing"));
        assertTrue(names.contains("reset"));
        assertEquals("utils.tree", definitions.get(0).importPath());
    }

    @Test
    void resolvesNestedAndParentRelativeImports() throws IOException {
        Files.createDirectories(tempDir.resolve("sub/nested/nested"));
        Files.writeString(tempDir.resolve("sub/nested/nested/nested2.tree"), "root nested_action();\n");
        Files.writeString(tempDir.resolve("util.tree"), "root util_action();\n");

        var workspace = new Workspace();
        String uri = tempDir.resolve("sub/nested/main.tree").toUri().toString();

        var nestedDoc = ForesterDocument.parse("import \"nested/nested2.tree\"\nroot m {}\n");
        assertTrue(workspace.importedDefinitions(nestedDoc, uri).stream()
                .anyMatch(d -> d.name().equals("nested_action")));

        var parentDoc = ForesterDocument.parse("import \"../../util.tree\"\nroot m {}\n");
        assertTrue(workspace.importedDefinitions(parentDoc, uri).stream()
                .anyMatch(d -> d.name().equals("util_action")));
    }

    @Test
    void appliesImportAliases() throws IOException {
        Files.writeString(tempDir.resolve("utils.tree"), """
                root action();
                root other();
                """);

        var workspace = new Workspace();
        var doc = ForesterDocument.parse(
                "import \"utils.tree\" { action => action1 }\nroot main sequence {}\n");
        String uri = tempDir.resolve("main.tree").toUri().toString();

        var definitions = workspace.importedDefinitions(doc, uri);
        var names = definitions.stream().map(Workspace.ImportedDefinition::name).toList();

        assertTrue(names.contains("action1"));
        assertTrue(!names.contains("action"));
        assertTrue(names.contains("other"));
    }

    @Test
    void importedCompletionShowsPathInDetail() {
        var workspace = new Workspace();
        var doc = ForesterDocument.parse("import \"std::actions\"\nroot main sequence { ");
        var items = CompletionService.complete(doc, workspace, "file:///project/main.tree", 1, 21).getItems();

        var store = items.stream()
                .filter(i -> i.getLabel().equals("store")).findFirst().orElseThrow();
        assertTrue(store.getDetail().startsWith("std::actions"));
        assertTrue(store.getDetail().contains("key: string"));
    }

    @Test
    void suggestsTreeFilesInImportString() throws IOException {
        Files.writeString(tempDir.resolve("utils.tree"), "root a();\n");
        Files.createDirectories(tempDir.resolve("sub"));
        Files.writeString(tempDir.resolve("sub/nested.tree"), "root b();\n");

        var workspace = new Workspace();
        var doc = ForesterDocument.parse("import \"\"\nroot m {}\n");
        String uri = tempDir.resolve("main.tree").toUri().toString();
        var labels = CompletionService.complete(doc, workspace, uri, 0, 8).getItems()
                .stream().map(CompletionItem::getLabel).toList();

        assertTrue(labels.contains("utils.tree"));
        assertTrue(labels.contains("sub"));
        assertTrue(labels.contains("../"));
    }

    @Test
    void composesImportPathIntoSubdirectory() throws IOException {
        Files.createDirectories(tempDir.resolve("sub"));
        Files.writeString(tempDir.resolve("sub/nested.tree"), "root b();\n");

        var workspace = new Workspace();
        var doc = ForesterDocument.parse("import \"sub/\"\nroot m {}\n");
        String uri = tempDir.resolve("main.tree").toUri().toString();
        var labels = CompletionService.complete(doc, workspace, uri, 0, 12).getItems()
                .stream().map(CompletionItem::getLabel).toList();

        assertTrue(labels.contains("nested.tree"));
    }
}
