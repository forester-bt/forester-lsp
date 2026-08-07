# Forester LSP — Emacs

Single-file integration: [`forester.el`](forester.el). It defines a minimal
`forester-mode` for `.tree` files (comments, strings via syntax table) and
attaches the LSP server through **eglot**, which is built into Emacs 29+.

## Install

Copy the file and load it — two lines total:

```bash
curl -fsSL --create-dirs -o ~/.emacs.d/lisp/forester.el \
  https://raw.githubusercontent.com/besok/forester-lsp/master/editors/emacs/forester.el
```

In your `init.el`:

```elisp
(load "~/.emacs.d/lisp/forester.el")
```

## Server binary

`forester-lsp` must be on `PATH`:

```bash
./gradlew installDist
export PATH="$PATH:/path/to/forester-lsp/build/install/forester-lsp/bin"
```

Or point Emacs at it directly (before loading the file):

```elisp
(setq forester-lsp-command
      '("/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp"))
```

Note for GUI Emacs on macOS: `PATH` from your shell profile may not be
inherited — use `forester-lsp-command` or the `exec-path-from-shell` package.

## Windows

Point the command at the batch launcher (Emacs runs `.bat` files fine through
its bundled cmdproxy):

```elisp
(setq forester-lsp-command
      '("C:/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp.bat"))
```

## Verify

Open a `.tree` file. The modeline shows `Forester` and eglot connects
automatically (`M-x eglot-events-buffer` shows the LSP traffic). Semantic
highlighting requires Emacs 29+ with eglot's semantic-token support enabled;
on older setups you still get comments/strings from the syntax table.

Emacs 28 users: install eglot from GNU ELPA first (`M-x package-install RET
eglot`).
