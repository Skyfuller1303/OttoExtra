package de.ottoextra.crafting.werkstatt;

/**
 * Where the T2 frame tab and the panel go, free of Minecraft. Everything follows the actual GUI
 * (its left/top and background size), not a fixed 176x222 chest. All numbers are GUI pixels.
 *
 * <p>The tab grows out of the frame's right outline column ({@code width - 1}): it sticks out
 * {@link #OUT} pixels and is {@link #HEIGHT} rows tall, {@link #TOP} rows below the GUI top. On a GUI
 * too low for that it moves up so it stays fully on the frame edge.
 */
final class TabGeometry {
    static final int OUT = 20;
    static final int HEIGHT = 22;
    static final int TOP = 16;
    /** Rows of frame kept free below the tab. */
    static final int BOTTOM_MARGIN = 2;

    /**
     * @param x0     column of the frame outline that opens into the tab (GUI left + width - 1)
     * @param y0     top outline row of the tab
     * @param right  right outline column of the tab; the open panel starts here
     */
    record Tab(int x0, int y0, int right) {
        int iconX() {
            return x0 + 1;
        }

        int iconY() {
            return y0 + 3;
        }
    }

    private TabGeometry() {
    }

    static Tab tab(int guiX, int guiY, int guiW, int guiH) {
        int w = Math.max(1, guiW);
        int offset = Math.max(0, Math.min(TOP, guiH - HEIGHT - BOTTOM_MARGIN));
        int x0 = guiX + w - 1;
        return new Tab(x0, guiY + offset, x0 + OUT);
    }

    /**
     * Left edge of the panel: right of the tab if it fits on screen (then tab and panel join),
     * else left of the GUI. {@code rightSide[0]} tells which.
     */
    static int panelX(Tab tab, int guiX, int panelW, int screenW, boolean[] rightSide) {
        boolean right = tab.right() + panelW <= screenW - 2 || guiX - 4 - panelW < 2;
        if (rightSide != null && rightSide.length > 0) {
            rightSide[0] = right;
        }
        return right ? tab.right() : guiX - 4 - panelW;
    }
}
