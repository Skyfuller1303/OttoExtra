package de.ottoextra.letter.ui;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.announcement.AnnouncementLetterPreflightService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
public final class AnnouncementPreflightScreen extends Screen {
    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft draft;
    private AnnouncementLetterPreflightService.Result result;
    public AnnouncementPreflightScreen(Screen parent, OttoExtraConfig config, LetterDraft draft) {
        super(Text.translatable("ottoextra.letter.preflight.title"));
        this.parent = parent;
        this.config = config;
        this.draft = draft;
        recheck();
    }
    private AnnouncementLetterPreflightService service() {
        return new AnnouncementLetterPreflightService(
                config.letter.announcementSafeLinesPerPage,
                config.letter.announcementSafeCharsPerLine,
                config.letter.announcementHardLinesPerPage,
                config.letter.announcementHardCharsPerLine);
    }
    private void recheck() {
        result = service().check(draft.meta.draftId, draft.pages);
    }
    @Override
    protected void init() {
        int cx = width / 2;
        ButtonWidget send = ButtonWidget.builder(
                Text.translatable("ottoextra.letter.preflight.send"), b -> {
                    LetterServices.sendAnnounceSubmit(config);
                    LetterServices.consumePending();
                    MinecraftClient.getInstance().setScreen(null);
                }).dimensions(cx - 156, height - 30, 100, 20).build();
        send.active = result.canSend();
        addDrawableChild(send);
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.letter.preflight.optimize"), b -> {
                    draft.pages = new ArrayList<>(service().optimize(draft.pages));
                    recheck();
                    clearAndInit();
                }).dimensions(cx - 52, height - 30, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(cx + 56, height - 30, 100, 20).build());
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 12, 0xFFE6C8A9);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.letter.preflight.summary2",
                        result.pageCount(), result.totalLines(), result.totalCharacters()),
                width / 2, 24, 0xFFB8A88F);
        if (!LetterServices.hasSubmitCommand(config)) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("ottoextra.letter.preflight.manualSubmitHint"),
                    width / 2, 36, 0xFFB8A88F);
        }
        int y = 50;
        for (var page : result.pages()) {
            if (y > height - 44) {
                ctx.drawCenteredTextWithShadow(textRenderer, "…", width / 2, y, 0xFFB8A88F);
                break;
            }
            int color = !page.ok() ? 0xFFCC8888 : page.clean() ? 0xFF9ACD78 : 0xFFE8C87A;
            String mark = !page.ok() ? "✘" : page.clean() ? "✔" : "!";
            String head = mark + " Seite " + (page.pageIndex() + 1) + ": "
                    + page.usedLines() + "/" + config.letter.announcementSafeLinesPerPage
                    + " Zeilen · längste " + page.maxLineLength() + "/"
                    + config.letter.announcementSafeCharsPerLine;
            ctx.drawTextWithShadow(textRenderer, head, width / 2 - 150, y, color);
            y += 11;
            for (String b : page.blockers()) {
                ctx.drawTextWithShadow(textRenderer, "   • " + b, width / 2 - 150, y, 0xFFCC8888);
                y += 10;
            }
            for (String w : page.warnings()) {
                ctx.drawTextWithShadow(textRenderer, "   • " + w, width / 2 - 150, y, 0xFFE8C87A);
                y += 10;
            }
        }
    }
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
