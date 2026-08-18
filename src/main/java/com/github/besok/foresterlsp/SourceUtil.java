package com.github.besok.foresterlsp;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Small shared helpers for mapping LSP (line, character) positions to ANTLR
 * tokens and offsets.
 */
final class SourceUtil {

    private SourceUtil() {
    }

    static int offsetOf(String text, int line, int character) {
        int offset = 0;
        int currentLine = 0;
        while (offset < text.length() && currentLine < line) {
            if (text.charAt(offset) == '\n') {
                currentLine++;
            }
            offset++;
        }
        return offset + character;
    }

    static Token previousToken(CommonTokenStream tokens, int offset) {
        Token prev = null;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.EOF) {
                break;
            }
            if (token.getStartIndex() >= offset) {
                break;
            }
            if (token.getChannel() == Token.DEFAULT_CHANNEL) {
                prev = token;
            }
        }
        return prev;
    }

    static Token tokenAt(CommonTokenStream tokens, int offset) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.EOF) {
                break;
            }
            if (token.getStartIndex() <= offset && offset <= token.getStopIndex()) {
                return token;
            }
        }
        return null;
    }

    static String unquote(String literal) {
        if (literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"")) {
            return literal.substring(1, literal.length() - 1);
        }
        return literal;
    }

    static Range rangeOf(Token token) {
        int line = token.getLine() - 1;
        int startChar = token.getCharPositionInLine();
        String[] segments = token.getText().split("\n", -1);
        int endLine = line + segments.length - 1;
        int endChar = segments.length == 1
                ? startChar + Math.max(1, segments[0].length())
                : Math.max(1, segments[segments.length - 1].length());
        return new Range(new Position(line, startChar), new Position(endLine, endChar));
    }
}
