package com.github.besok.foresterlsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class ForesterLanguageServer implements LanguageServer, LanguageClientAware {

    private final ForesterTextDocumentService textDocumentService;
    private LanguageClient client;

    public ForesterLanguageServer() {
        this.textDocumentService = new ForesterTextDocumentService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        var result = new InitializeResult();
        var capabilities = new ServerCapabilities();

        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setCompletionProvider(new CompletionOptions(true, List.of()));

        result.setCapabilities(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new ForesterWorkspaceService();
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    public static void main(String[] args) throws IOException {
        boolean useSocket = findArgument(args, "--socket") != null;
        int port = Integer.parseInt(findArgument(args, "--port", "5007"));

        ForesterLanguageServer server = new ForesterLanguageServer();

        if (useSocket) {
            try (var serverSocket = new ServerSocket(port)) {
                var socket = serverSocket.accept();
                startServer(server, socket.getInputStream(), socket.getOutputStream());
            }
        } else {
            startServer(server, System.in, System.out);
        }
    }

    private static void startServer(ForesterLanguageServer server, InputStream in, OutputStream out) {
        var launcher = LSPLauncher.createServerLauncher(server, in, out,
                Executors.newCachedThreadPool(), null);
        launcher.startListening();
    }

    private static String findArgument(String[] args, String name) {
        return findArgument(args, name, null);
    }

    private static String findArgument(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name)) {
                if (i + 1 < args.length) {
                    return args[i + 1];
                }
                return "true";
            }
        }
        return defaultValue;
    }
}
