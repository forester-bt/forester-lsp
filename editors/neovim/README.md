# Forester LSP — Neovim

Single-file plugin: [`forester.lua`](forester.lua). It registers the `forester`
filetype for `.tree` files and attaches the LSP client (semantic-token
highlighting, completion). Requires Neovim 0.8+.

## Install (one command)

Files in `plugin/` are auto-loaded — no plugin manager needed:

```bash
mkdir -p ~/.config/nvim/plugin && cp forester.lua ~/.config/nvim/plugin/
```

Or straight from GitHub:

```bash
curl -fsSL --create-dirs -o ~/.config/nvim/plugin/forester.lua \
  https://raw.githubusercontent.com/besok/forester-lsp/master/editors/neovim/forester.lua
```

## Server binary

The plugin runs `forester-lsp` from your `PATH`. Build and expose it:

```bash
./gradlew installDist
export PATH="$PATH:/path/to/forester-lsp/build/install/forester-lsp/bin"
```

Or point to it directly in your `init.lua` (before the plugin loads):

```lua
vim.g.forester_lsp_cmd = "/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp"
```

## Verify

Open any `.tree` file (e.g. `examples/sample.tree`) and run `:LspInfo` —
`forester-lsp` should be attached, and keywords/strings/comments colored.
Semantic-token highlight groups are `@lsp.type.keyword`, `@lsp.type.string`,
etc., supported by any modern colorscheme.

## Windows

The plugin directory is `~/AppData/Local/nvim/plugin/` instead of
`~/.config/nvim/plugin/`. Point the command at the batch launcher explicitly:

```lua
vim.g.forester_lsp_cmd = "C:/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp.bat"
```

If Neovim fails to spawn the `.bat` directly, wrap it:

```lua
vim.g.forester_lsp_cmd = { "cmd.exe", "/C", "C:/path/to/.../forester-lsp.bat" }
```

## Plugin manager (optional)

With lazy.nvim, instead of copying the file:

```lua
{
  "besok/forester-lsp",
  config = function(plugin)
    vim.cmd.source(plugin.dir .. "/editors/neovim/forester.lua")
  end,
}
```
