package de.ottoextra.resources;

import net.minecraft.client.gui.DrawContext;

/**
 * Small coloured gem per resource kind: the dots of the side HUD, and the fallback of the book
 * when an item picture is missing. 4x4 cells: light colour at (1..2,0..2) and (0,1..2), dark colour at (3,1..2) and (1..2,3).
 */
final class ResourceGem {
    private static final int UNKNOWN = 0xFF9AA6B5;

    private ResourceGem() { }

    /** Light colour (opaque ARGB) of a type; unknown types get a neutral grey-blue. */
    static int light(String type) {
        String base = ResourcePolicy.baseOf(type);
        if (base == null) return UNKNOWN;
        return switch (base) {
            case "IRON" -> 0xFFC9A58A;
            case "COAL" -> 0xFF55555F;
            case "SALT" -> 0xFFE6E2EC;
            case "PEAT" -> 0xFF7A5A3A;
            case "CLAY" -> 0xFF9AA3B5;
            case "GARDEN" -> 0xFF6FB35A;
            case "APIARY" -> 0xFFE0B43A;
            default -> 0xFFB56FC7;
        };
    }

    /** Dark colour: the light colour times 0.6 per channel, truncated. */
    static int dark(int light) {
        int r = (int) (((light >> 16) & 0xFF) * 0.6);
        int g = (int) (((light >> 8) & 0xFF) * 0.6);
        int b = (int) ((light & 0xFF) * 0.6);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Draws the gem with its top-left corner at (x, y); each cell is {@code scale} GUI pixels. */
    static void draw(DrawContext context, int x, int y, String type, int scale) {
        int light = light(type);
        int dark = dark(light);
        cell(context, x, y, scale, 1, 0, 2, 3, light);
        cell(context, x, y, scale, 0, 1, 1, 2, light);
        cell(context, x, y, scale, 3, 1, 1, 2, dark);
        cell(context, x, y, scale, 1, 3, 2, 1, dark);
    }

    /** Fills cells [cx, cx + w) x [cy, cy + h) when called with the cell span of a block. */
    private static void cell(DrawContext context, int x, int y, int scale, int cx, int cy, int w, int h, int color) {
        context.fill(x + cx * scale, y + cy * scale, x + (cx + w) * scale, y + (cy + h) * scale, color);
    }
}
