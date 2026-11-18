package de.ottoextra.regions;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.map.PoliticalOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class RegionActivityHud {
    private static final int MARGIN = 4;
    private static final int MIN_PANEL_WIDTH = 104;
    private static final int MAX_PANEL_WIDTH = 158;
    private static final int HEADER_HEIGHT = 15;
    private static final int ROW_HEIGHT = 16;
    private static final int FOOTER_HEIGHT = 14;
    private static final int ICON_SIZE = 12;
    private static final int PAD_X = 4;
    private static final int DOT_SIZE = 4;
    private static final int DOT_GAP = 3;
    /** Row content left of the name: dot, gap, coat of arms, gap. */
    private static final int ROW_LEAD = PAD_X + DOT_SIZE + DOT_GAP + ICON_SIZE + 4;

    // Neutral panel: pack tooltip grey-violet, rounded corners (1 px radius), bevel from top left.
    private static final int COL_PANEL = 0x961E1E24;
    private static final int COL_OUTLINE = 0xD7141418;
    private static final int COL_BEVEL_LIGHT = 0xD7514F59;
    private static final int COL_BEVEL_DARK = 0xD72E2D35;
    private static final int COL_ROW_BG = 0x503B3A45;
    private static final int COL_DIVIDER_DARK = 0xC8141418;
    private static final int COL_DIVIDER_LIGHT = 0xC83B3A45;
    private static final int COL_ICON_PLACEHOLDER = 0x553A3A3A;
    private static final int COL_TITLE = 0xFFD6C194;
    private static final int COL_FACTION = 0xFFF0EBE0;
    private static final int COL_FOOTER = 0xFF8F8C96;

    private static final float FOOTER_TEXT_SCALE = 0.75f;
    private static final int ICON_PX = 7;
    private static final int FOOTER_ICON_GAP = 2;
    private static final int FOOTER_ITEM_GAP = 6;
    /** Circular arrow: time of the last data update ("Stand"). */
    private static final String[] ICON_UPDATED = {
            "..XXX.X",
            ".X...XX",
            "X...XXX",
            "X......",
            "X.....X",
            ".X...X.",
            "..XXX.."};
    /** Clock face with hands: current time. */
    private static final String[] ICON_CLOCK = {
            "..XXX..",
            ".X...X.",
            "X..X..X",
            "X..XX.X",
            "X.....X",
            ".X...X.",
            "..XXX.."};

    private static final DateTimeFormatter UPDATED_TIME = DateTimeFormatter
            .ofPattern("HH:mm", Locale.GERMAN)
            .withZone(ZoneId.systemDefault());

    private static volatile OttoExtraConfig.Regions config;
    private static volatile BooleanSupplier onOttonien = () -> false;
    private static volatile int lastPanelWidth;
    private static volatile int lastPanelHeight;

    /** Unscaled size of the last rendered panel as {width, height}, or null if it never rendered. */
    public static int[] lastPanelSize() {
        int w = lastPanelWidth;
        int h = lastPanelHeight;
        return w > 0 && h > 0 ? new int[]{w, h} : null;
    }

    private RegionActivityHud() {
    }

    public static void configure(OttoExtraConfig.Regions regions,
                                 BooleanSupplier serverCheck) {
        config = regions;
        onOttonien = serverCheck != null ? serverCheck : () -> false;
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        OttoExtraConfig.Regions cfg = config;
        MinecraftClient client = MinecraftClient.getInstance();
        if (cfg == null || !cfg.enabled || !cfg.activityHudEnabled
                || client == null || client.player == null || client.options.hudHidden
                || !onOttonien.getAsBoolean()) {
            return;
        }

        RegionDataService data = RegionsServices.data();
        if (data == null || data.activityUpdatedAtMs() <= 0L) {
            return;
        }
        List<RegionDataService.ActivityEntry> all = data.activityEntries();
        if (all.isEmpty()) {
            return;
        }

        int screenHeight = client.getWindow().getScaledHeight();
        float scale = clampScale(cfg.activityHudScale);
        int availableUnscaledHeight = Math.max(ROW_HEIGHT,
                (int) ((screenHeight - 2.0f * MARGIN) / scale)
                        - HEADER_HEIGHT - FOOTER_HEIGHT);
        int maxByScreen = Math.max(1, availableUnscaledHeight / ROW_HEIGHT);
        int visibleCount = Math.min(all.size(),
                Math.min(Math.max(1, cfg.activityHudMaxEntries), maxByScreen));
        if (visibleCount <= 0) {
            return;
        }

        TextRenderer renderer = client.textRenderer;
        String title = Text.translatable("ottoextra.activityHud.title").getString();
        List<FooterItem> footer = footerItems(cfg, data.activityUpdatedAtMs(),
                System.currentTimeMillis());
        int footerWidth = footerWidth(renderer, footer);
        String more = all.size() > visibleCount
                ? Text.translatable("ottoextra.activityHud.more",
                all.size() - visibleCount).getString() : "";

        int panelWidth = compactPanelWidth(renderer, all, visibleCount, title, more, footerWidth);
        int panelHeight = HEADER_HEIGHT + visibleCount * ROW_HEIGHT + FOOTER_HEIGHT;
        int renderedWidth = Math.round(panelWidth * scale);
        int renderedHeight = Math.round(panelHeight * scale);
        lastPanelWidth = panelWidth;
        lastPanelHeight = panelHeight;
        int[] pos = position(client.getWindow().getScaledWidth(), screenHeight,
                renderedWidth, renderedHeight, cfg);
        int x = pos[0];
        int y = pos[1];
        int[] flowed = de.ottoextra.ui.hudlayout.HudFlow.place("ottoextra:activity", x, y, renderedWidth,
                renderedHeight, cfg.activityHudPosition != null
                        && cfg.activityHudPosition.toUpperCase(Locale.ROOT).startsWith("BOTTOM"),
                de.ottoextra.ui.hudlayout.HudFlow.PRIORITY_ACTIVITY_HUD);
        if (flowed != null) {
            x = flowed[0];
            y = flowed[1];
        }

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        try {
            drawPanel(ctx, 0, 0, panelWidth, panelHeight);
            ctx.drawTextWithShadow(renderer, title, PAD_X, 3, COL_TITLE);
            drawDivider(ctx, 2, HEADER_HEIGHT - 3, panelWidth - 4);

            int rowY = HEADER_HEIGHT;
            for (int i = 0; i < visibleCount; i++) {
                drawRow(ctx, renderer, data, all.get(i), 0, rowY, panelWidth);
                rowY += ROW_HEIGHT;
            }

            int footerY = panelHeight - 10;
            if (!more.isEmpty() || !footer.isEmpty()) {
                drawDivider(ctx, 2, rowY, panelWidth - 4);
            }
            if (!more.isEmpty()) {
                int available = Math.max(20, panelWidth - 2 * PAD_X - footerWidth - FOOTER_ITEM_GAP);
                drawSmallText(ctx, renderer,
                        renderer.trimToWidth(more, Math.round(available / FOOTER_TEXT_SCALE)),
                        PAD_X, footerY + 1);
            }
            if (!footer.isEmpty()) {
                drawFooter(ctx, renderer, footer,
                        Math.max(PAD_X, panelWidth - PAD_X - footerWidth), footerY);
            }
        } finally {
            matrices.popMatrix();
        }
    }

    private static int compactPanelWidth(TextRenderer renderer,
                                         List<RegionDataService.ActivityEntry> entries,
                                         int visibleCount, String title,
                                         String more, int footerWidth) {
        int width = Math.max(MIN_PANEL_WIDTH, renderer.getWidth(title) + 2 * PAD_X);
        width = Math.max(width, footerWidth + 2 * PAD_X);
        if (!more.isEmpty()) {
            width = Math.max(width, Math.round(renderer.getWidth(more) * FOOTER_TEXT_SCALE)
                    + footerWidth + FOOTER_ITEM_GAP + 2 * PAD_X);
        }
        for (int i = 0; i < visibleCount; i++) {
            String displayName = PoliticalOverlay.displayNameFor(entries.get(i).displayName());
            int nameWidth = Math.min(108, renderer.getWidth(displayName));
            width = Math.max(width, ROW_LEAD + nameWidth + PAD_X);
        }
        return Math.min(MAX_PANEL_WIDTH, width);
    }

    private static void drawRow(DrawContext ctx, TextRenderer renderer,
                                RegionDataService data,
                                RegionDataService.ActivityEntry entry,
                                int x, int y, int panelWidth) {
        ActivityLevel level = ActivityLevel.fromApiValue(entry.activityLevel());
        drawRoundedFlat(ctx, x + 3, y, panelWidth - 6, ROW_HEIGHT - 1, COL_ROW_BG);
        drawDot(ctx, x + PAD_X + 1, y + (ROW_HEIGHT - 1 - DOT_SIZE) / 2, level);

        int iconX = x + PAD_X + 1 + DOT_SIZE + DOT_GAP;
        int iconY = y + 1 + (ROW_HEIGHT - 1 - ICON_SIZE) / 2;
        Identifier banner = resolveBanner(data, entry);
        if (banner != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, iconX, iconY,
                    0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            ctx.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, COL_ICON_PLACEHOLDER);
        }

        int nameX = iconX + ICON_SIZE + 4;
        int maxNameWidth = Math.max(24, x + panelWidth - PAD_X - nameX);
        String displayName = PoliticalOverlay.displayNameFor(entry.displayName());
        String name = renderer.trimToWidth(displayName, maxNameWidth);
        int baseline = y + (ROW_HEIGHT - renderer.fontHeight) / 2 + 1;
        ctx.drawTextWithShadow(renderer, name, nameX, baseline, COL_FACTION);
    }

    /** One footer entry: a small computed icon followed by a time. */
    record FooterItem(String[] icon, String time) {
    }

    static List<FooterItem> footerItems(OttoExtraConfig.Regions cfg, long updatedAtMs, long nowMs) {
        FooterMode mode = footerMode(cfg);
        List<FooterItem> items = new java.util.ArrayList<>(2);
        if (mode == FooterMode.UPDATED_ONLY || mode == FooterMode.UPDATED_AND_CLOCK) {
            items.add(new FooterItem(ICON_UPDATED, formatTime(updatedAtMs)));
        }
        if (mode == FooterMode.CLOCK_ONLY || mode == FooterMode.UPDATED_AND_CLOCK) {
            items.add(new FooterItem(ICON_CLOCK, formatTime(nowMs)));
        }
        return items;
    }

    private static int footerWidth(TextRenderer renderer, List<FooterItem> items) {
        int width = 0;
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) width += FOOTER_ITEM_GAP;
            width += ICON_PX + FOOTER_ICON_GAP
                    + Math.round(renderer.getWidth(items.get(i).time()) * FOOTER_TEXT_SCALE);
        }
        return width;
    }

    private static void drawFooter(DrawContext ctx, TextRenderer renderer,
                                   List<FooterItem> items, int x, int y) {
        int cx = x;
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) cx += FOOTER_ITEM_GAP;
            FooterItem item = items.get(i);
            drawIcon(ctx, item.icon(), cx, y, COL_FOOTER);
            cx += ICON_PX + FOOTER_ICON_GAP;
            drawSmallText(ctx, renderer, item.time(), cx, y + 1);
            cx += Math.round(renderer.getWidth(item.time()) * FOOTER_TEXT_SCALE);
        }
    }

    private static void drawSmallText(DrawContext ctx, TextRenderer renderer, String text, int x, int y) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(FOOTER_TEXT_SCALE, FOOTER_TEXT_SCALE);
        ctx.drawText(renderer, text, 0, 0, COL_FOOTER, false);
        m.popMatrix();
    }

    /** Draws a 7x7 icon pattern ('X' = pixel) as single-pixel fills. */
    private static void drawIcon(DrawContext ctx, String[] pattern, int x, int y, int color) {
        for (int row = 0; row < pattern.length; row++) {
            String line = pattern[row];
            int col = 0;
            while (col < line.length()) {
                if (line.charAt(col) != 'X') {
                    col++;
                    continue;
                }
                int start = col;
                while (col < line.length() && line.charAt(col) == 'X') col++;
                ctx.fill(x + start, y + row, x + col, y + row + 1, color);
            }
        }
    }

    /** Rounded panel (corner pixel left out) with 1 px outline and top-left bevel; no overlapping fills. */
    private static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        drawPanel(ctx, x, y, w, h, 1.0f);
    }

    /** Same panel, shared with the animal info toast; {@code alpha} scales all colours for fading. */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, float alpha) {
        int outline = fade(COL_OUTLINE, alpha);
        int light = fade(COL_BEVEL_LIGHT, alpha);
        int dark = fade(COL_BEVEL_DARK, alpha);
        int panel = fade(COL_PANEL, alpha);
        int r = x + w;
        int b = y + h;
        // Outline without the four corner pixels, plus the inner diagonal pixels.
        ctx.fill(x + 1, y, r - 1, y + 1, outline);
        ctx.fill(x + 1, b - 1, r - 1, b, outline);
        ctx.fill(x, y + 1, x + 1, b - 1, outline);
        ctx.fill(r - 1, y + 1, r, b - 1, outline);
        ctx.fill(x + 1, y + 1, x + 2, y + 2, outline);
        ctx.fill(r - 2, y + 1, r - 1, y + 2, outline);
        ctx.fill(x + 1, b - 2, x + 2, b - 1, outline);
        ctx.fill(r - 2, b - 2, r - 1, b - 1, outline);
        // Bevel: light top/left, dark bottom/right.
        ctx.fill(x + 2, y + 1, r - 2, y + 2, light);
        ctx.fill(x + 1, y + 2, x + 2, b - 2, light);
        ctx.fill(x + 2, b - 2, r - 2, b - 1, dark);
        ctx.fill(r - 2, y + 2, r - 1, b - 2, dark);
        ctx.fill(x + 2, y + 2, r - 2, b - 2, panel);
    }

    private static int fade(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Flat rectangle with 1 px rounded corners. */
    private static void drawRoundedFlat(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void drawDivider(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, COL_DIVIDER_DARK);
        ctx.fill(x, y + 1, x + w, y + 2, COL_DIVIDER_LIGHT);
    }

    /** 4x4 activity dot: corners cut, light pixel top left, dark edge bottom/right. */
    private static void drawDot(DrawContext ctx, int x, int y, ActivityLevel level) {
        ctx.fill(x + 1, y, x + 3, y + 3, level.color);
        ctx.fill(x, y + 1, x + 1, y + 3, level.color);
        ctx.fill(x + 1, y + 3, x + 3, y + 4, level.dark);
        ctx.fill(x + 3, y + 1, x + 4, y + 3, level.dark);
        ctx.fill(x + 1, y + 1, x + 2, y + 2, level.light);
    }

    private static Identifier resolveBanner(RegionDataService data,
                                            RegionDataService.ActivityEntry entry) {
        BannerTextureService banners = RegionsServices.banners();
        if (banners == null) {
            return null;
        }
        return banners.bannerForRegion(data, entry.regionKey()).orElse(null);
    }

    static int[] position(int screenWidth, int screenHeight, int width, int height,
                          OttoExtraConfig.Regions cfg) {
        String value = cfg.activityHudPosition == null
                ? "MIDDLE_LEFT" : cfg.activityHudPosition.toUpperCase(Locale.ROOT);
        if ("CUSTOM".equals(value)) {
            int availableX = Math.max(0, screenWidth - width);
            int availableY = Math.max(0, screenHeight - height);
            return new int[]{
                    Math.round(clampUnit(cfg.activityHudCustomX) * availableX),
                    Math.round(clampUnit(cfg.activityHudCustomY) * availableY)
            };
        }
        boolean right = value.endsWith("RIGHT");
        boolean top = value.startsWith("TOP");
        boolean bottom = value.startsWith("BOTTOM");
        int x = right ? screenWidth - width - MARGIN : MARGIN;
        int y = top ? MARGIN : bottom ? screenHeight - height - MARGIN
                : (screenHeight - height) / 2;
        x += cfg.activityHudOffsetX;
        y += cfg.activityHudOffsetY;
        x = Math.max(0, Math.min(screenWidth - width, x));
        y = Math.max(0, Math.min(screenHeight - height, y));
        return new int[]{x, y};
    }

    static float clampScale(float value) {
        return Math.max(0.5f, Math.min(2.0f, value));
    }

    static float clampUnit(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    static String formatTime(long epochMillis) {
        return UPDATED_TIME.format(Instant.ofEpochMilli(epochMillis));
    }

    static FooterMode footerMode(OttoExtraConfig.Regions cfg) {
        if (cfg.activityHudShowLastUpdated) {
            return cfg.activityHudShowClock
                    ? FooterMode.UPDATED_AND_CLOCK : FooterMode.UPDATED_ONLY;
        }
        return cfg.activityHudShowClock ? FooterMode.CLOCK_ONLY : FooterMode.HIDDEN;
    }

    static String footerText(OttoExtraConfig.Regions cfg, long updatedAtMs, long nowMs) {
        String updated = formatTime(updatedAtMs);
        String now = formatTime(nowMs);
        return switch (footerMode(cfg)) {
            case UPDATED_AND_CLOCK -> Text.translatable(
                    "ottoextra.activityHud.updatedWithClock", updated, now).getString();
            case UPDATED_ONLY -> Text.translatable(
                    "ottoextra.activityHud.updated", updated).getString();
            case CLOCK_ONLY -> Text.translatable(
                    "ottoextra.activityHud.clock", now).getString();
            case HIDDEN -> "";
        };
    }

    enum FooterMode {
        UPDATED_AND_CLOCK,
        UPDATED_ONLY,
        CLOCK_ONLY,
        HIDDEN
    }

    private enum ActivityLevel {
        // Pack gold and life red instead of saturated material colors.
        HIGH(0xFFCD9F3D, 0xFFDBB451, 0xFF96752C),
        VERY_HIGH(0xFFB24147, 0xFFC4545A, 0xFF6E2226);

        private final int color;
        private final int light;
        private final int dark;

        ActivityLevel(int color, int light, int dark) {
            this.color = color;
            this.light = light;
            this.dark = dark;
        }

        private static ActivityLevel fromApiValue(int value) {
            return value >= 2 ? VERY_HIGH : HIGH;
        }
    }
}
