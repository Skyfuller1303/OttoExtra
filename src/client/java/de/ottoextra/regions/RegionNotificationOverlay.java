package de.ottoextra.regions;

import de.ottoextra.api.model.FactionRecord;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RegionNotificationOverlay {

    private static final long DEFAULT_DISPLAY_MS = 4_200L;
    private static final long FADE_IN_MS = 260L;
    private static final long FADE_OUT_MS = 360L;

    private static final int MIN_WIDTH = 170;
    private static final int MAX_WIDTH = 330;
    private static final int MAX_TEXT_WIDTH = 210;
    private static final int BANNER_SIZE = 24;
    private static final int PAD_LEFT = 5;
    private static final int PAD_RIGHT = 10;
    private static final int PAD_V = 4;
    private static final int ICON_GAP = 6;
    private static final int MARGIN = 6;

    private record Palette(int bg, int borderOut, int borderTl, int borderBr,
                           int title, int region, int hierarchy, int hint) {
    }

    private static final Palette LIGHT = new Palette(
            0xFFC8AC8E, 0xFF513E2A, 0xFFE6C8A9, 0xFFB8926E,
            0xFF503D29, 0xFF503D29, 0xFF7A5A3A, 0xFF6A4D33);
    private static final Palette DARK = new Palette(
            0xFF212D3B, 0xFF0A0A0A, 0xFF344459, 0xFF191F22,
            0xFFFFFFFF, 0xFFFFFFFF, 0xFF9BC7DC, 0xFF9D9D9D);

    public enum Position { TOP_CENTER, TOP_RIGHT, TOP_LEFT, CENTER }

    private static volatile String regionName = "";
    private static volatile String hierarchy = "";
    private static volatile long shownAt = 0;

    private static volatile boolean sticky = false;
    private static volatile String menuKeyName = "L";

    private static volatile de.ottoextra.config.OttoExtraConfig.Regions cfg;

    private RegionNotificationOverlay() {
    }

    public static void show(String name, String hierarchyLine) {
        regionName = name == null ? "" : name;
        hierarchy = hierarchyLine == null ? "" : hierarchyLine;
        shownAt = System.currentTimeMillis();
        sticky = false;
    }

    public static void holdPreview(String name, String hierarchyLine) {
        regionName = name == null ? "" : name;
        hierarchy = hierarchyLine == null ? "" : hierarchyLine;
        shownAt = System.currentTimeMillis();
        sticky = true;
    }

    public static void clear() {
        shownAt = 0;
        sticky = false;
    }

    public static void setMenuKeyName(String name) {
        if (name != null && !name.isBlank()) {
            menuKeyName = name;
        }
    }

    public static void configure(de.ottoextra.config.OttoExtraConfig.Regions regionsConfig) {
        cfg = regionsConfig;
    }

    private static Palette activePalette() {
        de.ottoextra.config.OttoExtraConfig.Regions c = cfg;
        if (c == null || c.theme == null || "light".equalsIgnoreCase(c.theme)) {
            return LIGHT;
        }
        if ("dark".equalsIgnoreCase(c.theme)) {
            return DARK;
        }
        if (c.customThemes != null) {
            for (de.ottoextra.config.OttoExtraConfig.RegionTheme t : c.customThemes) {
                if (t != null && t.name != null && t.name.equalsIgnoreCase(c.theme)) {
                    return paletteOf(t);
                }
            }
        }
        return LIGHT;
    }

    private static Palette paletteOf(de.ottoextra.config.OttoExtraConfig.RegionTheme t) {
        return new Palette(
                hex(t.bg, 0xFFC8AC8E), hex(t.borderOut, 0xFF513E2A),
                hex(t.borderTl, 0xFFE6C8A9), hex(t.borderBr, 0xFFB8926E),
                hex(t.title, 0xFF503D29), hex(t.region, 0xFF503D29),
                hex(t.hierarchy, 0xFF7A5A3A), hex(t.hint, 0xFF6A4D33));
    }

    private static int hex(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        String h = s.replace("#", "").trim();
        if (!h.matches("[0-9a-fA-F]{6}")) {
            return fallback;
        }
        return 0xFF000000 | Integer.parseInt(h, 16);
    }

    private static boolean needsShadow(Palette pal) {
        int bg = pal.bg();
        int r = (bg >> 16) & 0xFF, g = (bg >> 8) & 0xFF, b = bg & 0xFF;
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 128.0;
    }

    private record Style(Palette pal,
                         float baseTextScale, float titleScale, float regionScale,
                         float hierarchyScale, float hintScale,
                         boolean showBanner, boolean showEnteredTitle, boolean showHierarchy,
                         boolean showHint,
                         int maxTextWidth, int minWidth, int maxWidth, int topMargin,
                         int iconSize, int iconGap,
                         int padLeft, int padRight, int padTop, int padBottom) {
    }

    private static Style activeStyle() {
        de.ottoextra.config.OttoExtraConfig.Regions c = cfg;
        if (c != null && c.theme != null && c.customThemes != null
                && !"light".equalsIgnoreCase(c.theme) && !"dark".equalsIgnoreCase(c.theme)) {
            for (de.ottoextra.config.OttoExtraConfig.RegionTheme t : c.customThemes) {
                if (t != null && t.name != null && t.name.equalsIgnoreCase(c.theme)) {
                    return new Style(paletteOf(t),
                            t.baseTextScale, t.titleScale, t.regionScale, t.hierarchyScale, t.hintScale,
                            t.showBanner, t.showEnteredTitle, t.showHierarchy, t.showHint,
                            t.maxTextWidth, t.minToastWidth, t.maxToastWidth,
                            Math.max(4, t.screenTopMargin), t.iconSize, t.iconGap,
                            t.paddingLeft, t.paddingRight, t.paddingTop, t.paddingBottom);
                }
            }
        }
        Palette pal = activePalette();
        if (c == null) {
            return new Style(pal, 1f, 0.65f, 1f, 0.68f, 0.35f, true, true, true, false,
                    MAX_TEXT_WIDTH, MIN_WIDTH, MAX_WIDTH, MARGIN, BANNER_SIZE, ICON_GAP,
                    PAD_LEFT, PAD_RIGHT, PAD_V, PAD_V);
        }
        return new Style(pal, c.baseTextScale, c.titleScale, c.regionScale, c.hierarchyScale,
                c.hintScale, c.showBanner, true, true, c.hintTextEnabled,
                c.maxTextWidth, c.minToastWidth, c.maxToastWidth, Math.max(4, c.screenTopMargin),
                c.iconSize, c.iconGap, c.paddingLeft, c.paddingRight, c.paddingTop, c.paddingBottom);
    }

    private static long activeDisplayMs() {
        de.ottoextra.config.OttoExtraConfig.Regions c = cfg;
        return c == null ? DEFAULT_DISPLAY_MS : Math.max(1_500L, c.displayDurationMs);
    }

    private static Position activePosition() {
        de.ottoextra.config.OttoExtraConfig.Regions c = cfg;
        if (c == null || c.overlayPosition == null) {
            return Position.TOP_CENTER;
        }
        try {
            return Position.valueOf(c.overlayPosition.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Position.TOP_CENTER;
        }
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        long start = shownAt;
        if (start == 0) {
            return;
        }
        boolean hold = sticky;
        long elapsed = System.currentTimeMillis() - start;
        long displayMs = activeDisplayMs();
        if (!hold && elapsed > displayMs) {
            shownAt = 0;
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        float progress;
        if (hold) {
            progress = 1f;
        } else if (elapsed < FADE_IN_MS) {
            progress = elapsed / (float) FADE_IN_MS;
        } else if (elapsed > displayMs - FADE_OUT_MS) {
            progress = (displayMs - elapsed) / (float) FADE_OUT_MS;
        } else {
            progress = 1f;
        }
        progress = Math.max(0f, Math.min(1f, progress));
        float ease = 1f - (1f - progress) * (1f - progress);

        Style st = activeStyle();
        Palette pal = st.pal();
        int maxTextWidth = st.maxTextWidth();
        int minWidth = st.minWidth();
        int maxWidth = st.maxWidth();
        int bannerSize = st.iconSize();
        int iconGap = st.iconGap();
        int padLeft = st.padLeft();
        int padRight = st.padRight();
        int padTop = st.padTop();
        int padBottom = st.padBottom();
        int margin = st.topMargin();

        float base = st.baseTextScale();
        float titleScale = base * st.titleScale();
        float regionScale = base * st.regionScale();
        float hierarchyScale = base * st.hierarchyScale();
        float hintScale = base * st.hintScale();

        TextRenderer tr = client.textRenderer;
        String title = Text.translatable("ottoextra.regions.entered").getString();
        boolean hasHierarchy = st.showHierarchy() && !hierarchy.isBlank();
        String hint = Text.translatable("ottoextra.regions.hint", menuKeyName).getString();
        boolean renderHint = st.showHint();
        de.ottoextra.config.OttoExtraConfig.Regions regionCfg = cfg;
        FactionRecord currentFaction = null;
        RegionDataService regionData = RegionsServices.data();
        if (regionData != null) {
            currentFaction = regionData.factionForRegion(regionName).orElse(null);
        }
        String factionLine = currentFaction != null && currentFaction.name() != null
                ? currentFaction.name() : "";
        String leaderLine = currentFaction != null
                ? firstNonBlank(currentFaction.leader_name(), currentFaction.lord_name()) : "";
        MinecraftClient mc = MinecraftClient.getInstance();
        String coordinateLine = mc.player != null
                ? Math.round(mc.player.getX()) + " / " + Math.round(mc.player.getZ()) : "";

        Identifier banner = null;
        if (st.showBanner()) {
            banner = resolveBanner();
        }
        int iconArea = banner != null ? bannerSize + iconGap : 0;

        List<ScaledLine> lines = new ArrayList<>();
        if (st.showEnteredTitle()) {
            lines.add(new ScaledLine(Text.literal(title).asOrderedText(), titleScale, pal.title()));
        }
        int regionWrap = Math.max(20, (int) (maxTextWidth / Math.max(0.3f, regionScale)));

        String regionShown = de.ottoextra.map.PoliticalOverlay.displayNameFor(regionName);
        for (net.minecraft.text.OrderedText regionLine
                : tr.wrapLines(Text.literal(regionShown), regionWrap)) {
            lines.add(new ScaledLine(regionLine, regionScale, pal.region()));
        }
        if (hasHierarchy) {
            lines.add(new ScaledLine(Text.literal(hierarchy).asOrderedText(), hierarchyScale, pal.hierarchy()));
        }
        if (regionCfg != null && regionCfg.showFaction && !factionLine.isBlank()) {
            String shownFaction = de.ottoextra.map.PoliticalOverlay.displayNameFor(factionLine);
            lines.add(new ScaledLine(Text.literal("Herrschaft: " + shownFaction).asOrderedText(),
                    hierarchyScale, pal.hierarchy()));
        }
        if (regionCfg != null && regionCfg.showLeader && !leaderLine.isBlank()) {
            lines.add(new ScaledLine(Text.literal("Lehnsherr: " + leaderLine).asOrderedText(),
                    hierarchyScale, pal.hierarchy()));
        }
        if (regionCfg != null && regionCfg.showCoordinates && !coordinateLine.isBlank()) {
            lines.add(new ScaledLine(Text.literal("Koordinaten: " + coordinateLine).asOrderedText(),
                    hintScale, pal.hint()));
        }
        if (renderHint) {
            lines.add(new ScaledLine(Text.literal(hint).asOrderedText(), hintScale, pal.hint()));
        }

        int textW = 0;
        int contentH = 0;
        int gap = 2;
        for (ScaledLine sl : lines) {
            textW = Math.max(textW, Math.round(tr.getWidth(sl.text()) * sl.scale()));
            contentH += Math.round(tr.fontHeight * sl.scale());
        }
        contentH += gap * (lines.size() - 1);
        textW = Math.min(textW, maxTextWidth);

        int w = Math.max(minWidth, Math.min(maxWidth, padLeft + iconArea + textW + padRight));
        int h = Math.max(32, padTop + padBottom + contentH);

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int x;
        int y;
        switch (activePosition()) {
            case TOP_RIGHT -> {
                int targetX = screenW - w - margin;
                x = Math.round((screenW + 4) + (targetX - (screenW + 4)) * ease);
                y = margin;
            }
            case TOP_LEFT -> {
                x = Math.round((-w - 4) + (margin - (-w - 4)) * ease);
                y = margin;
            }
            case CENTER -> {
                x = (screenW - w) / 2;
                y = Math.max(margin, screenH - 92 - h);
            }
            default -> {
                x = (screenW - w) / 2;
                y = Math.round((-h - 4) + (margin - (-h - 4)) * ease);
            }
        }
        float alpha = progress;

        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, withAlpha(pal.borderOut(), alpha));
        ctx.fill(x, y, x + w, y + h, withAlpha(pal.bg(), alpha));
        ctx.fill(x, y, x + w, y + 1, withAlpha(pal.borderTl(), alpha));
        ctx.fill(x, y, x + 1, y + h, withAlpha(pal.borderTl(), alpha));
        ctx.fill(x, y + h - 1, x + w, y + h, withAlpha(pal.borderBr(), alpha));
        ctx.fill(x + w - 1, y, x + w, y + h, withAlpha(pal.borderBr(), alpha));

        int tx = x + padLeft;
        if (banner != null) {
            int by = y + (h - bannerSize) / 2;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, tx, by,
                    0f, 0f, bannerSize, bannerSize, bannerSize, bannerSize);
            tx += bannerSize + iconGap;
        }

        boolean shadow = needsShadow(pal);

        float ty = y + (h - contentH) / 2f;
        var matrices = ctx.getMatrices();
        for (ScaledLine sl : lines) {
            float s = sl.scale();
            matrices.pushMatrix();
            matrices.translate(tx, ty);
            matrices.scale(s, s);
            ctx.drawText(tr, sl.text(), 0, 0, withAlpha(sl.color(), alpha), shadow);
            matrices.popMatrix();
            ty += Math.round(tr.fontHeight * s) + gap;
        }
    }

    private record ScaledLine(net.minecraft.text.OrderedText text, float scale, int color) {
    }

    private static Identifier resolveBanner() {
        RegionDataService data = RegionsServices.data();
        BannerTextureService banners = RegionsServices.banners();
        if (data == null || banners == null || regionName.isBlank()) {
            return null;
        }
        Optional<FactionRecord> faction = data.factionForRegion(regionName);
        return faction.flatMap(banners::bannerFor).orElse(null);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }

    private static int withAlpha(int argb, float a) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        int newAlpha = Math.round(baseAlpha * Math.max(0f, Math.min(1f, a)));
        if (newAlpha < 4) {
            newAlpha = 0;
        }
        return (newAlpha << 24) | (argb & 0x00FFFFFF);
    }
}
