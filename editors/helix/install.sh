#!/usr/bin/env bash
# Appends the Forester language configuration to Helix's languages.toml.
set -euo pipefail

CONF="${XDG_CONFIG_HOME:-$HOME/.config}/helix/languages.toml"
SNIPPET="$(cd "$(dirname "$0")" && pwd)/languages.toml"

if grep -qs 'forester-lsp' "$CONF"; then
    echo "Forester is already configured in $CONF — nothing to do."
    exit 0
fi

mkdir -p "$(dirname "$CONF")"
cat "$SNIPPET" >> "$CONF"
echo "Added Forester configuration to $CONF"

if ! command -v forester-lsp >/dev/null; then
    echo "NOTE: 'forester-lsp' is not on PATH. Build it with './gradlew installDist'"
    echo "and add <repo>/build/install/forester-lsp/bin to PATH."
fi
