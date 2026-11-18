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
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MapSelectionPanel {
    private static final int WIDTH = 218;
    private static final int HEADER = 46;
    private static final int STRIP_HEIGHT = 17;
    private static final int STRIP_INSET = 3;
    private static final int FOOTER = STRIP_HEIGHT + STRIP_INSET + 4;
    private static final int LINE = 10;
    private static final int ICON = 9;
    private static final int COL_OUTLINE = 0xFF141418;
    private static final int COL_PANEL = 0xEB1E1E24;
    private static final int COL_BEVEL_LIGHT = 0xFF514F59;
    private static final int COL_BEVEL_DARK = 0xFF2E2D35;
    private static final int COL_STRIP = 0xFF2E2D35;
    private static final int COL_NAME = 0xFFD6C194;
    private static final int COL_MUTED = 0xFFA9A6B0;
    private static final int COL_TEXT = 0xFFF0EBE0;
    private static final int COL_RULE = 0xFF866C36;
    private static final int COL_RULE_DARK = 0xFF47391D;
    private static final int COL_DIAMOND_TOP = 0xFFDBB451;
    private static final int COL_DIAMOND_MID = 0xFFB58D36;
    private static final Identifier ICON_DISTANCE =
            Identifier.of("ottoextra", "textures/gui/worldmap/travel_distance.png");
    private static final Identifier ICON_WALK =
            Identifier.of("ottoextra", "textures/gui/worldmap/travel_walk.png");
    private static final Identifier ICON_HORSE =
            Identifier.of("ottoextra", "textures/gui/worldmap/travel_horse.png");

    private static int lastX, lastY, lastW, lastH;
    private static int bodyHeight, maxScroll;
    private static double scroll;
    private static LehenPolygon shownPolygon;
    private static MapSelectionInfo info;
    private static long refreshAt;
    private static int wrappedWidth;
    private static Object wrappedLanguage;
    private static List<OrderedText> body = List.of();
    private static List<Integer> bodyColors = List.of();

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
        updateContent(poly, tr, w - 14);
        int h = Math.min(availableHeight, HEADER + FOOTER + bodyHeight);
        int bodyTop = y + HEADER;
        int bodyBottom = y + h - FOOTER;
        maxScroll = Math.max(0, bodyHeight - (bodyBottom - bodyTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        lastX = x; lastY = y; lastW = w; lastH = h;

        drawPanel(ctx, x, y, w, h);

        Identifier banner = RegionsServices.banners() == null || RegionsServices.data() == null ? null
                : RegionsServices.banners().bannerForRegion(RegionsServices.data(), poly.key()).orElse(null);
        int headingX = x + 7;
        if (banner != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, x + 6, y + 6,
                    0f, 0f, 28, 28, 28, 28);
            headingX = x + 40;
        }
        int headingWidth = x + w - 14 - headingX;
        ctx.drawText(tr, fit(tr, info.title(), headingWidth), headingX, y + 8, COL_NAME, true);
        String subtitle = info.rank().isBlank() ? info.regionName() : info.rank();
        ctx.drawText(tr, fit(tr, subtitle, headingWidth), headingX, y + 19, COL_MUTED, false);
        ctx.drawText(tr, "×", x + w - 12, y + 5, COL_MUTED, false);
        drawRule(ctx, x + 6, y + 39, w - 13);

        ctx.enableScissor(x + 4, bodyTop, x + w - 4, bodyBottom);
        try {
            int ty = bodyTop - (int) scroll;
            for (int i = 0; i < body.size(); i++) {
                if (ty + LINE > bodyTop && ty < bodyBottom) {
                    ctx.drawText(tr, body.get(i), x + 7, ty, bodyColors.get(i), false);
                }
                ty += LINE;
            }
        } finally {
            ctx.disableScissor();
        }
        if (maxScroll > 0) {
            int track = bodyBottom - bodyTop;
            int thumb = Math.max(8, track * track / bodyHeight);
            int thumbY = bodyTop + (int) ((track - thumb) * scroll / maxScroll);
            ctx.fill(x + w - 6, bodyTop, x + w - 4, bodyBottom, COL_BEVEL_DARK);
            ctx.fill(x + w - 6, thumbY, x + w - 4, thumbY + thumb, COL_BEVEL_LIGHT);
        }
        renderTravel(ctx, tr, poly, cfg, x, y + h - STRIP_INSET - STRIP_HEIGHT, w);
    }

    /** Rounded panel (corner pixel left out) with 1 px outline and top-left bevel; no overlapping fills. */
    private static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        int r = x + w;
        int b = y + h;
        ctx.fill(x + 1, y, r - 1, y + 1, COL_OUTLINE);
        ctx.fill(x + 1, b - 1, r - 1, b, COL_OUTLINE);
        ctx.fill(x, y + 1, x + 1, b - 1, COL_OUTLINE);
        ctx.fill(r - 1, y + 1, r, b - 1, COL_OUTLINE);
        ctx.fill(x + 1, y + 1, x + 2, y + 2, COL_OUTLINE);
        ctx.fill(r - 2, y + 1, r - 1, y + 2, COL_OUTLINE);
        ctx.fill(x + 1, b - 2, x + 2, b - 1, COL_OUTLINE);
        ctx.fill(r - 2, b - 2, r - 1, b - 1, COL_OUTLINE);
        ctx.fill(x + 2, y + 1, r - 2, y + 2, COL_BEVEL_LIGHT);
        ctx.fill(x + 1, y + 2, x + 2, b - 2, COL_BEVEL_LIGHT);
        ctx.fill(x + 2, b - 2, r - 2, b - 1, COL_BEVEL_DARK);
        ctx.fill(r - 2, y + 2, r - 1, b - 2, COL_BEVEL_DARK);
        ctx.fill(x + 2, y + 2, r - 2, b - 2, COL_PANEL);
    }

    /** Two-pixel gold rule with a small diamond at each end. */
    private static void drawRule(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, COL_RULE);
        ctx.fill(x, y + 1, x + w, y + 2, COL_RULE_DARK);
        drawDiamond(ctx, x - 1, y - 1);
        drawDiamond(ctx, x + w - 2, y - 1);
    }

    private static void drawDiamond(DrawContext ctx, int x, int y) {
        ctx.fill(x + 1, y, x + 2, y + 1, COL_DIAMOND_TOP);
        ctx.fill(x, y + 1, x + 3, y + 2, COL_DIAMOND_MID);
        ctx.fill(x + 1, y + 2, x + 2, y + 3, COL_RULE_DARK);
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
        List<Integer> colors = new ArrayList<>();
        // Only the residents/leader sentence is highlighted; relations and descriptions are secondary.
        boolean first = !info.narrativeOnly() && info.residents() != null;
        for (String paragraph : paragraphs(info)) {
            List<OrderedText> wrapped = tr.wrapLines(Text.literal(paragraph), width);
            lines.addAll(wrapped);
            for (int i = 0; i < wrapped.size(); i++) colors.add(first ? COL_TEXT : COL_MUTED);
            first = false;
        }
        body = List.copyOf(lines);
        bodyColors = List.copyOf(colors);
        bodyHeight = body.size() * LINE;
    }

    static List<String> paragraphs(MapSelectionInfo d) {
        return MapSelectionText.paragraphs(d, MapSelectionPanel::lang);
    }

    private static void renderTravel(DrawContext ctx, TextRenderer tr, LehenPolygon poly,
                                     OttoExtraConfig.Map cfg, int x, int y, int w) {
        ctx.fill(x + STRIP_INSET, y, x + w - STRIP_INSET, y + STRIP_HEIGHT, COL_STRIP);
        ctx.fill(x + STRIP_INSET, y, x + w - STRIP_INSET, y + 1, COL_OUTLINE);
        int textY = y + (STRIP_HEIGHT - 8) / 2 + 1;
        int iconY = y + (STRIP_HEIGHT - ICON) / 2 + 1;
        MinecraftClient client = MinecraftClient.getInstance();
        int available = w - 14;
        if (client.player == null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICON_DISTANCE, x + 7, iconY,
                    0f, 0f, ICON, ICON, ICON, ICON);
            ctx.drawText(tr, fit(tr, lang("ottoextra.map.info.unknown"), available - ICON - 3),
                    x + 7 + ICON + 3, textY, COL_TEXT, false);
            return;
        }
        double dx = poly.centroidX() - client.player.getX();
        double dz = poly.centroidZ() - client.player.getZ();
        double blocks = Math.hypot(dx, dz);
        int direction = Math.floorMod((int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4)), 8);
        String distance = blocks >= 1000
                ? String.format(Locale.ROOT, "%.1f km", blocks / 1000)
                : lang("ottoextra.map.travel.blocks", Math.round(blocks));
        String[] values = {
                distance + " " + lang("ottoextra.map.info.direction." + direction),
                formatDuration(blocks / Math.max(0.1, cfg.walkBlocksPerSecond)),
                formatDuration(blocks / Math.max(0.1, cfg.horseBlocksPerSecond))
        };
        Identifier[] icons = {ICON_DISTANCE, ICON_WALK, ICON_HORSE};
        int fixed = 3 * (ICON + 3);
        int textTotal = 0;
        for (String value : values) textTotal += tr.getWidth(value);
        int gap = Math.max(2, Math.min(8, (available - fixed - textTotal) / 2));
        int overflow = fixed + textTotal + 2 * gap - available;
        if (overflow > 0) values[0] = fit(tr, values[0], Math.max(8, tr.getWidth(values[0]) - overflow));
        int cx = x + 7;
        for (int i = 0; i < 3; i++) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icons[i], cx, iconY,
                    0f, 0f, ICON, ICON, ICON, ICON);
            cx += ICON + 3;
            ctx.drawText(tr, values[i], cx, textY, COL_TEXT, false);
            cx += tr.getWidth(values[i]) + gap;
        }
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
