package de.ottoextra.map;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MapSelectionPanel {
    private static final int WIDTH = 278;
    private static final int HEADER = 51;
    private static final int FOOTER = 58;
    private static final int LINE = 12;
    private static final int BG = 0xFFD6BD94;
    private static final int BORDER = 0xFF72583A;
    private static final int LIGHT = 0xFFE7D1AB;
    private static final int TEXT = 0xFF503D29;
    private static final int MUTED = 0xFF786448;

    private static int lastX, lastY, lastW, lastH;
    private static int bodyHeight, maxScroll;
    private static double scroll;
    private static LehenPolygon shownPolygon;
    private static MapSelectionInfo info;
    private static long refreshAt;
    private static int wrappedWidth;
    private static Object wrappedLanguage;
    private static List<OrderedText> body = List.of();

    private MapSelectionPanel() { }

    public static void reset() {
        hide();
        shownPolygon = null;
        info = null;
        scroll = 0;
    }

    public static void hide() {
        lastW = lastH = 0;
    }

    public static void render(DrawContext ctx, XaeroMapBridge.View view, OttoExtraConfig.Map cfg) {
        LehenPolygon poly = PoliticalOverlay.selectedPolygon();
        if (poly == null || !cfg.clickInfoPanel) {
            reset();
            return;
        }
        int x = Math.min(Math.max(8, cfg.parchmentMarginPx + 8), Math.max(8, view.width() - WIDTH - 8));
        int y = Math.min(Math.max(38, cfg.parchmentMarginPx + 34), Math.max(8, view.height() - 170));
        int w = Math.min(WIDTH, view.width() - x - 8);
        int availableHeight = view.height() - y - 30;
        if (w < 130 || availableHeight < HEADER + FOOTER + LINE) {
            hide();
            return;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        updateContent(poly, tr, w - 26);
        int h = Math.min(availableHeight, HEADER + FOOTER + bodyHeight + 8);
        int bodyTop = y + HEADER;
        int bodyBottom = y + h - FOOTER;
        maxScroll = Math.max(0, bodyHeight - (bodyBottom - bodyTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        lastX = x; lastY = y; lastW = w; lastH = h;

        ctx.fill(x + 2, y + 3, x + w + 3, y + h + 3, 0x50000000);
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDER);
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 1, LIGHT);
        ctx.fill(x, y, x + 1, y + h, LIGHT);

        Identifier banner = RegionsServices.banners() == null ? null
                : RegionsServices.banners().bannerForRegion(RegionsServices.data(), poly.key()).orElse(null);
        if (banner != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, x + 8, y + 8,
                    0f, 0f, 32, 32, 32, 32);
        } else {
            // Neutral shield while a crest loads or is unavailable.
            ctx.fill(x + 12, y + 10, x + 36, y + 28, BORDER);
            ctx.fill(x + 14, y + 28, x + 34, y + 32, BORDER);
            ctx.fill(x + 19, y + 32, x + 29, y + 36, BORDER);
            ctx.fill(x + 14, y + 12, x + 34, y + 26, LIGHT);
        }
        int headingX = x + 48;
        ctx.drawText(tr, net.minecraft.util.Language.getInstance().reorder(
                        tr.trimToWidth(Text.literal(info.title()).formatted(Formatting.BOLD), w - 70)),
                headingX, y + 8, TEXT, false);
        String coords = "x " + Math.round(poly.centroidX()) + " / z " + Math.round(poly.centroidZ());
        if (!info.regionId().isBlank()) coords += " | ID: " + info.regionId();
        ctx.drawText(tr, fit(tr, coords, w - 56), headingX, y + 21, MUTED, false);
        ctx.drawText(tr, fit(tr, lang("ottoextra.map.info.fief", info.regionName()), w - 56),
                headingX, y + 33, MUTED, false);
        ctx.drawText(tr, "×", x + w - 14, y + 7, MUTED, false);
        ctx.fill(x + 9, y + HEADER - 5, x + w - 9, y + HEADER - 4, 0x5072583A);

        ctx.enableScissor(x + 8, bodyTop, x + w - 8, bodyBottom);
        try {
            int ty = bodyTop - (int) scroll;
            for (OrderedText line : body) {
                if (ty + LINE > bodyTop && ty < bodyBottom) ctx.drawText(tr, line, x + 10, ty, TEXT, false);
                ty += LINE;
            }
        } finally {
            ctx.disableScissor();
        }
        if (maxScroll > 0) {
            int track = bodyBottom - bodyTop;
            int thumb = Math.max(8, track * track / bodyHeight);
            int thumbY = bodyTop + (int) ((track - thumb) * scroll / maxScroll);
            ctx.fill(x + w - 6, bodyTop, x + w - 4, bodyBottom, 0x3072583A);
            ctx.fill(x + w - 6, thumbY, x + w - 4, thumbY + thumb, MUTED);
        }
        renderTravel(ctx, tr, poly, cfg, x + 10, bodyBottom + 7, w - 20);
    }

    private static void updateContent(LehenPolygon poly, TextRenderer tr, int width) {
        long now = System.currentTimeMillis();
        boolean changed = shownPolygon != poly;
        if (changed) {
            shownPolygon = poly;
            scroll = 0;
        }
        if (changed || info == null || now >= refreshAt) {
            MapSelectionInfo updated = MapSelectionInfo.resolve(poly.key(), RegionsServices.data());
            changed |= !updated.equals(info);
            info = updated;
            refreshAt = now + 1000;
        }
        Object language = net.minecraft.util.Language.getInstance();
        if (!changed && wrappedWidth == width && wrappedLanguage == language) return;
        wrappedWidth = width;
        wrappedLanguage = language;
        List<OrderedText> lines = new ArrayList<>();
        for (String paragraph : paragraphs(info)) {
            if (!lines.isEmpty()) lines.add(OrderedText.EMPTY);
            lines.addAll(tr.wrapLines(Text.literal(paragraph), width));
        }
        body = List.copyOf(lines);
        bodyHeight = body.size() * LINE;
    }

    static List<String> paragraphs(MapSelectionInfo d) {
        return MapSelectionText.paragraphs(d, MapSelectionPanel::lang);
    }

    private static void renderTravel(DrawContext ctx, TextRenderer tr, LehenPolygon poly,
                                     OttoExtraConfig.Map cfg, int x, int y, int width) {
        ctx.fill(x - 1, y - 3, x + width + 1, y - 2, 0x5072583A);
        ctx.drawText(tr, lang("ottoextra.map.info.distance"), x, y, TEXT, false);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            ctx.drawText(tr, lang("ottoextra.map.info.unknown"), x, y + LINE, MUTED, false);
            return;
        }
        double dx = poly.centroidX() - client.player.getX();
        double dz = poly.centroidZ() - client.player.getZ();
        double blocks = Math.hypot(dx, dz);
        int direction = Math.floorMod((int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4)), 8);
        String distance = blocks >= 1000
                ? String.format(Locale.ROOT, "%.1f km", blocks / 1000)
                : lang("ottoextra.map.travel.blocks", Math.round(blocks));
        ctx.drawText(tr, fit(tr, lang("ottoextra.map.info.airline", distance,
                lang("ottoextra.map.info.direction." + direction)), width), x, y + LINE, MUTED, false);
        ctx.drawText(tr, fit(tr, lang("ottoextra.map.info.walk") + ": "
                + formatDuration(blocks / Math.max(0.1, cfg.walkBlocksPerSecond)), width), x, y + LINE * 2, MUTED, false);
        ctx.drawText(tr, fit(tr, lang("ottoextra.map.info.horse") + ": "
                + formatDuration(blocks / Math.max(0.1, cfg.horseBlocksPerSecond)), width), x, y + LINE * 3, MUTED, false);
    }

    static boolean contains(double mouseX, double mouseY) {
        return lastW > 0 && PoliticalOverlay.selectedPolygon() != null
                && mouseX >= lastX && mouseX < lastX + lastW && mouseY >= lastY && mouseY < lastY + lastH;
    }

    public static boolean handleScroll(double mouseX, double mouseY, double amount) {
        if (!contains(mouseX, mouseY)) return false;
        scroll = Math.max(0, Math.min(maxScroll, scroll - amount * LINE * 2));
        return true;
    }

    public static boolean handleClick(Screen screen, XaeroMapBridge.View view, double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) return false;
        if (mouseX >= lastX + lastW - 24 && mouseY <= lastY + 24) {
            PoliticalOverlay.clearSelection();
            reset();
        } else {
            LehenPolygon poly = PoliticalOverlay.selectedPolygon();
            XaeroMapBridge.focusSmoothly(screen, poly.centroidX(), poly.centroidZ());
        }
        return true;
    }

    private static String formatDuration(double seconds) {
        long s = Math.max(0, Math.round(seconds));
        if (s < 60) return lang("ottoextra.map.info.time.seconds", s);
        long minutes = s / 60;
        if (minutes < 60) return lang("ottoextra.map.info.time.minutes", minutes);
        return lang("ottoextra.map.info.time.hours", minutes / 60, minutes % 60);
    }

    private static String fit(TextRenderer tr, String value, int width) {
        return tr.getWidth(value) <= width ? value : tr.trimToWidth(value, Math.max(0, width - tr.getWidth("…"))) + "…";
    }

    private static String lang(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }
}
