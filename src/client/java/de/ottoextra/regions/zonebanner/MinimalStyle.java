package de.ottoextra.regions.zonebanner;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Design D "Minimal": the zone name typed in letter by letter over a soft computed shadow field,
 * a thin gold underline that grows out of a small centered diamond, and a subtitle line with the
 * coat of arms. Everything is drawn with fills and text; no textures besides the coat of arms.
 */
final class MinimalStyle implements ZoneBannerStyle {

    // --- Palette ---------------------------------------------------------------------------------
    private static final int COLOR_NAME = 0xFFF0EBE0;
    private static final int COLOR_SUBTITLE = 0xFFD6C194;
    private static final int GOLD_DARK = 0xFF47391D;
    private static final int GOLD_MID_DARK = 0xFF866C36;
    private static final int GOLD_MID = 0xFFB58D36;
    private static final int GOLD_LIGHT = 0xFFDBB451;
    private static final int SHADOW_RGB = 0x141418;

    // --- Geometry (GUI pixels) -------------------------------------------------------------------
    /** Extra shadow field width on each side of the content. */
    private static final int SHADOW_PAD_X = 30;
    /** Extra shadow field height above and below the content; also the soft vertical ramp. */
    private static final int SHADOW_PAD_Y = 4;
    /** Peak opacity of the shadow field at its center. */
    private static final float SHADOW_PEAK = 0.35f;
    /** Horizontal segments per half of the shadow field (clamped); keeps the fill count bounded. */
    private static final int SHADOW_SEGMENTS_MIN = 8;
    private static final int SHADOW_SEGMENTS_MAX = 24;
    private static final int SHADOW_SEGMENT_PX = 4;
    /** Gap between the bottom of the name line box and the underline. */
    private static final int LINE_GAP = 2;
    /** The underline is this much wider than the name (total, both sides). */
    private static final int LINE_EXTRA_W = 20;
    /** Underline row plus its 1 px shadow row. */
    private static final int LINE_H = 2;
    /** Gap between the underline (incl. shadow) and the subtitle. */
    private static final int SUBTITLE_GAP = 4;
    /** Coat of arms edge length at subtitle scale 1. */
    private static final int ARMS_SIZE = 12;
    /** Gap between the coat of arms and the subtitle text at subtitle scale 1. */
    private static final int ARMS_GAP = 3;
    /** Minimum free space left and right of the name before the name scale is reduced. */
    private static final int SCREEN_MARGIN = 10;
    /** Letter drop distance at the start of its fade-in. */
    private static final int LETTER_DROP = 3;
    /** Subtitle rise distance during its fade-in. */
    private static final int SUBTITLE_RISE = 2;

    // --- Timeline (ms) ---------------------------------------------------------------------------
    private static final long SHADOW_IN_MS = 300;
    private static final long LETTERS_START_MS = 100;
    private static final long LETTER_STAGGER_MS = 40;
    private static final long LETTER_IN_MS = 180;
    /** Delay between the start of the last letter and the start of the underline phase. */
    private static final long LINE_DELAY_MS = 100;
    private static final long DIAMOND_IN_MS = 120;
    /** The line starts growing this long after the diamond starts appearing. */
    private static final long DIAMOND_LEAD_MS = 60;
    private static final long LINE_GROW_MS = 350;
    private static final long SUBTITLE_IN_MS = 300;
    private static final long ARMS_LATE_IN_MS = 300;

    private static final long EXIT_MS = 700;
    private static final long EXIT_LETTER_STAGGER_MS = 25;
    /** Maximum time between the first and last letter starting to fade out. */
    private static final long EXIT_LETTER_SPAN_MS = 300;
    private static final long EXIT_LETTER_OUT_MS = 200;
    private static final long EXIT_SUBTITLE_OUT_MS = 250;
    private static final long EXIT_LINE_START_MS = 150;
    private static final long EXIT_LINE_SHRINK_MS = 350;
    private static final long EXIT_DIAMOND_START_MS = 450;
    private static final long EXIT_DIAMOND_OUT_MS = 100;
    private static final long EXIT_SHADOW_START_MS = 400;

    // --- Per-banner state (render is only called from the render thread) ------------------------
    private String cachedName;
    /** Unscaled x offset of each code point inside the name; length = code point count. */
    private int[] letterOffsets = new int[0];
    private String[] letters = new String[0];

