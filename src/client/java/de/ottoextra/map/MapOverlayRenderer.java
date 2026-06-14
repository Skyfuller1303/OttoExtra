package de.ottoextra.map;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.Optional;

/**
 * Zeichnet das Ottonien-Overlay auf der Xaero-Worldmap: Lehengrenzen,
 * Regionsnamen (LOD) und Wappen (LOD).
 *
 * <p>Schräge Grenzsegmente entstehen als rotierte Fill-Quads
 * (Matrix translate+rotate, dann achsenparalleles fill) — keine Shader,
 * keine Buffer-Abhängigkeiten. Daten kommen ausschliesslich aus
 * {@link LehenPolygonStore} + den zentralen Regions-Diensten; im Render-Pfad
 * gibt es keine Netz-/Disk-Zugriffe.</p>
 */
public final class MapOverlayRenderer {

    // Pergament-Stil
    private static final int COL_BORDER = 0xCCB8893A;     // Gold, leicht transparent
    private static final int COL_BORDER_INSIDE = 0xFFE6C8A9; // Lehen des Spielers
    private static final int COL_NAME = 0xFFF4E9C8;
    private static final int COL_NAME_SECONDARY = 0xFFB9AC8C; // Lehensname unter dem Gefolge
    private static final int COL_NAME_SHADOW = 0xFF1A1208;

    private static final double CULL_MARGIN = 256.0;

    private MapOverlayRenderer() {
    }

