package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ForesterDocumentTest {

    @Test
    void collectsDefinitions() {
        var doc = ForesterDocument.parse("""
                import "std::actions"
                root main sequence {
                    say(msg = "hello")
                }
                """);

        assertEquals(1, doc.getDefinitions().size());
        var definition = doc.getDefinitions().get(0);
        assertEquals("main", definition.name());
        assertEquals("root", definition.treeType());
        assertEquals(1, definition.nameRange().getStart().getLine());
        assertEquals(5, definition.nameRange().getStart().getCharacter());
    }

    @Test
    void collectsImports() {
        var doc = ForesterDocument.parse("import \"std::actions\"\n");

        assertEquals(1, doc.getImports().size());
        var imp = doc.getImports().get(0);
        assertEquals("std::actions", imp.path());
        assertEquals(0, imp.pathRange().getStart().getLine());
        assertEquals(7, imp.pathRange().getStart().getCharacter());
    }

    @Test
    void validDocumentHasNoDiagnostics() {
        var doc = ForesterDocument.parse("""
                import "std::actions"
                root main sequence {
                    say(msg = "hello")
                }
                """);
        assertTrue(doc.getDiagnostics().isEmpty());
    }

    @Test
    void survivesInvalidInput() {
        var doc = ForesterDocument.parse("root\n");
        assertNotNull(doc.getParseTree());
        assertEquals(1, doc.getDiagnostics().size());
    }
}