    private long lastElapsedMs = Long.MAX_VALUE;
    /** Elapsed time at which the coat of arms was first available for this banner, or -1. */
    private long bannerSeenAtMs = -1;

    @Override
    public String id() {
        return "minimal";
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, ZoneBannerFrame frame, long elapsedMs, long displayMs) {
        String name = frame.name() == null ? "" : frame.name();
        String subtitle = frame.subtitle() == null ? "" : frame.subtitle();
        boolean sticky = frame.sticky();
        long t = Math.max(0, elapsedMs);
        if (!sticky && t >= displayMs) {
            return;
        }

        // A new banner restarts the timer (or changes the name): reset the per-banner state.
        if (t < lastElapsedMs || !name.equals(cachedName)) {
            bannerSeenAtMs = -1;
        }
        lastElapsedMs = t;
        updateLetterCache(tr, name);
        if (frame.banner() != null && bannerSeenAtMs < 0) {
            bannerSeenAtMs = t;
        }

        // --- Layout ---
        int screenW = frame.screenW();
        int cx = screenW / 2;
        int nameScale = Math.max(1, Math.round(2 * frame.textScale()));
        int rawNameW = tr.getWidth(name);
        while (nameScale > 1 && rawNameW * nameScale > screenW - 2 * SCREEN_MARGIN) {
            nameScale--;
        }
        float subScale = Math.max(1f, frame.textScale());
        int rawSubW = tr.getWidth(subtitle);
        if (subScale > 1f && rawSubW * subScale > screenW - 2 * SCREEN_MARGIN) {
            subScale = 1f;
        }

        int nameW = rawNameW * nameScale;
        int nameH = tr.fontHeight * nameScale;
        int nameX = cx - nameW / 2;
        int top = frame.topMargin();
        int nameY = top + SHADOW_PAD_Y;
        int lineY = nameY + nameH + LINE_GAP;
        int lineHalf = (nameW + LINE_EXTRA_W) / 2;

        int subW = Math.round(rawSubW * subScale);
        int subH = Math.round(tr.fontHeight * subScale);
        int armsSize = Math.round(ARMS_SIZE * subScale);
        int armsGap = Math.round(ARMS_GAP * subScale);
        int subY = lineY + LINE_H + SUBTITLE_GAP;
        boolean hasArms = frame.banner() != null;
        boolean hasSubRow = !subtitle.isEmpty() || hasArms;

        // Arms that show up late fade in and push the subtitle sideways smoothly instead of jumping.
        long letterCount = letters.length;
        long lineStart = LETTERS_START_MS + LETTER_STAGGER_MS * Math.max(0, letterCount - 1) + LINE_DELAY_MS;
        long lineGrowStart = lineStart + DIAMOND_LEAD_MS;
        long subtitleStart = lineGrowStart + LINE_GROW_MS;
        float armsAppear = 0f;
        if (hasArms) {
            armsAppear = bannerSeenAtMs <= subtitleStart
                    ? 1f
                    : ZoneBannerDraw.easeOutCubic(ZoneBannerDraw.phase(t, bannerSeenAtMs, ARMS_LATE_IN_MS));
        }
        int armsSlot = subtitle.isEmpty() ? armsSize : armsSize + armsGap;
        int subRowW = subW + Math.round(armsSlot * armsAppear);

        int contentHalf = Math.max(lineHalf + 1, (hasSubRow ? subRowW : 0) / 2 + 1);
        int contentBottom = hasSubRow ? subY + subH : lineY + LINE_H;

        // --- Timeline factors ---
        long exitStart = displayMs - EXIT_MS;
        boolean exiting = !sticky && t >= exitStart;

        float shadowIn = ZoneBannerDraw.easeInOutSine(ZoneBannerDraw.phase(t, 0, SHADOW_IN_MS));
        float shadowOut = exiting ? 1f - ZoneBannerDraw.easeInOutSine(
                ZoneBannerDraw.phase(t, exitStart + EXIT_SHADOW_START_MS, EXIT_MS - EXIT_SHADOW_START_MS)) : 1f;

        float diamondIn = ZoneBannerDraw.phase(t, lineStart, DIAMOND_IN_MS);
        float diamondOut = exiting ? 1f - ZoneBannerDraw.phase(t, exitStart + EXIT_DIAMOND_START_MS, EXIT_DIAMOND_OUT_MS) : 1f;
        float lineGrow = ZoneBannerDraw.easeOutCubic(ZoneBannerDraw.phase(t, lineGrowStart, LINE_GROW_MS));
        float lineShrink = exiting ? ZoneBannerDraw.easeInOutSine(
                ZoneBannerDraw.phase(t, exitStart + EXIT_LINE_START_MS, EXIT_LINE_SHRINK_MS)) : 0f;

        float subIn = ZoneBannerDraw.phase(t, subtitleStart, SUBTITLE_IN_MS);
        float subOut = exiting ? 1f - ZoneBannerDraw.phase(t, exitStart, EXIT_SUBTITLE_OUT_MS) : 1f;

        // --- Draw ---
        drawShadowField(ctx, cx, top, contentHalf + SHADOW_PAD_X, nameY, contentBottom,
                SHADOW_PEAK * shadowIn * shadowOut);

        drawName(ctx, tr, name, nameX, nameY, nameScale, t, exiting, exitStart);

        int visibleHalf = Math.round(lineHalf * lineGrow * (1f - lineShrink));
        drawUnderline(ctx, cx, lineY, visibleHalf, diamondIn * diamondOut);

        if (hasSubRow) {
            float subAlpha = ZoneBannerDraw.easeInOutSine(subIn) * subOut;
            int rise = Math.round(SUBTITLE_RISE * (1f - ZoneBannerDraw.easeOutCubic(subIn)));
            int rowX = cx - subRowW / 2;
            int y = subY + rise;
            if (hasArms && armsAppear > 0f) {
                int armsY = y + (subH - 1 - armsSize) / 2;
                ZoneBannerDraw.banner(ctx, frame.banner(), rowX, armsY, armsSize, subAlpha * armsAppear);
            }
            int textX = rowX + Math.round(armsSlot * armsAppear);
            ZoneBannerDraw.text(ctx, tr, subtitle, textX, y, subScale,
                    ZoneBannerDraw.alpha(COLOR_SUBTITLE, subAlpha), true);
        }
    }

