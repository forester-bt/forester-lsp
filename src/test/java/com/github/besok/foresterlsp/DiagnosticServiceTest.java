package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

class DiagnosticServiceTest {

    @Test
    void validDocumentHasNoDiagnostics() {
        var diagnostics = DiagnosticService.analyze("""
                import "std::actions"
                root main sequence {
                    say(msg = "hello")
                }
                """);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void reportsErrorForIncompleteDefinition() {
        var diagnostics = DiagnosticService.analyze("root\n");
        assertEquals(1, diagnostics.size());
        var diagnostic = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Error, diagnostic.getSeverity());
        assertEquals("forester", diagnostic.getSource());
        assertTrue(diagnostic.getRange().getStart().getLine() >= 0);
    }

    @Test
    void typedParamsAndInvocationProduceNoDiagnostics() {
        var diagnostics = DiagnosticService.analyze("foo bar(x: num) baz(1.5)\n");
        assertTrue(diagnostics.isEmpty(), "expected a valid definition, got: " + diagnostics);
    }
}
