const fs = require('fs');
const path = require('path');
const { window, workspace } = require('vscode');
const { LanguageClient } = require('vscode-languageclient/node');

let client;

function resolveServerCommand(context) {
    const configured = workspace.getConfiguration('forester').get('server.path');
    if (configured) {
        return configured;
    }
    // Dev default: the extension lives in editors/vscode inside the repo, so the
    // launcher produced by `./gradlew installDist` is two levels up.
    const script = process.platform === 'win32' ? 'forester-lsp.bat' : 'forester-lsp';
    const devPath = context.asAbsolutePath(
        path.join('..', '..', 'build', 'install', 'forester-lsp', 'bin', script));
    if (fs.existsSync(devPath)) {
        return devPath;
    }
    // Installed from a .vsix: fall back to a `forester-lsp` on PATH.
    return script;
}

function activate(context) {
    const command = resolveServerCommand(context);

    const isPath = command.includes('/') || command.includes('\\');
    if (!command.endsWith('.jar') && isPath && !fs.existsSync(command)) {
        window.showErrorMessage(
            `Forester LSP launcher not found at ${command}. ` +
            'Run "./gradlew installDist" in the repository, put forester-lsp on PATH, ' +
            'or set "forester.server.path".');
        return;
    }

    // Node refuses to spawn .bat/.cmd files without a shell (CVE-2024-27980).
    const isBatchScript = /\.(bat|cmd)$/i.test(command);
    const serverOptions = command.endsWith('.jar')
        ? { command: 'java', args: ['-jar', command] }
        : {
            command: isBatchScript && command.includes(' ') ? `"${command}"` : command,
            options: { shell: isBatchScript },
        };

    client = new LanguageClient(
        'forester',
        'Forester LSP',
        serverOptions,
        { documentSelector: [{ scheme: 'file', language: 'forester' }] });

    client.start();
}

function deactivate() {
    return client ? client.stop() : undefined;
}

module.exports = { activate, deactivate };
