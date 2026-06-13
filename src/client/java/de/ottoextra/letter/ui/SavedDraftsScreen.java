package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.SavedDraftStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltung gespeicherter Brief-Entwürfe: aktuellen Entwurf benennen +
 * speichern, gespeicherte laden (öffnet Editor) oder löschen. Liste über
 * {@link SavedDraftStore}, neueste zuerst.
 */
public final class SavedDraftsScreen extends Screen {

    private static final int ROW_H = 12;

    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft current;
    private final List<LetterDraft> saved = new ArrayList<>();
    private TextFieldWidget nameField;
    private String selectedId;
    private int scroll;
    private String status = "";

    public SavedDraftsScreen(Screen parent, OttoExtraConfig config, LetterDraft current) {
        super(Text.translatable("ottoextra.letter.drafts.title"));
        this.parent = parent;
        this.config = config;
        this.current = current;
    }

    private int listX() {
        return width / 2 - 100;
    }

    private int listTop() {
        return 70;
    }

    private int listBottom() {
        return height - 40;
    }

    @Override
    protected void init() {
        nameField = new TextFieldWidget(textRenderer, listX(), 36, 132, 18,
                Text.translatable("ottoextra.letter.drafts.nameHint"));
        nameField.setMaxLength(48);
        if (current != null && current.meta.name != null) {
            nameField.setText(current.meta.name);
        }
        addDrawableChild(nameField);
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.drafts.save"), b -> saveCurrent())
                .dimensions(listX() + 136, 36, 64, 18).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.drafts.load"), b -> loadSelected())
                .dimensions(width / 2 - 154, height - 30, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.drafts.delete"), b -> deleteSelected())
                .dimensions(width / 2 - 50, height - 30, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(width / 2 + 54, height - 30, 100, 20).build());
        reload();
    }

    private void reload() {
        saved.clear();
        saved.addAll(SavedDraftStore.list());
        scroll = 0;
    }

    private void saveCurrent() {
        if (current == null) {
            return;
        }
        LetterDraft stored = SavedDraftStore.save(current, nameField.getText());
        current.meta.name = stored.meta.name; // gleicher Entwurf -> künftig überschreiben
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
        LetterDraftCache.save(draft); // wird zum aktiven Arbeits-Entwurf
        MinecraftClient.getInstance().setScreen(new LetterEditorScreen(null, config));
    }

    private void deleteSelected() {
        if (selectedId == null) {
            return;
        }
        SavedDraftStore.delete(selectedId);
        selectedId = null;
        reload();
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(vertical) * 3,
                Math.max(0, saved.size() - visibleRows())));
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.x() >= listX() && click.x() <= listX() + 200
                && click.y() >= listTop() && click.y() <= listBottom()) {
            int idx = scroll + (int) ((click.y() - listTop()) / ROW_H);
            if (idx >= 0 && idx < saved.size()) {
                selectedId = saved.get(idx).meta.draftId;
                if (doubled) {
                    loadSelected();
                }
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private static String rowLabel(LetterDraft d) {
        String name = d.meta.name == null || d.meta.name.isBlank()
                ? Text.translatable("ottoextra.letter.drafts.unnamed").getString()
                : d.meta.name;
        return Text.translatable("ottoextra.letter.drafts.row", name, d.pages.size()).getString();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 16, 0xFFE6C8A9);
        if (saved.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.letter.drafts.empty"),
                    width / 2, listTop() + 8, 0xFF9B8B72);
        }
        int rows = visibleRows();
        for (int r = 0; r < rows; r++) {
            int idx = scroll + r;
            if (idx >= saved.size()) {
                break;
            }
            LetterDraft d = saved.get(idx);
            int y = listTop() + r * ROW_H;
            boolean isSel = d.meta.draftId.equals(selectedId);
            if (isSel) {
                ctx.fill(listX() - 2, y - 1, listX() + 202, y + ROW_H - 1, 0x337A5A3A);
            }
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(rowLabel(d), 198),
                    listX(), y, isSel ? 0xFFFFD479 : 0xFFE6C8A9);
        }
        if (!status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2,
                    listBottom() + 6, 0xFFB8A88F);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
