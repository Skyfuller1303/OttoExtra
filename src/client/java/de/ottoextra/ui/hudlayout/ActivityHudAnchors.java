package de.ottoextra.ui.hudlayout;

import java.util.Locale;

/** Anchor and offset math of the region activity HUD (same scheme as RegionActivityHud.position). */
public final class ActivityHudAnchors {
    public static final String[] ANCHORS = {"TOP_LEFT", "TOP_RIGHT", "MIDDLE_LEFT", "MIDDLE_RIGHT",
            "BOTTOM_LEFT", "BOTTOM_RIGHT"};
    public static final int MARGIN = 4;
    public static final int MAX_OFFSET = 1000;

    private ActivityHudAnchors() {
    }

    public record Placement(String anchor, int offsetX, int offsetY) {
    }

    private static int baseX(String anchor, int screenW, int w) {
        return anchor.endsWith("RIGHT") ? screenW - w - MARGIN : MARGIN;
    }

    private static int baseY(String anchor, int screenH, int h) {
        return anchor.startsWith("TOP") ? MARGIN : anchor.startsWith("BOTTOM") ? screenH - h - MARGIN
                : (screenH - h) / 2;
    }

    static int clampOffset(int value) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
    }

    public static int[] position(String anchor, int offsetX, int offsetY, int screenW, int screenH,
                                 int w, int h) {
        String value = anchor == null ? "MIDDLE_LEFT" : anchor.toUpperCase(Locale.ROOT);
        int x = baseX(value, screenW, w) + offsetX;
        int y = baseY(value, screenH, h) + offsetY;
        return new int[]{Math.max(0, Math.min(screenW - w, x)), Math.max(0, Math.min(screenH - h, y))};
    }

    /** The anchor whose base spot is nearest to the dropped top-left corner, plus the remaining offset. */
    public static Placement fromDrag(int x, int y, int w, int h, int screenW, int screenH) {
        int cx = Math.max(0, Math.min(Math.max(0, screenW - w), x));
        int cy = Math.max(0, Math.min(Math.max(0, screenH - h), y));
        String best = ANCHORS[0];
        long bestDist = Long.MAX_VALUE;
        for (String anchor : ANCHORS) {
            long dx = cx - baseX(anchor, screenW, w);
            long dy = cy - baseY(anchor, screenH, h);
            long dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = anchor;
            }
        }
        return new Placement(best, clampOffset(cx - baseX(best, screenW, w)),
                clampOffset(cy - baseY(best, screenH, h)));
    }
}
