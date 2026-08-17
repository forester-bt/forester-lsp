package com.github.besok.foresterlsp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

public class ForesterTextDocumentService implements TextDocumentService {

    private static final long DIAGNOSTICS_DELAY_MS = 300;

    private final Workspace workspace;
    private final Map<String, ForesterDocument> documents = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingDiagnostics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        var thread = new Thread(r, "forester-diagnostics");
        thread.setDaemon(true);
        return thread;
    });

    private volatile LanguageClient client;

    public ForesterTextDocumentService(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.put(uri, ForesterDocument.parse(params.getTextDocument().getText()));
        publishDiagnostics(uri);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        var changes = params.getContentChanges();
        if (changes.isEmpty()) {
            return;
        }
        String uri = params.getTextDocument().getUri();
        // Full sync: the last change contains the whole document.
        documents.put(uri, ForesterDocument.parse(changes.get(changes.size() - 1).getText()));
        scheduleDiagnostics(uri);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.remove(uri);
        ScheduledFuture<?> pending = pendingDiagnostics.remove(uri);
        if (pending != null) {
            pending.cancel(false);
        }
        LanguageClient c = client;
        if (c != null) {
            c.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
        }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        publishDiagnostics(params.getTextDocument().getUri());
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        ForesterDocument document = documents.get(params.getTextDocument().getUri());
        List<Integer> data = document == null ? List.of() : SemanticTokenizer.tokenize(document.getText());
        return CompletableFuture.completedFuture(new SemanticTokens(data));
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        ForesterDocument document = documents.get(params.getTextDocument().getUri());
        CompletionList list = document == null
                ? new CompletionList()
                : CompletionService.complete(document, workspace, params.getTextDocument().getUri(),
                        params.getPosition().getLine(), params.getPosition().getCharacter());
        return CompletableFuture.completedFuture(Either.forRight(list));
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }

    private void scheduleDiagnostics(String uri) {
        pendingDiagnostics.compute(uri, (key, previous) -> {
            if (previous != null) {
                previous.cancel(false);
            }
            return executor.schedule(() -> publishDiagnostics(key), DIAGNOSTICS_DELAY_MS, TimeUnit.MILLISECONDS);
        });
    }

    private void publishDiagnostics(String uri) {
        ForesterDocument document = documents.get(uri);
        LanguageClient c = client;
        if (document == null || c == null) {
            return;
        }
        c.publishDiagnostics(new PublishDiagnosticsParams(uri, document.getDiagnostics()));
    }
}
