package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.paste.BookImportService;
import de.ottoextra.letter.paste.PageSplitter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class BookImportDialog extends Screen {

    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft draft;
    private final List<String> bookPages;

    public BookImportDialog(Screen parent, OttoExtraConfig config, LetterDraft draft) {
        super(Text.translatable("ottoextra.letter.bookImport.title"));
        this.parent = parent;
        this.config = config;
        this.draft = draft;
        this.bookPages = BookImportService.readHeldBook();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 20;
        boolean hasBook = !bookPages.isEmpty();
        ButtonWidget oneToOne = ButtonWidget.builder(
                Text.translatable("ottoextra.letter.bookImport.oneToOne"), b -> {
                    draft.pages = new ArrayList<>(bookPages);
                    draft.meta.importedFromBook = true;
                    draft.meta.pagesOneToOne = true;
                    LetterDraftCache.save(draft);
                    close();
                }).dimensions(cx - 102, y, 100, 20).build();
        oneToOne.active = hasBook;
        addDrawableChild(oneToOne);
        ButtonWidget reflow = ButtonWidget.builder(
                Text.translatable("ottoextra.letter.bookImport.reflow"), b -> {
                    PageSplitter splitter = new PageSplitter(
                            config.letter.maxCharsPerLine, config.letter.maxLinesPerPage);
                    draft.pages = new ArrayList<>(splitter.split(String.join("\n", bookPages)));
                    draft.meta.importedFromBook = true;
                    draft.meta.pagesOneToOne = false;
                    LetterDraftCache.save(draft);
                    close();
                }).dimensions(cx + 2, y, 100, 20).build();
        reflow.active = hasBook;
        addDrawableChild(reflow);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(cx - 50, y + 26, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2,
                height / 2 - 44, 0xFFE6C8A9);
        Text info = bookPages.isEmpty()
                ? Text.translatable("ottoextra.letter.bookImport.noBook")
                : Text.translatable("ottoextra.letter.bookImport.found", bookPages.size());
        ctx.drawCenteredTextWithShadow(textRenderer, info, width / 2,
                height / 2 - 32, bookPages.isEmpty() ? 0xFFCC8888 : 0xFFB8A88F);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
