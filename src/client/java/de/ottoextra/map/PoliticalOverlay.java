package de.ottoextra.map;

import com.mojang.blaze3d.vertex.VertexFormat;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Politisches Karten-Overlay: färbt Lehen-Flächen nach Gefolge (Lehnsverband)
 * ein und fokussiert per Klick auf ein Lehen (Quelle: OttoMap-Gruppenansicht).
 *
 * <p>Gruppenbildung über den API-Vasallengraphen: Kind→Lord aus
 * {@code vassal_uuids}; der oberste Lord ist der Gruppenschlüssel. Farben aus
 * der Legacy-Palette (GROUP_TINTS), stabil über sortierte Gruppennamen.
 * Triangulation (Ear-Clipping) wird pro Polygon-Key dauerhaft gecacht; der
 * Gruppen-Lookup wird alle 60 s gegen die Regions-Dienste neu aufgebaut.
 * Rendering läuft immediate (unter den deferred Grenzen/Labels).</p>
 */
public final class PoliticalOverlay {

    /** Neutrale Fläche für Lehen ohne (auflösbare) Fraktion — sicht- und klickbar. */
    private static final int NEUTRAL_TINT = 0x5055585E;
    /** Alpha der Gruppen-Flächen — hoch genug, um auch über dunklem
     *  (geladenem) Terrain sichtbar zu bleiben. */
    private static final int GROUP_ALPHA = 0x52;

    private static final long GROUPS_TTL_MS = 60_000L;
    private static final int CLICK_HIGHLIGHT_COLOR = 0xFFE0A0;

    // Pro Polygon-INSTANZ (mehrteilige Lehen teilen sich den logischen Key)
    private static final Map<LehenPolygon, int[]> TRI_CACHE = new ConcurrentHashMap<>();

    private static volatile Map<String, Integer> tintByPolyKey = Map.of();
    /** Anzeigename -> aktueller Tint (für das Gefolgefarben-UI). */
    private static volatile Map<String, Integer> groupTintOverview = Map.of();
    /** Nutzer-Overrides: normalisierter Gruppenname -> "#RRGGBB" (Config). */
    private static volatile Map<String, String> userGroupColors = Map.of();
    /** Farb-Overrides je Lehen: Lehen-Key -> RGB (Config map.lehenColors). */
    private static volatile Map<String, Integer> userLehenColors = Map.of();
    /** Farb-Overrides je Fraktion: normalisierter Fraktionsname -> RGB. */
    private static volatile Map<String, Integer> userFactionColors = Map.of();
    /** Anzeige-Namens-Overrides: normalisierter Originalname -> Anzeigename. */
    private static volatile Map<String, String> groupNameOverrides = Map.of();
    private static volatile Set<String> vassalPolyKeys = Set.of();
    private static volatile Map<String, String> groupNameByPoly = Map.of();
    private static volatile List<GroupLabel> groupLabels = List.of();

    /** Gruppen-Anzeigename (oberster Lehnsherr/Verband) für ein Lehen, oder null. */
    public static String groupDisplayName(String polyKey) {
        return groupNameByPoly.get(polyKey);
    }

    /** Flächen-Tint (ARGB) eines Lehens für externe Renderer (Minimap). */
    public static int fillTintFor(String polyKey) {
        refreshGroupsIfStale();
        return tintByPolyKey.getOrDefault(polyKey, NEUTRAL_TINT);
    }
    private static long groupsBuiltAt;

    private static final net.minecraft.util.Identifier STRIPES_TEX =
            de.ottoextra.OttoExtra.id("textures/map/vassal_stripes.png");
    private static boolean stripesLoaded;
    private static net.minecraft.client.gl.GpuSampler stripesSampler;

    /** Sammel-Label eines Gefolges: oberster Lehnsherr + Flächen-Schwerpunkt.
     *  Bei Region-Verbänden (rootFaction null) kommt das Wappen vom
     *  Region-Bannerpfad. */
    public record GroupLabel(FactionRecord rootFaction, String displayName,
                             String bannerCacheKey, String bannerPath,
                             double centerX, double centerZ) {
    }

    /** Gruppen-Labels für die rausgezoomte politische Ansicht. */
    public static List<GroupLabel> groupLabels() {
        refreshGroupsIfStale();
        return groupLabels;
    }

    private static LehenPolygon clickedPoly;
    private static long clickTime;

    private PoliticalOverlay() {
    }

    // ---- Klick-Fokus ---------------------------------------------------------

    /**
     * Klick auf die Karte: trifft er ein Lehen, Kamera dorthin zentrieren und
     * Fläche kurz hervorheben. Liefert true bei Treffer.
     */
    public static boolean handleClick(net.minecraft.client.gui.screen.Screen screen,
                                      XaeroMapBridge.View view, double mouseX, double mouseY) {
        LehenPolygon poly = polyAt(view, mouseX, mouseY);
        if (poly == null) {
            return false;
        }
        if (clickedPoly == poly) {
            clickedPoly = null; // zweiter Klick hebt Auswahl auf
            return true;
        }
        clickedPoly = poly;
        clickTime = System.currentTimeMillis();
        XaeroMapBridge.setCamera(screen, poly.centroidX(), poly.centroidZ());
        return true;
    }

