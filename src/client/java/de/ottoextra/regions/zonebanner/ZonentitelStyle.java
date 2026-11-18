package de.ottoextra.regions.zonebanner;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Style A "Zonentitel": a boxless MMORPG-like zone title. Coat of arms on top, the name in
 * large letters flanked by two thin gold rules with diamond tips, the subtitle below, and a very
 * soft dark band behind the text block so it stays readable over a bright sky.
 */
final class ZonentitelStyle implements ZoneBannerStyle {

    // Palette
    private static final int COLOR_NAME = 0xFFF0EBE0;
    private static final int COLOR_SUBTITLE = 0xFFD6C194;
    private static final int GOLD_DARK = 0xFF47391D;
    private static final int GOLD_MID_DARK = 0xFF866C36;
    private static final int GOLD_MID = 0xFFB58D36;
    private static final int GOLD_LIGHT = 0xFFDBB451;
    private static final int BAND_COLOR = 0xFF141418;

    // Layout (GUI px; sizes tied to the name scale are given per name-scale unit)
    private static final int BANNER_SIZE_PER_SCALE = 12;   // 24 px at name scale 2
    private static final int BANNER_NAME_GAP = 4;
    private static final int GLYPH_HEIGHT = 7;             // cap height of the vanilla font
    private static final int LINE_HEIGHT = 8;
    private static final int NAME_SUBTITLE_GAP = 3;
    private static final int SUBTITLE_LINE_HEIGHT = 9;
    private static final int RULE_LENGTH = 40;
    private static final int RULE_TEXT_GAP = 6;
    private static final int RULE_MIN_LENGTH = 8;
    private static final int DIAMOND_RADIUS = 2;           // diamond is 5x5 px
    private static final int SCREEN_EDGE_PAD = 6;
    private static final int NAME_SCREEN_PAD = 16;
    private static final int GLINT_LENGTH = 4;

    // Background band
    private static final float BAND_PEAK_ALPHA = 0.40f;
    private static final int BAND_EXTRA_WIDTH = 160;       // band width = name width + this
    private static final int BAND_PLATEAU_EXTRA = 10;      // fully opaque core = name half width + this
    private static final int BAND_MIN_FALLOFF = 40;
    private static final int BAND_MAX_COLUMNS_PER_SIDE = 35;
    private static final int BAND_PAD_TOP = 4;
    private static final int BAND_PAD_BOTTOM = 4;
    private static final int BAND_EDGE_ROWS = 3;

    // Intro timeline (ms since trigger)
    private static final long BAND_IN_START = 0, BAND_IN_DURATION = 300;
    private static final long COAT_IN_START = 0, COAT_IN_DURATION = 300;
    private static final float COAT_SLIDE_PX = 8f;
    private static final long COAT_LATE_FADE_DURATION = 300;
    private static final long NAME_IN_START = 150, NAME_IN_DURATION = 350;
    private static final float NAME_RISE_PX = 4f;
    private static final long RULE_GROW_START = 300, RULE_GROW_DURATION = 450;
    private static final long DIAMOND_IN_DURATION = 120;
    private static final long SUBTITLE_IN_START = 450, SUBTITLE_IN_DURATION = 350;
    private static final long GLINT_START = 900, GLINT_DURATION = 600;

    // Exit timeline (ms relative to displayMs - EXIT_DURATION)
    private static final long EXIT_DURATION = 700;
    private static final long RULE_RETRACT_START = 0, RULE_RETRACT_DURATION = 300;
    private static final long DIAMOND_OUT_DURATION = 100;
    private static final long ELEMENTS_OUT_START = 200, ELEMENTS_OUT_DURATION = 350;
    private static final long BAND_OUT_START = 350, BAND_OUT_DURATION = 350;

    /** Elapsed time at which the coat of arms texture first became available, -1 if not yet. */
    private long bannerSeenAtMs = -1;

