# Forester LSP — Helix

Helix needs no plugin — only a [`languages.toml`](languages.toml) entry.

## Install (one command)

```bash
./install.sh
```

The script appends the entry to `~/.config/helix/languages.toml` (skipping if
already present). Alternatively, paste the contents of
[`languages.toml`](languages.toml) there yourself.

## Server binary

`forester-lsp` must be on `PATH`:

```bash
./gradlew installDist
export PATH="$PATH:/path/to/forester-lsp/build/install/forester-lsp/bin"
```

## Windows

`install.sh` is bash-only. Paste the contents of
[`languages.toml`](languages.toml) into `%AppData%\helix\languages.toml`
yourself, and set the command to the batch launcher:

```toml
[language-server.forester-lsp]
command = "C:/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp.bat"
```

## Verify

```bash
hx --health forester   # server should show a green check
hx examples/sample.tree
```

Keywords, strings, numbers, and comments are colored via LSP semantic tokens.
