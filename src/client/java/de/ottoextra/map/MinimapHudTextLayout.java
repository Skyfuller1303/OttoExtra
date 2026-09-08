package de.ottoextra.map;

/** GUI-pixel bounds shared by the minimap HUD and its layout editor. */
public final class MinimapHudTextLayout {
    public static final int LINE_HEIGHT = 10;
    public static final int MAX_HEIGHT = 160;

    private MinimapHudTextLayout() { }

    public static float clampScale(float scale) {
        return Float.isFinite(scale) ? Math.max(0.5F, Math.min(3F, scale)) : 1F;
    }

    public static int height(int configured, float scale) {
        int singleLine = Math.round(LINE_HEIGHT * clampScale(scale));
        return configured <= 0 ? singleLine : Math.max(singleLine, Math.min(MAX_HEIGHT, configured));
    }

    public static int maxLines(int height, float scale) {
        return Math.max(1, (int) Math.floor(Math.min(MAX_HEIGHT, Math.max(1, height))
                / (LINE_HEIGHT * (double) clampScale(scale)) + 0.00001));
    }

    public static int textWidth(int width, float scale) {
        return Math.max(1, (int) Math.floor(Math.max(1, width) / (double) clampScale(scale)));
    }
}
