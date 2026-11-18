package de.ottoextra.tacticalfactions;

/** Pure placement math of the squad HUD: a corner anchor plus a pixel offset from that corner. */
final class HudPlacement {
    static final int MAX_OFFSET = 1000;

    private HudPlacement() { }

    record Placement(HudCorner corner, int offsetX, int offsetY) { }

    static final float MIN_SCALE = 0.5f;
    static final float MAX_SCALE = 2.0f;

    static float clampScale(float value) {
        if (Float.isNaN(value)) return 1.0f;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    static int scaled(int size, float scale) {
        return Math.round(size * scale);
    }

    static int clampOffset(int value) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
    }

    private static int baseX(HudCorner corner, int screenW, int w, int margin) {
        return corner.right() ? screenW - w - margin : margin;
    }

    private static int baseY(HudCorner corner, int screenH, int h, int margin) {
        return corner.bottom() ? screenH - h - margin : margin;
    }

    /** Top-left position of the panel, clamped so it stays fully on screen. */
    static int[] position(HudCorner corner, int offsetX, int offsetY, int screenW, int screenH,
                          int w, int h, int margin) {
        int x = baseX(corner, screenW, w, margin) + offsetX;
        int y = baseY(corner, screenH, h, margin) + offsetY;
        return new int[]{Math.max(0, Math.min(Math.max(0, screenW - w), x)),
                Math.max(0, Math.min(Math.max(0, screenH - h), y))};
    }

    /**
     * Placement for a panel dropped with its top-left corner at (x, y): the corner nearest to the
     * panel centre becomes the anchor and the offset is the distance from that corner's base spot.
     */
    static Placement fromDrag(int x, int y, int w, int h, int screenW, int screenH, int margin) {
        boolean right = x + w / 2 > screenW / 2;
        boolean bottom = y + h / 2 > screenH / 2;
        HudCorner corner = bottom ? (right ? HudCorner.BOTTOM_RIGHT : HudCorner.BOTTOM_LEFT)
                : (right ? HudCorner.TOP_RIGHT : HudCorner.TOP_LEFT);
        int[] clamped = {Math.max(0, Math.min(Math.max(0, screenW - w), x)),
                Math.max(0, Math.min(Math.max(0, screenH - h), y))};
        return new Placement(corner,
                clampOffset(clamped[0] - baseX(corner, screenW, w, margin)),
                clampOffset(clamped[1] - baseY(corner, screenH, h, margin)));
    }
}
