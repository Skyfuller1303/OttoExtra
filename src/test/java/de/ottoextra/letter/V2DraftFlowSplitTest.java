package de.ottoextra.letter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V2DraftFlowSplitTest {

    @Test
    void splitPageAtCursorStartsNewWorkingPageWithRest() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("Eins zwei drei"));
        draft.meta.flowingDocument = "Eins zwei drei";
        draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;

        var position = V2DraftFlow.splitPageAt(draft, 0, 9, "letter", value -> value.length() * 6);

        assertEquals(List.of("Eins zwei", "drei"), draft.pages);
        assertEquals(new V2DraftFlow.EditorPosition(1, 0, -1), position);
    }

    @Test
    void splitPageAtEndAddsEmptyPage() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("Hallo"));
        draft.meta.flowingDocument = "Hallo";
        draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;

        var position = V2DraftFlow.splitPageAt(draft, 0, 5, "letter", value -> value.length() * 6);

        assertEquals(List.of("Hallo", ""), draft.pages);
        assertEquals(1, position.page());
    }
}
