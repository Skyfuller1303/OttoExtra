package de.ottoextra.crafting.werkstatt;

import de.ottoextra.ui.hudlayout.ActivityHudAnchors;

import java.util.function.ToIntFunction;

/**
 * Size, clipping and placement of the slim Werkstatt bar (design P1), free of Minecraft (text widths come
 * from a function). The bar is at least {@link #MIN_W} wide (a vanilla boss bar) and grows with its text
 * up to the screen width; a text that still does not fit is clipped with an ellipsis.
 */
final class WerkstattBarLayout {
    static final int MIN_W = 182;
    static final int H = 20;
    /** Anchor of the top-centre column; not one of the corner anchors of {@link ActivityHudAnchors}. */
    static final String TOP_CENTER = "TOP_CENTER";
    static final int ICON_X = 7;
    static final int ICON_Y = 2;
    /** Text column starts here (accent stripe and item icon left of it). */
    static final int TEXT_X = 26;
    static final int TEXT_Y = 4;
    /** Gap between the text and the right part (key, lock, fire time or check). */
    static final int TEXT_GAP = 8;
    static final int RIGHT_PAD = 6;
    static final int BAR_Y = 14;
    static final int BAR_H = 3;
    static final int FLAME_W = 7;
    static final int FLAME_GAP = 2;
    /** Lock icon plus its gap left of the flame. */
    static final int LOCK_W = 9;
    static final int CHECK_W = 8;
    /** Gap right of the stop key label. */
    static final int KEY_GAP = 4;
    /** Free space to the screen edges. */
    static final int SCREEN_MARGIN = 4;

    /**
     * @param width    bar width
     * @param textRoom room of the text left of the right part
     * @param right    width of the right part
     */
    record Layout(int width, int textRoom, int right) {
        int barWidth() {
            return Math.max(0, width - TEXT_X - RIGHT_PAD);
        }
    }

    private WerkstattBarLayout() {
    }

    /** Width of the right part: the check when finished, else stop key, lock and flame with fire time. */
    static int right(boolean finished, int clockW, boolean locked, int keyW) {
        if (finished) {
            return CHECK_W;
        }
        return FLAME_W + FLAME_GAP + Math.max(0, clockW) + (locked ? LOCK_W : 0) + (keyW > 0 ? keyW + KEY_GAP : 0);
    }

    static Layout compute(int textW, int right, int screenW) {
        int want = Math.max(MIN_W, TEXT_X + Math.max(0, textW) + TEXT_GAP + right + RIGHT_PAD);
        int width = Math.max(0, Math.min(want, screenW - 2 * SCREEN_MARGIN));
        int room = Math.max(0, width - TEXT_X - TEXT_GAP - right - RIGHT_PAD);
        return new Layout(width, room, right);
    }

    /** The text, or its longest prefix with "…" that fits into {@code room}; empty when not even that fits. */
    static String clip(ToIntFunction<String> width, String text, int room) {
        if (text == null || text.isEmpty() || width.applyAsInt(text) <= room) {
            return text == null ? "" : text;
        }
        int end = text.length();
        while (end > 0 && width.applyAsInt(text.substring(0, end) + "…") > room) {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end).stripTrailing() + "…";
    }

    /** Filled pixels of a progress bar with the inner width {@code inner}. */
    static int filled(int inner, float fraction) {
        return (int) (Math.max(0, inner) * Math.max(0f, Math.min(1f, fraction)));
    }

    /** Drawn top-left corner for an anchor; {@code columnY} is the y the top-centre column hands out. */
    static int[] position(String anchor, int offsetX, int offsetY, int screenW, int screenH, int w, int h,
                          int columnY) {
        if (TOP_CENTER.equals(anchor)) {
            int x = (screenW - w) / 2 + offsetX;
            int y = columnY + offsetY;
            return new int[]{Math.max(0, Math.min(screenW - w, x)), Math.max(0, Math.min(screenH - h, y))};
        }
        return ActivityHudAnchors.position(anchor, offsetX, offsetY, screenW, screenH, w, h);
    }

    /**
     * Placement after a drag in the layout editor: the top centre (base x centred, base y {@code columnTop})
     * when it is at least as near as the nearest corner anchor, else that corner.
     */
    static ActivityHudAnchors.Placement fromDrag(int x, int y, int w, int h, int screenW, int screenH,
                                                 int columnTop) {
        ActivityHudAnchors.Placement corner = ActivityHudAnchors.fromDrag(x, y, w, h, screenW, screenH);
        int cx = Math.max(0, Math.min(Math.max(0, screenW - w), x));
        int cy = Math.max(0, Math.min(Math.max(0, screenH - h), y));
        long dx = cx - (screenW - w) / 2;
        long dy = cy - columnTop;
        long cornerDist = (long) corner.offsetX() * corner.offsetX() + (long) corner.offsetY() * corner.offsetY();
        if (dx * dx + dy * dy <= cornerDist) {
            return new ActivityHudAnchors.Placement(TOP_CENTER, (int) dx, (int) dy);
        }
        return corner;
    }
}