    public static void render(DrawContext ctx, XaeroMapBridge.View view, OttoExtraConfig.Map cfg,
                              int mouseX, int mouseY) {
        if (view == null || cfg == null) {
            return;
        }
        if (!LehenPolygonStore.isLoaded()) {
            LehenPolygonStore.ensureLoaded();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        double qMinX = view.worldMinX() - CULL_MARGIN;
        double qMaxX = view.worldMaxX() + CULL_MARGIN;
        double qMinZ = view.worldMinZ() - CULL_MARGIN;
        double qMaxZ = view.worldMaxZ() + CULL_MARGIN;

        double playerX = client.player != null ? client.player.getX() : Double.NaN;
        double playerZ = client.player != null ? client.player.getZ() : Double.NaN;

        // Sichtbarkeit weich: ab minScale einblenden, voll bei 1.6x minScale
        double eff = view.effScale();
        float indFade = fadeIn(eff, cfg.nameMinScale);
        float nameAlpha = cfg.showNames ? indFade : 0f;
        float bannerAlpha = cfg.showBanners ? fadeIn(eff, cfg.bannerMinScale) * indFade : 0f;
        boolean drawNames = nameAlpha > 0.02f;
        boolean drawBanners = bannerAlpha > 0.02f;
        // Weit draussen: ein Label pro Gefolge (oberster Lehnsherr), Crossfade
        // zu den Einzel-Lehen beim Reinzoomen — unabhängig vom politischen
        // Layout (das steuert nur die Flächenfärbung)
        float groupAlpha = 1f - indFade;

        // Politische Flächen + Klick-Highlight: immediate, liegt damit unter
        // den (deferred) Grenzen/Labels dieses DrawContexts.
        PoliticalOverlay.renderFills(view, cfg.politicalFill, cfg.politicalMaxScale, mouseX, mouseY);
        if (cfg.showActivity) {
            // Crossfade wie die Labels: rausgezoomt am Lehnsherrn (groupAlpha),
            // reingezoomt am einzelnen Lehen (indFade).
            ActivityRenderer.render(view, 1.0f, indFade, groupAlpha);
        }

        // Grenzen: deduplizierte Segmente (geteilte Lehnsgrenzen genau einmal).
        // Beim Rauszoomen dezenter: Strich/Lücke skalieren mit dem Zoom.
        if (cfg.showBorders) {
            int width = Math.max(1, cfg.borderWidthPx);
            int borderCol = parseHexColor(cfg.borderColor, COL_BORDER);
            float styleScale = (float) Math.max(0.22, Math.min(1.0, eff / 0.35));
            // beim Rauszoomen zusaetzlich "duenner": Linien-Alpha absenken
            borderCol = withAlpha(borderCol, 0.5f + 0.5f * styleScale * styleScale);
            for (BorderSegment seg : LehenPolygonStore.segments()) {
                if (!seg.intersects(qMinX, qMinZ, qMaxX, qMaxZ)) {
                    continue;
                }
                drawSegment(ctx,
                        view.screenX(seg.x1()), view.screenY(seg.z1()),
                        view.screenX(seg.x2()), view.screenY(seg.z2()),
                        view.width(), view.height(),
                        borderCol, width, styleScale, cfg);
            }
        }

        // NPC-Dörfer UNTER den Lehnsnamen/Wappen (diese sollen oben liegen)
        if (cfg.showNpcVillages) {
            drawNpcVillages(ctx, tr, view, cfg);
        }

        if (drawNames || drawBanners) {
            for (LehenPolygon poly : LehenPolygonStore.polygons()) {
                if (!poly.labelOwner() || !poly.intersects(qMinX, qMinZ, qMaxX, qMaxZ)) {
                    continue;
                }
                drawLabel(ctx, tr, view, poly, drawNames ? nameAlpha : 0f,
                        drawBanners ? bannerAlpha : 0f, cfg);
            }
        }

        if (groupAlpha > 0.02f && (cfg.showNames || cfg.showBanners)) {
            drawGroupLabels(ctx, tr, view, cfg, groupAlpha);
        }
    }

    /** NPC-Dörfer als kleine Text-Labels mit Markierungspunkt (immer sichtbar). */
    private static void drawNpcVillages(DrawContext ctx, TextRenderer tr,
                                        XaeroMapBridge.View view, OttoExtraConfig.Map cfg) {
        if (cfg.npcVillages == null) {
            return;
        }
        float scale = 0.55f;
        for (OttoExtraConfig.NpcVillage v : cfg.npcVillages) {
            if (v == null || v.name == null || v.name.isBlank()) {
                continue;
            }
            float sx = view.screenX(v.x);
            float sy = view.screenY(v.z);
            if (sx < -64 || sy < -64 || sx > view.width() + 64 || sy > view.height() + 64) {
                continue;
            }
            int cx = Math.round(sx);
            int cy = Math.round(sy);
            drawOutlinedText(ctx, tr, v.name, cx, cy, scale, COL_NAME, 1f);
        }
    }

    /** Polygon-Key des Lehens, in dem der Spieler steht (oder null). */
    public static String insidePolygonKey(double px, double pz) {
        if (Double.isNaN(px)) {
            return null;
        }
        for (LehenPolygon poly : LehenPolygonStore.polygons()) {
            if (px >= poly.minX() && px <= poly.maxX() && pz >= poly.minZ() && pz <= poly.maxZ()
                    && contains(poly, px, pz)) {
                return poly.key();
            }
        }
        return null;
    }

    // ---- Grenzen -----------------------------------------------------------

    /**
     * Liniensegment: erst auf den Viewport geclippt (begrenzt Dash-Anzahl bei
     * hohem Zoom), dann optional als Strichgrafik, als rotierte fill-Quads.
     */
    private static void drawSegment(DrawContext ctx, float x1, float y1, float x2, float y2,
                                    int viewW, int viewH, int color, int widthPx,
                                    float styleScale, OttoExtraConfig.Map cfg) {
        // Liang-Barsky auf [-8, view+8]
        double dx = x2 - x1;
        double dy = y2 - y1;
        double t0 = 0;
        double t1 = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x1 + 8, viewW + 8 - x1, y1 + 8, viewH + 8 - y1};
        for (int k = 0; k < 4; k++) {
            if (p[k] == 0) {
                if (q[k] < 0) {
                    return;
                }
            } else {
                double r = q[k] / p[k];
                if (p[k] < 0) {
                    if (r > t1) {
                        return;
                    }
                    if (r > t0) {
                        t0 = r;
                    }
                } else {
                    if (r < t0) {
                        return;
                    }
                    if (r < t1) {
                        t1 = r;
                    }
                }
            }
        }
        float cx1 = (float) (x1 + t0 * dx);
        float cy1 = (float) (y1 + t0 * dy);
        float len = (float) ((t1 - t0) * Math.sqrt(dx * dx + dy * dy));
        if (len < 0.5f) {
            return;
        }

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx1, cy1);
        m.rotate((float) Math.atan2(dy, dx));
        int half = widthPx / 2;
        if (cfg.dashedBorders && cfg.dashGapPx > 0) {
            int dash = Math.max(2, Math.round(cfg.dashLengthPx * styleScale));
            int gap = Math.max(1, Math.round(cfg.dashGapPx * styleScale));
            int step = dash + gap;
            for (float s = 0; s < len; s += step) {
                int end = Math.round(Math.min(s + dash, len));
                ctx.fill(Math.round(s), -half, end, -half + widthPx, color);
            }
        } else {
            ctx.fill(0, -half, Math.round(len) + 1, -half + widthPx, color);
        }
        m.popMatrix();
    }

    // ---- Labels (Name + Wappen) ---------------------------------------------

    private static void drawLabel(DrawContext ctx, TextRenderer tr, XaeroMapBridge.View view,
                                  LehenPolygon poly, float nameAlpha, float bannerAlpha,
                                  OttoExtraConfig.Map cfg) {
        float sx = view.screenX(poly.centroidX());
        float sy = view.screenY(poly.centroidZ());
        if (sx < -64 || sy < -64 || sx > view.width() + 64 || sy > view.height() + 64) {
            return;
        }
        String lehenName = displayName(poly);
        String factionName = factionName(poly);
        // Hauptzeile = Gefolge (Fraktion), Lehensname kleiner darunter — beide
        // Zeilen auch bei gleichem Namen. Ohne Fraktionsdaten nur Lehensname.
        String primary = factionName != null ? factionName : lehenName;
        String secondary = factionName != null ? lehenName : null;

        // Größen smooth zwischen den Breakpoints A (weit draussen) und B (nah)
        float t = breakpointT(view.effScale(), cfg.labelZoomA, cfg.labelZoomB);
        float global = Math.max(0.3f, Math.min(3f, cfg.labelScale));
        float factionScale = lerp(cfg.factionScaleA, cfg.factionScaleB, t) * global;
        float lehenScale = lerp(cfg.lehenScaleA, cfg.lehenScaleB, t) * global;
        int bannerSize = Math.max(4, Math.round(lerp(cfg.bannerSizeA, cfg.bannerSizeB, t)));

        if (bannerAlpha > 0.02f) {
            Identifier banner = bannerFor(poly);
            if (banner != null) {
                int bx = Math.round(sx) - bannerSize / 2;
                int by = Math.round(sy) - bannerSize - (nameAlpha > 0.02f ? 7 : bannerSize / 2);
                int tint = withAlpha(0xFFFFFFFF, bannerAlpha);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, bx, by,
                        0f, 0f, bannerSize, bannerSize, bannerSize, bannerSize, tint);
            }
        }
        if (nameAlpha > 0.02f && !primary.isBlank()) {
            int ty = Math.round(sy) - Math.round(tr.fontHeight * factionScale / 2f);
            drawOutlinedText(ctx, tr, primary, Math.round(sx), ty, factionScale,
                    withAlpha(COL_NAME, nameAlpha), nameAlpha);
            if (secondary != null && !secondary.isBlank()) {
                drawOutlinedText(ctx, tr, secondary, Math.round(sx),
                        ty + Math.round(tr.fontHeight * factionScale) + 1, lehenScale,
                        withAlpha(COL_NAME_SECONDARY, nameAlpha), nameAlpha);
            }
        }
    }

    /**
     * Gefolge-Sammel-Labels (rausgezoomte politische Ansicht): Wappen + Name
     * des obersten Lehnsherrn am Flächen-Schwerpunkt der Vasallenschaft.
     */
    private static void drawGroupLabels(DrawContext ctx, TextRenderer tr, XaeroMapBridge.View view,
                                        OttoExtraConfig.Map cfg, float alpha) {
        var banners = RegionsServices.banners();
        float global = Math.max(0.3f, Math.min(3f, cfg.labelScale));
        // Sammel-Label dezent: Größen der "weit draussen"-Breakpoints (A)
        float scale = cfg.factionScaleA * global;
        int bannerSize = Math.max(8, cfg.bannerSizeA + 4);
        for (PoliticalOverlay.GroupLabel g : PoliticalOverlay.groupLabels()) {
            float sx = view.screenX(g.centerX());
            float sy = view.screenY(g.centerZ());
            if (sx < -64 || sy < -64 || sx > view.width() + 64 || sy > view.height() + 64) {
                continue;
            }
            if (cfg.showBanners && banners != null) {
                Identifier banner = g.rootFaction() != null
                        ? banners.bannerFor(g.rootFaction()).orElse(null)
                        : (g.bannerPath() != null && !g.bannerPath().isBlank()
                        ? banners.bannerForPath(g.bannerCacheKey(), g.bannerPath()).orElse(null)
                        : null);
                if (banner != null) {
                    int bx = Math.round(sx) - bannerSize / 2;
                    int by = Math.round(sy) - bannerSize - 7;
                    ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, bx, by,
                            0f, 0f, bannerSize, bannerSize, bannerSize, bannerSize,
                            withAlpha(0xFFFFFFFF, alpha));
                }
            }
            String groupName = PoliticalOverlay.displayNameFor(g.displayName());
            if (cfg.showNames && !groupName.isBlank()) {
                int ty = Math.round(sy) - Math.round(tr.fontHeight * scale / 2f);
                drawOutlinedText(ctx, tr, groupName, Math.round(sx), ty, scale,
                        withAlpha(COL_NAME, alpha), alpha);
            }
        }
    }

    /** Smoothstep-Position zwischen zwei Zoom-Breakpoints (0..1). */
    private static float breakpointT(double eff, double zoomA, double zoomB) {
        if (zoomB <= zoomA) {
            return 1f;
        }
        float t = (float) ((eff - zoomA) / (zoomB - zoomA));
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** Weiche Einblendung: 0 bei minScale, 1 ab 1.6x minScale. */
    private static float fadeIn(double eff, double minScale) {
        if (minScale <= 0) {
            return 1f;
        }
        float t = (float) ((eff - minScale) / (minScale * 0.6));
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** "#AARRGGBB" oder "#RRGGBB" (dann volldeckend); Fallback bei Murks. */
    private static int parseHexColor(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String s = hex.replace("#", "").trim();
        try {
            if (s.length() == 6) {
                return 0xFF000000 | Integer.parseInt(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException ignored) {
            // Fallback unten
        }
        return fallback;
    }

    /** Zentrierter Text mit 4-Offset-Outline, skaliert + weich eingeblendet. */
    private static void drawOutlinedText(DrawContext ctx, TextRenderer tr, String text,
                                         int centerX, int y, float scale, int color, float alpha) {
        float tw = tr.getWidth(text) * scale;
        int shadow = withAlpha(COL_NAME_SHADOW, alpha);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(centerX - tw / 2f, y);
        m.scale(scale, scale);
        ctx.drawText(tr, text, 1, 0, shadow, false);
        ctx.drawText(tr, text, -1, 0, shadow, false);
        ctx.drawText(tr, text, 0, 1, shadow, false);
        ctx.drawText(tr, text, 0, -1, shadow, false);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.popMatrix();
    }

    /** Gefolge-(Fraktions-)Name für ein Lehen-Polygon, oder null. */
    private static String factionName(LehenPolygon poly) {
        var data = RegionsServices.data();
        if (data == null) {
            return null;
        }
        // Nur echte Gefolge als Hauptzeile — Region-Verbände (Mährstein-Fehde)
        // zeigen am Einzel-Lehen schlicht den Lehensnamen.
        return data.factionForRegion(poly.key())
                .map(FactionRecord::name)
                .filter(n -> n != null && !n.isBlank())
                .orElse(null);
    }

    private static String displayName(LehenPolygon poly) {
        var data = RegionsServices.data();
        if (data != null) {
            Optional<RegionRecord> region = data.regionByName(poly.key());
            if (region.isPresent()) {
                RegionRecord r = region.get();
                if (r.name() != null && !r.name().isBlank()) {
                    return r.name();
                }
                if (r.original_name() != null && !r.original_name().isBlank()) {
                    return r.original_name();
                }
            }
        }
        return poly.key().replace("lehen_", "Lehen ");
    }

    private static Identifier bannerFor(LehenPolygon poly) {
        return bannerForKey(poly.key());
    }

    /** Banner-Lookup eines Lehens (Region-Banner vor Fraktions-Banner), oder null. */
    public static Identifier bannerForKey(String polyKey) {
        var data = RegionsServices.data();
        var banners = RegionsServices.banners();
        if (data == null || banners == null || polyKey == null) {
            return null;
        }
        // Region-Banner zuerst (fraktionslose Verbände, z. B. Mährstein-Fehde)
        var region = data.regionByName(polyKey);
        String regionBanner = region.map(r -> r.effectiveRegionBannerPath()).orElse(null);
        if (regionBanner != null && !regionBanner.isBlank()) {
            var id = banners.bannerForPath("region-" + polyKey, regionBanner);
            if (id.isPresent()) {
                return id.get();
            }
        }
        Optional<FactionRecord> faction = data.factionForRegion(polyKey);
        return faction.flatMap(banners::bannerFor).orElse(null);
    }

    /** Punkt-in-Polygon (Ray-Casting) für die Spieler-Hervorhebung. */
    private static boolean contains(LehenPolygon poly, double px, double pz) {
        boolean inside = false;
        int n = poly.pointCount();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly.xs()[i];
            double zi = poly.zs()[i];
            double xj = poly.xs()[j];
            double zj = poly.zs()[j];
            if ((zi > pz) != (zj > pz)
                    && px < (xj - xi) * (pz - zi) / (zj - zi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }
}
