package de.ottoextra.map.xaero;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.map.MapOverlayRenderer;
import de.ottoextra.regions.RegionMessageService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Drei unabhängige HUD-Elemente fürs aktuelle Lehen:
 * <b>Wappen</b> (Icon), <b>Gefolgename</b> und <b>Stand</b> (Hierarchie-Zeile
 * aus der Server-Actionbar, z. B. "Abtei in Grafschaft Holdstewik").
 *
 * <p>Jedes Element ist einzeln aktivierbar und per Drag&Drop frei
 * positionierbar ({@code MinimapBannerEditScreen}); gespeichert wird mit
 * Kanten-Anker (resize-stabil). Ohne gesetzte Position docken die Elemente
 * an der Minimap-Ecke an (Icon) bzw. unter dem vorigen Element.
 * Jeder Fehler deaktiviert das Feature einmalig, nie Crash.</p>
 */
public final class MinimapBannerOverlay {

    /** Element-Typ (steuert Config-Slot + Default-Andockung). */
    public enum Kind { BANNER, NAME, STATE, FACTION }

    /** Berechnetes Element (Screen-Koordinaten + Inhalt + Skalierung). */
    public record Element(Kind kind, int x, int y, int width, int height,
                          Identifier banner, int iconSize, String text, float scale) {
    }

    private static final int COL_NAME = 0xFFE6C8A9;
    private static final int COL_STATE = 0xFFB8A88F;

    private static boolean disabled = false;

    private MinimapBannerOverlay() {
    }

