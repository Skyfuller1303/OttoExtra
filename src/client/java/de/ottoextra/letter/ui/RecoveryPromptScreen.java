package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.model.AnnouncementSendProgress;
import de.ottoextra.letter.model.LetterSendProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class RecoveryPromptScreen extends Screen {

    private final OttoExtraConfig config;
    private final LetterSendProgress letter;
    private final AnnouncementSendProgress announcement;

    public RecoveryPromptScreen(OttoExtraConfig config, LetterSendProgress letter,
                                AnnouncementSendProgress announcement) {
        super(Text.translatable("ottoextra.letter.recovery.title"));
        this.config = config;
        this.letter = letter;
        this.announcement = announcement;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 10;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.recovery.resume"), b -> {
            if (announcement != null) {
                LetterServices.runAnnouncementQueue(config, announcement,
                        announcement.sentCommands, null);
            } else if (letter != null) {
                LetterServices.runLetterQueue(config, letter, letter.sentCommands, null);
            }
            close();
        }).dimensions(cx - 154, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.recovery.restart"), b -> {
            LetterServices.letterStore().clear();
            LetterServices.announcementStore().clear();
            MinecraftClient.getInstance().setScreen(new LetterEditorScreen(null, config));
        }).dimensions(cx - 50, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.recovery.discard"), b -> {
            LetterServices.letterStore().clear();
            LetterServices.announcementStore().clear();
            LetterDraftCache.clear();
            close();
        }).dimensions(cx + 54, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2,
                height / 2 - 44, 0xFFE6C8A9);
        String detail;
        if (announcement != null) {
            detail = Text.translatable("ottoextra.letter.recovery.announcement",
                    announcement.sentCommands, announcement.pendingCommands.size()).getString();
        } else {
            detail = Text.translatable("ottoextra.letter.recovery.letter",
                    letter != null ? letter.recipient : "?",
                    letter != null ? letter.sentCommands : 0,
                    letter != null ? letter.pendingCommands.size() : 0).getString();
        }
        ctx.drawCenteredTextWithShadow(textRenderer, detail, width / 2,
                height / 2 - 30, 0xFFB8A88F);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
