# Editor integrations

The server speaks standard LSP over stdio, so any LSP-capable editor works.
Build the launcher first:

```bash
./gradlew installDist   # -> build/install/forester-lsp/bin/forester-lsp
```

| Editor | Folder | Install |
| --- | --- | --- |
| VS Code | [`vscode/`](vscode/README.md) | one command (`.vsix`), or `F5` for development |
| Neovim | [`neovim/`](neovim/README.md) | copy one lua file into `~/.config/nvim/plugin/` |
| Helix | [`helix/`](helix/README.md) | `./install.sh` (appends to `languages.toml`) |
| Emacs | [`emacs/`](emacs/README.md) | copy one elisp file, one `load` line (eglot) |
| Sublime Text | [`sublime/`](sublime/README.md) | `./install.sh` (syntax + LSP client settings) |

## Windows

Everything works on Windows with two adjustments:

- The server launcher is `forester-lsp.bat` (produced by `gradlew.bat installDist`
  alongside the Unix script). Wherever a config references `forester-lsp`,
  Windows users should use the full path to the `.bat` — Windows does not
  resolve extension-less commands to batch files.
- The `install.sh` scripts (Helix, Sublime) are bash-only. On Windows, copy
  the config files manually; each editor README states the target path.

The VS Code extension handles Windows automatically (it picks the `.bat` and
spawns it through a shell). `.vsix` packaging and `F5` development work the
same on all platforms.

## JetBrains IDEs

An independent Forester plugin already exists for JetBrains IDEs, so no
integration is maintained here. Alternatively, this server can be used
through the [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij)
plugin as a user-defined language server pointing at the launcher.

## Zed

Requires a small Rust/WASM extension; not provided yet.
