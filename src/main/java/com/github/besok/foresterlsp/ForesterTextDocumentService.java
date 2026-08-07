package com.github.besok.foresterlsp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.services.TextDocumentService;

public class ForesterTextDocumentService implements TextDocumentService {

    private final Map<String, String> documents = new ConcurrentHashMap<>();

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        documents.put(params.getTextDocument().getUri(), params.getTextDocument().getText());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        // Full sync: the last change contains the whole document.
        var changes = params.getContentChanges();
        if (!changes.isEmpty()) {
            documents.put(params.getTextDocument().getUri(),
                    changes.get(changes.size() - 1).getText());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        documents.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        String text = documents.get(params.getTextDocument().getUri());
        List<Integer> data = text == null ? List.of() : SemanticTokenizer.tokenize(text);
        return CompletableFuture.completedFuture(new SemanticTokens(data));
    }
}
