package de.ottoextra.crafting.werkstatt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TabGeometryTest {
    @Test void chestOfAnyRowCountGetsTheTab16RowsBelowItsTop() {
        for (int rows : new int[] {1, 3, 6}) {
            int h = 114 + rows * 18;
            TabGeometry.Tab t = TabGeometry.tab(100, 40, 176, h);
            assertEquals(40 + TabGeometry.TOP, t.y0(), rows + " rows");
            assertEquals(100 + 175, t.x0(), "frame outline column 175 opens into the tab");
            assertEquals(100 + 195, t.right(), "tab sticks out 20 px");
            assertEquals(100 + 176, t.iconX());
            assertEquals(40 + 19, t.iconY());
        }
    }

    @Test void oddWidthsUseTheRealRightEdge() {
        TabGeometry.Tab t = TabGeometry.tab(10, 0, 195, 200);
        assertEquals(10 + 194, t.x0());
        assertEquals(10 + 214, t.right());
        assertEquals(10 + 1 + 194, t.iconX());
    }

    @Test void lowGuisMoveTheTabUpSoItStaysOnTheFrame() {
        assertEquals(TabGeometry.TOP, TabGeometry.tab(0, 0, 176, 40).y0());
        int y = TabGeometry.tab(0, 0, 176, 30).y0();
        assertEquals(30 - TabGeometry.HEIGHT - TabGeometry.BOTTOM_MARGIN, y);
        assertTrue(y + TabGeometry.HEIGHT <= 30);
        assertEquals(0, TabGeometry.tab(0, 0, 176, 10).y0());
    }

    @Test void panelJoinsTheTabWhenItFitsElseGoesLeft() {
        TabGeometry.Tab t = TabGeometry.tab(100, 20, 176, 222);
        boolean[] right = new boolean[1];
        assertEquals(t.right(), TabGeometry.panelX(t, 100, 206, 600, right));
        assertTrue(right[0]);
        TabGeometry.Tab narrow = TabGeometry.tab(220, 20, 176, 222);
        int x = TabGeometry.panelX(narrow, 220, 206, 500, right);
        assertFalse(right[0]);
        assertEquals(220 - 4 - 206, x);
    }
}
