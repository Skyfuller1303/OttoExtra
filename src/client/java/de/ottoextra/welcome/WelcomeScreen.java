package de.ottoextra.welcome;

import de.ottoextra.OttoExtra;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.List;

/** Einmaliger Begruessungsbildschirm beim ersten Start von OttoExtra. */
public final class WelcomeScreen extends Screen {

    private static final String REPOSITORY_URL =
            "https://github.com/Skyfuller1303/OttoExtra";

    private static final int MAX_PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 236;

    private final Screen parent;
    private Text status;

    WelcomeScreen(Screen parent) {
        super(Text.translatable("ottoextra.welcome.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int panelTop = height / 2 - PANEL_HEIGHT / 2;
        int buttonY = panelTop + PANEL_HEIGHT - 34;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.welcome.start"),
                button -> acceptAndClose())
                .dimensions(centerX - 106, buttonY, 102, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("ottoextra.welcome.github"),
                button -> openRepository())
                .dimensions(centerX + 4, buttonY, 102, 20)
                .build());
    }

    private void acceptAndClose() {
        WelcomeScreenManager.accept();
        closeToParent();
    }

    private void openRepository() {
        try {
            Util.getOperatingSystem().open(URI.create(REPOSITORY_URL));
        } catch (Throwable error) {
            OttoExtra.LOGGER.warn("GitHub-Seite konnte nicht geoeffnet werden: {}",
                    error.getMessage());
            if (client != null) {
                client.keyboard.setClipboard(REPOSITORY_URL);
            }
            status = Text.translatable("ottoextra.welcome.open_failed");
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (parent != null) {
            parent.render(ctx, mouseX, mouseY, delta);
        }
        ctx.fill(0, 0, width, height, 0x99000000);

        int panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(280, width - 32));
        int left = width / 2 - panelWidth / 2;
        int top = height / 2 - PANEL_HEIGHT / 2;
        int right = left + panelWidth;
        int bottom = top + PANEL_HEIGHT;

        drawPanel(ctx, left, top, right, bottom);

        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2,
                top + 16, 0xFFFFFFFF);

        Text version = Text.translatable(
                "ottoextra.welcome.version",
                WelcomeScreenManager.installedVersion());
        ctx.drawCenteredTextWithShadow(textRenderer, version, width / 2,
                top + 34, 0xFFB8A88F);

        int textWidth = panelWidth - 42;
        int y = top + 57;
        y = drawWrappedCentered(ctx,
                Text.translatable("ottoextra.welcome.intro"),
                textWidth, y, 0xFFE6C8A9, 10);
        y += 7;
        y = drawWrappedCentered(ctx,
                Text.translatable("ottoextra.welcome.features"),
                textWidth, y, 0xFFCFCFCF, 10);

        Text developers = Text.translatable("ottoextra.welcome.developers");
        ctx.drawCenteredTextWithShadow(textRenderer, developers, width / 2,
                top + 178, 0xFFFFD27A);

        if (status != null) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2,
                    top + 158, 0xFFFFC46B);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext ctx, int left, int top, int right, int bottom) {
        ctx.fill(left, top, right, bottom, 0xF0181715);
        ctx.fill(left, top, right, top + 2, 0xFFE6C8A9);
        ctx.fill(left, bottom - 2, right, bottom, 0xFFE6C8A9);
        ctx.fill(left, top, left + 2, bottom, 0xFFE6C8A9);
        ctx.fill(right - 2, top, right, bottom, 0xFFE6C8A9);

        ctx.fill(left + 4, top + 4, right - 4, top + 5, 0xFF6E5740);
        ctx.fill(left + 4, bottom - 5, right - 4, bottom - 4, 0xFF6E5740);
    }

    private int drawWrappedCentered(
            DrawContext ctx,
            Text text,
            int maxWidth,
            int startY,
            int color,
            int lineHeight) {
        List<OrderedText> lines = textRenderer.wrapLines(text, maxWidth);
        int y = startY;
        for (OrderedText line : lines) {
            ctx.drawCenteredTextWithShadow(textRenderer, line, width / 2, y, color);
            y += lineHeight;
        }
        return y;
    }

    private void closeToParent() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Der Bildschirm soll nicht versehentlich dauerhaft uebersprungen werden.
        return false;
    }

    @Override
    public void close() {
        // Nur „Los geht's“ bestaetigt und schliesst den einmaligen Hinweis.
    }
}
