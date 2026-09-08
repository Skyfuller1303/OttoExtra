package de.ottoextra.welcome;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Shared navy and gold styling for every page of first-run setup. */
final class WelcomeStyle {
    static final int GOLD = 0xFFE0C18A;
    static final int MUTED = 0xFFA5B6CE;
    static final int TEXT = 0xFFE9EFF8;
    static final int BACKGROUND = 0xFF10171F;
    static final int CARD = 0xFF131B24;
    static final int BORDER = 0xFF293746;
    static final int INSET = BACKGROUND;

    private WelcomeStyle() { }

    static void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
        ctx.fill(x, y, x + w, y + h, BACKGROUND);
        ctx.fill(x, y, x + w, y + 2, GOLD);
        ctx.fill(x + 12, y + h - 40, x + w - 12, y + h - 39, BORDER);
    }

    static class Button extends ButtonWidget {
        private final boolean accent;

        Button(int x, int y, int w, int h, net.minecraft.text.Text label, boolean accent, PressAction action) {
            super(x, y, w, h, label, action, DEFAULT_NARRATION_SUPPLIER);
            this.accent = accent;
        }

        void drawSurface(DrawContext ctx) {
            boolean focus = isHovered() || isFocused();
            int border = !active ? 0xFF202A36 : focus ? GOLD : accent ? 0xFFA88B53 : BORDER;
            int fill = !active ? BACKGROUND : focus ? 0xFF1C2835 : CARD;
            ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
            ctx.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, fill);
        }

        @Override
        protected void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
            drawSurface(ctx);
            drawLabel(ctx.getHoverListener(this, DrawContext.HoverType.NONE));
        }
    }
}
