package de.ottoextra.regions.zonebanner;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Design C "Plakette": a dark rounded plaque with gold corner brackets, the coat of arms on
 * the left, the fief name on top and the subtitle below a thin gold separator. A single
 * diagonal sheen sweeps across the plate once the intro has settled.
 */
final class PlaketteStyle implements ZoneBannerStyle {

    // Palette
    private static final int NAME_COLOR = 0xFFF0EBE0;
    private static final int SUBTITLE_COLOR = 0xFFD6C194;
    private static final int GOLD_SHADOW = 0xFF47391D;
    private static final int GOLD_DARK = 0xFF866C36;
    private static final int GOLD = 0xFFB58D36;
    private static final int GOLD_LIGHT = 0xFFDBB451;
    private static final int DARK_0 = 0xFF141418;
    private static final int DARK_1 = 0xFF1E1E24;
    private static final int DARK_2 = 0xFF2E2D35;
    private static final int BEVEL_LIGHT = 0xFF514F59;
    private static final int BEVEL_DARK = DARK_2;
    private static final int OUTLINE = DARK_0;

    // Geometry (GUI pixels)
    private static final int PAD_LEFT = 8;
    private static final int ARMS_SIZE = 24;
    private static final int ARMS_GAP = 8;
    private static final int PAD_RIGHT = 14;
    private static final int PAD_Y = 4;
    private static final int MIN_ARMS_PAD_Y = 7;
    private static final int SCREEN_MARGIN = 8;
    private static final int SEP_GAP_TOP = 2;
    private static final int SEP_GAP_BOTTOM = 3;
    private static final int SEP_FADE_LEN = 10;
    private static final int BRACKET_INSET = 3;
    private static final int BRACKET_LEN = 5;
    private static final float PLATE_ALPHA = 0.92f;
    /** How far the top/bottom rows of the gradient move toward the lighter/darker tone. */
    private static final float GRADIENT_TOP = 0.55f;
    private static final float GRADIENT_BOTTOM = 0.7f;
    private static final float PLACEHOLDER_ALPHA = 0.45f;

    // Sheen
    private static final float SHEEN_HALF_WIDTH = 7f;
    private static final float SHEEN_MAX_ALPHA = 0.10f;
    private static final float SHEEN_SLOPE = (float) Math.tan(Math.toRadians(30));

    // Timeline (ms)
    private static final long PLATE_IN_START = 0, PLATE_IN_DUR = 350;
    private static final int PLATE_SLIDE = 10;
    private static final long BRACKET_IN_START = 200, BRACKET_IN_DUR = 250;
    private static final int BRACKET_SNAP = 6;
    private static final long CONTENT_IN_START = 250, CONTENT_IN_DUR = 300;
    private static final int NAME_SLIDE = 3;
    private static final long SEP_IN_START = 400, SEP_IN_DUR = 250;
    private static final long SHEEN_START = 650, SHEEN_DUR = 600;
    private static final long EXIT_DUR = 600;
    private static final int EXIT_SLIDE = 8;
    private static final long BRACKET_EXIT_LEAD = 150, BRACKET_EXIT_DUR = 400;
    private static final long ARMS_LATE_FADE_DUR = 300;

    /** Tracks when the coat of arms texture became available so a late arrival fades in. */
    private long lastElapsed = Long.MAX_VALUE;
    private long armsAppearMs = -1;

