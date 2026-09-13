package de.ottoextra.tweaks.vote;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.List;

/** A reminder key never directly opens the browser or overwrites the clipboard. */
public final class VoteConsentScreen extends Screen {
    private final String username;
    private final List<URI> urls;
    private final Runnable confirmed;
    private boolean used;

    public VoteConsentScreen(String username, List<URI> urls, Runnable confirmed) {
        super(Text.translatable("ottoextra.privacy.vote.title"));
        this.username = username; this.urls = List.copyOf(urls); this.confirmed = confirmed;
    }
    @Override protected void init() {
        int w = Math.min(550, width - 32), x = (width - w) / 2;
        Text details = Text.translatable("ottoextra.privacy.vote.details", urls.size(), username,
                String.join("\n", urls.stream().map(URI::toString).toList()));
        addDrawableChild(new ScrollableTextWidget(x, 44, w, Math.max(30, height - 104), details, textRenderer));
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
                .dimensions(x, height - 44, w / 2 - 4, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.privacy.vote.open"), b -> {
            if (used) return;
            used = true;
            close();
            confirmed.run();
        }).dimensions(x + w / 2 + 4, height - 44, w / 2 - 4, 20).build());
    }
    @Override public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) return true;
        return super.keyPressed(input);
    }
    @Override public void close() { client.setScreen(null); }
    @Override public void render(DrawContext context, int x, int y, float delta) {
        context.fill(0, 0, width, height, 0xF0202020);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
        super.render(context, x, y, delta);
    }
}
