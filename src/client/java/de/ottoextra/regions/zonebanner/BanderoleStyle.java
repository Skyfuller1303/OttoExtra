package de.ottoextra.regions.zonebanner;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Design B "Banderole": a dark cloth ribbon that unfurls from the center, with notched tails
 * sliding out from behind its ends, gold trim lines, the coat of arms pinned behind the top
 * edge and an ocker subtitle below. Everything is drawn with fills; geometry is recomputed
 * for the current animated width so outline and trim always stay crisp.
 */
final class BanderoleStyle implements ZoneBannerStyle {

    // Palette
    private static final int COLOR_NAME = 0xFFF0EBE0;
    private static final int COLOR_SUBTITLE = 0xFFD6C194;
    private static final int OUTLINE = 0xFF141418;
    private static final int CLOTH_DARK = 0xFF1E1E24;
    private static final int CLOTH = 0xFF2E2D35;
    private static final int CLOTH_LIGHT = 0xFF3B3A45;
    private static final int CLOTH_HIGHLIGHT = 0xFF514F59;

    // Geometry (GUI pixels): tunable at runtime, see BanderoleTuning and /ottoextra banderole.
    private static final int ARMS_OVERSHOOT = 2;

    // Timeline (ms)
    private static final long UNFURL_END = 450;
    private static final long TAILS_START = 350, TAILS_DUR = 250;
    private static final long NAME_START = 400, NAME_DUR = 300;
    private static final long ARMS_START = 500, ARMS_DUR = 350;
    private static final float ARMS_DROP_SPLIT = 0.6f; // share of the arms phase used for the fall
    private static final long SUB_START = 650, SUB_DUR = 300;
    private static final long INTRO_END = 950;
    private static final long IDLE_RAMP = 300;
    private static final double SWAY_PERIOD = 2400.0;
    private static final long EXIT_DUR = 650;
    private static final long EXIT_TEXT_DUR = 250;
    private static final long BANNER_LATE_FADE = 300;

    // Late-loading coat of arms: remember when it first appeared during the current display.
    private long lastElapsed = Long.MAX_VALUE;
    private Identifier lastBanner;
    private long bannerAppearMs;

