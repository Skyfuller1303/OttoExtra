package de.ottoextra.map;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public final class ParchmentMapOverlay {
    private static final int EDGE = 0xA6513E2A;
    private static final int INNER = 0x80E6C8A9;
    private static final int PAPER = 0x34C8AC8E;
    private static final int TEXT = 0xFFE6C8A9;
    private static final int SHADOW = 0xCC2B1C10;
    private ParchmentMapOverlay() {
    }
    public static void render(DrawContext ctx, XaeroMapBridge.View view, OttoExtraConfig.Map cfg) {
        if (ctx == null || view == null || cfg == null || !cfg.parchmentMode) {
            return;
        }
        int w = view.width();
        int h = view.height();
        int m = Math.max(3, cfg.parchmentMarginPx);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Text title = Text.translatable("ottoextra.map.parchment.title");
        int titleW = tr.getWidth(title);
        int boxX = (w - titleW) / 2 - 7;
        int boxY = m + 5;
        ctx.fill(boxX - 1, boxY - 1, boxX + titleW + 15, boxY + 12, EDGE);
        ctx.fill(boxX, boxY, boxX + titleW + 14, boxY + 11, 0xD0A88968);
        ctx.drawText(tr, title, boxX + 7, boxY + 2, TEXT, true);
        drawScaleBar(ctx, view, m + 12, h - m - 17);
    }
    private static void drawScaleBar(DrawContext ctx, XaeroMapBridge.View view, int x, int y) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int targetPixels = 72;
        double rawBlocks = targetPixels / Math.max(0.0001, view.effScale());
        int blocks = niceDistance(rawBlocks);
        int pixels = Math.max(18, (int) Math.round(blocks * view.effScale()));
        ctx.fill(x, y, x + pixels, y + 2, EDGE);
        ctx.fill(x, y - 3, x + 2, y + 5, EDGE);
        ctx.fill(x + pixels - 2, y - 3, x + pixels, y + 5, EDGE);
        String label = blocks >= 1000
                ? (blocks % 1000 == 0 ? (blocks / 1000) + " km" : String.format(java.util.Locale.ROOT, "%.1f km", blocks / 1000.0))
                : Text.translatable("ottoextra.map.travel.blocks", blocks).getString();
        ctx.drawText(tr, label, x, y - 11, TEXT, true);
    }
    private static int niceDistance(double raw) {
        if (raw <= 1) {
            return 1;
        }
        double pow = Math.pow(10, Math.floor(Math.log10(raw)));
        double n = raw / pow;
        double nice = n < 1.5 ? 1 : n < 3.5 ? 2 : n < 7.5 ? 5 : 10;
        return Math.max(1, (int) Math.round(nice * pow));
    }
}
