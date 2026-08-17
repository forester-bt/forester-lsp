package com.github.besok.foresterlsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

class CompletionServiceTest {

    private static List<String> labels(ForesterDocument doc, int line, int character) {
        return labels(doc, new Workspace(), "file:///tmp/dummy.tree", line, character);
    }

    private static List<String> labels(ForesterDocument doc, Workspace workspace, String uri, int line, int character) {
        return CompletionService.complete(doc, workspace, uri, line, character)
                .getItems().stream().map(CompletionItem::getLabel).toList();
    }

    @Test
    void suggestsTreeTypesAtDocumentStart() {
        var doc = ForesterDocument.parse("");
        var labels = labels(doc, 0, 0);
        assertTrue(labels.contains("root"));
        assertTrue(labels.contains("sequence"));
        assertTrue(labels.contains("r_fallback"));
    }

    @Test
    void suggestsTypesAfterParamColon() {
        var doc = ForesterDocument.parse("foo bar(x: ");
        var labels = labels(doc, 0, 11);
        assertTrue(labels.contains("num"));
        assertTrue(labels.contains("bool"));
        assertFalse(labels.contains("root"));
    }

    @Test
    void suggestsTypesWhileTypingParamType() {
        var doc = ForesterDocument.parse("foo bar(x: str");
        var labels = labels(doc, 0, 14);
        assertTrue(labels.contains("string"));
        assertTrue(labels.contains("bool"));
    }

    @Test
    void suggestsTreeTypesInsideCalls() {
        var doc = ForesterDocument.parse("root main sequence { ");
        var labels = labels(doc, 0, 21);
        assertTrue(labels.contains("parallel"));
        assertTrue(labels.contains("sequence"));
    }

    @Test
    void suggestsParamNamesAfterCallBracket() {
        var doc = ForesterDocument.parse("impl wait(duration: num)\nroot main sequence { wait(");
        var labels = labels(doc, 1, 26);
        assertTrue(labels.contains("duration"));
        assertFalse(labels.contains("wait"));
    }

    @Test
    void suggestsNothingForDefinitionParams() {
        var doc = ForesterDocument.parse("impl wait(duration: num)\nfoo bar(");
        var labels = labels(doc, 1, 8);
        assertTrue(labels.isEmpty());
    }

    @Test
    void suggestsNothingWhileTypingNameAfterStaticTreeType() {
        var doc = ForesterDocument.parse("impl wait(duration: num)\nroot ma");
        var labels = labels(doc, 1, 7);
        assertTrue(labels.isEmpty());
    }

    @Test
    void suggestsNothingWhileTypingNameAfterCustomTreeType() {
        var doc = ForesterDocument.parse("impl wa");
        var labels = labels(doc, 0, 7);
        assertTrue(labels.isEmpty());
    }

    @Test
    void suggestsTreeTypesWhileTyping() {
        var doc = ForesterDocument.parse("seq");
        var labels = labels(doc, 0, 3);
        assertTrue(labels.contains("sequence"));
        assertTrue(labels.contains("root"));
    }

    @Test
    void suggestsImportedDefinitionsWhileTypingInCalls() {
        var doc = ForesterDocument.parse("import \"std::actions\"\nroot main sequence { su");
        var labels = labels(doc, 1, 23);
        assertTrue(labels.contains("success"));
        assertTrue(labels.contains("main"));
    }
}