    @Override
    public String id() {
        return "banderole";
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, ZoneBannerFrame frame, long elapsedMs, long displayMs) {
        long t = Math.max(0, elapsedMs);
        trackBanner(frame.banner(), t);
        if (!frame.sticky() && t >= displayMs) return;

        // Exit phases: text/arms fade first, then the band rolls back to the center.
        float exitText = 0f;
        float roll = 0f;
        if (!frame.sticky()) {
            long exitStart = displayMs - EXIT_DUR;
            exitText = ZoneBannerDraw.phase(t, exitStart, EXIT_TEXT_DUR);
            roll = ZoneBannerDraw.phase(t, exitStart + EXIT_TEXT_DUR, EXIT_DUR - EXIT_TEXT_DUR);
        }

        // Default 1.35x the text scale (see BanderoleTuning.NAME_SCALE).
        float nameScale = Math.max(0.5f, BanderoleTuning.NAME_SCALE.value() * frame.textScale());
        float subScale = Math.max(0.5f, BanderoleTuning.SUB_SCALE.value() * frame.textScale());

        // Layout; clamp the ribbon so band plus tails fit on narrow screens.
        int minW = BanderoleTuning.MIN_WIDTH.intValue();
        int padX = BanderoleTuning.PAD_X.intValue();
        int tailW = BanderoleTuning.TAIL_WIDTH.intValue();
        int maxBandW = Math.max(minW, frame.screenW() - 2 * (tailW + BanderoleTuning.SIDE_MARGIN.intValue()));
        String name = fitName(tr, frame.name(), nameScale, maxBandW - padX);
        int nameW = Math.round(tr.getWidth(name) * nameScale);
        int fullW = Math.max(minW, Math.min(maxBandW, nameW + padX));
        fullW += fullW & 1; // even, so both halves are equal
        // The name sits padTop px below the band's top outline; the glyph cell incl. descenders
        // and drop shadow keeps padBottom px above the bottom outline.
        int nameH = (int) Math.ceil(9 * nameScale);
        int padTop = BanderoleTuning.PAD_TOP.intValue();
        int bandH = 1 + padTop + nameH + BanderoleTuning.PAD_BOTTOM.intValue() + 1;
        int cx = frame.screenW() / 2;
        int top = frame.topMargin() + BanderoleTuning.OFFSET_Y.intValue();
        int bandY = top + BanderoleTuning.ARMS_SIZE.intValue() - BanderoleTuning.ARMS_OVERLAP.intValue();

        // Band width: unfurl in, roll back out.
        float widthF = ZoneBannerDraw.easeOutCubic(ZoneBannerDraw.phase(t, 0, UNFURL_END))
                * (1f - ZoneBannerDraw.easeInOutSine(roll));
        int halfW = Math.round(fullW * widthF / 2f);
        float bandA = 1f - roll;

        // Tails: slide out from behind the band ends, retract during the first half of the roll.
        float tailIn = ZoneBannerDraw.easeOutCubic(ZoneBannerDraw.phase(t, TAILS_START, TAILS_DUR));
        float tailOut = 1f - ZoneBannerDraw.easeOutCubic(Math.min(1f, roll * 2f));
        int tailLen = Math.round(tailW * tailIn * tailOut);
        float tailA = Math.min(ZoneBannerDraw.phase(t, TAILS_START, TAILS_DUR), bandA);

        // Idle sway, ramped in after the intro and out with the exit so the integer steps never jump.
        float swayAmp = frame.sticky() || exitText <= 0f
                ? ZoneBannerDraw.phase(t, INTRO_END, IDLE_RAMP)
                : Math.min(ZoneBannerDraw.phase(t, INTRO_END, IDLE_RAMP), 1f - exitText);
        double swayAngle = 2 * Math.PI * Math.max(0, t - INTRO_END) / SWAY_PERIOD;
        int swayLeft = (int) Math.round(Math.sin(swayAngle) * swayAmp);
        int swayRight = (int) Math.round(Math.sin(swayAngle + Math.PI / 2) * swayAmp);

        float textFade = 1f - exitText;

        if (halfW >= 3) {
            int left = cx - halfW;
            int right = cx + halfW;
            if (tailLen > 0 || tailA > 0f) {
                drawTail(ctx, left, -1, bandY, bandH, tailLen, swayLeft, tailA);
                drawTail(ctx, right, 1, bandY, bandH, tailLen, swayRight, tailA);
            }
            drawBand(ctx, left, right, bandY, bandH, bandA);
        }

        // Coat of arms in front of the band: it rests on the upper edge, overlapping it slightly.
        if (frame.banner() != null) {
            float armsP = ZoneBannerDraw.phase(t, ARMS_START, ARMS_DUR);
            float lateFade = ZoneBannerDraw.phase(t, bannerAppearMs, BANNER_LATE_FADE);
            float armsA = Math.min(ZoneBannerDraw.phase(t, ARMS_START, (long) (ARMS_DUR * ARMS_DROP_SPLIT)), lateFade)
                    * textFade;
            int armsSize = BanderoleTuning.ARMS_SIZE.intValue();
            ZoneBannerDraw.banner(ctx, frame.banner(), cx - armsSize / 2, top + armsOffset(armsP), armsSize, armsA);
        }

        // Name centered on the band; reveal by fade only.
        float nameA = ZoneBannerDraw.phase(t, NAME_START, NAME_DUR) * textFade;
        int nameX = cx - nameW / 2;
        int nameY = bandY + 1 + padTop;
        ZoneBannerDraw.text(ctx, tr, name, nameX, nameY, nameScale,
                ZoneBannerDraw.alpha(COLOR_NAME, nameA), true);

        String subtitle = frame.subtitle();
        if (subtitle != null && !subtitle.isEmpty()) {
            float subA = ZoneBannerDraw.phase(t, SUB_START, SUB_DUR) * textFade;
            int subW = Math.round(tr.getWidth(subtitle) * subScale);
            int subX = cx - subW / 2;
            int subY = bandY + bandH + BanderoleTuning.SUBTITLE_GAP.intValue();
            ZoneBannerDraw.text(ctx, tr, subtitle, subX, subY, subScale,
                    ZoneBannerDraw.alpha(COLOR_SUBTITLE, subA), true);
        }
    }

    /** Resets per-display state on a new trigger and records when a late banner shows up. */
    private void trackBanner(Identifier banner, long t) {
        if (t < lastElapsed) {
            // New display started (elapsed went backwards): arms arriving now use the normal intro.
            lastBanner = null;
            bannerAppearMs = 0;
        }
        if (banner != null && !banner.equals(lastBanner)) {
            bannerAppearMs = t < ARMS_START ? 0 : t;
        }
        lastBanner = banner;
        lastElapsed = t;
    }

    /** Vertical offset of the arms: fall from above, overshoot slightly, then settle at 0. */
    private static int armsOffset(float p) {
        if (p < ARMS_DROP_SPLIT) {
            int drop = BanderoleTuning.ARMS_DROP.intValue();
            float fall = ZoneBannerDraw.easeOutCubic(p / ARMS_DROP_SPLIT);
            return Math.round(-drop + (drop + ARMS_OVERSHOOT) * fall);
        }
        float settle = ZoneBannerDraw.easeInOutSine((p - ARMS_DROP_SPLIT) / (1f - ARMS_DROP_SPLIT));
        return Math.round(ARMS_OVERSHOOT * (1f - settle));
    }

