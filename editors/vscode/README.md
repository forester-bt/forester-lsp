 Forester LSP — VS Code

Thin client extension: registers the `forester` language for `.tree` files,
starts the LSP server over stdio, and gets semantic-token highlighting,
completion, comment toggling, and bracket matching.

## Supported features

Language-server features (from `forester-lsp`):

- **Syntax highlighting** via LSP semantic tokens
- **Diagnostics** — parse errors reported inline as you type
- **Completion** — triggered on `"`, `/`, and `.`
- **Go to definition**
- **Find references**
- **Document symbols** — outline / breadcrumbs for tree nodes
- **Formatting** (`Format Document`)

Editor features (from the language configuration):

- **Comment toggling** — `//` line comments and `/* */` block comments
- **Bracket matching** and auto-closing for `{}`, `[]`, `()`, and strings

## Prerequisite: build the server

```bash
./gradlew installDist   # launcher lands in build/install/forester-lsp/bin
```

## Install (one command)

From this folder:

```bash
npm install && npm run package && code --install-extension forester-lsp-client-0.0.1.vsix
```

This builds the `.vsix` and installs it into your regular VS Code. If the
`code` command isn't on your `PATH`, install the `.vsix` from the Extensions
view instead: click the **...** menu → **Install from VSIX…** and pick the
`forester-lsp-client-0.0.1.vsix` file.

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
