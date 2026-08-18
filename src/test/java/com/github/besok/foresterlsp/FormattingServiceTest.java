package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FormattingServiceTest {

    @Test
    void indentsNestedBlocksAndNormalizesSpacing() {
        String input = "import \"std::actions\"import \"utils.tree\"\n"
                + "root a sequence {check_battery(threshold=20.5) r_fallback{move_to(target={\"x\":1,\"y\":2}) recover(attempts=3)}}";
        String expected = """
                import "std::actions"
                import "utils.tree"

                root a sequence {
                    check_battery(threshold = 20.5)
                    r_fallback {
                        move_to(target = {"x": 1, "y": 2})
                        recover(attempts = 3)
                    }
                }
                """;
        assertEquals(expected, FormattingService.format(input));
    }

    @Test
    void keepsInlineObjectLiteral() {
        String input = "root a sequence { test_server(creds = {\"name\":\"name\"}) }";
        String expected = """
                root a sequence {
                    test_server(creds = {"name": "name"})
                }
                """;
        assertEquals(expected, FormattingService.format(input));
    }

    @Test
    void formatsParamsAndSemicolons() {
        String input = "impl wait(duration:num);";
        assertEquals("impl wait(duration: num);\n", FormattingService.format(input));
    }
}
