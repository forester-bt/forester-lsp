package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SemanticTokenizerTest {

    /** Absolute (decoded) form of one semantic token, for readable assertions. */
    record Tok(int line, int startChar, int length, String type) {
    }

    private static List<Tok> decode(List<Integer> data) {
        var result = new ArrayList<Tok>();
        int line = 0;
        int startChar = 0;
        for (int i = 0; i < data.size(); i += 5) {
            int deltaLine = data.get(i);
            line += deltaLine;
            startChar = deltaLine == 0 ? startChar + data.get(i + 1) : data.get(i + 1);
            result.add(new Tok(line, startChar, data.get(i + 2),
                    SemanticTokenizer.TOKEN_TYPES.get(data.get(i + 3))));
        }
        return result;
    }

    @Test
    void highlightsKeywordsIdentifiersAndStrings() {
        var tokens = decode(SemanticTokenizer.tokenize("""
                import "std::actions"
                root main sequence {
                    say(msg = "hello")
                }
                """));

        assertEquals(new Tok(0, 0, 6, "keyword"), tokens.get(0));   // import
        assertEquals(new Tok(0, 7, 14, "string"), tokens.get(1));   // "std::actions"
        assertEquals(new Tok(1, 0, 4, "keyword"), tokens.get(2));   // root
        assertEquals(new Tok(1, 5, 4, "variable"), tokens.get(3));  // main
        assertEquals(new Tok(1, 10, 8, "keyword"), tokens.get(4));  // sequence
        assertEquals(new Tok(2, 4, 3, "variable"), tokens.get(5));  // say
        assertEquals(new Tok(2, 8, 3, "variable"), tokens.get(6));  // msg
        assertEquals(new Tok(2, 12, 1, "operator"), tokens.get(7)); // =
        assertEquals(new Tok(2, 14, 7, "string"), tokens.get(8));   // "hello"
        assertEquals(9, tokens.size());
    }

    @Test
    void highlightsTypesNumbersAndComments() {
        var tokens = decode(SemanticTokenizer.tokenize(
                "// note\nretry(attempts: num) delay(1.5) // tail\n"));

        assertEquals(new Tok(0, 0, 7, "comment"), tokens.get(0));
        assertEquals(new Tok(1, 0, 5, "variable"), tokens.get(1));  // retry
        assertEquals(new Tok(1, 6, 8, "variable"), tokens.get(2));  // attempts
        assertEquals(new Tok(1, 16, 3, "type"), tokens.get(3));     // num
        assertEquals(new Tok(1, 21, 5, "variable"), tokens.get(4)); // delay
        assertEquals(new Tok(1, 27, 3, "number"), tokens.get(5));   // 1.5
        assertEquals(new Tok(1, 32, 7, "comment"), tokens.get(6));
    }

    @Test
    void splitsMultiLineBlockCommentsPerLine() {
        var tokens = decode(SemanticTokenizer.tokenize("/* a\n b\n*/ root x fallback"));

        assertEquals(new Tok(0, 0, 4, "comment"), tokens.get(0));
        assertEquals(new Tok(1, 0, 2, "comment"), tokens.get(1));
        assertEquals(new Tok(2, 0, 2, "comment"), tokens.get(2));
        assertEquals(new Tok(2, 3, 4, "keyword"), tokens.get(3));   // root
        assertEquals(new Tok(2, 8, 1, "variable"), tokens.get(4));  // x
        assertEquals(new Tok(2, 10, 8, "keyword"), tokens.get(5));  // fallback
    }

    @Test
    void booleanLiteralsAreKeywordsNotIdentifiers() {
        var tokens = decode(SemanticTokenizer.tokenize("a(x = TRUE, y = FALSE, z = TRUEISH)"));

        assertEquals("keyword", tokens.get(3).type());  // TRUE
        assertEquals("keyword", tokens.get(6).type());  // FALSE
        assertEquals("variable", tokens.get(9).type()); // TRUEISH stays an id
    }

    @Test
    void emptyDocumentProducesNoTokens() {
        assertTrue(SemanticTokenizer.tokenize("").isEmpty());
    }

    @Test
    void survivesInvalidInput() {
        // ERRCHAR swallows anything the lexer doesn't know; no exception, no bogus tokens.
        var tokens = decode(SemanticTokenizer.tokenize("§§§ root §"));
        assertEquals(List.of(new Tok(0, 4, 4, "keyword")), tokens);
    }
}
