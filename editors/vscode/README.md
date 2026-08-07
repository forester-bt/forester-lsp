# Forester LSP — VS Code

Thin client extension: registers the `forester` language for `.tree` files,
starts the LSP server over stdio, and gets semantic-token highlighting,
completion, comment toggling, and bracket matching.

## Prerequisite: build the server

```bash
./gradlew installDist   # launcher lands in build/install/forester-lsp/bin
```

## Install (one command)

From this folder:

```bash
npm install && npm run package && code --install-extension forester-lsp-client-0.0.1.vsix
```

This builds a `.vsix` and installs it into your regular VS Code.

When installed from a `.vsix`, tell the extension where the server is
(Settings → search "forester", or in `settings.json`):

```json
"forester.server.path": "/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp"
```

A path ending in `.jar` is also accepted (run via `java -jar`).

## Development (F5)

For hacking on the extension itself, no install needed:

1. `npm install` in this folder (once).
2. Open the repository root in VS Code and press `F5`
   ("Launch Forester Extension").
3. An Extension Development Host window opens with the `examples` folder —
   open `sample.tree`.

In dev mode the server path defaults to the repository's
`build/install/forester-lsp/bin` output, so no setting is required.

## Verify

Open a `.tree` file: keywords (`root`, `sequence`, …), strings, numbers, and
comments should be colored. Check "Output → Forester LSP" for server logs, and
"Developer: Inspect Editor Tokens and Scopes" to see the semantic token type
under the cursor.
