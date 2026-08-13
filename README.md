# Forester LSP

Language Server Protocol server for the [Forester](https://github.com/besok/forester) behavior tree language.

## Requirements

- Java 21+ (the Gradle toolchain is pinned to 21; see
  [`build.gradle.kts`](build.gradle.kts))

## Build

```bash
./gradlew build
```

## Run

Stdio mode (for editor integration):

```bash
./gradlew run
```

Socket mode:

```bash
./gradlew run --args="--socket --port 5007"
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Install

Build the distributable launcher:

```bash
./gradlew installDist   # -> build/install/forester-lsp/bin/forester-lsp
```

The `forester-lsp` command must be on `PATH` for editor integration. Either add
the bin directory to `PATH`:

```bash
export PATH="$PATH:$(pwd)/build/install/forester-lsp/bin"
```

or symlink the launcher into a directory already on `PATH` (e.g.
`~/.local/bin`):

```bash
ln -sf "$(pwd)/build/install/forester-lsp/bin/forester-lsp" ~/.local/bin/forester-lsp
```

## Editor integrations

See [`editors/`](editors/README.md):

- **VS Code** — [`editors/vscode`](editors/vscode/README.md): one-command
  `.vsix` install, or `F5` from this repo for development.
- **Neovim** — [`editors/neovim`](editors/neovim/README.md): single-file
  plugin, copy one lua file into `~/.config/nvim/plugin/`.
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
