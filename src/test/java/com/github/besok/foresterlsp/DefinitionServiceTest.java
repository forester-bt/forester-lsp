package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefinitionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void importPathNavigatesToFile() throws IOException {
        Files.writeString(tempDir.resolve("utils.tree"), "root a();\n");

        var workspace = new Workspace();
        workspace.addRoot(tempDir.toUri().toString());
        var doc = ForesterDocument.parse("import \"utils.tree\"\nroot main sequence {}\n");
        String uri = tempDir.resolve("main.tree").toUri().toString();

        var locations = DefinitionService.findDefinition(doc, workspace, uri, 0, 10);

        assertEquals(1, locations.size());
        assertEquals(tempDir.resolve("utils.tree").toUri().toString(), locations.get(0).getUri());
    }

    @Test
    void identifierNavigatesToLocalDefinition() {
        var doc = ForesterDocument.parse("impl wait(duration: num);\nroot main sequence { wait }\n");
        var workspace = new Workspace();

        var locations = DefinitionService.findDefinition(doc, workspace, "file:///p/main.tree", 1, 22);

        assertEquals(1, locations.size());
        assertEquals("file:///p/main.tree", locations.get(0).getUri());
        assertEquals(0, locations.get(0).getRange().getStart().getLine());
        assertEquals(5, locations.get(0).getRange().getStart().getCharacter());
    }

    @Test
    void identifierNavigatesToImportedDefinition() throws IOException {
        Files.writeString(tempDir.resolve("utils.tree"), "root do_thing();\n");

        var workspace = new Workspace();
        workspace.addRoot(tempDir.toUri().toString());
        var doc = ForesterDocument.parse("import \"utils.tree\"\nroot main sequence { do_thing }\n");
        String uri = tempDir.resolve("main.tree").toUri().toString();

        var locations = DefinitionService.findDefinition(doc, workspace, uri, 1, 22);

        assertEquals(1, locations.size());
        assertEquals(tempDir.resolve("utils.tree").toUri().toString(), locations.get(0).getUri());
        assertEquals(0, locations.get(0).getRange().getStart().getLine());
        assertEquals(5, locations.get(0).getRange().getStart().getCharacter());
    }
}
