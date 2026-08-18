package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReferenceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void findsMultipleReferences() {
        var doc = ForesterDocument.parse(
                "impl wait(duration: num);\nroot main sequence { wait() wait() }\n");

        var locations = ReferenceService.findReferences(doc, new Workspace(), "file:///p/main.tree", 1, 22);

        assertEquals(2, locations.size());
        assertEquals(1, locations.get(0).getRange().getStart().getLine());
        assertEquals(21, locations.get(0).getRange().getStart().getCharacter());
    }

    @Test
    void definitionNameIsNotAReference() {
        var doc = ForesterDocument.parse("impl wait(duration: num);\n");

        var locations = ReferenceService.findReferences(doc, new Workspace(), "file:///p/main.tree", 0, 6);

        assertTrue(locations.isEmpty());
    }

    @Test
    void findsReferencesAcrossFiles() throws IOException {
        Files.writeString(tempDir.resolve("utils.tree"), "impl login(creds:object);\n");
        Files.writeString(tempDir.resolve("main.tree"),
                "import \"utils.tree\"\nroot main sequence { login(creds = {}) }\n");

        var workspace = new Workspace();
        workspace.addRoot(tempDir.toUri().toString());
        String utilsUri = tempDir.resolve("utils.tree").toUri().toString();
        var doc = workspace.parse(tempDir.resolve("utils.tree"));

        var locations = ReferenceService.findReferences(doc, workspace, utilsUri, 0, 6);

        assertEquals(1, locations.size());
        assertEquals(tempDir.resolve("main.tree").toUri().toString(), locations.get(0).getUri());
        assertEquals(1, locations.get(0).getRange().getStart().getLine());
    }
}
