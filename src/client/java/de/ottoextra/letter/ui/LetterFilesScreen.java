package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.letter.*;
import de.ottoextra.letter.archive.LetterTextArchive;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.file.*;
import java.util.*;

/** Local TXT archive browser. Import creates a new, unlocked letter draft. */
public final class LetterFilesScreen extends Screen {
    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft current;
    private List<Path> files = List.of();
    private Path selected;
    private int scroll;
    private String status = "";
    private ButtonWidget importButton;

    public LetterFilesScreen(Screen parent, OttoExtraConfig config, LetterDraft current) {
        super(Text.translatable("ottoextra.letter.txt.title"));
        this.parent = parent;
        this.config = config;
        this.current = current;
    }

    private int left() { return (width - listWidth()) / 2; }
    private int listWidth() { return Math.min(380, width - 16); }
    private int rows() { return Math.max(1, (height - 120) / 16); }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.txt.folder"), b -> {
            try {
                Files.createDirectories(OttoExtraPaths.lettersDir());
                Util.getOperatingSystem().open(OttoExtraPaths.lettersDir());
            } catch (Exception e) { failed(); }
        }).dimensions(width / 2 - 152, 36, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.txt.refresh"), b -> reload())
                .dimensions(width / 2 + 2, 36, 150, 20).build());
        importButton = addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.txt.import"), b -> importSelected())
                .dimensions(width / 2 - 152, height - 30, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(width / 2 + 2, height - 30, 150, 20).build());
        reload();
    }

    private void reload() {
        try {
            Files.createDirectories(OttoExtraPaths.lettersDir());
            try (var stream = Files.list(OttoExtraPaths.lettersDir())) {
                files = stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                        .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed()).toList();
            }
            if (!files.contains(selected)) selected = null;
            status = files.isEmpty() ? Text.translatable("ottoextra.letter.txt.empty").getString() : "";
        } catch (Exception e) { files = List.of(); selected = null; failed(); }
        scroll = Math.min(scroll, Math.max(0, files.size() - rows()));
        importButton.active = selected != null;
    }

    private void importSelected() {
        if (selected == null) return;
        try {
            // Read first, so a damaged file cannot displace the editor's current draft.
            var pages = LetterTextArchive.read(selected);
            LetterDraft imported = LetterDraft.empty();
            imported.pages = new ArrayList<>(pages);
            imported.meta.name = selected.getFileName().toString();
            imported.meta.pagesOneToOne = true;
            // These are explicit book pages, not old editor work pages to migrate/reflow together.
            imported.meta.flowingDocument = String.join("\f", pages);
            imported.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;
            if (current != null && current.pages.stream().anyMatch(page -> !page.isBlank())) {
                LetterDraft saved = SavedDraftStore.save(current, current.meta.name);
                LetterDraft checked = SavedDraftStore.load(saved.meta.draftId);
                if (checked == null || checked.meta.updatedAtMs != saved.meta.updatedAtMs
                        || !checked.pages.equals(current.pages)) throw new java.io.IOException("Draft backup failed");
            }
            LetterDraftCache.save(imported);
            LetterDraft checked = LetterDraftCache.load();
            if (!checked.meta.draftId.equals(imported.meta.draftId) || !checked.pages.equals(imported.pages)) {
                throw new java.io.IOException("Imported draft could not be saved");
            }
            client.setScreen(new LetterEditorScreen(null, config, imported));
        } catch (Exception e) { failed(); }
    }

    private void failed() { status = Text.translatable("ottoextra.letter.txt.failed").getString(); }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.x() >= left() && click.x() < left() + listWidth()
                && click.y() >= 66 && click.y() < 66 + rows() * 16) {
            int index = scroll + (int) ((click.y() - 66) / 16);
            if (index < files.size()) {
                selected = files.get(index);
                importButton.active = true;
                if (doubled) importSelected();
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.clamp(scroll - (int) Math.signum(vertical) * 3, 0, Math.max(0, files.size() - rows()));
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 16, 0xFFE6C8A9);
        for (int i = 0; i < rows() && scroll + i < files.size(); i++) {
            Path file = files.get(scroll + i);
            int y = 66 + i * 16;
            if (file.equals(selected)) context.fill(left() - 2, y - 2, left() + listWidth() + 2, y + 12, 0x557A5A3A);
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(file.getFileName().toString(), listWidth()),
                    left(), y, file.equals(selected) ? 0xFFFFD479 : 0xFFE6C8A9);
        }
        context.drawCenteredTextWithShadow(textRenderer, textRenderer.trimToWidth(status, width - 16),
                width / 2, height - 44, 0xFFE6C8A9);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