    @Override
    public String id() {
        return "plakette";
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, ZoneBannerFrame frame, long elapsedMs, long displayMs) {
        boolean sticky = frame.sticky();
        long t = Math.max(0, elapsedMs);
        if (!sticky && t >= displayMs) return;
        trackArms(frame.banner(), t);

        // --- Exit factors ---
        long exitStart = displayMs - EXIT_DUR;
        float exit = sticky ? 0f : ZoneBannerDraw.easeInOutSine(ZoneBannerDraw.phase(t, exitStart, EXIT_DUR));
        float bracketExit = sticky ? 0f
                : ZoneBannerDraw.easeInOutSine(ZoneBannerDraw.phase(t, exitStart - BRACKET_EXIT_LEAD, BRACKET_EXIT_DUR));
        float exitAlpha = 1f - exit;

        // --- Text metrics and layout ---
        int nameScale = Math.max(1, Math.round(2 * frame.textScale()));
        float subScale = Math.max(1f, frame.textScale());
        int glyphH = tr.fontHeight - 1;
        String subtitle = frame.subtitle() == null ? "" : frame.subtitle();
        boolean hasSub = !subtitle.isEmpty();

        int chrome = PAD_LEFT + ARMS_SIZE + ARMS_GAP + PAD_RIGHT;
        int maxTextW = Math.max(16, frame.screenW() - 2 * SCREEN_MARGIN - chrome);
        String name = fit(tr, frame.name() == null ? "" : frame.name(), maxTextW / (float) nameScale);
        if (hasSub) subtitle = fit(tr, subtitle, maxTextW / subScale);
        int nameW = tr.getWidth(name) * nameScale;
        int subW = hasSub ? Math.round(tr.getWidth(subtitle) * subScale) : 0;
        int textW = Math.max(nameW, subW);

        int nameH = glyphH * nameScale;
        int subH = Math.round(glyphH * subScale);
        int textBlockH = nameH + SEP_GAP_TOP + 1 + (hasSub ? SEP_GAP_BOTTOM + subH : 0);
        int plateW = chrome + textW;
        int plateH = Math.max(ARMS_SIZE + 2 * MIN_ARMS_PAD_Y, textBlockH + 2 * PAD_Y);

        // --- Intro factors ---
        float plateIn = ZoneBannerDraw.easeOutCubic(ZoneBannerDraw.phase(t, PLATE_IN_START, PLATE_IN_DUR));
        float plateA = ZoneBannerDraw.phase(t, PLATE_IN_START, PLATE_IN_DUR) * exitAlpha;
        if (plateA <= 0f) return;

        int slideY = Math.round(-PLATE_SLIDE * (1f - plateIn)) - Math.round(EXIT_SLIDE * exit);
        int px = (frame.screenW() - plateW) / 2;
        int py = frame.topMargin() + slideY;

        drawPlate(ctx, px, py, plateW, plateH, plateA);

        // --- Sheen (under content so text stays crisp) ---
        if (t >= SHEEN_START && t < SHEEN_START + SHEEN_DUR) {
            float p = ZoneBannerDraw.easeInOutSine(ZoneBannerDraw.phase(t, SHEEN_START, SHEEN_DUR));
            drawSheen(ctx, px, py, plateW, plateH, p, plateA);
        }

        // --- Coat of arms column ---
        int armsX = px + PAD_LEFT;
        int armsY = py + (plateH - ARMS_SIZE) / 2;
        float armsA = 0f;
        if (frame.banner() != null) {
            long armsStart = Math.max(CONTENT_IN_START, armsAppearMs);
            long armsDur = armsAppearMs > CONTENT_IN_START ? ARMS_LATE_FADE_DUR : CONTENT_IN_DUR;
            armsA = ZoneBannerDraw.phase(t, armsStart, armsDur);
        }
        float contentIn = ZoneBannerDraw.phase(t, CONTENT_IN_START, CONTENT_IN_DUR);
        float placeholderA = PLACEHOLDER_ALPHA * contentIn * (1f - armsA) * exitAlpha;
        if (placeholderA > 0f) drawArmsPlaceholder(ctx, armsX, armsY, placeholderA);
        if (armsA > 0f) ZoneBannerDraw.banner(ctx, frame.banner(), armsX, armsY, ARMS_SIZE, armsA * exitAlpha);

        // --- Name ---
        int textX = armsX + ARMS_SIZE + ARMS_GAP;
        int textY = py + (plateH - textBlockH) / 2;
        float nameA = contentIn * exitAlpha;
        int nameOffset = Math.round(-NAME_SLIDE * (1f - ZoneBannerDraw.easeOutCubic(contentIn)));
        ZoneBannerDraw.text(ctx, tr, name, textX + nameOffset, textY, nameScale,
                ZoneBannerDraw.alpha(NAME_COLOR, nameA), true);

        // --- Separator and subtitle ---
        float sepIn = ZoneBannerDraw.phase(t, SEP_IN_START, SEP_IN_DUR);
        int sepY = textY + nameH + SEP_GAP_TOP;
        int sepLen = Math.round(textW * ZoneBannerDraw.easeOutCubic(sepIn));
        drawSeparator(ctx, textX, sepY, sepLen, plateA);
        if (hasSub) {
            ZoneBannerDraw.text(ctx, tr, subtitle, textX, sepY + 1 + SEP_GAP_BOTTOM, subScale,
                    ZoneBannerDraw.alpha(SUBTITLE_COLOR, sepIn * exitAlpha), false);
        }

        // --- Corner brackets ---
        float bracketIn = ZoneBannerDraw.phase(t, BRACKET_IN_START, BRACKET_IN_DUR);
        float bracketA = bracketIn * (1f - bracketExit) * ZoneBannerDraw.phase(t, PLATE_IN_START, PLATE_IN_DUR);
        if (bracketA > 0f) {
            int snap = Math.round(BRACKET_SNAP * (1f - ZoneBannerDraw.easeOutCubic(bracketIn)));
            drawBrackets(ctx, px, py, plateW, plateH, snap, bracketA);
        }
    }

