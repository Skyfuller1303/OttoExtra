package de.ottoextra.letter.ui;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.format.LetterFormattingCodes;
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
    private static final int TEXT_W = 166;
    private static final int LINE_H = 12;
    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft draft;
    private int page;
    public LetterPreviewScreen(Screen parent, OttoExtraConfig config, LetterDraft draft, int page) {
        super(Text.translatable("ottoextra.letter.preview.title"));
        this.parent = parent;
        this.config = config;
        this.draft = draft;
        this.draft.repair();
        this.page = Math.max(0, Math.min(page, draft.pages.size() - 1));
    }
    private int panelX() {
        return (width - PANEL_W) / 2;
    }
    private int panelY() {
        return Math.max(4, (height - PANEL_H) / 2);
    }
    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> switchPage(-1))
                .dimensions(x + 12, y + PANEL_H - 28, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> switchPage(1))
                .dimensions(x + 40, y + PANEL_H - 28, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(x + PANEL_W - 78, y + PANEL_H - 28, 66, 18).build());
    }
    private void switchPage(int dir) {
        page = Math.max(0, Math.min(draft.pages.size() - 1, page + dir));
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int px = panelX();
        int py = panelY();
        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, PAPER_DARK);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, PAPER);
        ctx.drawText(textRenderer,
                Text.translatable("ottoextra.letter.pageIndicator", page + 1, draft.pages.size()),
                px + 26, py + 13, TEXT, false);
        ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.preview.readonly"),
                px + 116, py + 13, 0x99765B41, false);
        String raw = draft.pages.get(page);
        List<int[]> spans = lineSpans(raw);
        int maxLines = "PAGE".equalsIgnoreCase(config.letter.sendMode)
                ? config.letter.pageModeMaxLinesPerPage : config.letter.maxLinesPerPage;
        for (int i = 0; i < Math.min(maxLines, spans.size()); i++) {
            int[] span = spans.get(i);
            int y = py + 35 + i * LINE_H;
            ctx.fill(px + 25, y + LINE_H - 2, px + PANEL_W - 25, y + LINE_H - 1, LINE);
            String part = raw.substring(span[0], span[1]);
            String prefix = LetterFormattingCodes.activePrefixBefore(raw, span[0]);
            ctx.drawText(textRenderer, prefix + part, px + 27, y, TEXT, false);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
    private List<int[]> lineSpans(String raw) {
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
            String candidate = LetterFormattingCodes.activePrefixBefore(raw, start)
                    + raw.substring(start, i + 1);
            if (i > start && textRenderer.getWidth(candidate) > TEXT_W) {
                int at = lastSpace > start ? lastSpace : i;
                spans.add(new int[]{start, at});
                start = lastSpace > start ? at + 1 : at;
                lastSpace = -1;
                continue;
            }
            i++;
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
