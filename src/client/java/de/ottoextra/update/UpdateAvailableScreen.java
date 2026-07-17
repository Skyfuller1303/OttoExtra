package de.ottoextra.update;
import de.ottoextra.OttoExtra;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.net.URI;
import java.util.List;
public final class UpdateAvailableScreen extends Screen {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 150;
    private final Screen parent;
    private final UpdateInfo update;
    private Text status;
    public UpdateAvailableScreen(Screen parent, UpdateInfo update) {
        super(Text.translatable("ottoextra.update.title"));
        this.parent = parent;
        this.update = update;
    }
    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonY = height / 2 + 42;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.update.now"),
                button -> openRelease())
                .dimensions(centerX - 104, buttonY, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.update.later"),
                button -> close())
                .dimensions(centerX + 4, buttonY, 100, 20)
                .build());
    }
    private void openRelease() {
        try {
            Util.getOperatingSystem().open(URI.create(update.releaseUrl()));
            close();
        } catch (Throwable error) {
            OttoExtra.LOGGER.warn("Release-Seite konnte nicht geoeffnet werden: {}",
                    error.getMessage());
            if (client != null) {
                client.keyboard.setClipboard(update.releaseUrl());
            }
            status = Text.translatable("ottoextra.update.open_failed");
        }
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (parent != null) {
            parent.render(ctx, mouseX, mouseY, delta);
        }
        ctx.fill(0, 0, width, height, 0x88000000);
        int left = width / 2 - PANEL_WIDTH / 2;
        int top = height / 2 - PANEL_HEIGHT / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;
        ctx.fill(left, top, right, bottom, 0xEE171717);
        ctx.fill(left, top, right, top + 2, 0xFFE6C8A9);
        ctx.fill(left, bottom - 2, right, bottom, 0xFFE6C8A9);
        ctx.fill(left, top, left + 2, bottom, 0xFFE6C8A9);
        ctx.fill(right - 2, top, right, bottom, 0xFFE6C8A9);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2,
                top + 14, 0xFFFFFFFF);
        Text versions = Text.translatable("ottoextra.update.versions",
                update.currentVersion(), update.latestVersion());
        ctx.drawCenteredTextWithShadow(textRenderer, versions, width / 2,
                top + 40, 0xFFE6C8A9);
        List<OrderedText> hint = textRenderer.wrapLines(
                Text.translatable("ottoextra.update.hint"), PANEL_WIDTH - 32);
        int hintY = top + 60;
        for (OrderedText line : hint) {
            ctx.drawCenteredTextWithShadow(textRenderer, line, width / 2,
                    hintY, 0xFFB8B8B8);
            hintY += 10;
        }
        if (status != null) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2,
                    top + 104, 0xFFFFC46B);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
