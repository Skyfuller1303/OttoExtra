package de.ottoextra.letter.calibration;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterCalibrationCorpusTest {

    @Test
    void corpusHasSixteenUniquelyMarkedCases() {
        var cases = LetterCalibrationCorpus.all();
        assertEquals(16, cases.size());
        assertEquals(16, new HashSet<>(cases.stream().map(
                LetterCalibrationCorpus.TestCase::id).toList()).size());
        for (var test : cases) {
            assertTrue(test.pages().stream().anyMatch(page -> page.contains("OE-" + test.id())),
                    "Fehlende Kennung in " + test.id());
        }
    }

    @Test
    void corpusCoversPagesWhitespaceEscapesAndFormatting() {
        assertTrue(LetterCalibrationCorpus.byId("L06").pages().getFirst().contains("\n\n"));
        assertTrue(LetterCalibrationCorpus.byId("L07").pages().getFirst().contains("\\n"));
        assertTrue(LetterCalibrationCorpus.byId("L10").pages().getFirst().contains("§k"));
        assertEquals(2, LetterCalibrationCorpus.byId("L15").pages().size());
        assertEquals(2, LetterCalibrationCorpus.byId("L16").pages().size());
    }

    @Test
    void v2CorpusHasEightUniquelyMarkedCases() {
        var cases = LetterCalibrationCorpus.v2();
        assertEquals(8, cases.size());
        assertEquals(8, new HashSet<>(cases.stream().map(
                LetterCalibrationCorpus.TestCase::id).toList()).size());
        for (var test : cases) {
            assertTrue(test.pages().stream().anyMatch(page -> page.contains("OE-" + test.id())),
                    "Fehlende Kennung in " + test.id());
        }
        assertTrue(LetterCalibrationCorpus.byId("V202").pages().getFirst().contains("\n\n"));
        assertTrue(LetterCalibrationCorpus.byId("V206").pages().getFirst().contains("\\n"));
    }

    @Test
    void refineCorpusHasSixBudgetAndWhitespaceCases() {
        var cases = LetterCalibrationCorpus.v2Refine();
        assertEquals(6, cases.size());
        assertEquals(6, new HashSet<>(cases.stream().map(
                LetterCalibrationCorpus.TestCase::id).toList()).size());
        assertTrue(LetterCalibrationCorpus.byId("R05").pages().getFirst().contains("\u2007"));
        assertEquals(2, LetterCalibrationCorpus.byId("R06").pages().size());
    }

    @Test
    void finalCorpusHasEightPixelBoundaryCases() {
        var cases = LetterCalibrationCorpus.v2Final();
        assertEquals(8, cases.size());
        assertEquals(8, new HashSet<>(cases.stream().map(
                LetterCalibrationCorpus.TestCase::id).toList()).size());
        for (var test : cases) {
            assertTrue(test.pages().stream().anyMatch(page -> page.contains("OE-" + test.id())),
                    "Fehlende Kennung in " + test.id());
        }
        assertTrue(LetterCalibrationCorpus.byId("F06").pages().getFirst().contains("\n\n"));
        assertEquals(2, LetterCalibrationCorpus.byId("F08").pages().size());
    }

    @Test
    void boundaryCorpusHasFifteenAdaptiveCases() {
        var cases = LetterCalibrationCorpus.v2Boundary();
        assertEquals(15, cases.size());
        assertEquals(15, new HashSet<>(cases.stream().map(
                LetterCalibrationCorpus.TestCase::id).toList()).size());
        for (var test : cases) {
            assertTrue(test.pages().stream().anyMatch(page -> page.contains("OE-" + test.id())),
                    "Fehlende Kennung in " + test.id());
        }
        assertEquals("B01", cases.getFirst().id());
        assertEquals("B15", cases.getLast().id());
    }
}
