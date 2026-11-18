package de.ottoextra.ui.complete;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabCompletionModelTest {
    private static final List<String> NAMES = List.of("Mattis", "matthias_K", "Anna", "Annabell", "Johanna",
            "Haus von Brandt", "", "  ", "anna");

    @Test void prefixMatchesComeBeforeSubstringMatchesCaseInsensitive() {
        assertEquals(List.of("Anna", "Annabell", "Johanna"), TabCompletionModel.matches("ANN", NAMES));
        assertEquals(List.of("matthias_K", "Mattis"), TabCompletionModel.matches("mat", NAMES));
    }

    @Test void blankQueryOrBlankCandidatesMatchNothing() {
        assertEquals(List.of(), TabCompletionModel.matches("", NAMES));
        assertEquals(List.of(), TabCompletionModel.matches("   ", NAMES));
        assertEquals(List.of(), TabCompletionModel.matches(null, NAMES));
        assertEquals(List.of(), TabCompletionModel.matches("a", null));
    }

    @Test void caseInsensitiveDuplicatesAreDropped() {
        // "anna" duplicates "Anna"; the first spelling wins.
        assertEquals(1, TabCompletionModel.matches("anna", NAMES).stream().filter("Anna"::equalsIgnoreCase).count());
    }

    @Test void tabCompletesThenCyclesForwardAndBackward() {
        TabCompletionModel m = new TabCompletionModel();
        String t = m.cycle("ann", NAMES, false);
        assertEquals("Anna", t);
        t = m.cycle(t, NAMES, false);
        assertEquals("Annabell", t);
        t = m.cycle(t, NAMES, false);
        assertEquals("Johanna", t);
        t = m.cycle(t, NAMES, false);
        assertEquals("Anna", t, "wraps around");
        t = m.cycle(t, NAMES, true);
        assertEquals("Johanna", t, "Shift+Tab goes backwards and wraps");
    }

    @Test void shiftTabFirstPressStartsAtTheLastMatch() {
        TabCompletionModel m = new TabCompletionModel();
        assertEquals("Johanna", m.cycle("ann", NAMES, true));
    }

    @Test void noMatchReturnsNullSoTabKeepsMovingTheFocus() {
        TabCompletionModel m = new TabCompletionModel();
        assertNull(m.cycle("xyz", NAMES, false));
        assertNull(m.cycle("", NAMES, false));
        assertNull(m.cycle("Mattis", List.of("Mattis"), false), "already complete with a single match");
    }

    @Test void exactTextSkipsToTheNextMatch() {
        TabCompletionModel m = new TabCompletionModel();
        assertEquals("Annabell", m.cycle("Anna", NAMES, false));
    }

    @Test void typingResetsTheCycle() {
        TabCompletionModel m = new TabCompletionModel();
        String t = m.cycle("ann", NAMES, false);
        assertTrue(m.cycling(t));
        assertFalse(m.cycling(t + "b"));
        // The edited text starts a fresh completion.
        assertEquals("Annabell", m.cycle("Annab", NAMES, false));
    }

    @Test void escapeRestoresTheTypedText() {
        TabCompletionModel m = new TabCompletionModel();
        String t = m.cycle("joh", NAMES, false);
        assertEquals("Johanna", t);
        assertEquals("joh", m.cancel(t));
        assertFalse(m.cycling(t));
        assertNull(m.cancel(t), "nothing to cancel any more");
    }

    @Test void lastWordIsCompletedWhenTheWholeTextDoesNotMatch() {
        TabCompletionModel m = new TabCompletionModel();
        assertEquals("Brief an Mattis", m.cycle("Brief an Matti", NAMES, false));
        // A whole-field match wins over the last word.
        assertEquals("Haus von Brandt", new TabCompletionModel().cycle("haus v", NAMES, false));
    }

    @Test void truncatedApplyKeepsCycling() {
        TabCompletionModel m = new TabCompletionModel();
        m.cycle("ann", NAMES, false);
        m.applied("Ann");
        assertTrue(m.cycling("Ann"));
        assertEquals("Annabell", m.cycle("Ann", NAMES, false));
    }

    @Test void shownListsLiveMatchesWhileTypingAndTheSelectionWhileCycling() {
        TabCompletionModel m = new TabCompletionModel();
        TabCompletionModel.Shown live = m.shown("ann", NAMES);
        assertEquals(List.of("Anna", "Annabell", "Johanna"), live.items());
        assertEquals(-1, live.selected());
        String t = m.cycle("ann", NAMES, false);
        t = m.cycle(t, NAMES, false);
        TabCompletionModel.Shown cycling = m.shown(t, NAMES);
        assertEquals("Annabell", cycling.items().get(cycling.selected()));
        assertTrue(m.shown("", NAMES).isEmpty());
        assertTrue(m.shown("Johanna", List.of("Johanna")).isEmpty(), "single match already typed");
    }

    @Test void shownIsAWindowOfAtMostFiveAroundTheSelection() {
        List<String> many = List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8");
        TabCompletionModel m = new TabCompletionModel();
        assertEquals(TabCompletionModel.MAX_SHOWN, m.shown("a", many).items().size());
        assertEquals(8, m.shown("a", many).total());
        String t = m.cycle("a", many, false);
        for (int i = 0; i < 6; i++) {
            t = m.cycle(t, many, false);
        }
        assertEquals("a7", t);
        TabCompletionModel.Shown w = m.shown(t, many);
        assertEquals(TabCompletionModel.MAX_SHOWN, w.items().size());
        assertEquals("a7", w.items().get(w.selected()));
        assertEquals("a8", w.items().get(w.items().size() - 1));
    }
}
