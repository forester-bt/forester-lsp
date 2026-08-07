package com.github.besok.foresterlsp;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import com.github.besok.foresterlsp.grammar.TreeLexer;

/**
 * Produces LSP semantic token data for a Forester `.tree` document by running
 * the ANTLR lexer over the full text and mapping lexer token types to the
 * semantic token legend declared by the server.
 */
public final class SemanticTokenizer {

    public static final List<String> TOKEN_TYPES = List.of(
            "keyword",
            "type",
            "string",
            "number",
            "comment",
            "operator",
            "variable");

    public static final List<String> TOKEN_MODIFIERS = List.of();

    private static final int KEYWORD = 0;
    private static final int TYPE = 1;
    private static final int STRING = 2;
    private static final int NUMBER = 3;
    private static final int COMMENT = 4;
    private static final int OPERATOR = 5;
    private static final int VARIABLE = 6;

    private SemanticTokenizer() {
    }

    /**
     * Returns the token data in the relative encoding required by the LSP:
     * five integers per token (deltaLine, deltaStartChar, length, tokenType,
     * tokenModifiers). Multi-line tokens (block comments) are split into one
     * entry per line.
     */
    public static List<Integer> tokenize(String text) {
        var lexer = new TreeLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();

        var data = new ArrayList<Integer>();
        int prevLine = 0;
        int prevChar = 0;

        for (Token token : lexer.getAllTokens()) {
            int semanticType = semanticType(token.getType());
            if (semanticType < 0) {
                continue;
            }

            int line = token.getLine() - 1;
            int startChar = token.getCharPositionInLine();
            String[] lines = token.getText().split("\n", -1);

            for (int i = 0; i < lines.length; i++) {
                String segment = lines[i];
                int segLine = line + i;
                int segChar = i == 0 ? startChar : 0;
                int segLength = segment.endsWith("\r") ? segment.length() - 1 : segment.length();
                if (segLength == 0) {
                    continue;
                }

                data.add(segLine - prevLine);
                data.add(segLine == prevLine ? segChar - prevChar : segChar);
                data.add(segLength);
                data.add(semanticType);
                data.add(0);

                prevLine = segLine;
                prevChar = segChar;
            }
        }
        return data;
    }

    private static int semanticType(int antlrType) {
        return switch (antlrType) {
            case TreeLexer.ROOT, TreeLexer.PARALLEL,
                    TreeLexer.SEQUENCE, TreeLexer.MSEQUENCE, TreeLexer.RSEQUENCE,
                    TreeLexer.FALLBACK, TreeLexer.RFALLBACK,
                    TreeLexer.IMPORT,
                    TreeLexer.TRUE, TreeLexer.FALSE -> KEYWORD;
            case TreeLexer.ARRAY_T, TreeLexer.NUM_T, TreeLexer.OBJECT_T,
                    TreeLexer.STRING_T, TreeLexer.BOOL_T, TreeLexer.TREE_T,
                    TreeLexer.ANY_T -> TYPE;
            case TreeLexer.STRING -> STRING;
            case TreeLexer.NUMBER -> NUMBER;
            case TreeLexer.BlockComment, TreeLexer.LineComment -> COMMENT;
            case TreeLexer.EQ, TreeLexer.EQ_A, TreeLexer.DOT_DOT -> OPERATOR;
            case TreeLexer.ID -> VARIABLE;
            default -> -1;
        };
    }
}
