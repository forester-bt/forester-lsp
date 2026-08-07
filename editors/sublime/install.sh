#!/usr/bin/env bash
# Installs the Forester syntax definition and LSP client settings into
# Sublime Text's Packages/User directory.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"

case "$(uname -s)" in
    Darwin) USER_DIR="$HOME/Library/Application Support/Sublime Text/Packages/User" ;;
    Linux)  USER_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/sublime-text/Packages/User" ;;
    *)      echo "Unsupported OS: $(uname -s). Copy the files to Sublime's Packages/User manually." >&2
            exit 1 ;;
esac

mkdir -p "$USER_DIR"
cp "$HERE/Forester.sublime-syntax" "$USER_DIR/"
echo "Installed Forester.sublime-syntax to $USER_DIR"

if [ -f "$USER_DIR/LSP.sublime-settings" ]; then
    if grep -qs 'forester-lsp' "$USER_DIR/LSP.sublime-settings"; then
        echo "forester-lsp already present in LSP.sublime-settings — nothing to do."
    else
        echo "You already have an LSP.sublime-settings — merge this client into its \"clients\" section:"
        sed -n '3,8p' "$HERE/LSP.sublime-settings"
    fi
else
    cp "$HERE/LSP.sublime-settings" "$USER_DIR/"
    echo "Installed LSP.sublime-settings to $USER_DIR"
fi

if ! command -v forester-lsp >/dev/null; then
    echo "NOTE: 'forester-lsp' is not on PATH. Build it with './gradlew installDist'"
    echo "and add <repo>/build/install/forester-lsp/bin to PATH."
fi
echo "Make sure the 'LSP' package is installed (Package Control: Install Package -> LSP)."
