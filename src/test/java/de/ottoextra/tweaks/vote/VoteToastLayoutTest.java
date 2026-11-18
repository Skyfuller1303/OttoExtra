package de.ottoextra.tweaks.vote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteToastLayoutTest {

    @Test
    void enterAndXFitTheDesignedCard() {
        // "Enter" 25 px, "Abstimmen" 47 px, "X" 5 px, "Später" 29 px.
        VoteToastLayout.Layout l = VoteToastLayout.compute(25, 47, 5, 29, 640);
        assertEquals(196, l.width());
        assertEquals(58, VoteToastLayout.H);
        assertEquals(196 - 33 - 6, l.textRoom());
        assertEquals(31, l.openChipW());
        assertEquals(11, l.laterChipW());
        assertEquals(196 - 6 - 29 - 3 - 11, l.laterX());
        assertTrue(l.showLater());
        assertEquals(196 - 6, l.lineWidth());
    }

    @Test
    void longKeyNamesWidenTheCardInsteadOfOverlapping() {
        VoteToastLayout.Layout l = VoteToastLayout.compute(70, 47, 60, 29, 640);
        int left = 76 + 3 + 47;
        int right = 66 + 3 + 29;
        assertEquals(33 + left + 8 + right + 6, l.width());
        assertTrue(l.showLater());
        assertEquals(33 + left + 8, l.laterX());
    }

    @Test
    void unboundKeysDropTheirParts() {
        VoteToastLayout.Layout noOpen = VoteToastLayout.compute(0, 47, 5, 29, 640);
        assertEquals(0, noOpen.openChipW());
        assertEquals(0, noOpen.actionRoom());
        assertTrue(noOpen.showLater());
        VoteToastLayout.Layout noLater = VoteToastLayout.compute(25, 47, 0, 29, 640);
        assertFalse(noLater.showLater());
        assertEquals(196, noLater.width());
    }

    @Test
    void narrowScreenClipsAndLeavesOutLater() {
        VoteToastLayout.Layout l = VoteToastLayout.compute(25, 47, 5, 29, 150);
        assertEquals(142, l.width());
        assertEquals(142 - 33 - 6, l.textRoom());
        assertEquals(103 - 31 - 3, l.actionRoom());
        assertFalse(l.showLater());
        VoteToastLayout.Layout tiny = VoteToastLayout.compute(25, 47, 5, 29, 20);
        assertEquals(12, tiny.width());
        assertEquals(0, tiny.textRoom());
        assertEquals(0, tiny.actionRoom());
        assertEquals(2, tiny.headerRoom());
    }

    @Test
    void remainingTimeLineShrinksToZero() {
        assertEquals(190, VoteToastLayout.remaining(190, 0, 10_000));
        assertEquals(95, VoteToastLayout.remaining(190, 5_000, 10_000));
        assertEquals(0, VoteToastLayout.remaining(190, 12_000, 10_000));
        assertEquals(190, VoteToastLayout.remaining(190, -5, 10_000));
        assertEquals(0, VoteToastLayout.remaining(190, 0, 0));
    }
}
