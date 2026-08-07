-- Forester behavior-tree language support for Neovim (0.8+).
--
-- Single-file install: copy this file into your config's plugin/ directory
-- (it is auto-loaded on startup, no plugin manager needed):
--
--   cp forester.lua ~/.config/nvim/plugin/forester.lua
--
-- The server launcher must be on PATH (build it with `./gradlew installDist`,
-- it lands in build/install/forester-lsp/bin), or set an explicit command:
--
--   vim.g.forester_lsp_cmd = "/path/to/forester-lsp/build/install/forester-lsp/bin/forester-lsp"

vim.filetype.add({ extension = { tree = "forester" } })

vim.api.nvim_create_autocmd("FileType", {
  pattern = "forester",
  group = vim.api.nvim_create_augroup("forester_lsp", { clear = true }),
  callback = function(args)
    vim.bo[args.buf].commentstring = "// %s"

    local cmd = vim.g.forester_lsp_cmd or "forester-lsp"
    if type(cmd) == "string" then
      cmd = { cmd }
    end
    if vim.fn.executable(cmd[1]) ~= 1 then
      vim.notify(
        ("forester-lsp: launcher '%s' not found. Build it with `./gradlew installDist`, "
          .. "add build/install/forester-lsp/bin to PATH, or set vim.g.forester_lsp_cmd."):format(cmd[1]),
        vim.log.levels.WARN)
      return
    end

    local root = vim.fs.find({ ".git" }, { upward = true, path = args.file })[1]
    vim.lsp.start({
      name = "forester-lsp",
      cmd = cmd,
      root_dir = root and vim.fs.dirname(root) or vim.fs.dirname(args.file),
    })
  end,
})
