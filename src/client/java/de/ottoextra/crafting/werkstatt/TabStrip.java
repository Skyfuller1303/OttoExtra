package de.ottoextra.crafting.werkstatt;

/**
 * Widths of the panel tabs, free of Minecraft. All labels should fit next to each other in the
 * room of the tab row (the toggle button never overlaps it): first the inner padding
 * shrinks from {@link #MAX_PAD} to {@link #MIN_PAD} and the gap from 2 to 1; only if that is still
 * too wide does the caller fall back to the short labels, and as a last resort every tab gets an
 * equal share and its label is clipped.
 */
final class TabStrip {
    static final int MAX_PAD = 4;
    static final int MIN_PAD = 2;

    /**
     * @param widths  tab widths including padding
     * @param gap     space between tabs
     * @param fits    all labels fit unclipped
     */
    record Layout(int[] widths, int gap, boolean fits) {
        int total() {
            int sum = 0;
            for (int w : widths) {
                sum += w;
            }
            return sum + gap * Math.max(0, widths.length - 1);
        }
    }

    private TabStrip() {
    }

    /** Tab widths for labels of the given text widths in {@code room} pixels. */
    static Layout layout(int[] labelWidths, int room) {
        int n = labelWidths.length;
        if (n == 0) {
            return new Layout(new int[0], 0, true);
        }
        for (int gap = 2; gap >= 1; gap--) {
            for (int pad = MAX_PAD; pad >= MIN_PAD; pad--) {
                int[] widths = new int[n];
                int total = gap * (n - 1);
                for (int i = 0; i < n; i++) {
                    widths[i] = labelWidths[i] + 2 * pad;
                    total += widths[i];
                }
                if (total <= room) {
                    return new Layout(widths, gap, true);
                }
            }
        }
        int[] widths = new int[n];
        int share = Math.max(1, (room - (n - 1)) / n);
        for (int i = 0; i < n; i++) {
            widths[i] = share;
        }
        return new Layout(widths, 1, false);
    }
}
