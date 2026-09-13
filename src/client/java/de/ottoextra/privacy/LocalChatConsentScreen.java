package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.text.Text;

public final class LocalChatConsentScreen extends Screen {
    private final Screen parent;
    private final OttoExtraConfig config;
    private final String scope;
    private LocalChatConsentScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.set.tweaks.chatLog"));
        this.parent = parent; this.config = config; this.scope = config.tweaks.chatLogScope;
    }
    public static void change(OttoExtraConfig config, boolean allow) {
        if (!allow) {
            LocalPrivacy.decideChat(config, false, config.tweaks.chatLogScope);
            config.save();
        } else {
            var client = MinecraftClient.getInstance();
            client.setScreen(new LocalChatConsentScreen(client.currentScreen, config));
        }
    }
    @Override protected void init() {
        int w = Math.min(550, width - 32), x = (width - w) / 2;
        Text information = Text.translatable("ottoextra.privacy.chat.details",
                Text.translatable("ottoextra.privacy.chat.scope." + scope));
        addDrawableChild(new ScrollableTextWidget(x, 44, w, Math.max(30, height - 104), information, textRenderer));
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.consent.deny"), b -> decide(false))
                .dimensions(x, height - 44, w / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.consent.allow"), b -> decide(true))
                .dimensions(x + w / 2 + 4, height - 44, w / 2 - 4, 20).build());
    }
    private void decide(boolean allow) { LocalPrivacy.decideChat(config, allow, scope); config.save(); close(); }
    @Override public void close() { client.setScreen(parent); }
    @Override public void render(DrawContext context, int x, int y, float delta) {
        context.fill(0, 0, width, height, 0xF0202020);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        super.render(context, x, y, delta);
    }
}