    /** Recomputes the per-letter layout when the name changes. */
    private void updateLetterCache(TextRenderer tr, String name) {
        if (name.equals(cachedName)) {
            return;
        }
        cachedName = name;
        int[] cps = name.codePoints().toArray();
        letters = new String[cps.length];
        letterOffsets = new int[cps.length];
        int charIndex = 0;
        for (int i = 0; i < cps.length; i++) {
            letterOffsets[i] = tr.getWidth(name.substring(0, charIndex));
            letters[i] = new String(Character.toChars(cps[i]));
            charIndex += Character.charCount(cps[i]);
        }
    }

    /** Typewriter reveal: each letter fades in and drops into place; on exit fades out left to right. */
    private void drawName(DrawContext ctx, TextRenderer tr, String name, int nameX, int nameY, int scale,
                          long t, boolean exiting, long exitStart) {
        int count = letters.length;
        if (count == 0) {
            return;
        }
        long lastLetterDone = LETTERS_START_MS + LETTER_STAGGER_MS * (count - 1) + LETTER_IN_MS;
        if (!exiting && t >= lastLetterDone) {
            // Fully shown: draw the word in one piece.
            ZoneBannerDraw.text(ctx, tr, name, nameX, nameY, scale, COLOR_NAME, true);
            return;
        }
        long exitStagger = count > 1
                ? Math.min(EXIT_LETTER_STAGGER_MS, EXIT_LETTER_SPAN_MS / (count - 1))
                : 0;
        for (int i = 0; i < count; i++) {
            String letter = letters[i];
            if (letter.isBlank()) {
                continue;
            }
            float in = ZoneBannerDraw.phase(t, LETTERS_START_MS + LETTER_STAGGER_MS * i, LETTER_IN_MS);
            float out = exiting ? 1f - ZoneBannerDraw.phase(t, exitStart + exitStagger * i, EXIT_LETTER_OUT_MS) : 1f;
            int color = ZoneBannerDraw.alpha(COLOR_NAME, ZoneBannerDraw.easeInOutSine(in) * out);
            if (color == 0) {
                continue;
            }
            int drop = Math.round(LETTER_DROP * (1f - ZoneBannerDraw.easeOutCubic(in)));
            ZoneBannerDraw.text(ctx, tr, letter, nameX + letterOffsets[i] * scale, nameY - drop, scale, color, true);
        }
    }

