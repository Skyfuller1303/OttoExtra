package de.ottoextra.welcome;

import de.ottoextra.OttoExtra;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Skippable release title; all permission decisions remain in the consent wizard. */
public final class ReleaseWelcomeScreen extends Screen {
    private static final Identifier LOGO = OttoExtra.id("icon.png");
    private final Screen parent;
    private final String version = WelcomeScreenManager.installedVersion();

    ReleaseWelcomeScreen(Screen parent) {
        super(Text.literal("OttoExtra " + WelcomeState.releaseSeries(WelcomeScreenManager.installedVersion())));
        this.parent = parent;
    }

    private int panelWidth() { return Math.min(560, width - 20); }
    private int panelHeight() { return Math.min(330, height - 20); }
    private boolean compact() { return panelWidth() < 480 || panelHeight() < 300; }

    @Override protected void init() {
        int w = panelWidth(), h = panelHeight();
        int left = (width - w) / 2, top = (height - h) / 2;
        boolean compact = compact();
        int bw = compact ? w - 40 : (w - 56) / 2;
        int x = left + (compact ? 20 : 24);
        int y = top + h - (compact ? 58 : 43);
        addDrawableChild(new WelcomeStyle.Button(x, y, bw, 22,
                Text.translatable("ottoextra.release.review"), true,
                button -> client.setScreen(new WelcomeScreen(parent, () -> client.setScreen(this)))));
        addDrawableChild(new WelcomeStyle.Button(compact ? x : x + bw + 8,
                compact ? y + 26 : y, bw, 22,
                Text.translatable("ottoextra.release.skip"), false, button -> close()));
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xFF0B1017);
        int w = panelWidth(), h = panelHeight();
        int left = (width - w) / 2, top = (height - h) / 2;
        boolean compact = compact();
        ctx.fill(left + 4, top + 5, left + w + 4, top + h + 5, 0x70000000);
        ctx.fill(left, top, left + w, top + h, WelcomeStyle.BACKGROUND);
        ctx.fill(left, top, left + w, top + 2, WelcomeStyle.GOLD);

        int size = compact ? 44 : 64;
        int logoX = left + 24, logoY = top + 23;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY, 0f, 0f,
                size, size, 128, 128, 128, 128);
        int textX = logoX + size + 18;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) textX, (float) (logoY + 3));
        float scale = compact ? 1.8f : 2.6f;
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(textRenderer, "OttoExtra", 0, 0, WelcomeStyle.TEXT);
        ctx.getMatrices().popMatrix();
        ctx.drawTextWithShadow(textRenderer, WelcomeState.releaseSeries(version), textX,
                logoY + (compact ? 28 : 38), WelcomeStyle.GOLD);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.release.subtitle"),
                left + 24, top + (compact ? 82 : 111), WelcomeStyle.MUTED);

        if (!compact) {
            int cardWidth = (w - 64) / 3;
            for (int i = 0; i < 3; i++) {
                int x = left + 24 + i * (cardWidth + 8), y = top + 139;
                ctx.fill(x, y, x + cardWidth, y + 82, WelcomeStyle.BORDER);
                ctx.fill(x + 1, y + 1, x + cardWidth - 1, y + 81, WelcomeStyle.CARD);
                ctx.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.release.card." + i + ".title"),
                        x + 10, y + 12, WelcomeStyle.GOLD);
                wrap(ctx, Text.translatable("ottoextra.release.card." + i + ".body"),
                        x + 10, y + 32, cardWidth - 20, WelcomeStyle.MUTED);
            }
        }
        wrap(ctx, Text.translatable("ottoextra.release.skip_hint"), left + 24,
                top + (compact ? 105 : h - 78), w - 48, WelcomeStyle.MUTED);
        if (!compact) ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.welcome.version", version), width / 2,
                top + h - 15, WelcomeStyle.MUTED);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void wrap(DrawContext ctx, Text text, int x, int y, int w, int color) {
        for (var line : textRenderer.wrapLines(text, w)) {
            ctx.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 11;
        }
    }

    @Override public void close() {
        if (client == null) return;
        WelcomeScreenManager.skipRelease();
        client.setScreen(parent);
    }
}
