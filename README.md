# Forester LSP

Language Server Protocol server for the [Forester](https://github.com/besok/forester) behavior tree language.

## Requirements

- Java 17+

## Build

```bash
.\gradlew.bat build
```

## Run

Stdio mode (for editor integration):

```bash
.\gradlew.bat run
```

Socket mode:

```bash
.\gradlew.bat run --args="--socket --port 5007"
```

## Editor integrations

See [`editors/`](editors/README.md):

- **VS Code** — [`editors/vscode`](editors/vscode/README.md): one-command
  `.vsix` install, or `F5` from this repo for development.
- **Neovim** — [`editors/neovim`](editors/neovim/README.md): single-file
  plugin, copy one lua file into `~/.config/nvim/plugin/`.
- **Helix** — [`editors/helix`](editors/helix/README.md): `./install.sh`.
- **Emacs** — [`editors/emacs`](editors/emacs/README.md): single-file
  eglot integration.
- **Sublime Text** — [`editors/sublime`](editors/sublime/README.md):
  `./install.sh`.
- **JetBrains** — covered by an independent plugin (see
  [`editors/README.md`](editors/README.md)).

## Features

- Full-text document synchronization
- Syntax highlighting via LSP semantic tokens (`textDocument/semanticTokens/full`)
- Completion provider (stub)
- ANTLR4-based parsing for `.tree` files

## License

BSD 3-Clause