    /** Gruppen-Übersicht fürs Farb-UI (Anzeigename -> aktueller Tint), sortiert. */
    public static Map<String, Integer> groupTintOverview() {
        refreshGroupsIfStale();
        return groupTintOverview;
    }

    /** Nutzer-Farb-Overrides setzen (normalisierte Namen -> "#RRGGBB") + Rebuild. */
    public static void setUserGroupColors(Map<String, String> colors) {
        Map<String, String> normalized = new HashMap<>();
        if (colors != null) {
            colors.forEach((name, hex) -> {
                if (name != null && hex != null && !hex.isBlank()) {
                    normalized.put(normalizeName(name), hex.trim());
                }
            });
        }
        userGroupColors = Map.copyOf(normalized);
        invalidateGroups();
    }

    /** Farb-Overrides je Lehen setzen (Lehen-Key -> "#RRGGBB") + Rebuild. */
    public static void setUserLehenColors(Map<String, String> colors) {
        Map<String, Integer> parsed = new HashMap<>();
        if (colors != null) {
            colors.forEach((key, hex) -> {
                Integer rgb = parseHex(hex);
                if (key != null && !key.isBlank() && rgb != null) {
                    parsed.put(key, rgb);
                }
            });
        }
        userLehenColors = Map.copyOf(parsed);
        invalidateGroups();
    }

    /** Farb-Overrides je Fraktion setzen (Fraktionsname -> "#RRGGBB") + Rebuild. */
    public static void setUserFactionColors(Map<String, String> colors) {
        Map<String, Integer> parsed = new HashMap<>();
        if (colors != null) {
            colors.forEach((name, hex) -> {
                Integer rgb = parseHex(hex);
                if (name != null && !name.isBlank() && rgb != null) {
                    parsed.put(normalizeName(name), rgb);
                }
            });
        }
        userFactionColors = Map.copyOf(parsed);
        invalidateGroups();
    }

    /** Standard-Farbe einer Gruppe aus der ausgelieferten JSON (group_colors), oder null. */
    public static String jsonDefaultColor(String groupName) {
        Integer rgb = LehenPolygonStore.groupColors().get(normalizeName(groupName));
        return rgb == null ? null : String.format("#%06X", rgb & 0xFFFFFF);
    }

    /** Anzeige-Namens-Overrides setzen (Originalname -> Anzeigename). */
    public static void setGroupNameOverrides(Map<String, String> overrides) {
        Map<String, String> normalized = new HashMap<>();
        if (overrides != null) {
            overrides.forEach((name, custom) -> {
                if (name != null && custom != null && !custom.isBlank()) {
                    normalized.put(normalizeName(name), custom.trim());
                }
            });
        }
        groupNameOverrides = Map.copyOf(normalized);
        invalidateGroups(); // Gruppenlabels neu bauen, sonst bleibt der alte Name auf der Karte
    }

    /** Anzeigename eines Gefolges (Override -> Originalname). */
    public static String displayNameFor(String originalName) {
        if (originalName == null) {
            return "";
        }
        return groupNameOverrides.getOrDefault(normalizeName(originalName), originalName);
    }

    /** Farb-/Gruppenzuordnung beim nächsten Frame neu aufbauen. */
    public static void invalidateGroups() {
        groupsBuiltAt = 0;
        TRI_CACHE.clear();
    }

    /** "#RRGGBB" -> RGB-Int, sonst null. */
    private static Integer parseHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Lehen-Polygon unter der Maus (Screen-Koordinaten), oder null. */
    private static LehenPolygon polyAt(XaeroMapBridge.View view, double mouseX, double mouseY) {
        if (view == null) {
            return null;
        }
        double worldX = view.cameraX() + (mouseX - view.width() / 2.0) / view.effScale();
        double worldZ = view.cameraZ() + (mouseY - view.height() / 2.0) / view.effScale();
        for (LehenPolygon poly : LehenPolygonStore.polygons()) {
            if (worldX >= poly.minX() && worldX <= poly.maxX()
                    && worldZ >= poly.minZ() && worldZ <= poly.maxZ()
                    && containsPoint(poly, worldX, worldZ)) {
                return poly;
            }
        }
        return null;
    }

    // ---- Rendering -----------------------------------------------------------

