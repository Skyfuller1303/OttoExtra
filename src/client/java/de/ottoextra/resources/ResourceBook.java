package de.ottoextra.resources;

import de.ottoextra.ui.theme.OttonienTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * Book look shared by the addon screens: slate-blue cover with two parchment pages. Colours and
 * geometry follow the design sketches (docs/design, generator gen_vorkommen_buch.py); no text
 * in the book has a shadow.
 */
final class ResourceBook {
    // Parchment and ink of the sketches.
    static final int PA = 0xFFC9A97C;
    static final int PA_L = 0xFFD8BC92;
    static final int PA_D = 0xFFB08F63;
    static final int PA_S = 0xFFBF9E70;
    static final int INK = 0xFF35281C;
    static final int INK_M = 0xFF6E5A44;
    static final int INK_F = 0xFF8E785E;
    static final int TERRA = 0xFF9E4B32;
    static final int BRASS = 0xFF866C36;
    static final int OK_INK = 0xFF3F6B2C;
    static final int WARN_INK = 0xFF8A6410;
    static final int SOON_INK = 0xFFA0491E;
    // Slate buttons.
    static final int SLATE = 0xFF2E3A4A;
    static final int SLATE_T = 0xFFE8E2D6;
    static final int PRIMARY = 0xFF35552A;
    static final int DANGER = 0xFF6A2E26;
    private static final int HOVER_OUTLINE = 0xFFE2A650;

    /** Scale of the small caption font of the sketches (3 of 4 pixels). */
    static final float SMALL = 0.75f;

    private static final int SPINE = 10;
    private static final int EDGE = 5;

    /** Parchment area inside the cover. */
    record Page(int x, int y, int w, int h) { }

    private ResourceBook() { }

    /** Page bounds of a book of at most maxW x maxH centred on the screen. */
    static Page bounds(int screenWidth, int screenHeight, int maxW, int maxH) {
        int w = Math.min(maxW, screenWidth - 8);
        int h = Math.min(maxH, screenHeight - 8);
        int x = (screenWidth - w) / 2;
        int y = (screenHeight - h) / 2;
        return new Page(x + SPINE, y + EDGE, w - SPINE - EDGE, h - 2 * EDGE);
    }

    /** The two pages of a double-page book. */
    record Spread(Page left, Page right) { }

    /**
     * Double page of at most maxW x maxH centred on the screen: a narrow left page, a gap with the
     * cover showing through, and a wide right page.
     */
    static Spread spread(int screenWidth, int screenHeight, int maxW, int maxH) {
        Page whole = bounds(screenWidth, screenHeight, maxW, maxH);
        int gap = 4;
        int leftW = Math.min(141, whole.w() * 34 / 100);
        Page left = new Page(whole.x(), whole.y(), leftW, whole.h());
        Page right = new Page(whole.x() + leftW + gap, whole.y(), whole.w() - leftW - gap, whole.h());
        return new Spread(left, right);
    }

    /** Dimmed background, cover and two parchment pages. */
    static void drawSpread(DrawContext context, int screenWidth, int screenHeight, Spread spread) {
        context.fill(0, 0, screenWidth, screenHeight, 0xB0101010);
        Page l = spread.left();
        Page r = spread.right();
        cover(context, l.x() - SPINE, l.y() - EDGE, r.x() + r.w() + EDGE - (l.x() - SPINE), l.h() + 2 * EDGE);
        page(context, l, true);
        page(context, r, false);
    }

    /** Parchment page of the sketch: dark edge, light top line, shaded strip at the spine side. */
    private static void page(DrawContext context, Page p, boolean left) {
        rect(context, p.x(), p.y(), p.x() + p.w(), p.y() + p.h(), PA_D);
        rect(context, p.x() + 1, p.y() + 1, p.x() + p.w() - 1, p.y() + p.h() - 1, PA);
        rect(context, p.x() + 1, p.y() + 1, p.x() + p.w() - 1, p.y() + 2, PA_L);
        if (left) rect(context, p.x() + p.w() - 4, p.y() + 1, p.x() + p.w() - 1, p.y() + p.h() - 1, PA_S);
        else rect(context, p.x() + 1, p.y() + 1, p.x() + 4, p.y() + p.h() - 1, PA_S);
    }

