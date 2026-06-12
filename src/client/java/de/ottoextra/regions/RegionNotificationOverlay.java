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

/**
 * Region-Betreten-Toast oben rechts: Wappen + "Du betrittst" + Regionsname +
 * Hierarchie + Tasten-Hinweis. Slide-in von rechts mit Alpha-Fade.
 *
 * <p>Design portiert aus OttoRegions (dunkles Theme): Anzeige 4200 ms,
 * Fade-in 260 ms, Fade-out 360 ms, Panelbreite 170–330 px, Banner 24 px.</p>
 */
public final class RegionNotificationOverlay {

    private static final long DISPLAY_MS = 4_200L;
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

    /** Farbpalette (OttoRegions-Themes; Light = Ottonien-Server-Standard). */
    private record Palette(int bg, int borderOut, int borderTl, int borderBr,
                           int title, int region, int hierarchy, int hint) {
    }

    private static final Palette LIGHT = new Palette(
            0xFFC8AC8E, 0xFF513E2A, 0xFFE6C8A9, 0xFFB8926E,
            0xFF503D29, 0xFF503D29, 0xFF7A5A3A, 0xFF6A4D33);
    private static final Palette DARK = new Palette(
            0xFF212D3B, 0xFF0A0A0A, 0xFF344459, 0xFF191F22,
            0xFFFFFFFF, 0xFFFFFFFF, 0xFF9BC7DC, 0xFF9D9D9D);

    /** Overlay-Positionen (Server-Standard: TOP_CENTER). */
    public enum Position { TOP_CENTER, TOP_RIGHT, TOP_LEFT, CENTER }

    private static volatile String regionName = "";
    private static volatile String hierarchy = "";
    private static volatile long shownAt = 0;
    private static volatile String menuKeyName = "L";
    /** Live-Referenz auf die Regions-Config — Änderungen (auch aus dem Menü) greifen sofort. */
    private static volatile de.ottoextra.config.OttoExtraConfig.Regions cfg;

    private RegionNotificationOverlay() {
    }

    public static void show(String name, String hierarchyLine) {
        regionName = name == null ? "" : name;
        hierarchy = hierarchyLine == null ? "" : hierarchyLine;
        shownAt = System.currentTimeMillis();
    }

