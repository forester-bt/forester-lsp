package com.github.besok.foresterlsp;

import java.util.List;

import org.eclipse.lsp4j.Diagnostic;

/**
 * Convenience entry point for producing syntax diagnostics; delegates to the
 * parsed document model.
 */
public final class DiagnosticService {

    private DiagnosticService() {
    }

    public static List<Diagnostic> analyze(String text) {
        return ForesterDocument.parse(text).getDiagnostics();
    }
}