    @Override
    public String id() {
        return "zonentitel";
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, ZoneBannerFrame frame, long elapsedMs, long displayMs) {
        boolean sticky = frame.sticky();
        if (!sticky && elapsedMs >= displayMs) return;
        long t = Math.max(0, elapsedMs);
        String name = frame.name() == null ? "" : frame.name();
        String subtitle = frame.subtitle() == null ? "" : frame.subtitle();
        int screenW = frame.screenW();

        // ---- Exit factors (1 = fully shown) ----
        long exitStart = displayMs - EXIT_DURATION;
        float ruleRetract = 1f, diamondOut = 1f, elementsOut = 1f, bandOut = 1f;
        if (!sticky) {
            ruleRetract = 1f - ZoneBannerDraw.easeInOutSine(
                    ZoneBannerDraw.phase(t, exitStart + RULE_RETRACT_START, RULE_RETRACT_DURATION));
            diamondOut = 1f - ZoneBannerDraw.phase(t, exitStart + RULE_RETRACT_START, DIAMOND_OUT_DURATION);
            elementsOut = 1f - ZoneBannerDraw.easeInOutSine(
                    ZoneBannerDraw.phase(t, exitStart + ELEMENTS_OUT_START, ELEMENTS_OUT_DURATION));
            bandOut = 1f - ZoneBannerDraw.easeInOutSine(
                    ZoneBannerDraw.phase(t, exitStart + BAND_OUT_START, BAND_OUT_DURATION));
        }

        // ---- Layout ----
        int nameScale = Math.max(1, Math.round(2 * frame.textScale()));
        int rawNameW = tr.getWidth(name);
        while (nameScale > 1 && rawNameW * nameScale > screenW - NAME_SCREEN_PAD) nameScale--;
        float subScale = Math.max(1f, frame.textScale());
        int nameW = rawNameW * nameScale;
        int subW = Math.round(tr.getWidth(subtitle) * subScale);

        int bannerSize = BANNER_SIZE_PER_SCALE * nameScale;
        int bannerX = (screenW - bannerSize) / 2;
        int bannerY = frame.topMargin();
        // Space for the coat of arms is always reserved so a late-loading texture causes no jump.
        int nameX = (screenW - nameW) / 2;
        int nameY = bannerY + bannerSize + BANNER_NAME_GAP;
        int nameBottom = nameY + LINE_HEIGHT * nameScale;
        int subX = (screenW - subW) / 2;
        int subY = nameBottom + NAME_SUBTITLE_GAP;
        int blockBottom = subtitle.isEmpty() ? nameBottom : subY + Math.round(SUBTITLE_LINE_HEIGHT * subScale);
        int ruleY = nameY + (GLYPH_HEIGHT * nameScale) / 2;

        // ---- Background band ----
        float bandIn = ZoneBannerDraw.easeInOutSine(ZoneBannerDraw.phase(t, BAND_IN_START, BAND_IN_DURATION));
        drawBand(ctx, screenW, nameW, nameY - BAND_PAD_TOP, blockBottom + BAND_PAD_BOTTOM, bandIn * bandOut);

        // ---- Coat of arms ----
        Identifier banner = frame.banner();
        updateBannerSeen(banner, t);
        if (banner != null) {
            float p = ZoneBannerDraw.easeOutCubic(ZoneBannerDraw.phase(t, COAT_IN_START, COAT_IN_DURATION));
            float late = bannerSeenAtMs <= COAT_IN_START + COAT_IN_DURATION ? 1f
                    : ZoneBannerDraw.easeInOutSine(ZoneBannerDraw.phase(t, bannerSeenAtMs, COAT_LATE_FADE_DURATION));
            float a = p * late * elementsOut;
            if (a > 0f) {
                var m = ctx.getMatrices();
                m.pushMatrix();
                m.translate(0f, -COAT_SLIDE_PX * (1f - p));
                ZoneBannerDraw.banner(ctx, banner, bannerX, bannerY, bannerSize, a);
                m.popMatrix();
            }
        }

        // ---- Name ----
        float nameP = ZoneBannerDraw.phase(t, NAME_IN_START, NAME_IN_DURATION);
        float nameA = ZoneBannerDraw.easeInOutSine(nameP) * elementsOut;
        float nameRise = NAME_RISE_PX * (1f - ZoneBannerDraw.easeOutCubic(nameP));
        ZoneBannerDraw.text(ctx, tr, name, nameX, nameY + nameRise, nameScale,
                ZoneBannerDraw.alpha(COLOR_NAME, nameA), true);

        // ---- Gold rules ----
        int ruleMax = Math.min(RULE_LENGTH,
                (screenW - nameW) / 2 - RULE_TEXT_GAP - (2 * DIAMOND_RADIUS + 1) - SCREEN_EDGE_PAD);
        if (ruleMax >= RULE_MIN_LENGTH && !name.isEmpty()) {
            float grow = ZoneBannerDraw.easeOutCubic(ZoneBannerDraw.phase(t, RULE_GROW_START, RULE_GROW_DURATION));
            int len = Math.round(ruleMax * grow * ruleRetract);
            float diamondA = ZoneBannerDraw.phase(t, RULE_GROW_START + RULE_GROW_DURATION, DIAMOND_IN_DURATION)
                    * diamondOut * elementsOut;
            float glintP = ZoneBannerDraw.phase(t, GLINT_START, GLINT_DURATION);
            float ruleA = elementsOut;
            int leftInner = nameX - RULE_TEXT_GAP;
            int rightInner = nameX + nameW + RULE_TEXT_GAP;
            drawRule(ctx, leftInner, ruleY, len, -1, ruleA, diamondA, glintP);
            drawRule(ctx, rightInner, ruleY, len, 1, ruleA, diamondA, glintP);
        }

        // ---- Subtitle ----
        if (!subtitle.isEmpty()) {
            float subA = ZoneBannerDraw.easeInOutSine(
                    ZoneBannerDraw.phase(t, SUBTITLE_IN_START, SUBTITLE_IN_DURATION)) * elementsOut;
            ZoneBannerDraw.text(ctx, tr, subtitle, subX, subY, subScale,
                    ZoneBannerDraw.alpha(COLOR_SUBTITLE, subA), true);
        }
    }

