package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottoextra.letter.send.LetterV2Planner;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class LetterPreviewScreen extends Screen {

    private static final int PAPER = 0xFFC8AC8E;
    private static final int PAPER_DARK = 0xFFB18F69;
    private static final int LINE = 0x88643C38;
    private static final int TEXT = 0xFF503D29;
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 276;
    private static final int TEXT_W = LetterV2Planner.BOOK_PAGE_WIDTH;
    private static final int LINE_H = 12;

    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft draft;
    private final List<String> pages;
    private int page;

    public LetterPreviewScreen(Screen parent, OttoExtraConfig config, LetterDraft draft, int page) {
        super(Text.translatable("ottoextra.letter.preview.title"));
        this.parent = parent;
        this.config = config;
        this.draft = draft;
        this.draft.repair();
        List<String> preview = LetterServices.previewPages(config, draft);
        this.pages = preview.isEmpty() ? List.of("") : preview;
        this.page = Math.max(0, Math.min(page, pages.size() - 1));
    }

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelHeight() {
        return Math.max(1, Math.min(PANEL_H, height - 8));
    }

    private int lineHeight() {
        return Math.max(9, Math.min(LINE_H,
                (panelHeight() - 35 - 52) / LetterV2Planner.BOOK_MAX_LINES));
    }

    private int panelY() {
        return Math.max(4, (height - panelHeight()) / 2);
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        int ph = panelHeight();
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> switchPage(-1))
                .dimensions(x + 12, y + ph - 28, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> switchPage(1))
                .dimensions(x + 40, y + ph - 28, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(x + PANEL_W - 78, y + ph - 28, 66, 18).build());
    }

    private void switchPage(int dir) {
        page = Math.max(0, Math.min(pages.size() - 1, page + dir));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        int ph = panelHeight();
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + ph + 2, PAPER_DARK);
        ctx.fill(px, py, px + PANEL_W, py + ph, PAPER);
        ctx.drawText(textRenderer,
                Text.translatable("ottoextra.letter.bookPageIndicator", page + 1, pages.size()),
                px + 26, py + 13, TEXT, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.preview.readonly"),
                px + 26, py + 24, 0x99765B41, false);

        String raw = pages.get(page);
        List<int[]> spans = lineSpans(raw);
        int maxLines = LetterV2Planner.BOOK_MAX_LINES;
        int lineHeight = lineHeight();
        for (int i = 0; i < Math.min(maxLines, spans.size()); i++) {
            int[] span = spans.get(i);
            int y = py + 35 + i * lineHeight;
            ctx.fill(px + 25, y + lineHeight - 2,
                    px + PANEL_W - 25, y + lineHeight - 1, LINE);
            String part = raw.substring(span[0], span[1]);
            String prefix = LetterFormattingCodes.activePrefixBefore(raw, span[0]);
            ctx.drawText(textRenderer, prefix + part, px + 27, y, TEXT, false);
        }
        if (spans.size() > maxLines) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.preview.overflow",
                    spans.size(), maxLines), px + 12, py + ph - 48, 0xFF992222, false);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private List<int[]> lineSpans(String raw) {
        return lineSpans(textRenderer, raw);
    }

    /** Zeilenumbruch einer Buchseite; auch vom Buchmodus des Editors genutzt. */
    static List<int[]> lineSpans(net.minecraft.client.font.TextRenderer textRenderer, String raw) {
        List<int[]> spans = new ArrayList<>();
        int start = 0;
        int lastSpace = -1;
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\n') {
                spans.add(new int[]{start, i});
                start = ++i;
                lastSpace = -1;
                continue;
            }
            if (c == ' ') {
                lastSpace = i;
            }
            int next = LetterEditorTextOperations.nextCharacterEnd(raw, i);
            String candidate = LetterFormattingCodes.activePrefixBefore(raw, start)
                    + raw.substring(start, next);
            if (i > start && textRenderer.getWidth(candidate) > TEXT_W) {
                int at = lastSpace > start ? lastSpace : i;
                spans.add(new int[]{start, at});
                start = lastSpace > start ? at + 1 : at;
                lastSpace = -1;
                continue;
            }
            i = next;
        }
        spans.add(new int[]{start, raw.length()});
        return spans;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
