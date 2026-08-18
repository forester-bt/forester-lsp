package com.github.besok.foresterlsp;

import java.util.ArrayList;
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
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextEdit;
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
        workspace.invalidate(uri);
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
        workspace.invalidate(uri);
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
        workspace.invalidate(params.getTextDocument().getUri());
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
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        ForesterDocument document = documents.get(params.getTextDocument().getUri());
        List<? extends Location> locations = document == null
                ? List.of()
                : DefinitionService.findDefinition(document, workspace, params.getTextDocument().getUri(),
                        params.getPosition().getLine(), params.getPosition().getCharacter());
        return CompletableFuture.completedFuture(Either.forLeft(locations));
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        ForesterDocument document = documents.get(params.getTextDocument().getUri());
        List<? extends Location> locations = document == null
                ? List.of()
                : ReferenceService.findReferences(document, workspace, params.getTextDocument().getUri(),
                        params.getPosition().getLine(), params.getPosition().getCharacter());
        return CompletableFuture.completedFuture(locations);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        ForesterDocument document = documents.get(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
        for (ForesterDocument.Definition definition : document.getDefinitions()) {
            Range range = definition.fullRange();
            Range selection = definition.nameRange();
            if (!contains(range, selection)) {
                range = selection;
            }
            var symbol = new DocumentSymbol(definition.name(), SymbolKind.Function,
                    range, selection, definition.treeType());
            symbols.add(Either.forRight(symbol));
        }
        return CompletableFuture.completedFuture(symbols);
    }

    private static boolean contains(Range outer, Range inner) {
        return isBeforeOrEqual(outer.getStart(), inner.getStart())
                && isBeforeOrEqual(inner.getEnd(), outer.getEnd());
    }

    private static boolean isBeforeOrEqual(Position a, Position b) {
        if (a.getLine() != b.getLine()) {
            return a.getLine() <= b.getLine();
        }
        return a.getCharacter() <= b.getCharacter();
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        ForesterDocument document = documents.get(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        String text = document.getText();
        String formatted = FormattingService.format(text);
        String[] lines = text.split("\n", -1);
        Position end = new Position(lines.length - 1, lines[lines.length - 1].length());
        TextEdit edit = new TextEdit(new Range(new Position(0, 0), end), formatted);
        return CompletableFuture.completedFuture(List.of(edit));
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