    public static void render(DrawContext ctx, OttoExtraConfig.Map cfg) {
        if (disabled) {
            return;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) {
                return;
            }
            // Spielerliste (Tab) offen -> Wappen-Overlay ausblenden, sonst
            // überlappt es die Tabliste.
            if (client.options.playerListKey.isPressed()) {
                return;
            }
            MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (session == null || !session.isActive()) {
                return;
            }
            // Wie Xaero: bei offenem GUI / F3 ausblenden — mit denselben
            // Ausnahmen wie der MinimapRenderer (Chat/Death/Xaero-Screens)
            if (client.currentScreen != null && session.getHideMinimapUnderScreen()
                    && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)
                    && !(client.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen)
                    && !(client.currentScreen instanceof xaero.lib.client.gui.IScreenBase)) {
                return;
            }
            // Nur beim echten F3-Screen verstecken (wie Xaeros MinimapRenderer).
            // shouldShowDebugHud() ist seit 1.21.11 auch true, wenn nur einzelne
            // Debug-Eintraege aktiv sind (z. B. F3+B-Hitboxen) — dann bleibt die
            // Minimap sichtbar und das Wappen-HUD muss es auch.
            if (client.debugHudEntryList.isF3Enabled() && session.getHideMinimapUnderF3()) {
                return;
            }
            for (Element e : computeElements(client, cfg, session)) {
                draw(ctx, client, e, cfg);
            }
        } catch (Throwable t) {
            disabled = true;
            OttoExtra.LOGGER.warn("[map] Minimap-Wappen deaktiviert: {}", t.toString());
        }
    }

    /**
     * Aktive Elemente für die aktuelle Spielerposition (auch vom Drag-Screen
     * genutzt); leer, wenn kein Lehen erkannt.
     */
    public static List<Element> computeElements(MinecraftClient client, OttoExtraConfig.Map cfg,
                                                MinimapSession session) {
        List<Element> out = new ArrayList<>(3);
        String key = MapOverlayRenderer.insidePolygonKey(
                client.player.getX(), client.player.getZ());
        if (key == null) {
            return out;
        }
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        var tr = client.textRenderer;
        String[] info = nameAndState(key, cfg != null && cfg.minimapLiegeTop);
        String regionName = info[0];
        String stateLine = info[1];
        String factionName = info[2];

        // Default-Andockpunkt: Minimap-Ecke (Legacy-Offsets)
        int size = Math.max(8, cfg.minimapBannerSize);
        int dockX = sw - size - 2;
        int dockY = 2;
        if (session != null) {
            double guiScale = client.getWindow().getScaleFactor();
            int x = session.getEffectiveX(sw, guiScale);
            int y = session.getEffectiveY(sh, guiScale);
            int w = session.getWidth(guiScale);
            int h = session.getHeight(guiScale);
            Boolean circle = XaeroMinimapBorders.circleShape();
            boolean round = circle != null ? circle : cfg.minimapBannerRound;
            int offX = round ? cfg.minimapBannerOffsetXRound : cfg.minimapBannerOffsetX;
            int offY = round ? cfg.minimapBannerOffsetYRound : cfg.minimapBannerOffsetY;
            dockX = Math.min(x + w - size + offX, sw - size - 2);
            dockY = Math.min(y + h + 2 + offY, sh - size - 2);
        }
        int nextDockY = dockY;

        if (cfg.minimapBanner) {
            Identifier banner = MapOverlayRenderer.bannerForKey(key);
            if (banner != null) {
                int[] pos = resolve(cfg.bannerHudX, cfg.bannerHudY,
                        cfg.bannerHudFromRight, cfg.bannerHudFromBottom,
                        size, size, dockX, dockY, sw, sh);
                out.add(new Element(Kind.BANNER, pos[0], pos[1], size, size, banner, size, null, 1.0f));
                nextDockY = pos[1] + size + 2;
            }
        }
        if (cfg.minimapBannerShowName && regionName != null && !regionName.isBlank()) {
            float sc = clampScale(cfg.nameHudScale);
            int w = Math.max(20, cfg.nameHudWidth); // feste Breite, linksbündig
            int h = Math.round(10 * sc);
            int[] pos = resolve(cfg.nameHudX, cfg.nameHudY,
                    cfg.nameHudFromRight, cfg.nameHudFromBottom,
                    w, h, sw - w - 2 < dockX ? sw - w - 2 : dockX, nextDockY, sw, sh);
            out.add(new Element(Kind.NAME, pos[0], pos[1], w, h, null, 0, regionName, sc));
            nextDockY = pos[1] + h + 2;
        }
        if (cfg.minimapBannerShowState && stateLine != null && !stateLine.isBlank()) {
            float sc = clampScale(cfg.stateHudScale);
            int w = Math.max(20, cfg.stateHudWidth); // feste Breite, linksbündig
            int h = Math.round(10 * sc);
            int[] pos = resolve(cfg.stateHudX, cfg.stateHudY,
                    cfg.stateHudFromRight, cfg.stateHudFromBottom,
                    w, h, sw - w - 2 < dockX ? sw - w - 2 : dockX, nextDockY, sw, sh);
            out.add(new Element(Kind.STATE, pos[0], pos[1], w, h, null, 0, stateLine, sc));
            nextDockY = pos[1] + h + 2;
        }
        // Lokaler Gefolge-Namens-Override (Gefolge-Liste) auch hier anwenden.
        String factionShown = de.ottoextra.map.PoliticalOverlay.displayNameFor(factionName);
        if (cfg.minimapBannerShowFaction && factionShown != null && !factionShown.isBlank()) {
            float sc = clampScale(cfg.factionHudScale);
            int w = Math.max(20, cfg.factionHudWidth); // feste Breite, linksbündig
            int h = Math.round(10 * sc);
            int[] pos = resolve(cfg.factionHudX, cfg.factionHudY,
                    cfg.factionHudFromRight, cfg.factionHudFromBottom,
                    w, h, sw - w - 2 < dockX ? sw - w - 2 : dockX, nextDockY, sw, sh);
            out.add(new Element(Kind.FACTION, pos[0], pos[1], w, h, null, 0, factionShown, sc));
        }
        return out;
    }

    /**
     * Name + Stand-Zeile: bevorzugt die geparste Server-Actionbar
     * (exaktes Serverformat, z. B. "Abtei in Grafschaft Holdstewik"); ohne
     * frische Nachricht Fallback aus den API-Daten
     * ({@code rank_name} + {@code lord_name}-Kette).
     */
    private static String[] nameAndState(String polyKey, boolean liegeTop) {
        var data = de.ottoextra.regions.RegionsServices.data();
        String apiName = null;
        String apiState = null;
        String apiFaction = null;
        if (data != null) {
            var region = data.regionByName(polyKey).orElse(null);
            if (region != null) {
                apiName = fixMojibake(region.name_override() != null
                        && !region.name_override().isBlank()
                        ? region.name_override() : region.name());
            }
            var faction = data.factionForRegion(polyKey).orElse(null);
            if (faction != null) {
                apiFaction = fixMojibake(faction.name());
                String rank = fixMojibake(faction.rank_name());
                // Lehnsherr: direkter lord_name ODER oberster der Kette (einstellbar)
                String[] liege = resolveLiege(data, faction, liegeTop);
                String lordName = liege[0];
                String lordRank = liege[1];
                if (rank != null && !rank.isBlank()) {
                    apiState = lordName != null && !lordName.isBlank()
                            ? rank + " in " + (lordRank != null ? lordRank + " " : "") + lordName
                            : rank;
                }
            }
        }
        var current = RegionMessageService.current();
        if (current != null && current.regionName() != null && !current.regionName().isBlank()) {
            // Actionbar nur nutzen, wenn sie zum aktuellen Polygon passt
            boolean matches = apiName == null
                    || normalize(current.regionName()).equals(normalize(apiName));
            if (matches) {
                // Im "oberster Lehnsherr"-Modus die berechnete Kette bevorzugen
                // (die Server-Actionbar nennt nur den direkten Lehnsherrn).
                String state = liegeTop ? apiState
                        : (current.hierarchyLine() != null && !current.hierarchyLine().isBlank()
                                ? current.hierarchyLine() : apiState);
                return new String[]{current.regionName(), state, apiFaction};
            }
        }
        return new String[]{apiName, apiState, apiFaction};
    }

    /**
     * Lehnsherr eines Lehens auflösen. {@code top=false}: direkter
     * {@code lord_name}. {@code top=true}: der Kette {@code lord_name} bis zum
     * obersten Lehnsherrn folgen. Rückgabe {@code [name, rang]} (Rang ggf. null).
     */
    private static String[] resolveLiege(de.ottoextra.regions.RegionDataService data,
                                         de.ottoextra.api.model.FactionRecord faction,
                                         boolean top) {
        String directLord = fixMojibake(faction.lord_name());
        if (directLord == null || directLord.isBlank()) {
            return new String[]{null, null};
        }
        java.util.Map<String, de.ottoextra.api.model.FactionRecord> byName =
                new java.util.HashMap<>();
        for (var f : data.allFactions()) {
            if (f.name() != null && !f.name().isBlank()) {
                byName.merge(f.name().toLowerCase(java.util.Locale.ROOT), f,
                        de.ottoextra.api.model.FactionRecord::better);
            }
        }
        String lordName = directLord;
        de.ottoextra.api.model.FactionRecord lordRec =
                byName.get(directLord.toLowerCase(java.util.Locale.ROOT));
        if (top) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            if (faction.name() != null) {
                seen.add(faction.name().toLowerCase(java.util.Locale.ROOT));
            }
            for (int depth = 0; depth < 8 && lordRec != null; depth++) {
                String up = lordRec.lord_name();
                if (up == null || up.isBlank()
                        || !seen.add(lordName.toLowerCase(java.util.Locale.ROOT))) {
                    break;
                }
                lordName = up;
                lordRec = byName.get(up.toLowerCase(java.util.Locale.ROOT));
            }
        }
        String lordRank = lordRec != null ? fixMojibake(lordRec.rank_name()) : null;
        return new String[]{fixMojibake(lordName), lordRank};
    }

    private static String normalize(String s) {
        return de.ottoextra.regions.RegionNameKeys.normalize(s);
    }

    /** Doppelt UTF-8-kodierte Umlaute der API reparieren. */
    private static String fixMojibake(String s) {
        if (s == null) {
            return null;
        }
        String fixed = s;
        for (int i = 0; i < 2 && (fixed.indexOf('Ã') >= 0 || fixed.indexOf('Â') >= 0); i++) {
            String decoded = new String(fixed.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                    java.nio.charset.StandardCharsets.UTF_8);
            if (decoded.indexOf('�') >= 0) {
                break;
            }
            fixed = decoded;
        }
        return fixed;
    }

    /** Freie Anker-Position auflösen; ohne gesetzte Position Dock-Fallback. */
    private static int[] resolve(int cfgX, int cfgY, boolean fromRight, boolean fromBottom,
                                 int w, int h, int dockX, int dockY, int sw, int sh) {
        int x;
        int y;
        if (cfgX >= 0) {
            x = fromRight ? sw - cfgX - w : cfgX;
            y = fromBottom ? sh - cfgY - h : cfgY;
        } else {
            x = dockX;
            y = dockY;
        }
        x = Math.max(0, Math.min(x, sw - w));
        y = Math.max(0, Math.min(y, sh - h));
        return new int[]{x, y};
    }

    public static void draw(DrawContext ctx, MinecraftClient client, Element e,
                            OttoExtraConfig.Map cfg) {
        switch (e.kind()) {
            case BANNER -> ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.banner(),
                    e.x(), e.y(), 0f, 0f, e.iconSize(), e.iconSize(), e.iconSize(), e.iconSize());
            case NAME -> drawScaledText(ctx, client, e,
                    parseColor(cfg != null ? cfg.nameHudColor : null, COL_NAME));
            case STATE -> drawScaledText(ctx, client, e,
                    parseColor(cfg != null ? cfg.stateHudColor : null, COL_STATE));
            case FACTION -> drawScaledText(ctx, client, e,
                    parseColor(cfg != null ? cfg.factionHudColor : null, COL_NAME));
        }
    }

    private static int parseColor(String hex, int fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        try {
            return 0xFF000000 | (Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void drawScaledText(DrawContext ctx, MinecraftClient client, Element e, int color) {
        // feste Boxbreite: Überlauf kürzen, immer linksbündig ab e.x()
        String text = client.textRenderer.trimToWidth(e.text(),
                Math.max(10, Math.round(e.width() / e.scale())));
        if (Math.abs(e.scale() - 1.0f) < 0.01f) {
            ctx.drawText(client.textRenderer, text, e.x(), e.y(), color, true);
            return;
        }
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(e.x(), e.y());
        m.scale(e.scale(), e.scale());
        ctx.drawText(client.textRenderer, text, 0, 0, color, true);
        m.popMatrix();
    }

    /** Erlaubter Skalenbereich für Text-Elemente. */
    public static float clampScale(float s) {
        return Math.max(0.5f, Math.min(3.0f, s));
    }

    /** Drag-Position eines Elements speichern (Kanten-Anker, nächste Kante gewinnt). */
    public static void savePosition(OttoExtraConfig.Map cfg, Kind kind,
                                    int x, int y, int w, int h, int sw, int sh) {
        boolean fromRight = x + w / 2 > sw / 2;
        boolean fromBottom = y + h / 2 > sh / 2;
        int ax = fromRight ? sw - x - w : x;
        int ay = fromBottom ? sh - y - h : y;
        switch (kind) {
            case BANNER -> {
                cfg.bannerHudX = ax;
                cfg.bannerHudY = ay;
                cfg.bannerHudFromRight = fromRight;
                cfg.bannerHudFromBottom = fromBottom;
            }
            case NAME -> {
                cfg.nameHudX = ax;
                cfg.nameHudY = ay;
                cfg.nameHudFromRight = fromRight;
                cfg.nameHudFromBottom = fromBottom;
            }
            case STATE -> {
                cfg.stateHudX = ax;
                cfg.stateHudY = ay;
                cfg.stateHudFromRight = fromRight;
                cfg.stateHudFromBottom = fromBottom;
            }
            case FACTION -> {
                cfg.factionHudX = ax;
                cfg.factionHudY = ay;
                cfg.factionHudFromRight = fromRight;
                cfg.factionHudFromBottom = fromBottom;
            }
        }
    }

    /** Alle freien Positionen zurücksetzen (Andockung an Minimap-Ecke). */
    public static void resetPositions(OttoExtraConfig.Map cfg) {
        cfg.bannerHudX = -1;
        cfg.bannerHudY = -1;
        cfg.nameHudX = -1;
        cfg.nameHudY = -1;
        cfg.stateHudX = -1;
        cfg.stateHudY = -1;
        cfg.factionHudX = -1;
        cfg.factionHudY = -1;
    }
}
