package de.ottoextra.letter.format;

import net.minecraft.client.font.TextRenderer;
import de.ottoextra.ui.theme.OttonienTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

/** Tintensäule links vom Buch: 16 Farbtropfen, Stilkacheln B I U S und die breite Kachel Tx. */
public final class LetterFormattingSidebar {

    private static final int INK = OttonienTheme.PICTO;

    private static final char[] COLOR_CODES =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final int[] COLOR_RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final String[] COLOR_NAMES = {
            "Schwarz", "Dunkelblau", "Dunkelgrün", "Dunkelaqua", "Dunkelrot", "Dunkelviolett",
            "Gold", "Grau", "Dunkelgrau", "Blau", "Grün", "Aqua", "Rot", "Hellviolett",
            "Gelb", "Weiß"};

    private static final char CLEAR_FORMATTING = '\0';
    private static final char[] STYLE_CODES = {'l', 'o', 'n', 'm', CLEAR_FORMATTING};
    private static final String[] STYLE_LABELS = {"B", "I", "U", "S", "Tx"};
    private static final String[] STYLE_NAMES = {
            "Fett", "Kursiv", "Unterstrichen", "Durchgestrichen",
            "Formatierung aus Auswahl entfernen"};

    private static final int DROP_W = 12;
    private static final int DROP_H = 11;
    private static final int TILE = 13;
    private static final int PAD_X = 1;
    private static final int PAD_Y = 2;

    /** Breite der Säule inklusive 1 px Lederkante. */
    public static final int WIDTH = 2 * DROP_W + 2 * PAD_X;
    private static final int FRAME_H = 8 * DROP_H + 2 * PAD_Y;

    private final Consumer<String> insertCode;
    private final Runnable clearFormatting;

    private int x;
    private int y;
    private int h;

    private final int[] colorX = new int[16];
    private final int[] colorY = new int[16];
    private final int[] styleX = new int[STYLE_CODES.length];
    private final int[] styleY = new int[STYLE_CODES.length];
    private final int[] styleW = new int[STYLE_CODES.length];

    public LetterFormattingSidebar(Consumer<String> insertCode, Runnable clearFormatting) {
        this.insertCode = insertCode;
        this.clearFormatting = clearFormatting;
    }

    /** Gesamthöhe der Säule (unabhängig von der Position). */
    public static int height() {
        return FRAME_H + 3 + 2 * (TILE + 2) + TILE;
    }

    /** x, y = linke obere Ecke der Säule. */
    public void setBounds(int x, int y) {
        this.x = x;
        this.y = y;
        layout();
    }

    private void layout() {
        for (int i = 0; i < 16; i++) {
            colorX[i] = x + PAD_X + (i % 2) * DROP_W;
            colorY[i] = y + PAD_Y + (i / 2) * DROP_H;
        }
        int tilesTop = y + FRAME_H + 3;
        for (int i = 0; i < 4; i++) {
            styleX[i] = x + PAD_X + (i % 2) * DROP_W;
            styleY[i] = tilesTop + (i / 2) * (TILE + 2);
            styleW[i] = DROP_W;
        }
        styleX[4] = x + PAD_X;
        styleY[4] = tilesTop + 2 * (TILE + 2);
        styleW[4] = 2 * DROP_W;
        h = styleY[4] + TILE - y;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        ctx.fill(x, y, x + WIDTH, y + FRAME_H, 0xFF2E0F06);
        OttonienTheme.parchment(ctx, x + 1, y + 1, WIDTH - 2, FRAME_H - 2, 3, false, false);

        for (int i = 0; i < 16; i++) {
            boolean hovered = hit(mouseX, mouseY, colorX[i], colorY[i], DROP_W, DROP_H);
            if (hovered) {
                ctx.fill(colorX[i] + 1, colorY[i] + 1, colorX[i] + DROP_W - 1,
                        colorY[i] + DROP_H - 1, 0x55FFFFFF);
            }
            OttonienTheme.inkDrop(ctx, colorX[i] + 6, colorY[i] + 5, COLOR_RGB[i]);
        }

        for (int i = 0; i < STYLE_CODES.length; i++) {
            boolean hovered = hit(mouseX, mouseY, styleX[i], styleY[i], styleW[i], TILE);
            OttonienTheme.tile(ctx, styleX[i], styleY[i], styleW[i], TILE, hovered);
            String label = STYLE_CODES[i] == CLEAR_FORMATTING
                    ? STYLE_LABELS[i]
                    : "§" + STYLE_CODES[i] + STYLE_LABELS[i] + "§r";
            int lw = tr.getWidth(label);
            ctx.drawText(tr, label, styleX[i] + (styleW[i] - lw + 1) / 2, styleY[i] + 3, INK, false);
        }

        renderTooltips(ctx, tr, mouseX, mouseY);
    }

    private void renderTooltips(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        for (int i = 0; i < 16; i++) {
            if (hit(mouseX, mouseY, colorX[i], colorY[i], DROP_W, DROP_H)) {
                ctx.drawTooltip(tr, List.of(
                        Text.literal("§" + COLOR_CODES[i] + COLOR_NAMES[i]),
                        Text.translatable("ottoextra.letter.fmt.code", "§" + COLOR_CODES[i],
                                "&" + COLOR_CODES[i]),
                        Text.translatable("ottoextra.letter.fmt.colorHint")), mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < STYLE_CODES.length; i++) {
            if (hit(mouseX, mouseY, styleX[i], styleY[i], styleW[i], TILE)) {
                if (STYLE_CODES[i] == CLEAR_FORMATTING) {
                    ctx.drawTooltip(tr, List.of(
                            Text.translatable("ottoextra.letter.fmt.clearSelection"),
                            Text.translatable("ottoextra.letter.fmt.clearHint")), mouseX, mouseY);
                } else {
                    ctx.drawTooltip(tr, List.of(
                            Text.literal(STYLE_NAMES[i]),
                            Text.translatable("ottoextra.letter.fmt.code", "§" + STYLE_CODES[i],
                                    "&" + STYLE_CODES[i])), mouseX, mouseY);
                }
                return;
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return contains(mouseX, mouseY);
        }
        for (int i = 0; i < 16; i++) {
            if (hit(mouseX, mouseY, colorX[i], colorY[i], DROP_W, DROP_H)) {
                insertCode.accept("§" + COLOR_CODES[i]);
                return true;
            }
        }
        for (int i = 0; i < STYLE_CODES.length; i++) {
            if (hit(mouseX, mouseY, styleX[i], styleY[i], styleW[i], TILE)) {
                if (STYLE_CODES[i] == CLEAR_FORMATTING) {
                    clearFormatting.run();
                } else {
                    insertCode.accept("§" + STYLE_CODES[i]);
                }
                return true;
            }
        }
        return contains(mouseX, mouseY);
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + h;
    }

    private static boolean hit(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }
}