    /** Trims the name with "..." if it would not fit into the clamped band. */
    private static String fitName(TextRenderer tr, String name, float scale, int maxPx) {
        if (name == null) return "";
        int maxUnscaled = Math.max(0, (int) (maxPx / scale));
        if (tr.getWidth(name) <= maxUnscaled) return name;
        String ellipsis = "...";
        int room = Math.max(0, maxUnscaled - tr.getWidth(ellipsis));
        return tr.trimToWidth(name, room) + ellipsis;
    }

    /** The cloth band between {@code left} (inclusive) and {@code right} (exclusive). */
    private static void drawBand(DrawContext ctx, int left, int right, int y, int h, float a) {
        int bottom = y + h;
        // Outline with omitted corner pixels.
        fill(ctx, left + 1, y, right - 1, y + 1, OUTLINE, a);
        fill(ctx, left + 1, bottom - 1, right - 1, bottom, OUTLINE, a);
        fill(ctx, left, y + 1, left + 1, bottom - 1, OUTLINE, a);
        fill(ctx, right - 1, y + 1, right, bottom - 1, OUTLINE, a);

        // Vertical cloth shading.
        int inL = left + 1;
        int inR = right - 1;
        fill(ctx, inL, y + 1, inR, y + 2, CLOTH_HIGHLIGHT, a);
        fill(ctx, inL, y + 2, inR, y + 3, CLOTH_LIGHT, a);
        fill(ctx, inL, y + 3, inR, bottom - 3, CLOTH, a);
        fill(ctx, inL, bottom - 3, inR, bottom - 1, CLOTH_DARK, a);

    }

    /**
     * One tail. Coordinates are expressed as {@code u}, the distance outward from the band end:
     * {@code u >= 0} lies outside the band, negative {@code u} is tucked under it.
     *
     * @param edge  band edge x: left edge (inclusive) for dir -1, right edge (exclusive) for dir +1
     * @param len   currently visible tail length beyond the band end
     */
    private static void drawTail(DrawContext ctx, int edge, int dir, int bandY, int h,
                                 int len, int sway, float a) {
        if (ZoneBannerDraw.alpha(0xFF000000, a) == 0) return;
        int tuck = BanderoleTuning.TAIL_TUCK.intValue();
        int notchDepth = BanderoleTuning.TAIL_NOTCH.intValue();
        int fold = BanderoleTuning.FOLD.intValue();
        int tailTop = bandY + BanderoleTuning.TAIL_DROP.intValue() + sway;
        int bandBottom = bandY + h;
        int mid2 = h - 1; // twice the center row index, for a symmetric notch
        for (int r = 0; r < h; r++) {
            int y = tailTop + r;
            boolean underBand = y < bandBottom;
            int notch = Math.round(notchDepth * (1f - Math.abs(2 * r - mid2) / (float) mid2));
            int uOuter = len - notch;                  // exclusive outer limit of this row
            // Under the band the tail starts one pixel inside it, so the band's omitted corner
            // pixel shows the tail outline instead of the sky.
            int uInner = underBand ? -1 : -tuck;
            if (uOuter <= uInner) continue;
            if (r == 0 || r == h - 1) {
                span(ctx, edge, dir, y, uInner, uOuter, OUTLINE, a);
                continue;
            }
            span(ctx, edge, dir, y, uInner, uOuter - 1, CLOTH_DARK, a);
            span(ctx, edge, dir, y, uOuter - 1, uOuter, OUTLINE, a);
            if (underBand) {
                span(ctx, edge, dir, y, -1, 0, OUTLINE, a);
            } else {
                span(ctx, edge, dir, y, -tuck, -tuck + 1, OUTLINE, a);
                int i = y - bandBottom;
                if (i < fold) {
                    // Fold shadow right under the band end.
                    span(ctx, edge, dir, y, -(fold - i), 0, OUTLINE, a);
                }
            }
        }
    }

    /** Fills the half-open u-range [u0, u1) of one row, mirrored according to {@code dir}. */
    private static void span(DrawContext ctx, int edge, int dir, int y, int u0, int u1, int color, float a) {
        if (u1 <= u0) return;
        if (dir < 0) {
            fill(ctx, edge - u1, y, edge - u0, y + 1, color, a);
        } else {
            fill(ctx, edge + u0, y, edge + u1, y + 1, color, a);
        }
    }

    private static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int color, float a) {
        if (x2 <= x1 || y2 <= y1) return;
        int c = ZoneBannerDraw.alpha(color, a);
        if (c == 0) return;
        ctx.fill(x1, y1, x2, y2, c);
    }
}
