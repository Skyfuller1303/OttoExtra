package de.ottoextra.regions;

import de.ottoextra.api.model.RegionRecord;
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
import java.util.Optional;
import java.util.function.BooleanSupplier;

public final class RegionActivityHud {
    private static final int MARGIN = 4;
    private static final int MIN_PANEL_WIDTH = 104;
    private static final int MAX_PANEL_WIDTH = 158;
    private static final int HEADER_HEIGHT = 13;
    private static final int ROW_HEIGHT = 16;
    private static final int FOOTER_HEIGHT = 11;
    private static final int ICON_SIZE = 12;
    private static final int PAD_X = 3;

    private static final int COL_BG = 0x48000000;
    private static final int COL_ROW_BG = 0x26000000;
    private static final int COL_TITLE = 0xFFE8E8E8;
    private static final int COL_FACTION = 0xFFFFFFFF;
    private static final int COL_FOOTER = 0xFFAAAAAA;

    private static final DateTimeFormatter UPDATED_TIME = DateTimeFormatter
            .ofPattern("HH:mm", Locale.GERMAN)
            .withZone(ZoneId.systemDefault());

    private static volatile OttoExtraConfig.Regions config;
    private static volatile BooleanSupplier onOttonien = () -> false;

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
        String updated = footerText(cfg, data.activityUpdatedAtMs(),
                System.currentTimeMillis());
        String more = all.size() > visibleCount
                ? Text.translatable("ottoextra.activityHud.more",
                all.size() - visibleCount).getString() : "";

        int panelWidth = compactPanelWidth(renderer, all, visibleCount, title, more, updated);
        int panelHeight = HEADER_HEIGHT + visibleCount * ROW_HEIGHT + FOOTER_HEIGHT;
        int renderedWidth = Math.round(panelWidth * scale);
        int renderedHeight = Math.round(panelHeight * scale);
        int[] pos = position(client.getWindow().getScaledWidth(), screenHeight,
                renderedWidth, renderedHeight, cfg);
        int x = pos[0];
        int y = pos[1];

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        try {
            ctx.fill(0, 0, panelWidth, panelHeight, COL_BG);
            ctx.drawTextWithShadow(renderer, title, PAD_X, 2, COL_TITLE);

            int rowY = HEADER_HEIGHT;
            for (int i = 0; i < visibleCount; i++) {
                drawRow(ctx, renderer, data, all.get(i), 0, rowY, panelWidth);
                rowY += ROW_HEIGHT;
            }

            int footerY = panelHeight - 9;
            if (!more.isEmpty()) {
                int available = Math.max(20, panelWidth - 9 - renderer.getWidth(updated));
                ctx.drawText(renderer, renderer.trimToWidth(more, available),
                        PAD_X, footerY, COL_FOOTER, false);
            }
            if (!updated.isEmpty()) {
                int updatedX = panelWidth - PAD_X - renderer.getWidth(updated);
                ctx.drawText(renderer, updated, Math.max(PAD_X, updatedX),
                        footerY, COL_FOOTER, false);
            }
        } finally {
            matrices.popMatrix();
        }
    }

    private static int compactPanelWidth(TextRenderer renderer,
                                         List<RegionDataService.ActivityEntry> entries,
                                         int visibleCount, String title,
                                         String more, String updated) {
        int width = Math.max(MIN_PANEL_WIDTH, renderer.getWidth(title) + 2 * PAD_X);
        width = Math.max(width, renderer.getWidth(updated) + 2 * PAD_X);
        if (!more.isEmpty()) {
            width = Math.max(width,
                    renderer.getWidth(more) + renderer.getWidth(updated) + 9);
        }
        for (int i = 0; i < visibleCount; i++) {
            String displayName = PoliticalOverlay.displayNameFor(entries.get(i).displayName());
            int nameWidth = Math.min(108, renderer.getWidth(displayName));
            width = Math.max(width, 20 + nameWidth + PAD_X);
        }
        return Math.min(MAX_PANEL_WIDTH, width);
    }

    private static void drawRow(DrawContext ctx, TextRenderer renderer,
                                RegionDataService data,
                                RegionDataService.ActivityEntry entry,
                                int x, int y, int panelWidth) {
        ActivityLevel level = ActivityLevel.fromApiValue(entry.activityLevel());
        ctx.fill(x + 1, y, x + panelWidth - 1, y + ROW_HEIGHT, COL_ROW_BG);
        ctx.fill(x + 1, y + 1, x + 3, y + ROW_HEIGHT - 1, level.color);

        int iconX = x + PAD_X + 2;
        int iconY = y + 2;
        Identifier banner = resolveBanner(data, entry);
        if (banner != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, iconX, iconY,
                    0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            ctx.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0x553A3A3A);
        }

        int nameX = iconX + ICON_SIZE + 3;
        int maxNameWidth = Math.max(24, x + panelWidth - PAD_X - nameX);
        String displayName = PoliticalOverlay.displayNameFor(entry.displayName());
        String name = renderer.trimToWidth(displayName, maxNameWidth);
        int baseline = y + (ROW_HEIGHT - renderer.fontHeight) / 2;
        ctx.drawTextWithShadow(renderer, name, nameX, baseline, COL_FACTION);
    }

    private static Identifier resolveBanner(RegionDataService data,
                                            RegionDataService.ActivityEntry entry) {
        BannerTextureService banners = RegionsServices.banners();
        if (banners == null) {
            return null;
        }
        Identifier factionBanner = data.factionForRegion(entry.regionKey())
                .flatMap(banners::bannerFor).orElse(null);
        if (factionBanner != null) {
            return factionBanner;
        }
        Optional<RegionRecord> region = data.regionByName(entry.regionKey());
        if (region.isPresent()) {
            String path = region.get().effectiveRegionBannerPath();
            if (path != null && !path.isBlank()) {
                return banners.bannerForPath("activity-" + entry.regionKey(), path)
                        .orElse(null);
            }
        }
        return null;
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
        HIGH(0xFFFFD54F),
        VERY_HIGH(0xFFFF5252);

        private final int color;

        ActivityLevel(int color) {
            this.color = color;
        }

        private static ActivityLevel fromApiValue(int value) {
            return value >= 2 ? VERY_HIGH : HIGH;
        }
    }
}
