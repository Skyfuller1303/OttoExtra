package de.ottoextra.horses.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * Pixel dice in the slate style of the animal book, with ordinary button keyboard and narration
 * behaviour. No tooltip: vanilla tooltips draw their text with a shadow, the book has none.
 */
public final class AnimalNameDiceButton extends ButtonWidget {
    static final int WIDTH = 16;
    static final int HEIGHT = 14;
    private static final int[][] PIPS = {{4, 3}, {9, 3}, {6, 5}, {4, 8}, {9, 8}};

    public AnimalNameDiceButton(int x, int y, Runnable roll) {
        super(x, y, WIDTH, HEIGHT, net.minecraft.text.Text.translatable("ottoextra.horses.record.shuffle"),
                button -> roll.run(), DEFAULT_NARRATION_SUPPLIER);
    }

    @Override protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY();
        boolean hover = active && (isHovered() || isFocused());
        AnimalBook.slate(context, x, y, width, height, active ? AnimalBook.SLATE : AnimalBook.shade(AnimalBook.SLATE, 0.8),
                hover);
        int face = active ? AnimalBook.SLATE_T : 0xFF8A8F98;
        context.fill(x + 3, y + 2, x + 13, y + 12, face);
        for (int[] pip : PIPS) {
            context.fill(x + pip[0], y + pip[1], x + pip[0] + 2, y + pip[1] + 2, AnimalBook.INK);
        }
    }
}