    /**
     * Politische Flächen + Klick-Highlight, immediate (vor den deferred
     * DrawContext-Elementen — liegt damit unter Grenzen/Labels).
     */
    public static void renderFills(XaeroMapBridge.View view, boolean politicalEnabled,
                                   double maxScale, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Fade: politische Flächen oberhalb der Config-Zoomgrenze ausblenden
        // (Rampe = letztes Drittel der Grenze)
        double ramp = Math.max(0.01, maxScale / 3.0);
        float zoomAlpha = clamp01((float) ((maxScale - view.effScale()) / ramp));
        // Nutzer-Deckkraft (Slider) × Tag/Nacht-Faktor (nachts dunkler -> weniger).
        float opacity = overlayOpacity(client);

        BufferBuilder buf = null;
        int quads = 0;
        if (politicalEnabled && zoomAlpha > 0.02f) {
            refreshGroupsIfStale();
            LehenPolygon hoveredPart = polyAt(view, mouseX, mouseY);
            String hoveredKey = hoveredPart != null ? hoveredPart.key() : null;
            Map<String, Integer> tints = tintByPolyKey;
            for (LehenPolygon poly : LehenPolygonStore.polygons()) {
                if (!poly.intersects(view.worldMinX(), view.worldMinZ(),
                        view.worldMaxX(), view.worldMaxZ())) {
                    continue;
                }
                // Ohne Fraktion/Gruppe: neutrale Fläche, bleibt sicht- und klickbar
                int tint = tints.getOrDefault(poly.key(), NEUTRAL_TINT);
                if (buf == null) {
                    buf = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS, RenderPipelines.GUI.getVertexFormat());
                }
                // Vasallen: Grundfläche nur 30% der Lehnsherr-Farbe — die
                // Streifen (voll) liefern den Rest des Musters
                float strength = vassalPolyKeys.contains(poly.key()) ? 0.3f : 1.0f;
                int col = withAlpha(tint, zoomAlpha * strength * opacity);
                if (poly.key().equals(hoveredKey)) {
                    col = lighten(col);
                }
                quads += emitPolygon(buf, poly, view, col);
            }
        }

