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