    /** Remembers when the coat of arms texture first showed up, so a late load can fade in. */
    private void updateBannerSeen(Identifier banner, long t) {
        if (banner == null) {
            bannerSeenAtMs = -1;
        } else if (bannerSeenAtMs < 0 || t < bannerSeenAtMs) {
            // t < seen means a new banner was triggered while the texture stayed loaded.
            bannerSeenAtMs = bannerSeenAtMs < 0 ? t : 0;
        }
    }

    /**
     * Soft dark band: a solid core behind the name, smoothstep falloff to both sides drawn as
     * columns, and {@link #BAND_EDGE_ROWS} fading rows at the top and bottom of each column.
     */
    private static void drawBand(DrawContext ctx, int screenW, int nameW, int top, int bottom, float a) {
        if (a <= 0f || bottom - top <= 2 * BAND_EDGE_ROWS) return;
        int cx = screenW / 2;
        int halfW = Math.min((nameW + BAND_EXTRA_WIDTH) / 2, screenW / 2);
        int plateau = Math.max(0, Math.min(nameW / 2 + BAND_PLATEAU_EXTRA, halfW - BAND_MIN_FALLOFF));
        int falloff = halfW - plateau;
        float peak = BAND_PEAK_ALPHA * a;

        if (plateau > 0) bandColumn(ctx, cx - plateau, cx + plateau, top, bottom, peak);
        if (falloff <= 0) return;
        int colW = Math.max(2, (falloff + BAND_MAX_COLUMNS_PER_SIDE - 1) / BAND_MAX_COLUMNS_PER_SIDE);
        for (int d = 0; d < falloff; d += colW) {
            int w = Math.min(colW, falloff - d);
            float u = 1f - (d + w * 0.5f) / falloff;
            float colA = peak * smoothstep(u);
            if (ZoneBannerDraw.alpha(BAND_COLOR, colA) == 0) continue;
            bandColumn(ctx, cx + plateau + d, cx + plateau + d + w, top, bottom, colA);
            bandColumn(ctx, cx - plateau - d - w, cx - plateau - d, top, bottom, colA);
        }
    }

    private static void bandColumn(DrawContext ctx, int x1, int x2, int top, int bottom, float a) {
        int rows = BAND_EDGE_ROWS;
        for (int i = 0; i < rows; i++) {
            int c = ZoneBannerDraw.alpha(BAND_COLOR, a * (i + 1) / (rows + 1f));
            if (c == 0) continue;
            ctx.fill(x1, top + i, x2, top + i + 1, c);
            ctx.fill(x1, bottom - i - 1, x2, bottom - i, c);
        }
        int core = ZoneBannerDraw.alpha(BAND_COLOR, a);
        if (core != 0) ctx.fill(x1, top + rows, x2, bottom - rows, core);
    }

    /**
     * One gold rule starting at {@code inner} and growing in direction {@code dir} (-1 left, +1 right):
     * 1 px line with a 1 px shadow below, a shaded diamond at the outer end and an optional glint.
     */
    private static void drawRule(DrawContext ctx, int inner, int y, int len, int dir,
                                 float a, float diamondA, float glintP) {
        if (len <= 0 || a <= 0f) return;
        int x1 = dir > 0 ? inner : inner - len;
        int x2 = dir > 0 ? inner + len : inner;
        ctx.fill(x1, y, x2, y + 1, ZoneBannerDraw.alpha(GOLD_MID, a));
        ctx.fill(x1, y + 1, x2, y + 2, ZoneBannerDraw.alpha(GOLD_DARK, a));

        // Glint: a short bright segment travelling once from the inner to the outer end.
        if (glintP > 0f && glintP < 1f) {
            float travel = ZoneBannerDraw.easeInOutSine(glintP) * (len + GLINT_LENGTH) - GLINT_LENGTH;
            int g0 = Math.max(0, Math.round(travel));
            int g1 = Math.min(len, Math.round(travel) + GLINT_LENGTH);
            float glintA = a * (float) Math.sin(Math.PI * glintP);
            int c = ZoneBannerDraw.alpha(GOLD_LIGHT, glintA);
            if (g1 > g0 && c != 0) {
                if (dir > 0) ctx.fill(inner + g0, y, inner + g1, y + 1, c);
                else ctx.fill(inner - g1, y, inner - g0, y + 1, c);
            }
        }

        // Diamond tip whose inner column overlaps the line end.
        float da = diamondA * a;
        if (da <= 0f) return;
        int r = DIAMOND_RADIUS;
        int cx = dir > 0 ? inner + len + r - 1 : inner - len - r;
        int[] rowColors = {GOLD_LIGHT, GOLD_LIGHT, GOLD_MID, GOLD_MID_DARK, GOLD_DARK};
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            ctx.fill(cx - half, y + dy, cx + half + 1, y + dy + 1,
                    ZoneBannerDraw.alpha(rowColors[dy + r], da));
        }
        // Highlight pixel in the middle row for a faceted look.
        ctx.fill(cx - 1, y, cx, y + 1, ZoneBannerDraw.alpha(GOLD_LIGHT, da));
    }

    private static float smoothstep(float x) {
        float v = ZoneBannerDraw.clamp01(x);
        return v * v * (3f - 2f * v);
    }
}
