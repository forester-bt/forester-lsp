# Forester LSP — Sublime Text

Two small files, installed by one script:

- [`Forester.sublime-syntax`](Forester.sublime-syntax) — registers `.tree`
  files under the `source.forester` scope and provides basic fallback
  highlighting on its own.
- [`LSP.sublime-settings`](LSP.sublime-settings) — tells the
  [LSP package](https://packagecontrol.io/packages/LSP) to run `forester-lsp`
  for that scope (semantic highlighting, completion).

## Install (one command)

Prerequisite: install the **LSP** package via Package Control.

```bash
./install.sh
```

The script copies both files into Sublime's `Packages/User` directory
(macOS and Linux). If you already have an `LSP.sublime-settings`, it prints
the client block for you to merge instead of overwriting.

## Server binary

`forester-lsp` must be on `PATH`:

```bash
./gradlew installDist
export PATH="$PATH:/path/to/forester-lsp/build/install/forester-lsp/bin"
```

(GUI Sublime on macOS may not see your shell `PATH`; if so, put an absolute
path into the `command` of `LSP.sublime-settings`.)

## Windows

`install.sh` is bash-only. Copy both files manually into the folder that
"Preferences → Browse Packages…" opens (`%AppData%\Sublime Text\Packages\User`),
and set the command to the batch launcher:

```json
"command": ["C:/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp.bat"]
```

## Verify

Open a `.tree` file — the syntax in the bottom-right should read "Forester".
Run "LSP: Troubleshoot Server" from the command palette to check the server
started.
