package de.ottoextra.horses.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Pixel dice with ordinary button keyboard and narration behavior. */
public final class AnimalNameDiceButton extends ButtonWidget {
    private static final int[][] PIPS = {{6, 5}, {12, 5}, {9, 8}, {6, 11}, {12, 11}};

    public AnimalNameDiceButton(int x, int y, Runnable roll) {
        super(x, y, 20, 18, net.minecraft.text.Text.translatable("ottoextra.horses.record.shuffle"),
                button -> roll.run(), DEFAULT_NARRATION_SUPPLIER);
        setTooltip(Tooltip.of(getMessage()));
    }

    @Override protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY();
        int gold = active ? 0xFFDAA84A : 0xFF777777;
        context.fill(x, y, x + width, y + height, gold);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1,
                isHovered() || isFocused() ? 0xFF304153 : 0xFF131B24);
        context.fill(x + 4, y + 3, x + 16, y + 15, gold);
        for (int[] pip : PIPS) {
            context.fill(x + pip[0], y + pip[1], x + pip[0] + 2, y + pip[1] + 2, 0xFF10171F);
        }
    }
}
