package de.ottoextra.resources;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Picker for a symbol of set 2, shared by the list editor and the marker editor: the twelve shapes
 * in two rows of six on the left, the six colour swatches in one row to the right of the first row.
 * Positions are relative to the top-left corner (x, y) of the first shape box; the labels "Symbol"
 * and "Farbe" sit 8 px above it. List pickers may add a thirteenth tile with the coat of arms of the
 * matching Gefolge, in the second row below the first swatch. While it is selected no shape is
 * marked and the swatches are dimmed, because the colour does not change the arms.
 */
final class SymbolPicker {
    static final int BOX = 18;
    static final int STEP = 20;
    static final int PER_ROW = 6;
    static final int SWATCH_STEP = 14;
    /** Height of the shape rows. */
    static final int HEIGHT = rows() * STEP - (STEP - BOX);

    private SymbolPicker() { }

    private static int rows() {
        return (ListMeta.ICONS.size() + PER_ROW - 1) / PER_ROW;
    }

    static int shapeX(int x, int index) {
        return x + (index % PER_ROW) * STEP;
    }

    static int shapeY(int y, int index) {
        return y + (index / PER_ROW) * STEP;
    }

    static int swatchX(int x, int index) {
        return x + PER_ROW * STEP + 8 + index * SWATCH_STEP;
    }

    /** Width of the whole picker. */
    static int width() {
        return swatchX(0, ListMeta.COLORS.size() - 1) + 13;
    }

    static int armsX(int x) {
        return swatchX(x, 0) - 1;
    }

    static int armsY(int y) {
        return y + STEP;
    }

    /** True if the mouse is over the coat-of-arms tile (only meaningful while it is shown). */
    static boolean armsAt(int x, int y, double mouseX, double mouseY) {
        int sx = armsX(x);
        int sy = armsY(y);
        return mouseX >= sx && mouseX < sx + BOX && mouseY >= sy && mouseY < sy + BOX;
    }

    /** Shape under the mouse, or null. */
    static String shapeAt(int x, int y, double mouseX, double mouseY) {
        for (int index = 0; index < ListMeta.ICONS.size(); index++) {
            int sx = shapeX(x, index);
            int sy = shapeY(y, index);
            if (mouseX >= sx && mouseX < sx + BOX && mouseY >= sy && mouseY < sy + BOX) {
                return ListMeta.ICONS.get(index);
            }
        }
        return null;
    }

    /** Colour under the mouse, or null. */
    static String colorAt(int x, int y, double mouseX, double mouseY) {
        for (int index = 0; index < ListMeta.COLORS.size(); index++) {
            int sx = swatchX(x, index);
            if (mouseX >= sx - 1 && mouseX < sx + 12 && mouseY >= y + 2 && mouseY < y + 15) {
                return ListMeta.COLORS.get(index);
            }
        }
        return null;
    }

    /** Labels, the shape boxes drawn in the chosen colour and the colour swatches; no text shadow. */
    static void draw(DrawContext context, TextRenderer font, int x, int y, String shape, String color) {
        draw(context, font, x, y, shape, color, null, false);
    }

    /**
     * Like {@link #draw(DrawContext, TextRenderer, int, int, String, String)} plus the coat-of-arms
     * tile when {@code arms} is not null; {@code armsSelected} marks it instead of the shape.
     */
    static void draw(DrawContext context, TextRenderer font, int x, int y, String shape, String color,
                     net.minecraft.util.Identifier arms, boolean armsSelected) {
        boolean armsOn = arms != null && armsSelected;
        ResourceBook.small(context, font, Text.translatable("ottoextra.resources.book.symbol"), x, y - 8,
                ResourceBook.INK_M);
        ResourceBook.small(context, font, Text.translatable("ottoextra.resources.book.color"), swatchX(x, 0) - 1,
                y - 8, ResourceBook.INK_M);
        for (int index = 0; index < ListMeta.ICONS.size(); index++) {
            String each = ListMeta.ICONS.get(index);
            boolean on = !armsOn && each.equals(shape);
            int sx = shapeX(x, index);
            int sy = shapeY(y, index);
            ResourceBook.rect(context, sx, sy, sx + BOX, sy + BOX, on ? ResourceBook.TERRA : ResourceBook.PA_D);
            ResourceBook.rect(context, sx + 1, sy + 1, sx + BOX - 1, sy + BOX - 1,
                    on ? ResourceBook.PA_L : ResourceBook.PA);
            ResourceListIcons.draw(context, each, color, sx + 1, sy + 1, 16);
        }
        for (int index = 0; index < ListMeta.COLORS.size(); index++) {
            String each = ListMeta.COLORS.get(index);
            int sx = swatchX(x, index);
            int swatch = ResourceListIcons.swatch(each);
            ResourceBook.rect(context, sx - 1, y + 2, sx + 12, y + 15,
                    each.equals(color) ? ResourceBook.TERRA : ResourceBook.PA);
            ResourceBook.rect(context, sx, y + 3, sx + 11, y + 14, swatch);
            ResourceBook.rect(context, sx, y + 3, sx + 11, y + 4, ResourceBook.shade(swatch, 1.3));
            if (armsOn) ResourceBook.rect(context, sx - 1, y + 2, sx + 12, y + 15, (ResourceBook.PA & 0xFFFFFF) | 0xA0000000);
        }
        if (arms != null) {
            int sx = armsX(x);
            int sy = armsY(y);
            ResourceBook.rect(context, sx, sy, sx + BOX, sy + BOX, armsOn ? ResourceBook.TERRA : ResourceBook.PA_D);
            ResourceBook.rect(context, sx + 1, sy + 1, sx + BOX - 1, sy + BOX - 1,
                    armsOn ? ResourceBook.PA_L : ResourceBook.PA);
            ResourceListIcons.drawArms(context, arms, sx + 1, sy + 1, 16);
        }
    }
}
