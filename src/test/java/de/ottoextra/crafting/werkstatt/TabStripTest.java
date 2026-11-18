package de.ottoextra.crafting.werkstatt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TabStripTest {
    /** Auftrag, Brennstoff, Warteschlange in the Minecraft font. */
    private static final int[] LABELS = {39, 56, 73};

    @Test void roomyRowUsesTheFullPadding() {
        TabStrip.Layout l = TabStrip.layout(LABELS, 200);
        assertTrue(l.fits());
        assertEquals(39 + 2 * TabStrip.MAX_PAD, l.widths()[0]);
        assertEquals(2, l.gap());
        assertTrue(l.total() <= 200);
    }

    @Test void rowRightOfTheButtonShrinksPaddingButKeepsEveryLabelWhole() {
        // Panel 200 wide starting 2 pixels right of the GUI; button (18) plus 2 pixels: 182 left.
        TabStrip.Layout l = TabStrip.layout(LABELS, 182);
        assertTrue(l.fits());
        assertTrue(l.total() <= 182);
        for (int i = 0; i < LABELS.length; i++) {
            assertTrue(l.widths()[i] >= LABELS[i] + 2 * TabStrip.MIN_PAD, "label " + i + " fits");
        }
    }

    @Test void tooNarrowRowReportsThatLabelsDoNotFit() {
        TabStrip.Layout l = TabStrip.layout(LABELS, 120);
        assertFalse(l.fits());
        assertTrue(l.total() <= 120);
        assertEquals(0, TabStrip.layout(new int[0], 10).widths().length);
    }
}