    /** Book cover ramp, dark to light (grey-blue leather). */
    private static final int[] COVER = {0xFF141A21, 0xFF222B36, 0xFF313C49, 0xFF3B4756, 0xFF4F5C6D};
    /** Brass studs, same tones as the theme's gold ramp. */
    private static final int[] STUDS = {0xFF47391D, 0xFF866C36, 0xFFB58D36, 0xFFDBB451};

    /**
     * Cover with the geometry and shading steps of the theme's book (leather edge, spine with
     * bands, inner lines, two studs), in the addon's own colour ramp.
     */
    static void cover(DrawContext ctx, int bx, int by, int bw, int bh) {
        int[] ramp = COVER;
        OttonienTheme.rect(ctx, bx - 1, by - 1, bx + bw + 1, by + bh + 1, ramp[0]);
        OttonienTheme.rect(ctx, bx, by, bx + bw, by + bh, ramp[2]);
        OttonienTheme.rect(ctx, bx, by, bx + bw, by + 1, ramp[3]);
        OttonienTheme.rect(ctx, bx, by, bx + 1, by + bh, ramp[3]);
        OttonienTheme.rect(ctx, bx, by + bh - 1, bx + bw, by + bh, ramp[1]);
        OttonienTheme.rect(ctx, bx, by, bx + 7, by + bh, ramp[1]);
        for (int i = by + 10; i < by + bh - 8; i += 22) {
            OttonienTheme.rect(ctx, bx + 1, i, bx + 6, i + 2, ramp[3]);
            OttonienTheme.rect(ctx, bx + 1, i + 2, bx + 6, i + 3, ramp[0]);
        }
        OttonienTheme.rect(ctx, bx + 9, by + 2, bx + bw - 2, by + 3, ramp[1]);
        OttonienTheme.rect(ctx, bx + 9, by + bh - 3, bx + bw - 2, by + bh - 2, ramp[3]);
        int[][] studs = {{bx + bw - 6, by + 3}, {bx + bw - 6, by + bh - 6}};
        for (int[] s : studs) {
            OttonienTheme.rect(ctx, s[0], s[1], s[0] + 3, s[1] + 3, STUDS[1]);
            OttonienTheme.rect(ctx, s[0], s[1], s[0] + 1, s[1] + 1, STUDS[3]);
            OttonienTheme.rect(ctx, s[0] + 2, s[1] + 2, s[0] + 3, s[1] + 3, STUDS[0]);
        }
    }

    // ---- drawing helpers -------------------------------------------------------------

    static void rect(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        context.fill(x0, y0, x1, y1, color);
    }

    /** One-pixel line from x0 to x1 in the given parchment shade. */
    static void rule(DrawContext context, int x0, int x1, int y, int color) {
        rect(context, x0, y, x1, y + 1, color);
    }

    /** One-pixel line in the dark parchment shade. */
    static void rule(DrawContext context, int x, int y, int w) {
        rule(context, x, x + w, y, PA_D);
    }

    /** Colour times a factor per channel, truncated and clamped, opaque (shade() of the sketches). */
    static int shade(int color, double factor) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static void text(DrawContext context, TextRenderer font, String text, int x, int y, int color) {
        context.drawText(font, text, x, y, color, false);
    }

    static void text(DrawContext context, TextRenderer font, Text text, int x, int y, int color) {
        context.drawText(font, text, x, y, color, false);
    }

