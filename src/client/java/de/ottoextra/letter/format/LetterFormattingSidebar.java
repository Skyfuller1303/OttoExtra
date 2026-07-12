package de.ottoextra.letter.format;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public final class LetterFormattingSidebar {

    private static final int PAPER = 0xFFC8AC8E;
    private static final int PAPER_DARK = 0xFFB18F69;
    private static final int PAPER_LINE = 0x55643C38;
    private static final int INK = 0xFF503D29;
    private static final int FRAME = 0xFF4A382A;

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
    private static final char[] STYLE_CODES = {'l', 'o', 'n', 'm', 'r', CLEAR_FORMATTING};
    private static final String[] STYLE_LABELS = {"B", "I", "U", "S", "R", "Tx"};
    private static final String[] STYLE_NAMES = {
            "Fett", "Kursiv", "Unterstrichen", "Durchgestrichen", "Reset (Format aus)",
            "Formatierung aus Auswahl entfernen"};

    private static final int COLS = 2;
    private static final int CELL = 11;
    private static final int GAP = 3;
    private static final int STEP = CELL + GAP;
    private static final int PAD = 4;
    private static final int DIVIDER_GAP = 5;

    public static final int WIDTH = COLS * CELL + (COLS - 1) * GAP + 2 * PAD;

    private final Consumer<String> insertCode;
    private final Runnable clearFormatting;

    private int x;
    private int y;
    private int h;

    private final int[] colorX = new int[16];
    private final int[] colorY = new int[16];
    private final int[] styleX = new int[STYLE_CODES.length];
    private final int[] styleY = new int[STYLE_CODES.length];
    private int dividerY;

    public LetterFormattingSidebar(Consumer<String> insertCode, Runnable clearFormatting) {
        this.insertCode = insertCode;
        this.clearFormatting = clearFormatting;
    }

    public void setBounds(int x, int y) {
        this.x = x;
        this.y = y;
        layout();
    }

    private void layout() {
        int colorRows = 16 / COLS;
        int styleRows = (STYLE_CODES.length + COLS - 1) / COLS;
        int colorsH = colorRows * STEP - GAP;
        int stylesH = styleRows * STEP - GAP;
        int top = y + PAD;
        int gridX = x + PAD;

        for (int i = 0; i < 16; i++) {
            colorX[i] = gridX + (i % COLS) * STEP;
            colorY[i] = top + (i / COLS) * STEP;
        }
        dividerY = top + colorsH + DIVIDER_GAP / 2;
        int stylesTop = top + colorsH + DIVIDER_GAP;
        for (int i = 0; i < STYLE_CODES.length; i++) {
            styleX[i] = gridX + (i % COLS) * STEP;
            styleY[i] = stylesTop + (i / COLS) * STEP;
        }
        h = (stylesTop + stylesH) - y + PAD;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {

        ctx.fill(x - 1, y - 1, x + WIDTH + 1, y + h + 1, FRAME);
        ctx.fill(x, y, x + WIDTH, y + h, PAPER);
        ctx.fill(x + 2, y + 2, x + WIDTH - 2, y + 3, PAPER_DARK);

        for (int i = 0; i < 16; i++) {
            int cx = colorX[i];
            int cyy = colorY[i];
            boolean hovered = hit(mouseX, mouseY, cx, cyy, CELL, CELL);
            ctx.fill(cx - 1, cyy - 1, cx + CELL + 1, cyy + CELL + 1, hovered ? INK : FRAME);
            ctx.fill(cx, cyy, cx + CELL, cyy + CELL, 0xFF000000 | COLOR_RGB[i]);
            if (hovered) {
                ctx.fill(cx, cyy, cx + CELL, cyy + 1, 0x66FFFFFF);
            }
        }

        ctx.fill(x + PAD, dividerY, x + WIDTH - PAD, dividerY + 1, PAPER_LINE);

        for (int i = 0; i < STYLE_CODES.length; i++) {
            int bx = styleX[i];
            int by = styleY[i];
            boolean hovered = hit(mouseX, mouseY, bx, by, CELL, CELL);
            ctx.fill(bx - 1, by - 1, bx + CELL + 1, by + CELL + 1, FRAME);
            ctx.fill(bx, by, bx + CELL, by + CELL, hovered ? PAPER_DARK : 0xFFBE9D78);

            String label = STYLE_CODES[i] == CLEAR_FORMATTING
                    ? STYLE_LABELS[i]
                    : "§" + STYLE_CODES[i] + STYLE_LABELS[i] + "§r";
            int lw = tr.getWidth(label);
            ctx.drawText(tr, label, bx + (CELL - lw + 1) / 2, by + 2, INK, false);
        }

        renderTooltips(ctx, tr, mouseX, mouseY);
    }

    private void renderTooltips(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        for (int i = 0; i < 16; i++) {
            if (hit(mouseX, mouseY, colorX[i], colorY[i], CELL, CELL)) {
                ctx.drawTooltip(tr, List.of(
                        Text.literal("§" + COLOR_CODES[i] + COLOR_NAMES[i]),
                        Text.translatable("ottoextra.letter.fmt.code", "§" + COLOR_CODES[i],
                                "&" + COLOR_CODES[i]),
                        Text.translatable("ottoextra.letter.fmt.colorHint")), mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < STYLE_CODES.length; i++) {
            if (hit(mouseX, mouseY, styleX[i], styleY[i], CELL, CELL)) {
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
            if (hit(mouseX, mouseY, colorX[i], colorY[i], CELL, CELL)) {
                insertCode.accept("§" + COLOR_CODES[i]);
                return true;
            }
        }
        for (int i = 0; i < STYLE_CODES.length; i++) {
            if (hit(mouseX, mouseY, styleX[i], styleY[i], CELL, CELL)) {
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
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }
}
