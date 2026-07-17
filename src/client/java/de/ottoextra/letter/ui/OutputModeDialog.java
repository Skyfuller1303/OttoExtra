package de.ottoextra.letter.ui;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.model.LetterOutputMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
public final class OutputModeDialog extends Screen {
    private final Screen parent;
    private final OttoExtraConfig config;
    private final LetterDraft draft;
    public OutputModeDialog(Screen parent, OttoExtraConfig config, LetterDraft draft) {
        super(Text.translatable("ottoextra.letter.mode.title"));
        this.parent = parent;
        this.config = config;
        this.draft = draft;
    }
    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 20;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.mode.brief"), b -> {
            draft.meta.mode = LetterOutputMode.BRIEF;
            LetterDraftCache.save(draft);
            client.setScreen(new RecipientScreen(parent, config, draft));
        }).dimensions(cx - 102, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.letter.mode.announcement"), b -> {
            draft.meta.mode = LetterOutputMode.VERKUENDUNG;
            LetterDraftCache.save(draft);
            client.setScreen(new AnnouncementPreflightScreen(parent, config, draft));
        }).dimensions(cx + 2, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(cx - 50, y + 26, 100, 20).build());
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2,
                height / 2 - 44, 0xFFE6C8A9);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.letter.mode.hint"), width / 2,
                height / 2 + 54, 0xFFB8A88F);
    }
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