    /** Resets on a new trigger and remembers when the arms texture first showed up. */
    private void trackArms(Identifier banner, long t) {
        if (t < lastElapsed) armsAppearMs = -1;
        lastElapsed = t;
        if (banner != null && armsAppearMs < 0) armsAppearMs = t;
        if (banner == null) armsAppearMs = -1;
    }

    /** Trims {@code s} to fit {@code maxUnscaledW} (unscaled font pixels), appending an ellipsis. */
    private static String fit(TextRenderer tr, String s, float maxUnscaledW) {
        int max = (int) Math.floor(maxUnscaledW);
        if (s.isEmpty() || tr.getWidth(s) <= max) return s;
        String ellipsis = "…";
        int room = Math.max(0, max - tr.getWidth(ellipsis));
        return tr.trimToWidth(s, room).stripTrailing() + ellipsis;
    }

    /** Body gradient, rounded 1 px outline and inner bevel. */
    private static void drawPlate(DrawContext ctx, int x, int y, int w, int h, float a) {
        int x2 = x + w;
        int y2 = y + h;
        float bodyA = PLATE_ALPHA * a;

        // Vertical gradient, one fill per row; the row between top and bottom stays at DARK_1.
        int innerRows = h - 2;
        for (int i = 0; i < innerRows; i++) {
            float f = innerRows <= 1 ? 0f : (i / (float) (innerRows - 1)) * 2f - 1f; // -1 top .. 1 bottom
            int c = f < 0 ? lerp(DARK_1, DARK_2, -f * GRADIENT_TOP) : lerp(DARK_1, DARK_0, f * GRADIENT_BOTTOM);
            ctx.fill(x + 1, y + 1 + i, x2 - 1, y + 2 + i, ZoneBannerDraw.alpha(c, bodyA));
        }

        // Inner bevel (top/left light, bottom/right dark), corners left to the outline diagonal.
        int light = ZoneBannerDraw.alpha(BEVEL_LIGHT, a);
        int dark = ZoneBannerDraw.alpha(BEVEL_DARK, a);
        ctx.fill(x + 2, y + 1, x2 - 2, y + 2, light);
        ctx.fill(x + 1, y + 2, x + 2, y2 - 2, light);
        ctx.fill(x + 2, y2 - 2, x2 - 2, y2 - 1, dark);
        ctx.fill(x2 - 2, y + 2, x2 - 1, y2 - 2, dark);

        // Outline with the corner pixels omitted and the inner diagonal pixels outlined.
        int o = ZoneBannerDraw.alpha(OUTLINE, a);
        ctx.fill(x + 1, y, x2 - 1, y + 1, o);
        ctx.fill(x + 1, y2 - 1, x2 - 1, y2, o);
        ctx.fill(x, y + 1, x + 1, y2 - 1, o);
        ctx.fill(x2 - 1, y + 1, x2, y2 - 1, o);
        ctx.fill(x + 1, y + 1, x + 2, y + 2, o);
        ctx.fill(x2 - 2, y + 1, x2 - 1, y + 2, o);
        ctx.fill(x + 1, y2 - 2, x + 2, y2 - 1, o);
        ctx.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, o);
    }

    /**
     * Diagonal soft light band clipped to the plate interior. Each row gets per-pixel fills only
     * where the band covers it, so the band never leaks outside the plate (no scissor needed).
     */
    private static void drawSheen(DrawContext ctx, int x, int y, int w, int h, float progress, float a) {
        int ix1 = x + 2, ix2 = x + w - 2;
        int iy1 = y + 2, iy2 = y + h - 2;
        if (ix2 <= ix1 || iy2 <= iy1) return;
        float slant = (iy2 - iy1) * SHEEN_SLOPE * 0.5f;
        float start = ix1 - SHEEN_HALF_WIDTH - slant;
        float end = ix2 + SHEEN_HALF_WIDTH + slant;
        float centerX = start + (end - start) * progress;
        float midY = (iy1 + iy2) * 0.5f;

        for (int row = iy1; row < iy2; row++) {
            // Top rows lean right ("/" shape).
            float cx = centerX + (midY - (row + 0.5f)) * SHEEN_SLOPE;
            int from = Math.max(ix1, (int) Math.floor(cx - SHEEN_HALF_WIDTH));
            int to = Math.min(ix2, (int) Math.ceil(cx + SHEEN_HALF_WIDTH));
            for (int col = from; col < to; col++) {
                float d = Math.abs(col + 0.5f - cx) / SHEEN_HALF_WIDTH;
                if (d >= 1f) continue;
                float fall = 0.5f * (1f + (float) Math.cos(Math.PI * d));
                int alpha = Math.round(255 * SHEEN_MAX_ALPHA * fall * a);
                if (alpha <= 0) continue;
                ctx.fill(col, row, col + 1, row + 1, (alpha << 24) | 0xFFFFFF);
            }
        }
    }

    /** Subtle recessed slot shown while the coat of arms texture is not loaded yet. */
    private static void drawArmsPlaceholder(DrawContext ctx, int x, int y, float a) {
        ctx.fill(x + 1, y + 1, x + ARMS_SIZE - 1, y + ARMS_SIZE - 1, ZoneBannerDraw.alpha(DARK_0, a));
        ctx.fill(x + 1, y + ARMS_SIZE - 1, x + ARMS_SIZE - 1, y + ARMS_SIZE, ZoneBannerDraw.alpha(DARK_2, a));
    }

    /** 1 px gold line whose last {@link #SEP_FADE_LEN} pixels fade out toward the tip. */
    private static void drawSeparator(DrawContext ctx, int x, int y, int len, float a) {
        if (len <= 0 || a <= 0f) return;
        int fadeLen = Math.min(SEP_FADE_LEN, len);
        int solid = len - fadeLen;
        if (solid > 0) ctx.fill(x, y, x + solid, y + 1, ZoneBannerDraw.alpha(GOLD_DARK, a));
        for (int i = 0; i < fadeLen; i++) {
            float f = 1f - (i + 1) / (float) (fadeLen + 1);
            int c = ZoneBannerDraw.alpha(GOLD_DARK, a * f);
            if (c != 0) ctx.fill(x + solid + i, y, x + solid + i + 1, y + 1, c);
        }
    }

    /** Four L-shaped gold brackets; {@code snap} pushes each one outward diagonally. */
    private static void drawBrackets(DrawContext ctx, int x, int y, int w, int h, int snap, float a) {
        int left = x + BRACKET_INSET - snap;
        int right = x + w - 1 - BRACKET_INSET + snap;
        int top = y + BRACKET_INSET - snap;
        int bottom = y + h - 1 - BRACKET_INSET + snap;
        int shadow = ZoneBannerDraw.alpha(GOLD_SHADOW, a);
        int gold = ZoneBannerDraw.alpha(GOLD, a);
        int light = ZoneBannerDraw.alpha(GOLD_LIGHT, a);
        // Shadows first (offset 1 px down/right), then the brackets on top.
        bracket(ctx, left + 1, top + 1, 1, 1, shadow, shadow);
        bracket(ctx, right + 1, top + 1, -1, 1, shadow, shadow);
        bracket(ctx, left + 1, bottom + 1, 1, -1, shadow, shadow);
        bracket(ctx, right + 1, bottom + 1, -1, -1, shadow, shadow);
        bracket(ctx, left, top, 1, 1, gold, light);
        bracket(ctx, right, top, -1, 1, gold, light);
        bracket(ctx, left, bottom, 1, -1, gold, light);
        bracket(ctx, right, bottom, -1, -1, gold, light);
    }

    /** One L with its corner pixel at (cx, cy); arms extend in direction (dx, dy). */
    private static void bracket(DrawContext ctx, int cx, int cy, int dx, int dy, int color, int cornerColor) {
        int hx1 = dx > 0 ? cx + 1 : cx - BRACKET_LEN + 1;
        int hx2 = dx > 0 ? cx + BRACKET_LEN : cx;
        ctx.fill(hx1, cy, hx2, cy + 1, color);
        int vy1 = dy > 0 ? cy + 1 : cy - BRACKET_LEN + 1;
        int vy2 = dy > 0 ? cy + BRACKET_LEN : cy;
        ctx.fill(cx, vy1, cx + 1, vy2, color);
        ctx.fill(cx, cy, cx + 1, cy + 1, cornerColor);
    }

    /** Linear blend between two opaque RGB colors; result is opaque. */
    private static int lerp(int from, int to, float f) {
        f = ZoneBannerDraw.clamp01(f);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