    /**
     * Gold underline centered on {@code cx}: 1 px line with 1 px dark shadow below, plus a 3 px wide
     * diamond on the center pixel.
     */
    private static void drawUnderline(DrawContext ctx, int cx, int lineY, int half, float diamondAlpha) {
        if (half > 0) {
            ctx.fill(cx - half, lineY, cx + half + 1, lineY + 1, GOLD_MID);
            ctx.fill(cx - half, lineY + 1, cx + half + 1, lineY + 2, GOLD_DARK);
        }
        if (diamondAlpha <= 0f) {
            return;
        }
        ctx.fill(cx, lineY - 1, cx + 1, lineY, fillAlpha(GOLD_LIGHT, diamondAlpha));
        ctx.fill(cx - 1, lineY, cx, lineY + 1, fillAlpha(GOLD_MID, diamondAlpha));
        ctx.fill(cx, lineY, cx + 1, lineY + 1, fillAlpha(GOLD_LIGHT, diamondAlpha));
        ctx.fill(cx + 1, lineY, cx + 2, lineY + 1, fillAlpha(GOLD_MID, diamondAlpha));
        ctx.fill(cx, lineY + 1, cx + 1, lineY + 2, fillAlpha(GOLD_MID_DARK, diamondAlpha));
        ctx.fill(cx, lineY + 2, cx + 1, lineY + 3, fillAlpha(GOLD_DARK, diamondAlpha));
    }

    /**
     * Soft dark field behind the text. Horizontally the opacity falls off from the center to 0 at
     * the outer edge (cosine curve); vertically it is flat over the content and ramps down over
     * {@link #SHADOW_PAD_Y} rows above and below. Drawn as non-overlapping segments, so at most
     * (2 * SHADOW_PAD_Y + 1) * (2 * SHADOW_SEGMENTS_MAX - 1) fills.
     */
    private static void drawShadowField(DrawContext ctx, int cx, int top, int halfW,
                                        int contentTop, int contentBottom, float peak) {
        if (peak <= 0f || halfW <= 0) {
            return;
        }
        int segments = Math.max(SHADOW_SEGMENTS_MIN, Math.min(SHADOW_SEGMENTS_MAX, halfW / SHADOW_SEGMENT_PX));
        int[] bounds = new int[segments + 1];
        float[] weights = new float[segments];
        for (int i = 0; i <= segments; i++) {
            bounds[i] = Math.round(halfW * (i / (float) segments));
        }
        for (int i = 0; i < segments; i++) {
            float u = (i + 0.5f) / segments;
            weights[i] = 1f - ZoneBannerDraw.easeInOutSine(u);
        }

        // Ramp rows above the content, the flat content band, ramp rows below.
        for (int row = 0; row < SHADOW_PAD_Y; row++) {
            float v = ZoneBannerDraw.easeInOutSine((row + 0.5f) / SHADOW_PAD_Y);
            int yTop = top + row;
            shadowRow(ctx, cx, yTop, yTop + 1, bounds, weights, peak * v);
            int yBottom = contentBottom + SHADOW_PAD_Y - 1 - row;
            shadowRow(ctx, cx, yBottom, yBottom + 1, bounds, weights, peak * v);
        }
        shadowRow(ctx, cx, Math.max(contentTop, top + SHADOW_PAD_Y), contentBottom, bounds, weights, peak);
    }

    private static void shadowRow(DrawContext ctx, int cx, int y1, int y2, int[] bounds, float[] weights, float peak) {
        if (y2 <= y1) {
            return;
        }
        // Center segment spans both sides of cx.
        int c = fillAlpha(0xFF000000 | SHADOW_RGB, peak * weights[0]);
        if (c != 0) {
            ctx.fill(cx - bounds[1], y1, cx + bounds[1], y2, c);
        }
        for (int i = 1; i < weights.length; i++) {
            int color = fillAlpha(0xFF000000 | SHADOW_RGB, peak * weights[i]);
            if (color == 0 || bounds[i + 1] <= bounds[i]) {
                continue;
            }
            ctx.fill(cx - bounds[i + 1], y1, cx - bounds[i], y2, color);
            ctx.fill(cx + bounds[i], y1, cx + bounds[i + 1], y2, color);
        }
    }

    /** Opacity for plain fills (no text quirk), rounded to the nearest alpha step. */
    private static int fillAlpha(int argb, float a) {
        int na = Math.round(((argb >>> 24) & 0xFF) * ZoneBannerDraw.clamp01(a));
        return na <= 0 ? 0 : (na << 24) | (argb & 0x00FFFFFF);
    }
}
