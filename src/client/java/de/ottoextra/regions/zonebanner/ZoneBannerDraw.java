package de.ottoextra.regions.zonebanner;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Small shared helpers for zone banner styles. Styles may add private helpers of their own. */
public final class ZoneBannerDraw {

    private ZoneBannerDraw() {
    }

    /** Applies an opacity factor 0..1 to an ARGB color; tiny alphas become 0 (MC draws alpha<4 text opaque). */
    public static int alpha(int argb, float a) {
        int base = (argb >>> 24) & 0xFF;
        int na = Math.round(base * clamp01(a));
        if (na < 4) na = 0;
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    public static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /** Normalized progress of {@code t} inside [start, start+duration]. */
    public static float phase(long t, long start, long duration) {
        if (duration <= 0) return t >= start ? 1f : 0f;
        return clamp01((t - start) / (float) duration);
    }

    public static float easeOutCubic(float x) {
        float p = 1f - clamp01(x);
        return 1f - p * p * p;
    }

    public static float easeInOutSine(float x) {
        return (float) (-(Math.cos(Math.PI * clamp01(x)) - 1) / 2);
    }

    /** Draws a square texture (the coat of arms) with an opacity factor. */
    public static void banner(DrawContext ctx, Identifier id, int x, int y, int size, float a) {
        if (id == null || size <= 0) return;
        int color = alpha(0xFFFFFFFF, a);
        if (color == 0) return;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, size, size, color);
    }

    /** Draws text scaled around its top-left corner; returns the scaled width. */
    public static int text(DrawContext ctx, TextRenderer tr, String s, float x, float y,
                           float scale, int argb, boolean shadow) {
        if (s == null || s.isEmpty() || ((argb >>> 24) & 0xFF) == 0) return 0;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        ctx.drawText(tr, s, 0, 0, argb, shadow);
        m.popMatrix();
        return Math.round(tr.getWidth(s) * scale);
    }
}