    public static void clear() {
        shownAt = 0;
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
        return c != null && "dark".equalsIgnoreCase(c.theme) ? DARK : LIGHT;
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

    /** HudRenderCallback-Ziel. */
    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        long start = shownAt;
        if (start == 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > DISPLAY_MS) {
            shownAt = 0;
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        // Fade + Slide-Fortschritt (0..1 sichtbar)
        float progress;
        if (elapsed < FADE_IN_MS) {
            progress = elapsed / (float) FADE_IN_MS;
        } else if (elapsed > DISPLAY_MS - FADE_OUT_MS) {
            progress = (DISPLAY_MS - elapsed) / (float) FADE_OUT_MS;
        } else {
            progress = 1f;
        }
        progress = Math.max(0f, Math.min(1f, progress));
        float ease = 1f - (1f - progress) * (1f - progress); // ease-out

        // Live-Layoutwerte aus der Config (Erweitert-Menü wirkt sofort)
        de.ottoextra.config.OttoExtraConfig.Regions c = cfg;
        int maxTextWidth = c != null ? c.maxTextWidth : MAX_TEXT_WIDTH;
        int minWidth = c != null ? c.minToastWidth : MIN_WIDTH;
        int maxWidth = c != null ? c.maxToastWidth : MAX_WIDTH;
        int bannerSize = c != null ? c.iconSize : BANNER_SIZE;
        int iconGap = c != null ? c.iconGap : ICON_GAP;
        int padLeft = c != null ? c.paddingLeft : PAD_LEFT;
        int padRight = c != null ? c.paddingRight : PAD_RIGHT;
        int padTop = c != null ? c.paddingTop : PAD_V;
        int padBottom = c != null ? c.paddingBottom : PAD_V;
        int margin = Math.max(4, c != null ? c.screenTopMargin : MARGIN);

        // Gemeinsame Basis-Schriftgröße; Zeilen-Scales sind relative Faktoren darauf.
        float base = c != null ? c.baseTextScale : 1.0f;
        float titleScale = base * (c != null ? c.titleScale : 0.65f);
        float regionScale = base * (c != null ? c.regionScale : 1.0f);
        float hierarchyScale = base * (c != null ? c.hierarchyScale : 0.68f);
        float hintScale = base * (c != null ? c.hintScale : 0.35f);

        TextRenderer tr = client.textRenderer;
        Palette pal = activePalette();
        String title = Text.translatable("ottoextra.regions.entered").getString();
        boolean hasHierarchy = !hierarchy.isBlank();
        String hint = Text.translatable("ottoextra.regions.hint", menuKeyName).getString();
        boolean renderHint = c == null || c.hintTextEnabled;

        // Banner (falls Daten + Config)
        Identifier banner = null;
        if (c == null || c.showBanner) {
            banner = resolveBanner();
        }
        int iconArea = banner != null ? bannerSize + iconGap : 0;

        // Zeilenliste mit individueller Skalierung (Schriftgrößen aus Config)
        List<ScaledLine> lines = new ArrayList<>();
        lines.add(new ScaledLine(Text.literal(title).asOrderedText(), titleScale, pal.title()));
        int regionWrap = Math.max(20, (int) (maxTextWidth / Math.max(0.3f, regionScale)));
        for (net.minecraft.text.OrderedText regionLine
                : tr.wrapLines(Text.literal(regionName), regionWrap)) {
            lines.add(new ScaledLine(regionLine, regionScale, pal.region()));
        }
        if (hasHierarchy) {
            lines.add(new ScaledLine(Text.literal(hierarchy).asOrderedText(), hierarchyScale, pal.hierarchy()));
        }
        if (renderHint) {
            lines.add(new ScaledLine(Text.literal(hint).asOrderedText(), hintScale, pal.hint()));
        }

        // Breite/Höhe aus skalierten Massen
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

        // Position + Slide-Richtung
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
                y = Math.max(margin, screenH - 92 - h); // über der Hotbar, statisch
            }
            default -> { // TOP_CENTER (Server-Standard): Slide von oben
                x = (screenW - w) / 2;
                y = Math.round((-h - 4) + (margin - (-h - 4)) * ease);
            }
        }
        float alpha = progress;

        // Panel: Aussenrand, Innenrand (top/left hell, right/bottom dunkel), Fläche
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, withAlpha(pal.borderOut(), alpha));
        ctx.fill(x, y, x + w, y + h, withAlpha(pal.bg(), alpha));
        ctx.fill(x, y, x + w, y + 1, withAlpha(pal.borderTl(), alpha));
        ctx.fill(x, y, x + 1, y + h, withAlpha(pal.borderTl(), alpha));
        ctx.fill(x, y + h - 1, x + w, y + h, withAlpha(pal.borderBr(), alpha));
        ctx.fill(x + w - 1, y, x + w, y + h, withAlpha(pal.borderBr(), alpha));

        // Banner links, vertikal zentriert
        int tx = x + padLeft;
        if (banner != null) {
            int by = y + (h - bannerSize) / 2;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, tx, by,
                    0f, 0f, bannerSize, bannerSize, bannerSize, bannerSize);
            tx += bannerSize + iconGap;
        }

        // Helle Palette: Schatten stört Lesbarkeit auf Pergament
        boolean shadow = pal == DARK;
        // Echte vertikale Zentrierung des gesamten Textblocks im Panel
        // (unabhängig von Padding-Asymmetrie und Zeilenanzahl).
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

    /** Eine Toast-Zeile mit eigener Schrift-Skalierung und Farbe. */
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

    private static int withAlpha(int argb, float a) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        int newAlpha = Math.round(baseAlpha * Math.max(0f, Math.min(1f, a)));
        if (newAlpha < 4) {
            newAlpha = 0; // vermeidet Geister-Pixel bei sehr kleinem Alpha
        }
        return (newAlpha << 24) | (argb & 0x00FFFFFF);
    }
}
