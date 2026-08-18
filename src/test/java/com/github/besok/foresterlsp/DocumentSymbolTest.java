package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;

class DocumentSymbolTest {

    @Test
    void buildsDocumentSymbolsForDefinitions() {
        var service = new ForesterTextDocumentService(new Workspace());
        service.didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem("file:///p/main.tree", "forester", 1,
                        "impl wait(duration: num);\nroot main sequence {}\n")));

        var params = new DocumentSymbolParams(new TextDocumentIdentifier("file:///p/main.tree"));
        var symbols = service.documentSymbol(params).join();

        assertEquals(2, symbols.size());
        assertEquals("wait", symbols.get(0).getRight().getName());
        assertEquals("main", symbols.get(1).getRight().getName());
        assertEquals("impl", symbols.get(0).getRight().getDetail());
    }
}