    /** Caption text at three quarters of the normal size. */
    static void small(DrawContext context, TextRenderer font, String text, int x, int y, int color) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate((float) x, (float) y);
        matrices.scale(SMALL, SMALL);
        context.drawText(font, text, 0, 0, color, false);
        matrices.popMatrix();
    }

    static void small(DrawContext context, TextRenderer font, Text text, int x, int y, int color) {
        small(context, font, text.getString(), x, y, color);
    }

    /** Width of a caption in GUI pixels. */
    static int smallWidth(TextRenderer font, String text) {
        return Math.round(font.getWidth(text) * SMALL);
    }

    /** Caption right-aligned to {@code right}. */
    static void smallRight(DrawContext context, TextRenderer font, String text, int right, int y, int color) {
        small(context, font, text, right - smallWidth(font, text), y, color);
    }

    /** Wrapped caption lines, {@code step} pixels apart; returns the y below the last line. */
    static int smallWrapped(DrawContext context, TextRenderer font, Text text, int x, int y, int width, int step,
                            int color) {
        List<OrderedText> lines = font.wrapLines(text, Math.round(width / SMALL));
        for (OrderedText line : lines) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate((float) x, (float) y);
            matrices.scale(SMALL, SMALL);
            context.drawText(font, line, 0, 0, color, false);
            matrices.popMatrix();
            y += step;
        }
        return y;
    }

    /** Text shortened with an ellipsis to fit {@code maxWidth}. */
    static String clip(TextRenderer font, String text, int maxWidth) {
        if (font.getWidth(text) <= maxWidth) return text;
        return font.trimToWidth(text, Math.max(0, maxWidth - font.getWidth("…"))) + "…";
    }

    /** Caption shortened with an ellipsis to fit {@code maxWidth} GUI pixels at the small size. */
    static String clipSmall(TextRenderer font, String text, int maxWidth) {
        return clip(font, text, Math.round(maxWidth / SMALL));
    }

    /** Slate plate of the sketches: dark outline, fill, light top and dark bottom line. */
    static void slate(DrawContext context, int x, int y, int w, int h, int fill, boolean hover) {
        rect(context, x, y, x + w, y + h, hover ? HOVER_OUTLINE : COVER[0]);
        rect(context, x + 1, y + 1, x + w - 1, y + h - 1, fill);
        rect(context, x + 1, y + 1, x + w - 1, y + 2, shade(fill, 1.3));
        rect(context, x + 1, y + h - 2, x + w - 1, y + h - 1, shade(fill, 0.7));
    }

    /** Selection tile of the sketches: terracotta edge and light fill when selected. */
    static void chip(DrawContext context, int x, int y, int w, int h, boolean selected, boolean hover) {
        rect(context, x, y, x + w, y + h, selected ? TERRA : hover ? BRASS : PA_D);
        rect(context, x + 1, y + 1, x + w - 1, y + h - 1, selected ? PA_L : PA);
    }

    /** 9x9 check box of the sketches. */
    static void checkbox(DrawContext context, int x, int y, boolean checked) {
        rect(context, x, y, x + 9, y + 9, INK_M);
        rect(context, x + 1, y + 1, x + 8, y + 8, PA_L);
        if (!checked) return;
        int[][] tick = {{2, 4}, {3, 5}, {4, 6}, {5, 5}, {6, 4}, {7, 3}};
        for (int[] p : tick) rect(context, x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, INK);
    }

    /** Button width: the sketch width, or wider when the label needs it. */
    static int buttonWidth(TextRenderer font, Text label, int sketchWidth) {
        return Math.max(sketchWidth, font.getWidth(label) + 10);
    }

    // ---- timers ------------------------------------------------------------------------

    /** Minutes until full, rounded up; 0 when ready. */
    static int minutesLeft(ResourceSnapshot row) {
        if ("READY".equals(row.status())) return 0;
        return Math.max(1, (Math.max(0, row.secondsUntilFull()) + 59) / 60);
    }

    /** "bereit", "8 min" or "1:14 h" as in the sketches. */
    static String timeText(ResourceSnapshot row) {
        int minutes = minutesLeft(row);
        if (minutes <= 0) return Text.translatable("ottoextra.resources.hud.ready").getString();
        return clockText(minutes);
    }

    /** "8 min" below an hour, "1:14 h" from an hour on. */
    static String clockText(int minutes) {
        if (minutes < 60) return minutes + " min";
        return minutes / 60 + ":" + (minutes % 60 < 10 ? "0" : "") + minutes % 60 + " h";
    }

    /** Ink of the time on parchment: green ready, rust within ten minutes, amber otherwise. */
    static int timeInk(ResourceSnapshot row) {
        int minutes = minutesLeft(row);
        return minutes <= 0 ? OK_INK : minutes <= 10 ? SOON_INK : WARN_INK;
    }

    /** Recovered share 0..1; ready rows are full. */
    static double progress(ResourceSnapshot row) {
        if ("READY".equals(row.status())) return 1.0;
        if (row.totalSeconds() <= 0) return 0.0;
        return 1.0 - Math.min(1.0, Math.max(0, row.secondsUntilFull()) / (double) row.totalSeconds());
    }

    // ---- widgets -----------------------------------------------------------------------
    // Own widget classes so the screens never call inherited Minecraft members through a type
    // of the core mod. They extend Minecraft classes directly and are remapped with the addon.

    enum Tone { SLATE, PRIMARY, DANGER }

    /** Slate button of the sketches: 14 px high, light label, no text shadow. */
    static final class Button extends ButtonWidget {
        private final Tone tone;

        Button(int x, int y, int width, net.minecraft.text.Text message, Tone tone, PressAction onPress) {
            this(x, y, width, 14, message, tone, onPress);
        }

        /** Slate button of any height, e.g. 20 px in the screens outside the book. */
        Button(int x, int y, int width, int height, net.minecraft.text.Text message, Tone tone,
               PressAction onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.tone = tone;
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            boolean hover = active && isHovered();
            int fill = switch (tone) {
                case PRIMARY -> PRIMARY;
                case DANGER -> DANGER;
                default -> SLATE;
            };
            slate(context, getX(), getY(), getWidth(), getHeight(), fill, hover);
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            net.minecraft.text.Text label = getMessage();
            int color = active ? SLATE_T : 0xFF8A8F98;
            context.drawText(font, label, getX() + (getWidth() - font.getWidth(label)) / 2,
                    getY() + (getHeight() - 8) / 2, color, false);
        }
    }

    /**
     * Ink field of the sketches: text on the parchment with a line below, terracotta while
     * focused, hint in faint ink. The visible box is (x, y, width, 13); text starts 2 px in.
     */
    /**
     * Text field of the sketches: no background, an underline, ink colours and no text shadow.
     * Vanilla {@code TextFieldWidget.renderWidget} (1.21.11) draws the text, the suggestion and the
     * "_" cursor with its {@code textShadow} flag, which is switched off here, and the selection as a
     * plain highlight; only the placeholder is always drawn with a shadow. So the field never hands
     * its hint to vanilla and draws it itself without shadow, at the spot where vanilla would
     * (text origin = widget origin without background). Scrolling, selection and cursor stay vanilla.
     */
    static final class Field extends TextFieldWidget {
        private final TextRenderer font;
        private final net.minecraft.text.Text hint;

        Field(TextRenderer font, int x, int y, int width, net.minecraft.text.Text hint) {
            super(font, x + 2, y, Math.max(8, width - 4), 13, hint);
            this.font = font;
            this.hint = hint;
            setDrawsBackground(false);
            setTextShadow(false);
            setEditableColor(INK);
            setUneditableColor(INK_M);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            if (!isVisible()) return;
            rect(context, getX() - 2, getY() + 12, getX() + getWidth() + 2, getY() + 13,
                    isFocused() ? TERRA : INK_M);
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(0f, 2f);
            super.renderWidget(context, mouseX, mouseY, deltaTicks);
            if (getText().isEmpty() && !isFocused()) {
                context.drawText(font, clip(font, hint.getString(), getWidth()), getX(), getY(), INK_F, false);
            }
            matrices.popMatrix();
        }
    }

    /** Check box of the sketches with its label; the whole line is clickable. */
    static final class Toggle extends ButtonWidget {
        private boolean checked;
        private final int labelColor;
        private final Consumer<Boolean> onChange;

        Toggle(int x, int y, int width, net.minecraft.text.Text label, boolean checked, int labelColor,
               Consumer<Boolean> onChange) {
            super(x, y, width, 11, label, button -> { }, DEFAULT_NARRATION_SUPPLIER);
            this.checked = checked;
            this.labelColor = labelColor;
            this.onChange = onChange;
        }

        @Override
        public void onPress(net.minecraft.client.input.AbstractInput input) {
            checked = !checked;
            onChange.accept(checked);
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            checkbox(context, getX(), getY() + 1, checked);
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            context.drawText(font, getMessage(), getX() + 14, getY() + 2,
                    !active ? INK_F : isHovered() ? INK : labelColor, false);
        }
    }
}
