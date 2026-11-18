package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.LetterTextTools;
import de.ottoextra.letter.SavedDraftStore;
import de.ottoextra.ui.theme.InkTextField;
import de.ottoextra.ui.theme.OttonienTheme;
import de.ottoextra.ui.theme.SlateButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class SavedDraftsScreen extends Screen {

    private static final int BOOK_W = 182;
    private static final int LEAF_W = 170;
    private static final int RIBBON_TOP = 24;
    private static final int ROW_H = 21;
    private static final String[] TAB_KEYS = {"ottoextra.letter.tab.letter",
            "ottoextra.letter.tab.book", "ottoextra.letter.tab.drafts"};
    private static final int[] TAB_COLORS = {OttonienTheme.TERRACOTTA, OttonienTheme.BRASS,
            OttonienTheme.SLATE_TAB};

    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft current;
    private final List<LetterDraft> saved = new ArrayList<>();
    private InkTextField nameField;
    private ButtonWidget saveButton;
    private ButtonWidget loadButton;
    private ButtonWidget deleteButton;
    private ButtonWidget backButton;
    private String selectedId;
    private int scroll;
    private String status = "";

    private int bx;
    private int by;
    private int bh = 196;
    private int lfx;
    private int lfy;
    private int lfw = LEAF_W;
    private int lfh;
    private boolean narrow;
    private boolean leafOverlay;
    private int txtX;
    private int txtY;
    private final List<int[]> ribbonBoxes = new ArrayList<>();
    private final List<Integer> ribbonIds = new ArrayList<>();
    private List<String> previewLines = List.of();
    private String previewFor = "";
    private int previewWidth = -1;


    public SavedDraftsScreen(Screen parent, OttoExtraConfig config, LetterDraft current) {
        super(Text.translatable("ottoextra.letter.drafts.title"));
        this.parent = parent;
        this.config = config;
        this.current = current;
    }

    private int pageX() {
        return bx + 10;
    }

    private int pageY() {
        return by + 6;
    }

    private int pageW() {
        return BOOK_W - 16;
    }

    private int pageH() {
        return bh - 12;
    }

    private int listTop() {
        return pageY() + 42;
    }

    private int listBottom() {
        return pageY() + pageH() - 16;
    }

    private boolean bookContentVisible() {
        return !(narrow && leafOverlay);
    }

    private boolean leafVisible() {
        return !narrow || leafOverlay;
    }

    @Override
    protected void init() {
        bh = Math.max(110, Math.min(196, height - RIBBON_TOP - 28));
        by = RIBBON_TOP + Math.max(0, (height - RIBBON_TOP - 28 - bh) / 2);
        int margin = 4;
        int leafW = LEAF_W;
        int need = BOOK_W + 8 + leafW + 2 * margin;
        narrow = false;
        if (need > width) {
            leafW = Math.max(140, leafW - (need - width));
            need = BOOK_W + 8 + leafW + 2 * margin;
        }
        if (need > width) {
            narrow = true;
            leafW = Math.min(leafW, Math.max(120, width - 2 * margin));
            need = BOOK_W;
        }
        lfw = leafW;
        bx = narrow ? (width - BOOK_W) / 2 : (width - need) / 2 + margin;
        lfx = narrow ? bx + (BOOK_W - lfw) / 2 : bx + BOOK_W + 8;
        lfy = narrow ? by + 4 : by + 32;
        lfh = narrow ? bh - 8 : bh - 30;
        txtX = narrow ? bx + BOOK_W - 20 : bx + BOOK_W + 6;
        txtY = narrow ? by - 20 : by + 10;
        int left = bx;
        int right = narrow ? bx + BOOK_W : lfx + lfw;

        int x0 = pageX() + 10;
        int fw = pageW() - 20 - 58;
        nameField = new InkTextField(textRenderer, x0, pageY() + 16, fw, 12,
                Text.translatable("ottoextra.letter.drafts.nameHint"), false);
        nameField.setMaxLength(48);
        if (current != null && current.meta.name != null) {
            nameField.setText(current.meta.name);
        }
        addDrawableChild(nameField);
        saveButton = new SlateButton(x0 + fw + 4, pageY() + 16, 54, 14,
                Text.translatable("ottoextra.letter.drafts.save"), b -> saveCurrent());
        addDrawableChild(saveButton);

        int bw2 = (lfw - 26) / 2;
        loadButton = new SlateButton(lfx + 10, lfy + lfh - 24, bw2, 16,
                Text.translatable("ottoextra.letter.drafts.load"), b -> loadSelected());
        addDrawableChild(loadButton);
        deleteButton = new SlateButton(lfx + 16 + bw2, lfy + lfh - 24, bw2, 16,
                Text.translatable("ottoextra.letter.drafts.delete"), b -> deleteSelected()).danger();
        addDrawableChild(deleteButton);
        backButton = new SlateButton((left + right) / 2 - 30, by + bh + 8, 60, 16,
                Text.translatable("gui.back"), b -> close());
        backButton.visible = by + bh + 8 + 16 <= height;
        addDrawableChild(backButton);

        ribbonBoxes.clear();
        ribbonIds.clear();
        int rx = bx + 18;
        for (int k = 0; k < 3; k++) {
            if (k == 1 && !(parent instanceof LetterEditorScreen && config.letter.previewEnabled)) {
                continue;
            }
            int w = textRenderer.getWidth(Text.translatable(TAB_KEYS[k])) + 10;
            int len = k == 2 ? 22 : 17;
            ribbonBoxes.add(new int[]{rx, by - len, w, len + 6});
            ribbonIds.add(k);
            rx += w + 4;
        }
        reload();
        applyVisibility();
    }

    private void applyVisibility() {
        boolean book = bookContentVisible();
        nameField.visible = book;
        saveButton.visible = book;
        boolean leaf = leafVisible();
        loadButton.visible = leaf;
        deleteButton.visible = leaf;
        loadButton.active = selectedDraft() != null;
        deleteButton.active = selectedDraft() != null;
    }

    private LetterDraft selectedDraft() {
        if (selectedId == null) {
            return null;
        }
        for (LetterDraft d : saved) {
            if (selectedId.equals(d.meta.draftId)) {
                return d;
            }
        }
        return null;
    }

    private void reload() {
        saved.clear();
        saved.addAll(SavedDraftStore.list());
        scroll = 0;
        previewFor = "";
        if (selectedDraft() == null) {
            selectedId = null;
        }
        if (nameField != null) {
            applyVisibility();
        }
    }

    private void saveCurrent() {
        if (current == null) {
            return;
        }
        LetterDraft stored = SavedDraftStore.save(current, nameField.getText());
        current.meta.name = stored.meta.name;
        status = Text.translatable("ottoextra.letter.drafts.saved").getString();
        reload();
    }

    private void loadSelected() {
        if (selectedId == null) {
            return;
        }
        LetterDraft draft = SavedDraftStore.load(selectedId);
        if (draft == null) {
            return;
        }
        LetterDraftCache.save(draft);
        MinecraftClient.getInstance().setScreen(new LetterEditorScreen(null, config));
    }

    private void deleteSelected() {
        if (selectedId == null) {
            return;
        }
        SavedDraftStore.delete(selectedId);
        selectedId = null;
        leafOverlay = false;
        reload();
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private Screen editorTarget() {
        return parent != null ? parent : new LetterEditorScreen(null, config, current);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (bookContentVisible()) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(vertical) * 3,
                    Math.max(0, saved.size() - visibleRows())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() == 0) {
            for (int i = 0; i < ribbonBoxes.size(); i++) {
                int[] b = ribbonBoxes.get(i);
                if (in(mx, my, b[0], b[1], b[2], b[3])) {
                    int id = ribbonIds.get(i);
                    if (id == 0) {
                        MinecraftClient.getInstance().setScreen(editorTarget());
                    } else if (id == 1 && parent instanceof LetterEditorScreen editor) {
                        editor.openBookView();
                        MinecraftClient.getInstance().setScreen(editor);
                    }
                    return true;
                }
            }
            if (in(mx, my, txtX, txtY, 16, 16) || (!narrow && in(mx, my, txtX + 20, txtY + 4, 30, 9))) {
                client.setScreen(new LetterFilesScreen(this, config, current));
                return true;
            }
            if (narrow && leafOverlay && in(mx, my, lfx + lfw - 30, lfy + 6, 22, 12)) {
                leafOverlay = false;
                applyVisibility();
                return true;
            }
            if (bookContentVisible() && in(mx, my, pageX() + 5, listTop() - 2, pageW() - 10,
                    listBottom() - listTop() + 2)) {
                int idx = scroll + (int) ((my - (listTop() - 2)) / ROW_H);
                if (idx >= 0 && idx < saved.size()) {
                    selectedId = saved.get(idx).meta.draftId;
                    previewFor = "";
                    if (doubled) {
                        loadSelected();
                    } else if (narrow) {
                        leafOverlay = true;
                    }
                    applyVisibility();
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private String timeText(LetterDraft d) {
        if (d.meta.updatedAtMs <= 0) {
            return "";
        }
        LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(d.meta.updatedAtMs),
                ZoneId.systemDefault());
        String clock = t.format(DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate today = LocalDate.now();
        if (t.toLocalDate().equals(today)) {
            return Text.translatable("ottoextra.letter.drafts.today", clock).getString();
        }
        if (t.toLocalDate().equals(today.minusDays(1))) {
            return Text.translatable("ottoextra.letter.drafts.yesterday", clock).getString();
        }
        return t.format(DateTimeFormatter.ofPattern("dd.MM. HH:mm"));
    }

    private String ellipsize(String value, int maxW) {
        if (textRenderer.getWidth(value) <= maxW) {
            return value;
        }
        return textRenderer.trimToWidth(value, Math.max(0, maxW - textRenderer.getWidth("…"))) + "…";
    }

    private void drawBold(DrawContext ctx, String s, int x, int y, int color, int maxW) {
        ctx.drawText(textRenderer, Text.literal(ellipsize(s, maxW))
                .formatted(net.minecraft.util.Formatting.BOLD), x, y, color, false);
    }

    private String nameOf(LetterDraft d) {
        return d.meta.name == null || d.meta.name.isBlank()
                ? Text.translatable("ottoextra.letter.drafts.unnamed").getString() : d.meta.name;
    }

    private List<String> previewFor(LetterDraft d, int maxW, int maxLines) {
        String key = d.meta.draftId + "/" + d.meta.updatedAtMs;
        if (key.equals(previewFor) && maxW == previewWidth) {
            return previewLines;
        }
        String plain = LetterTextTools.plainText(String.join("\n", d.pages));
        List<String> lines = new ArrayList<>();
        boolean truncated = false;
        outer:
        for (String paragraph : plain.split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                if (lines.size() >= maxLines) {
                    truncated = true;
                    break;
                }
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split(" +")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (textRenderer.getWidth(candidate) > maxW && !line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                    if (lines.size() >= maxLines) {
                        truncated = true;
                        break outer;
                    }
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
            if (lines.size() >= maxLines) {
                truncated = lines.size() > maxLines;
                break;
            }
        }
        while (!lines.isEmpty() && lines.getLast().isEmpty()) {
            lines.removeLast();
        }
        if (truncated && !lines.isEmpty()) {
            lines.set(lines.size() - 1, ellipsize(lines.getLast() + " …", maxW));
        }
        previewFor = key;
        previewWidth = maxW;
        previewLines = lines;
        return lines;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        OttonienTheme.redBook(ctx, bx, by, BOOK_W, bh);
        OttonienTheme.parchment(ctx, pageX(), pageY(), pageW(), pageH(), 61, true, true);
        for (int i = 0; i < 4; i++) {
            ctx.fill(pageX() + i, pageY() + 1, pageX() + i + 1, pageY() + pageH() - 1,
                    OttonienTheme.shade(OttonienTheme.PA, .88 + i * .03));
        }
        for (int i = 0; i < ribbonBoxes.size(); i++) {
            int[] b = ribbonBoxes.get(i);
            int id = ribbonIds.get(i);
            boolean active = id == 2;
            int len = active ? 22 : 17;
            OttonienTheme.ribbon(ctx, b[0], by - len, by + 6, b[2],
                    active ? TAB_COLORS[id] : OttonienTheme.shade(TAB_COLORS[id], .85));
            ctx.drawText(textRenderer, Text.translatable(TAB_KEYS[id]), b[0] + 5, by - len + 5,
                    active ? OttonienTheme.LABEL : OttonienTheme.LABEL_MUTED, false);
        }
        // TXT-Kachel
        boolean txtHover = in(mouseX, mouseY, txtX, txtY, 16, 16);
        OttonienTheme.tile(ctx, txtX, txtY, 16, 16, txtHover);
        OttonienTheme.pictoPage(ctx, txtX + 3, txtY + 3, OttonienTheme.PICTO);
        if (!narrow) {
            ctx.drawText(textRenderer, "TXT", txtX + 22, txtY + 4, OttonienTheme.LABEL_MUTED, false);
        }
        if (bookContentVisible()) {
            renderBookPage(ctx);
        }
        if (leafVisible()) {
            renderLeaf(ctx, mouseX, mouseY);
        }
        super.render(ctx, mouseX, mouseY, delta);
        if (!status.isEmpty() && backButton.visible) {
            ctx.drawText(textRenderer, ellipsize(status, Math.max(20, backButton.getX() - bx - 8)),
                    bx, by + bh + 12, 0xFFD6C194, false);
        }
        if (txtHover) {
            ctx.drawTooltip(textRenderer, List.of(Text.translatable("ottoextra.letter.txt.title")),
                    mouseX, mouseY);
        }
    }

    private void renderBookPage(DrawContext ctx) {
        int lx = pageX();
        int lw = pageW();
        int x0 = lx + 10;
        ctx.drawText(textRenderer, Text.translatable("ottoextra.letter.drafts.saveCurrent"), x0,
                pageY() + 7, OttonienTheme.INK_MUTED, false);
        ctx.fill(x0, pageY() + 36, lx + lw - 10, pageY() + 37, OttonienTheme.PA_S);
        if (saved.isEmpty()) {
            ctx.drawText(textRenderer, ellipsize(
                    Text.translatable("ottoextra.letter.drafts.empty").getString(), lw - 20),
                    x0, listTop() + 4, OttonienTheme.INK_MUTED, false);
        }
        int rows = visibleRows();
        for (int r = 0; r < rows; r++) {
            int idx = scroll + r;
            if (idx >= saved.size()) {
                break;
            }
            LetterDraft d = saved.get(idx);
            int yy = listTop() + r * ROW_H;
            boolean sel = d.meta.draftId.equals(selectedId);
            if (sel) {
                ctx.fill(lx + 5, yy - 2, lx + lw - 5, yy + 18, OttonienTheme.PA_L);
                ctx.fill(lx + 5, yy - 2, lx + 7, yy + 18, OttonienTheme.SLATE_TAB);
            }
            OttonienTheme.pictoPage(ctx, x0, yy + 3, OttonienTheme.PICTO);
            String tag = Text.translatable("ottoextra.letter.drafts.pagesTag", d.pages.size()).getString();
            int tagW = textRenderer.getWidth(tag) + 6;
            boolean unnamed = d.meta.name == null || d.meta.name.isBlank();
            ctx.drawText(textRenderer, ellipsize(nameOf(d), lw - 20 - 13 - 2), x0 + 13, yy,
                    unnamed ? OttonienTheme.INK_MUTED : OttonienTheme.INK, false);
            ctx.fill(lx + lw - 10 - tagW, yy + 8, lx + lw - 10, yy + 17, OttonienTheme.BRASS);
            ctx.drawText(textRenderer, tag, lx + lw - 10 - tagW + 3, yy + 9, OttonienTheme.LABEL, false);
            String when = timeText(d);
            if (!when.isEmpty()) {
                ctx.drawText(textRenderer, ellipsize(when, lw - 20 - 13 - tagW - 6), x0 + 13, yy + 9,
                        OttonienTheme.INK_MUTED, false);
            }
            if (r < rows - 1 && idx < saved.size() - 1) {
                ctx.fill(x0, yy + 19, lx + lw - 10, yy + 20, OttonienTheme.PA_S);
            }
        }
        Text count = Text.translatable("ottoextra.letter.drafts.count", saved.size());
        ctx.drawText(textRenderer, count, lx + (lw - textRenderer.getWidth(count)) / 2,
                pageY() + pageH() - 13, OttonienTheme.INK_MUTED, false);
    }

    private void renderLeaf(DrawContext ctx, int mouseX, int mouseY) {
        OttonienTheme.leaf(ctx, lfx, lfy, lfw, lfh, 62, false);
        int x = lfx + 10;
        int w = lfw - 20;
        LetterDraft d = selectedDraft();
        if (narrow && leafOverlay) {
            boolean hover = in(mouseX, mouseY, lfx + lfw - 30, lfy + 6, 22, 12);
            OttonienTheme.tile(ctx, lfx + lfw - 30, lfy + 6, 22, 12, hover);
            ctx.drawText(textRenderer, "◂", lfx + lfw - 30 + 8, lfy + 8, OttonienTheme.PICTO, false);
        }
        if (d == null) {
            ctx.drawText(textRenderer, ellipsize(
                    Text.translatable("ottoextra.letter.drafts.empty").getString(), w),
                    x, lfy + 10, OttonienTheme.INK_MUTED, false);
            return;
        }
        drawBold(ctx, nameOf(d), x, lfy + 9, OttonienTheme.INK, w - (narrow ? 30 : 0));
        String when = timeText(d);
        String info = Text.translatable("ottoextra.letter.drafts.pagesInfo", d.pages.size()).getString()
                + (when.isEmpty() ? "" : " - " + when);
        ctx.drawText(textRenderer, ellipsize(info, w), x, lfy + 19, OttonienTheme.INK_MUTED, false);
        ctx.fill(x, lfy + 30, x + w, lfy + 31, OttonienTheme.PA_S);
        int maxLines = Math.max(1, (lfh - 36 - 30) / 10);
        int y = lfy + 36;
        for (String line : previewFor(d, w, maxLines)) {
            ctx.drawText(textRenderer, line, x, y, OttonienTheme.INK, false);
            y += 10;
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
