package de.ottoextra.resources;

/**
 * Pure placement math of the timer panel: a corner anchor, a pixel offset from that corner and a
 * scale, as stored in {@link ResourcesConfig}. Same rules as the squad HUD of Tactical Factions, so
 * both behave alike in OttoExtra's "HUD anordnen" editor.
 */
final class ResourceHudPlacement {
    static final int MARGIN = 4;
    static final int MAX_OFFSET = 1000;
    static final float MIN_SCALE = 0.5f;
    static final float MAX_SCALE = 2.0f;
    /** Default distance from the top: the panel used to sit 60 px below the screen edge. */
    static final int DEFAULT_OFFSET_Y = 56;

    enum Corner {
        TOP_LEFT(false, false), TOP_RIGHT(true, false), BOTTOM_LEFT(false, true), BOTTOM_RIGHT(true, true);

        final boolean right;
        final boolean bottom;

        Corner(boolean right, boolean bottom) {
            this.right = right;
            this.bottom = bottom;
        }

        /** The corner with this name, or top right for anything unknown. */
        static Corner parse(String name) {
            for (Corner corner : values()) {
                if (corner.name().equals(name)) return corner;
            }
            return TOP_RIGHT;
        }
    }

    record Placement(Corner corner, int offsetX, int offsetY) { }

    private ResourceHudPlacement() {
    }

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

    private static int baseX(Corner corner, int screenW, int w) {
        return corner.right ? screenW - w - MARGIN : MARGIN;
    }

    private static int baseY(Corner corner, int screenH, int h) {
        return corner.bottom ? screenH - h - MARGIN : MARGIN;
    }

    /** Top-left position of a panel of the given (already scaled) size, kept fully on screen. */
    static int[] position(Corner corner, int offsetX, int offsetY, int screenW, int screenH, int w, int h) {
        int x = baseX(corner, screenW, w) + offsetX;
        int y = baseY(corner, screenH, h) + offsetY;
        return new int[] {Math.max(0, Math.min(Math.max(0, screenW - w), x)),
                Math.max(0, Math.min(Math.max(0, screenH - h), y))};
    }

    /**
     * Placement of a panel dropped with its top-left corner at (x, y): the corner nearest to the
     * panel centre becomes the anchor, the offset is the distance from that corner's base spot.
     */
    static Placement fromDrag(int x, int y, int w, int h, int screenW, int screenH) {
        boolean right = x + w / 2 > screenW / 2;
        boolean bottom = y + h / 2 > screenH / 2;
        Corner corner = bottom ? (right ? Corner.BOTTOM_RIGHT : Corner.BOTTOM_LEFT)
                : (right ? Corner.TOP_RIGHT : Corner.TOP_LEFT);
        int clampedX = Math.max(0, Math.min(Math.max(0, screenW - w), x));
        int clampedY = Math.max(0, Math.min(Math.max(0, screenH - h), y));
        return new Placement(corner, clampOffset(clampedX - baseX(corner, screenW, w)),
                clampOffset(clampedY - baseY(corner, screenH, h)));
    }
}
