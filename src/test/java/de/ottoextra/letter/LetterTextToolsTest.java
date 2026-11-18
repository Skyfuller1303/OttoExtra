package de.ottoextra.letter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterTextToolsTest {

    @Test
    void plainTextStripsFormattingCodesOnly() {
        assertEquals("Hallo Welt & §x", LetterTextTools.plainText("§lHallo §r§cWelt & §x"));
        assertEquals("", LetterTextTools.plainText(null));
        assertEquals("a§", LetterTextTools.plainText("a§"));
    }

    @Test
    void splitPageDividesAtCursorAndClampsOutOfRangeValues() {
        assertEquals(List.of("Hal", "lo", "Zwei"),
                LetterTextTools.splitPage(List.of("Hallo", "Zwei"), 0, 3));
        assertEquals(List.of("Hallo", "", "Zwei"),
                LetterTextTools.splitPage(List.of("Hallo", "Zwei"), 0, 99));
        assertEquals(List.of("", "Hallo"), LetterTextTools.splitPage(List.of("Hallo"), 0, -4));
        assertArrayEquals(new String[]{"", ""}, LetterTextTools.splitAt(null, 2));
    }

    @Test
    void bookBreaksMapPredictedPageStartsToEditorOffsets() {
        List<String> editor = List.of("Eins zwei\ndrei vier", "fünf");
        List<String> book = List.of("Eins zwei", "drei vier\nfünf");
        List<List<int[]>> breaks = LetterTextTools.bookBreaks(editor, book);
        assertEquals(2, breaks.size());
        assertEquals(1, breaks.get(0).size());
        assertEquals(10, breaks.get(0).get(0)[0]);
        assertEquals(2, breaks.get(0).get(0)[1]);
        assertTrue(breaks.get(1).isEmpty());
    }

    @Test
    void bookBreaksIgnoreFormattingCodesAndWhitespace() {
        List<String> editor = List.of("§cA B  C");
        List<String> book = List.of("§cA", "§r§cB C");
        List<List<int[]>> breaks = LetterTextTools.bookBreaks(editor, book);
        assertEquals(1, breaks.get(0).size());
        assertEquals(4, breaks.get(0).get(0)[0]);
    }

    @Test
    void bookBreaksAreEmptyWhenVisibleTextDiffers() {
        List<List<int[]>> breaks = LetterTextTools.bookBreaks(List.of("abc"), List.of("ab", "xyz"));
        assertTrue(breaks.get(0).isEmpty());
    }
}
