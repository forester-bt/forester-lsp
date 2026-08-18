package com.github.besok.foresterlsp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import com.github.besok.foresterlsp.grammar.TreeLexer;

/**
 * Re-indents and normalizes spacing in a `.tree` document by re-emitting its
 * token stream with canonical whitespace.
 */
public final class FormattingService {

    private enum Brace {
        BLOCK, OBJECT, IMPORT
    }

    private static final String INDENT = "    ";

    private FormattingService() {
    }

    public static String format(String text) {
        var lexer = new TreeLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        var tokens = new CommonTokenStream(lexer);
        tokens.fill();

        List<Token> significant = new ArrayList<>();
        for (Token token : tokens.getTokens()) {
            if (token.getType() == Token.EOF) {
                break;
            }
            if (token.getChannel() == Token.HIDDEN_CHANNEL) {
                continue;
            }
            significant.add(token);
        }

        StringBuilder sb = new StringBuilder();
        Deque<Brace> braces = new ArrayDeque<>();
        int indent = 0;
        int parenDepth = 0;
        int blockDepth = 0;
        boolean atLineStart = true;
        Token prev = null;
        Token prevPrev = null;

        for (int i = 0; i < significant.size(); i++) {
            Token token = significant.get(i);
            Token next = i + 1 < significant.size() ? significant.get(i + 1) : null;
            int type = token.getType();
            String value = token.getText();

            if (isComment(type)) {
                indent(sb, atLineStart, indent);
                sb.append(value);
                sb.append('\n');
                atLineStart = true;
                prevPrev = prev;
                prev = token;
                continue;
            }

            if (type == TreeLexer.RBC) {
                Brace brace = braces.isEmpty() ? Brace.BLOCK : braces.pop();
                if (brace == Brace.BLOCK) {
                    blockDepth--;
                    indent = Math.max(0, indent - 1);
                    indent(sb, atLineStart, indent);
                }
                sb.append('}');
                atLineStart = false;
                if (brace != Brace.OBJECT) {
                    sb.append('\n');
                    if (brace == Brace.IMPORT && next != null && next.getType() != TreeLexer.IMPORT) {
                        sb.append('\n');
                    }
                    atLineStart = true;
                }
                prevPrev = prev;
                prev = token;
                continue;
            }

            if (atLineStart) {
                sb.append(INDENT.repeat(indent));
            } else if (spaceBefore(prev, type)) {
                sb.append(' ');
            }

            sb.append(value);
            atLineStart = false;

            if (type == TreeLexer.LBC) {
                Brace brace = classifyBrace(prev, prevPrev);
                braces.push(brace);
                if (brace == Brace.BLOCK) {
                    blockDepth++;
                    sb.append('\n');
                    indent++;
                    atLineStart = true;
                }
            } else if (type == TreeLexer.SEMI) {
                sb.append('\n');
                atLineStart = true;
            } else if (type == TreeLexer.STRING && prev != null && prev.getType() == TreeLexer.IMPORT) {
                if (next == null || next.getType() != TreeLexer.LBC) {
                    sb.append('\n');
                    if (next != null && next.getType() != TreeLexer.IMPORT) {
                        sb.append('\n');
                    }
                    atLineStart = true;
                }
            } else if (type == TreeLexer.LPR) {
                parenDepth++;
            } else if (type == TreeLexer.RPR) {
                parenDepth--;
                if (parenDepth == 0 && blockDepth > 0 && next != null
                        && (next.getType() == TreeLexer.ID || isStaticType(next.getType()))) {
                    sb.append('\n');
                    atLineStart = true;
                }
            }

            prevPrev = prev;
            prev = token;
        }

        return sb.toString().stripTrailing() + "\n";
    }

    private static Brace classifyBrace(Token prev, Token prevPrev) {
        if (prev != null && prev.getType() == TreeLexer.STRING
                && prevPrev != null && prevPrev.getType() == TreeLexer.IMPORT) {
            return Brace.IMPORT;
        }
        if (isBlockBrace(prev)) {
            return Brace.BLOCK;
        }
        return Brace.OBJECT;
    }

    private static boolean isBlockBrace(Token prev) {
        if (prev == null) {
            return false;
        }
        int type = prev.getType();
        return type == TreeLexer.ID || type == TreeLexer.RPR || isStaticType(type);
    }

    private static boolean isStaticType(int type) {
        return switch (type) {
            case TreeLexer.ROOT, TreeLexer.PARALLEL, TreeLexer.SEQUENCE, TreeLexer.MSEQUENCE,
                    TreeLexer.RSEQUENCE, TreeLexer.FALLBACK, TreeLexer.RFALLBACK -> true;
            default -> false;
        };
    }

    private static boolean isComment(int type) {
        return type == TreeLexer.BlockComment || type == TreeLexer.LineComment;
    }

    private static boolean spaceBefore(Token prev, int currType) {
        if (prev == null) {
            return false;
        }
        if (currType == TreeLexer.RPR || currType == TreeLexer.RBR || currType == TreeLexer.COMMA
                || currType == TreeLexer.SEMI || currType == TreeLexer.COLON || currType == TreeLexer.DOT_DOT
                || currType == TreeLexer.LPR || currType == TreeLexer.LBR) {
            return false;
        }
        int prevType = prev.getType();
        return prevType != TreeLexer.LPR && prevType != TreeLexer.LBR
                && prevType != TreeLexer.LBC && prevType != TreeLexer.DOT_DOT;
    }

    private static void indent(StringBuilder sb, boolean atLineStart, int level) {
        if (!atLineStart) {
            sb.append('\n');
        }
        sb.append(INDENT.repeat(level));
    }
}
