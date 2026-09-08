package de.ottoextra.letter.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterEditorTextOperationsTest {

    @Test void importedPageOverflowFollowsCursorAndCanScrollBack() {
        assertEquals(3, LetterEditorTextOperations.firstVisibleLine(0, 16, 17, 14, true));
        assertEquals(0, LetterEditorTextOperations.firstVisibleLine(3, 0, 17, 14, true));
        assertEquals(0, LetterEditorTextOperations.firstVisibleLine(0, 16, 17, 14, false));
        assertEquals(0, LetterEditorTextOperations.firstVisibleLine(99, 0, 3, 14, false));
    }

    @Test void cursorAndDeleteKeepSupplementaryCharactersWhole() {
        String text = "A😀B";
        assertEquals(1, LetterEditorTextOperations.cursorBoundary(text, 2, -1));
        assertEquals(3, LetterEditorTextOperations.cursorBoundary(text, 2, 1));
        int cursor = LetterEditorTextOperations.cursorBoundary(text, 2, -1);
        assertEquals("AB", text.substring(0, cursor) + text.substring(
                LetterEditorTextOperations.nextCharacterEnd(text, cursor)));
    }
    @Test void returningFromEmptyPageKeepsCursorAfterImportedPrefix() {
        String text = "Importiert\nZusatz";
        int locked = "Importiert\n".length();
        assertEquals(locked, LetterEditorTextOperations.editableCursor(text, 0, locked, 1));
        assertTrue(LetterEditorTextOperations.preservesLockedPrefix(text, "Importiert\nNeu", locked));
        assertTrue(LetterEditorTextOperations.preservesLockedPrefix(text, "Importiert\n", locked));
        assertFalse(LetterEditorTextOperations.preservesLockedPrefix(text, "XImportiert\nZusatz", locked));
        assertFalse(LetterEditorTextOperations.preservesLockedPrefix(text, "mportiert\nZusatz", locked));
        assertFalse(LetterEditorTextOperations.preservesLockedPrefix(text, "Importiert", locked));
    }

    @Test
    void macEditorShortcutsAcceptCommandOrControl() {
        assertTrue(LetterEditorTextOperations.editorShortcut(true, false, true));
        assertTrue(LetterEditorTextOperations.editorShortcut(true, true, false));
        assertFalse(LetterEditorTextOperations.editorShortcut(true, false, false));
        assertFalse(LetterEditorTextOperations.editorShortcut(false, false, true));
    }

    @Test
    void macWordShortcutsAcceptCommandControlAndOption() {
        assertTrue(LetterEditorTextOperations.wordShortcut(true, false, true, false));
        assertTrue(LetterEditorTextOperations.wordShortcut(true, true, false, false));
        assertTrue(LetterEditorTextOperations.wordShortcut(true, false, false, true));
        assertFalse(LetterEditorTextOperations.wordShortcut(true, false, false, false));
        assertFalse(LetterEditorTextOperations.wordShortcut(false, false, true, true));
    }

    @Test
    void ctrlBackspaceSelectsTheWholePreviousWord() {
        String text = "Erstes zweites drittes ";

        LetterEditorTextOperations.Range range =
                LetterEditorTextOperations.previousWord(text, text.length(), 0);

        assertEquals("drittes ", text.substring(range.start(), range.end()));
    }

    @Test
    void ctrlDeleteSelectsTheWholeFollowingWord() {
        String text = "Erstes zweites drittes";
        int cursor = "Erstes".length();

        LetterEditorTextOperations.Range range =
                LetterEditorTextOperations.nextWord(text, cursor);

        assertEquals(" zweites", text.substring(range.start(), range.end()));
    }

    @Test
    void wordDeletionNeverSplitsFormattingCodes() {
        String text = "§lErstes§r zweites";

        LetterEditorTextOperations.Range first =
                LetterEditorTextOperations.nextWord(text, 0);
        LetterEditorTextOperations.Range second =
                LetterEditorTextOperations.nextWord(text, first.end());

        assertEquals("Erstes", text.substring(first.start(), first.end()));
        assertEquals(" zweites", text.substring(second.start(), second.end()));
    }

    @Test
    void singleCharacterDeletionTreatsFormattingCodeAsOneUnit() {
        String text = "A§rB";

        assertEquals(1, LetterEditorTextOperations.previousCharacterStart(text, 3));
        assertEquals(3, LetterEditorTextOperations.nextCharacterEnd(text, 1));
    }

    @Test
    void cursorCannotStopInsideFormattingCode() {
        String text = "A§rB";

        assertEquals(1, LetterEditorTextOperations.cursorBoundary(text, 2, -1));
        assertEquals(3, LetterEditorTextOperations.cursorBoundary(text, 2, 1));
    }
}
