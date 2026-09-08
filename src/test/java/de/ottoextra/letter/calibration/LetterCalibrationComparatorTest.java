package de.ottoextra.letter.calibration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterCalibrationComparatorTest {

    @Test
    void distinguishesPageTextAndStyleChanges() {
        assertEquals(List.of(), LetterCalibrationComparator.compare(
                List.of("§cHallo"), List.of("§cHallo")));
        assertEquals(List.of("PAGE_1_STYLE"), LetterCalibrationComparator.compare(
                List.of("§cHallo"), List.of("§aHallo")));
        assertEquals(List.of("PAGE_1_TEXT"), LetterCalibrationComparator.compare(
                List.of("Hallo"), List.of("Hallo!")));
        assertTrue(LetterCalibrationComparator.compare(
                List.of("A"), List.of("A", "B")).getFirst().startsWith("PAGE_COUNT"));
    }

    @Test
    void ignoresOnlyServerPaddingAtEndOfPage() {
        assertEquals(List.of(), LetterCalibrationComparator.compare(
                List.of("§cHallo"), List.of("§cHallo ")));
        assertEquals(List.of(), LetterCalibrationComparator.compare(
                List.of("Oben\n\nUnten"), List.of("Oben \n\u2007\nUnten ")));
        assertEquals(List.of("PAGE_1_TEXT"), LetterCalibrationComparator.compare(
                List.of("Hallo  Welt"), List.of("Hallo Welt")));
    }

    @Test
    void ignoresRedundantCodesWithIdenticalVisualStyle() {
        assertEquals(List.of(), LetterCalibrationComparator.compare(
                List.of("§6§lOben\n\nUnten§r"),
                List.of("§6§lOben \n\u2007 \n§6§lUnten§r ")));
    }
}