        // Ohne politisches Layout: gehovertes Lehen dezent aufhellen
        if (!politicalEnabled && zoomAlpha > 0.02f) {
            LehenPolygon hoveredPart = polyAt(view, mouseX, mouseY);
            if (hoveredPart != null) {
                if (buf == null) {
                    buf = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS, RenderPipelines.GUI.getVertexFormat());
                }
                int hl = withAlpha(0x30FFFFFF, zoomAlpha);
                for (LehenPolygon part : LehenPolygonStore.polygons()) {
                    if (part.key().equals(hoveredPart.key())) {
                        quads += emitPolygon(buf, part, view, hl);
                    }
                }
            }
        }

        // Klick-Highlight: 3 s voll, 0.5 s ausblenden (Legacy-Puls)
        if (clickedPoly != null) {
            long elapsed = System.currentTimeMillis() - clickTime;
            float a = elapsed < 3000L ? 1.0f
                    : elapsed < 3500L ? 1.0f - (elapsed - 3000L) / 500.0f : 0.0f;
            if (a <= 0.0f) {
                clickedPoly = null;
            } else {
                if (buf == null) {
                    buf = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS, RenderPipelines.GUI.getVertexFormat());
                }
                int hl = (int) (a * 80.0f) << 24 | CLICK_HIGHLIGHT_COLOR;
                for (LehenPolygon part : LehenPolygonStore.polygons()) {
                    if (part.key().equals(clickedPoly.key())) {
                        quads += emitPolygon(buf, part, view, hl);
                    }
                }
            }
        }

        // Erst Flächen zeichnen (Tessellator-Buffer MUSS beendet sein, bevor
        // der nächste begin() kommt — ein gemeinsamer Allocator!)
        if (buf != null) {
            if (quads == 0) {
                buf.endNullable();
            } else {
                BufferBuilder finalBuf = buf;
                PaintedMapRenderer.withGuiOrtho(client, () ->
                        PaintedMapRenderer.drawImmediate(client, finalBuf, RenderPipelines.GUI, null, null));
            }
        }

        // Danach Vasallen-Schraffur: Streifen (screen-locked, -35 Grad, repeat)
        if (politicalEnabled && zoomAlpha > 0.02f) {
            ensureStripes(client);
            if (stripesSampler == null) {
                return;
            }
            BufferBuilder stripeBuf = null;
            Map<String, Integer> tints = tintByPolyKey;
            Set<String> vassals = vassalPolyKeys;
            for (LehenPolygon poly : LehenPolygonStore.polygons()) {
                if (!vassals.contains(poly.key())
                        || !poly.intersects(view.worldMinX(), view.worldMinZ(),
                        view.worldMaxX(), view.worldMaxZ())) {
                    continue;
                }
                Integer tint = tints.get(poly.key());
                if (tint == null) {
                    continue;
                }
                if (stripeBuf == null) {
                    stripeBuf = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS, RenderPipelines.GUI_TEXTURED.getVertexFormat());
                }
                // Streifen in voller Lehnsherr-Farbe (Grundfläche darunter: 30%)
                emitPolygonStriped(stripeBuf, poly, view, withAlpha(tint, zoomAlpha * opacity));
            }
            if (stripeBuf != null) {
                BufferBuilder finalStripes = stripeBuf;
                var tex = client.getTextureManager().getTexture(STRIPES_TEX);
                PaintedMapRenderer.withGuiOrtho(client, () ->
                        PaintedMapRenderer.drawImmediate(client, finalStripes,
                                RenderPipelines.GUI_TEXTURED, tex.getGlTextureView(), stripesSampler));
            }
        }
    }

    /** Streifen-UVs: Screen-Koordinaten um -35 Grad gedreht, 16-px-Kachel. */
    private static void emitPolygonStriped(BufferBuilder buf, LehenPolygon poly,
                                           XaeroMapBridge.View view, int argb) {
        int[] tris = triangles(poly);
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        final float cos = (float) Math.cos(Math.toRadians(-35));
        final float sin = (float) Math.sin(Math.toRadians(-35));
        // Kachelgröße zoomabhängig: weiter draussen feineres Muster
        final float tile = Math.max(6f, Math.min(16f, (float) (view.effScale() * 53.0)));
        for (int t = 0; t + 2 < tris.length; t += 3) {
            for (int k = 0; k < 4; k++) {
                int idx = tris[t + Math.min(k, 2)]; // v0,v1,v2,v2 (degeneriertes Quad)
                float sx = view.screenX(poly.xs()[idx]);
                float sy = view.screenY(poly.zs()[idx]);
                float u = (sx * cos - sy * sin) / tile;
                float v = (sx * sin + sy * cos) / tile;
                buf.vertex(sx, sy, 0).texture(u, v).color(r, g, b, a);
            }
        }
    }

    private static void ensureStripes(MinecraftClient client) {
        if (stripesLoaded) {
            return;
        }
        try (var stream = client.getResourceManager().getResource(STRIPES_TEX)
                .orElseThrow(() -> new IllegalStateException("Resource fehlt: " + STRIPES_TEX))
                .getInputStream()) {
            net.minecraft.client.texture.NativeImage image =
                    net.minecraft.client.texture.NativeImage.read(stream);
            client.getTextureManager().registerTexture(STRIPES_TEX,
                    new net.minecraft.client.texture.NativeImageBackedTexture(STRIPES_TEX::toString, image));
            stripesSampler = com.mojang.blaze3d.systems.RenderSystem.getDevice().createSampler(
                    com.mojang.blaze3d.textures.AddressMode.REPEAT,
                    com.mojang.blaze3d.textures.AddressMode.REPEAT,
                    com.mojang.blaze3d.textures.FilterMode.NEAREST,
                    com.mojang.blaze3d.textures.FilterMode.NEAREST,
                    1, java.util.OptionalDouble.empty());
            stripesLoaded = true;
        } catch (Exception e) {
            de.ottoextra.OttoExtra.LOGGER.warn("[map] Streifen-Textur fehlt: {}", e.toString());
            stripesLoaded = true; // kein Retry-Sturm; Schraffur bleibt aus
            stripesSampler = null;
        }
    }

    /** Dreiecke als degenerierte Quads (v0,v1,v2,v2) in den GUI-Buffer. */
    private static int emitPolygon(BufferBuilder buf, LehenPolygon poly,
                                   XaeroMapBridge.View view, int argb) {
        int[] tris = triangles(poly);
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        int emitted = 0;
        for (int t = 0; t + 2 < tris.length; t += 3) {
            float x0 = view.screenX(poly.xs()[tris[t]]);
            float y0 = view.screenY(poly.zs()[tris[t]]);
            float x1 = view.screenX(poly.xs()[tris[t + 1]]);
            float y1 = view.screenY(poly.zs()[tris[t + 1]]);
            float x2 = view.screenX(poly.xs()[tris[t + 2]]);
            float y2 = view.screenY(poly.zs()[tris[t + 2]]);
            buf.vertex(x0, y0, 0).color(r, g, b, a);
            buf.vertex(x1, y1, 0).color(r, g, b, a);
            buf.vertex(x2, y2, 0).color(r, g, b, a);
            buf.vertex(x2, y2, 0).color(r, g, b, a);
            emitted++;
        }
        return emitted;
    }

    // ---- Gefolge-Gruppen (API-Vasallengraph) ----------------------------------

    private static void refreshGroupsIfStale() {
        long now = System.currentTimeMillis();
        if (now - groupsBuiltAt < GROUPS_TTL_MS && !tintByPolyKey.isEmpty()) {
            return;
        }
        groupsBuiltAt = now;
        RegionDataService data = RegionsServices.data();
        if (data == null) {
            return;
        }
        // Gefolge-Cluster über lord_name (public-region-list): Kette
        // Fraktionsname -> lord_name bis zum obersten Lord (leeres lord_name).
        List<FactionRecord> factions = data.allFactions();
        Map<String, FactionRecord> byName = new HashMap<>();
        for (FactionRecord f : factions) {
            if (f.name() != null && !f.name().isBlank()) {
                // Duplikat-Namen: besseren Datensatz wählen (stale "Ungelandet"-
                // Einträge würden sonst die Lehnsherr-Kette abreißen)
                byName.merge(normalizeName(f.name()), f, FactionRecord::better);
            }
        }
        // Polygon -> Wurzel-Gruppe. Zwei Quellen:
        // 1) Fraktions-Kette lord_name bis zum obersten Lord
        // 2) Region-Hierarchie (parent_region_id/vassal_region_refs) für
        //    fraktionslose Verbände wie die Mährstein-Fehde
        Map<String, String> groupOfPoly = new HashMap<>();
        Map<String, String> factionOfPoly = new HashMap<>(); // polyKey -> normalisierter Fraktionsname
        Map<String, GroupMeta> metaOfGroup = new HashMap<>();
        Set<String> vassals = new HashSet<>();
        for (LehenPolygon poly : LehenPolygonStore.polygons()) {
            FactionRecord f = data.factionForRegion(poly.key()).orElse(null);
            String root = null;
            boolean isVassal = false;
            if (f != null && f.name() != null && !f.name().isBlank()) {
                FactionRecord current = f;
                String self = normalizeName(f.name());
                factionOfPoly.put(poly.key(), self);
                root = self;
                for (int depth = 0; depth < 8; depth++) {
                    String lordName = current.lord_name();
                    if (lordName == null || lordName.isBlank()) {
                        break;
                    }
                    String lordKey = normalizeName(lordName);
                    if (lordKey.equals(root)) {
                        break;
                    }
                    root = lordKey;
                    FactionRecord lord = byName.get(lordKey);
                    if (lord == null) {
                        break; // Lord nicht im Datensatz: Name selbst bleibt Gruppe
                    }
                    current = lord;
                }
                isVassal = !root.equals(self);
                FactionRecord rootFaction = byName.getOrDefault(root, current);
                metaOfGroup.putIfAbsent(root, GroupMeta.ofFaction(rootFaction, root));
            } else {
                // Region-Hierarchie: zum Wurzel-Lehen klettern
                RegionRecord r = data.regionByName(poly.key()).orElse(null);
                boolean isRoot = r != null && r.vassal_region_refs() != null
                        && !r.vassal_region_refs().isEmpty();
                if (r == null || (!r.hasParentRegion() && !isRoot)) {
                    continue; // wirklich verbandslos -> neutral
                }
                RegionRecord cur = r;
                for (int depth = 0; depth < 8 && cur.hasParentRegion(); depth++) {
                    RegionRecord parent = data.regionByName(parentKey(cur)).orElse(null);
                    if (parent == null) {
                        break;
                    }
                    cur = parent;
                }
                // Wurzel-Region mit Fraktion? Dann in deren Gefolge-Gruppe
                FactionRecord rf = data.factionForRegion(cur.id()).orElse(null);
                if (rf != null && rf.name() != null && !rf.name().isBlank()) {
                    root = normalizeName(rf.name());
                    metaOfGroup.putIfAbsent(root, GroupMeta.ofFaction(rf, root));
                } else {
                    root = "region:" + normalizeName(cur.name());
                    metaOfGroup.putIfAbsent(root, GroupMeta.ofRegion(cur));
                }
                isVassal = !poly.key().equals(cur.id());
            }
            groupOfPoly.put(poly.key(), root);
            if (isVassal) {
                vassals.add(poly.key()); // Vasall: bekommt Streifen-Muster
            }
        }
        Map<String, Integer> tintOfGroup = assignDomainColors(metaOfGroup, groupOfPoly);
        Map<String, Integer> result = new HashMap<>();
        groupOfPoly.forEach((polyKey, group) -> {
            Integer tint = tintOfGroup.get(group);
            if (tint != null) { // null würde Map.copyOf sprengen -> neutral lassen
                result.put(polyKey, tint);
            }
        });
        // Pro-Fraktion-Farbe: überschreibt die Verband-(Gruppen-)Farbe, nur für
        // die Lehen genau dieser Fraktion.
        factionOfPoly.forEach((polyKey, fac) -> {
            Integer rgb = userFactionColors.get(fac);
            if (rgb != null) {
                result.put(polyKey, (GROUP_ALPHA << 24) | (rgb & 0xFFFFFF));
            }
        });
        // Pro-Lehen-Farbe (höchste Priorität): überschreibt Fraktion/Gruppe.
        userLehenColors.forEach((polyKey, rgb) ->
                result.put(polyKey, (GROUP_ALPHA << 24) | (rgb & 0xFFFFFF)));
        tintByPolyKey = Map.copyOf(result);
        // Gruppen-Übersicht fürs Farb-UI (Anzeigename -> aktueller Tint)
        Map<String, Integer> overview = new java.util.TreeMap<>();
        metaOfGroup.forEach((g, meta) -> {
            Integer tint = tintOfGroup.get(g);
            if (tint != null) {
                overview.put(meta.displayName(), tint);
            }
        });
        groupTintOverview = Map.copyOf(overview);
        vassalPolyKeys = Set.copyOf(vassals);
        Map<String, String> names = new HashMap<>();
        groupOfPoly.forEach((pk, g) -> {
            GroupMeta m = metaOfGroup.get(g);
            if (m != null) {
                names.put(pk, m.displayName());
            }
        });
        groupNameByPoly = Map.copyOf(names);
        groupLabels = buildGroupLabels(groupOfPoly, metaOfGroup);
    }

    /** Anzeige-/Banner-Infos einer Gruppe (Fraktions- oder Region-Wurzel). */
    record GroupMeta(String displayName, FactionRecord rootFaction,
                     String bannerCacheKey, String bannerPath) {
        static GroupMeta ofFaction(FactionRecord f, String fallbackName) {
            String name = f != null && f.name() != null && !f.name().isBlank()
                    ? f.name() : fallbackName;
            return new GroupMeta(name, f, null, null);
        }

        static GroupMeta ofRegion(RegionRecord r) {
            return new GroupMeta(r.name() != null ? r.name() : r.id(), null,
                    "region-" + r.id(), r.effectiveRegionBannerPath());
        }
    }

    private static String parentKey(RegionRecord r) {
        if (r.parent_region_ref() != null && !r.parent_region_ref().isBlank()) {
            return r.parent_region_ref();
        }
        return "lehen_" + r.parent_region_id();
    }

    /**
     * Pro Gefolge ein Label am flächengewichteten Schwerpunkt aller
     * zugehörigen Lehen-Polygone; Name/Wappen vom obersten Lehnsherrn.
     */
    private static List<GroupLabel> buildGroupLabels(Map<String, String> groupOfPoly,
                                                     Map<String, GroupMeta> metaOfGroup) {
        Map<String, double[]> acc = new HashMap<>(); // group -> [sumX*w, sumZ*w, sumW]
        for (LehenPolygon poly : LehenPolygonStore.polygons()) {
            String group = groupOfPoly.get(poly.key());
            if (group == null) {
                continue;
            }
            double area = Math.abs(shoelaceArea(poly));
            if (area < 1) {
                continue;
            }
            double[] a = acc.computeIfAbsent(group, k -> new double[3]);
            a[0] += poly.centroidX() * area;
            a[1] += poly.centroidZ() * area;
            a[2] += area;
        }
        List<GroupLabel> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : acc.entrySet()) {
            double[] a = e.getValue();
            if (a[2] <= 0) {
                continue;
            }
            GroupMeta meta = metaOfGroup.get(e.getKey());
            String display = meta != null ? meta.displayName() : e.getKey();
            out.add(new GroupLabel(meta != null ? meta.rootFaction() : null, display,
                    meta != null ? meta.bannerCacheKey() : null,
                    meta != null ? meta.bannerPath() : null,
                    a[0] / a[2], a[1] / a[2]));
        }
        return List.copyOf(out);
    }

    private static double shoelaceArea(LehenPolygon poly) {
        double[] xs = poly.xs();
        double[] zs = poly.zs();
        int n = poly.pointCount();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sum += xs[i] * zs[j] - xs[j] * zs[i];
        }
        return sum / 2.0;
    }

    /**
     * Domain-Farben nach dem Godot-Kartentool: Name-Hash (djb2) seeded
     * Godot-PCG32 -> RGB je [0.2, 0.6]. Der Konfliktpass läuft hier gegen die
     * KARTEN-NACHBARN (geteilte Grenzsegmente) mit realer HSV-Schwelle —
     * angrenzende Gefolge können nicht mehr fast identisch ausfallen.
     * Vasallen erben die Farbe des obersten Lehnsherrn (Gruppen-Mechanik).
     */
    private static Map<String, Integer> assignDomainColors(Map<String, GroupMeta> metaOfGroup,
                                                           Map<String, String> groupOfPoly) {
        List<String> sorted = new ArrayList<>(metaOfGroup.keySet());
        sorted.sort(String::compareTo);
        // Gruppen-Adjazenz über geteilte Grenzsegmente
        Map<String, Set<String>> adjacent = new HashMap<>();
        for (BorderSegment seg : LehenPolygonStore.segments()) {
            List<String> owners = seg.ownerKeys();
            for (int i = 0; i < owners.size(); i++) {
                for (int j = i + 1; j < owners.size(); j++) {
                    String ga = groupOfPoly.get(owners.get(i));
                    String gb = groupOfPoly.get(owners.get(j));
                    if (ga == null || gb == null || ga.equals(gb)) {
                        continue;
                    }
                    adjacent.computeIfAbsent(ga, k -> new HashSet<>()).add(gb);
                    adjacent.computeIfAbsent(gb, k -> new HashSet<>()).add(ga);
                }
            }
        }
        Map<String, float[]> colorOfGroup = new HashMap<>();
        Map<String, Integer> tintOfGroup = new HashMap<>();
        // Pass 0: Nutzer-Overrides aus der Config (ModMenu-Gefolgefarben) —
        // höchste Priorität, schlagen auch die JSON-Fixfarben.
        Map<String, String> userColors = userGroupColors;
        for (String key : sorted) {
            GroupMeta meta = metaOfGroup.get(key);
            String name = meta != null ? meta.displayName() : key;
            String hex = userColors.get(normalizeName(name));
            Integer rgb = parseHex(hex);
            if (rgb != null) {
                colorOfGroup.put(key, new float[]{
                        ((rgb >>> 16) & 0xFF) / 255f,
                        ((rgb >>> 8) & 0xFF) / 255f,
                        (rgb & 0xFF) / 255f});
                tintOfGroup.put(key, (GROUP_ALPHA << 24) | rgb);
            }
        }
        // Pass 1: fixe Farben aus lehen_polygons.json (group_colors) — werden
        // nie verschoben; generierte Nachbarn weichen ihnen aus.
        Map<String, Integer> fixedColors = LehenPolygonStore.groupColors();
        for (String key : sorted) {
            if (tintOfGroup.containsKey(key)) {
                continue;
            }
            GroupMeta meta = metaOfGroup.get(key);
            String name = meta != null ? meta.displayName() : key;
            Integer fixed = fixedColors.get(normalizeName(name));
            if (fixed != null) {
                colorOfGroup.put(key, new float[]{
                        ((fixed >>> 16) & 0xFF) / 255f,
                        ((fixed >>> 8) & 0xFF) / 255f,
                        (fixed & 0xFF) / 255f});
                tintOfGroup.put(key, (GROUP_ALPHA << 24) | fixed);
            }
        }
        // Pass 2: Godot-Farben für den Rest
        for (String key : sorted) {
            if (tintOfGroup.containsKey(key)) {
                continue;
            }
            GroupMeta meta = metaOfGroup.get(key);
            String name = meta != null ? meta.displayName() : key;
            Pcg32 rng = new Pcg32(godotStringHash(name) & 0xFFFFFFFFL);
            float[] c = {
                    rng.randfRange(0.2f, 0.6f),
                    rng.randfRange(0.2f, 0.6f),
                    rng.randfRange(0.2f, 0.6f)
            };
            Set<String> neighbors = adjacent.getOrDefault(key, Set.of());
            for (int i = 0; i < 10; i++) {
                boolean conflict = false;
                for (String nb : neighbors) {
                    float[] existing = colorOfGroup.get(nb);
                    if (existing != null && hsvSimilarity(c, existing) < 0.16f) {
                        c = hueShift(c, 0.15f);
                        conflict = true;
                        break;
                    }
                }
                if (!conflict) {
                    break;
                }
            }
            colorOfGroup.put(key, c);
            int rgb = (Math.round(c[0] * 255) << 16)
                    | (Math.round(c[1] * 255) << 8)
                    | Math.round(c[2] * 255);
            tintOfGroup.put(key, (GROUP_ALPHA << 24) | rgb);
        }
        return tintOfGroup;
    }

    /** Godot String.hash(): djb2 (hash*33 + zeichen, Start 5381, uint32). */
    private static int godotStringHash(String s) {
        int h = 5381;
        for (int i = 0; i < s.length(); i++) {
            h = (h << 5) + h + s.charAt(i);
        }
        return h;
    }

    /** Godot RandomNumberGenerator (PCG32, identisches Seeding/Output). */
    private static final class Pcg32 {
        private static final long MULT = 6364136223846793005L;
        private static final long DEFAULT_INC = 1442695040888963407L;
        private long state;
        private final long inc;

        Pcg32(long seed) {
            this.inc = (DEFAULT_INC << 1) | 1L;
            this.state = 0;
            nextInt();
            this.state += seed;
            nextInt();
        }

        int nextInt() {
            long old = state;
            state = old * MULT + inc;
            int xorshifted = (int) (((old >>> 18) ^ old) >>> 27);
            int rot = (int) (old >>> 59);
            return Integer.rotateRight(xorshifted, rot);
        }

        float randf() {
            return (float) ((nextInt() & 0xFFFFFFFFL) / 4294967295.0);
        }

        float randfRange(float from, float to) {
            return randf() * (to - from) + from;
        }
    }

    /** Godot hsv_similarity: 0.6*HueDist (zirkulär) + 0.25*SatDist + 0.15*ValDist. */
    private static float hsvSimilarity(float[] rgb1, float[] rgb2) {
        float[] a = rgbToHsv(rgb1);
        float[] b = rgbToHsv(rgb2);
        float hueDist = Math.abs(a[0] - b[0]);
        hueDist = Math.min(hueDist, 1.0f - hueDist);
        float satDist = Math.abs(a[1] - b[1]);
        float valDist = Math.abs(a[2] - b[2]);
        return hueDist * 0.6f + satDist * 0.25f + valDist * 0.15f;
    }

    private static float[] hueShift(float[] rgb, float amount) {
        float[] hsv = rgbToHsv(rgb);
        float h = (hsv[0] + amount) % 1.0f;
        if (h < 0) {
            h += 1.0f;
        }
        int packed = hsvToRgb(h * 360.0f, hsv[1], hsv[2]);
        return new float[]{
                ((packed >>> 16) & 0xFF) / 255f,
                ((packed >>> 8) & 0xFF) / 255f,
                (packed & 0xFF) / 255f
        };
    }

    /** RGB [0..1] -> {h (0..1), s, v}. */
    private static float[] rgbToHsv(float[] rgb) {
        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) {
            h = 0;
        } else if (max == r) {
            h = ((g - b) / d % 6 + 6) % 6 / 6f;
        } else if (max == g) {
            h = ((b - r) / d + 2) / 6f;
        } else {
            h = ((r - g) / d + 4) / 6f;
        }
        float s = max == 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }

    // ---- Geometrie -------------------------------------------------------------

    private static boolean containsPoint(LehenPolygon poly, double px, double pz) {
        boolean inside = false;
        int n = poly.pointCount();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly.xs()[i];
            double zi = poly.zs()[i];
            double xj = poly.xs()[j];
            double zj = poly.zs()[j];
            if ((zi > pz) != (zj > pz) && px < (xj - xi) * (pz - zi) / (zj - zi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Ear-Clipping-Triangulation (Legacy-Algorithmus), gecacht pro Polygon. */
    private static int[] triangles(LehenPolygon poly) {
        return TRI_CACHE.computeIfAbsent(poly, PoliticalOverlay::triangulate);
    }

    private static int[] triangulate(LehenPolygon poly) {
        int n = poly.pointCount();
        if (n < 3) {
            return new int[0];
        }
        double[] xs = poly.xs();
        double[] zs = poly.zs();
        double signedArea = 0.0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            signedArea += xs[i] * zs[j] - xs[j] * zs[i];
        }
        boolean ccw = signedArea > 0.0;
        List<Integer> idx = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            idx.add(i);
        }
        List<Integer> tris = new ArrayList<>(n * 3);
        int safety = n * n + 10;
        while (idx.size() > 3 && safety-- > 0) {
            int sz = idx.size();
            boolean found = false;
            for (int i = 0; i < sz; i++) {
                int a = idx.get((i - 1 + sz) % sz);
                int b = idx.get(i);
                int c = idx.get((i + 1) % sz);
                double cross = (xs[b] - xs[a]) * (zs[c] - zs[a]) - (zs[b] - zs[a]) * (xs[c] - xs[a]);
                if (ccw ? cross <= 0.0 : cross >= 0.0) {
                    continue;
                }
                boolean ear = true;
                for (int j = 0; j < sz; j++) {
                    int p = idx.get(j);
                    if (p == a || p == b || p == c) {
                        continue;
                    }
                    if (pointInTriangle(xs[p], zs[p], xs[a], zs[a], xs[b], zs[b], xs[c], zs[c])) {
                        ear = false;
                        break;
                    }
                }
                if (!ear) {
                    continue;
                }
                tris.add(a);
                tris.add(b);
                tris.add(c);
                idx.remove(i);
                found = true;
                break;
            }
            if (!found) {
                break;
            }
        }
        if (idx.size() == 3) {
            tris.add(idx.get(0));
            tris.add(idx.get(1));
            tris.add(idx.get(2));
        }
        // Fallback bei Ear-Clipping-Abbruch (z. B. Selbstüberschneidung):
        // vollständig = n-2 Dreiecke; sonst Fan-Triangulation — bei konkaven
        // Stellen leicht ungenau, aber die Fläche ist komplett und klickbar.
        if (tris.size() / 3 != n - 2) {
            tris.clear();
            for (int i = 1; i + 1 < n; i++) {
                tris.add(0);
                tris.add(i);
                tris.add(i + 1);
            }
        }
        int[] result = new int[tris.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = tris.get(i);
        }
        // CCW-Polygone liefern CCW-Dreiecke -> Backface-Culling frisst die
        // Fläche. Auf CW (Screen-Winding der übrigen Polygone) drehen.
        if (ccw) {
            for (int i = 0; i + 2 < result.length; i += 3) {
                int tmp = result[i + 1];
                result[i + 1] = result[i + 2];
                result[i + 2] = tmp;
            }
        }
        return result;
    }

    private static boolean pointInTriangle(double px, double pz, double ax, double az,
                                           double bx, double bz, double cx, double cz) {
        double d1 = (px - bx) * (az - bz) - (ax - bx) * (pz - bz);
        double d2 = (px - cx) * (bz - cz) - (bx - cx) * (pz - cz);
        double d3 = (px - ax) * (cz - az) - (cx - ax) * (pz - az);
        boolean hasNeg = d1 < 0.0 || d2 < 0.0 || d3 < 0.0;
        boolean hasPos = d1 > 0.0 || d2 > 0.0 || d3 > 0.0;
        return !hasNeg || !hasPos;
    }

    private static int withAlpha(int tint, float factor) {
        int a = Math.round(((tint >>> 24) & 0xFF) * factor);
        return (a << 24) | (tint & 0xFFFFFF);
    }

    /** Hover-Aufhellung: RGB Richtung Weiss, Alpha verstärkt. */
    private static int lighten(int argb) {
        int a = Math.min(255, Math.round(((argb >>> 24) & 0xFF) * 1.9f));
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r += Math.round((255 - r) * 0.35f);
        g += Math.round((255 - g) * 0.35f);
        b += Math.round((255 - b) * 0.35f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /**
     * Gesamt-Deckkraft des politischen Overlays: Nutzer-Slider (Tag) interpoliert
     * mit dem Nacht-Slider nach Tageszeit. Nachts ist die Karte dunkler -> weniger
     * Deckkraft (automatisch).
     */
    private static float overlayOpacity(MinecraftClient client) {
        var map = de.ottoextra.config.OttoExtraConfig.active().map;
        float day = clamp01(map.politicalOpacity / 100f);
        float night = clamp01(map.politicalOpacityNight / 100f);
        float t = dayFactor(client); // 1 = Tag, 0 = Nacht
        return night + (day - night) * t;
    }

    /** Tagesanteil 0..1 (1 = heller Tag, 0 = tiefe Nacht) mit weichen Dämmerungen. */
    private static float dayFactor(MinecraftClient client) {
        if (client.world == null) {
            return 1f;
        }
        long t = ((client.world.getTimeOfDay() % 24000L) + 24000L) % 24000L;
        if (t < 12000L) {
            return 1f;                          // Tag
        }
        if (t < 13800L) {
            return 1f - (t - 12000L) / 1800f;   // Abenddämmerung
        }
        if (t < 22200L) {
            return 0f;                          // Nacht
        }
        return (t - 22200L) / 1800f;            // Morgendämmerung
    }

    /** Mojibake-feste Normalisierung (ae/oe/ue-Faltung) — lord_name kommt aus
     *  der API teils anders kodiert als faction.name. */
    private static String normalizeName(String name) {
        return de.ottoextra.regions.RegionNameKeys.normalize(name);
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60.0f) % 2 - 1));
        float m = v - c;
        float r, g, b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        int ri = Math.round((r + m) * 255);
        int gi = Math.round((g + m) * 255);
        int bi = Math.round((b + m) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
