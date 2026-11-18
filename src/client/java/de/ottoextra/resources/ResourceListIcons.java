package de.ottoextra.resources;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.regions.BannerTextureService;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The 72 symbols of set 2 (12 shapes x 6 colours), 16x16 textures shipped with the addon and used
 * for lists and markers alike. A list whose
 * name is the name of a Gefolge known to the core mod shows that Gefolge's coat of arms instead.
 */
final class ResourceListIcons {
    private static final Map<String, Identifier> IDS = new HashMap<>();
    /** Coat of arms per list name key; an empty value means "no Gefolge of that name". */
    private static final Map<String, Identifier> ARMS = new HashMap<>();
    private static final Identifier NONE = Identifier.of(OttoExtraResources.MOD_ID, "none");
    private static final long ARMS_REFRESH_MILLIS = 5_000L;
    private static long armsCheckedAt;

    private ResourceListIcons() { }

    /** Swatch colour of a colour name for the colour picker (opaque ARGB). */
    static int swatch(String color) {
        return switch (color) {
            case "rot" -> 0xFFB5482A;
            case "gruen" -> 0xFF4C8A3A;
            case "gold" -> 0xFFA2802E;
            case "violett" -> 0xFF7A4FA3;
            case "grau" -> 0xFF5B5E6B;
            default -> 0xFF3F7AB8;
        };
    }

    /** Texture of a symbol; unknown names fall back to the default shield. */
    static Identifier texture(String icon, String color) {
        String known = ListMeta.canonicalIcon(icon);
        String shape = ListMeta.ICONS.contains(known) ? known : ListMeta.DEFAULT_ICON;
        String tone = ListMeta.COLORS.contains(color) ? color : ListMeta.DEFAULT_COLOR;
        return IDS.computeIfAbsent(shape + "_" + tone, key -> Identifier.of(OttoExtraResources.MOD_ID,
                MarkerSymbols.FOLDER + key + ".png"));
    }

    /** Texture of a marker symbol; unknown keys give the violet marker. */
    static Identifier markerTexture(String symbol) {
        return texture(MarkerSymbols.shape(symbol), MarkerSymbols.color(symbol));
    }

    /** Draws a marker symbol at 16x16. */
    static void drawMarker(DrawContext context, String symbol, int x, int y) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, markerTexture(symbol), x, y, 0f, 0f, 16, 16, 16, 16);
    }

    /** Draws the symbol scaled to {@code size} GUI pixels (16 = 1x, 12 = 0.75x). */
    static void draw(DrawContext context, String icon, String color, int x, int y, int size) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture(icon, color), x, y, 0f, 0f, size, size, 16, 16,
                16, 16);
    }

    /** Draws a coat-of-arms texture scaled to {@code size} GUI pixels. */
    static void drawArms(DrawContext context, Identifier arms, int x, int y, int size) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, arms, x, y, 0f, 0f, size, size, size, size);
    }

    /**
     * Symbol of a list: the coat of arms of the Gefolge with the list's name when the core mod
     * knows one and has its banner, otherwise the chosen symbol.
     */
    static void drawList(DrawContext context, ResourceList list, int x, int y, int size) {
        if (list == null) {
            draw(context, ListMeta.DEFAULT_ICON, ListMeta.DEFAULT_COLOR, x, y, size);
            return;
        }
        Identifier arms = list.useArms ? coatOfArms(list.name) : null;
        if (arms != null) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, arms, x, y, 0f, 0f, size, size, size, size);
        } else {
            draw(context, list.icon, list.color, x, y, size);
        }
    }

    /** True when the list shows a coat of arms instead of its own symbol. */
    static boolean hasCoatOfArms(ResourceList list) {
        return list != null && coatOfArms(list.name) != null;
    }

    /**
     * Banner texture of the Gefolge whose name equals {@code name} (compared like Lehen names), or
     * null. Faction data and banners come from the core mod; a banner that is still loading counts
     * as none and is asked for again a few seconds later.
     */
    static Identifier coatOfArms(String name) {
        String key = ResourceRepository.territoryKey(name);
        if (key.isEmpty()) return null;
        long now = System.currentTimeMillis();
        if (now - armsCheckedAt > ARMS_REFRESH_MILLIS) {
            ARMS.clear();
            armsCheckedAt = now;
        }
        Identifier cached = ARMS.get(key);
        if (cached == null) {
            cached = resolve(key);
            ARMS.put(key, cached);
        }
        return cached == NONE ? null : cached;
    }

    private static Identifier resolve(String key) {
        try {
            RegionDataService data = RegionsServices.data();
            BannerTextureService banners = RegionsServices.banners();
            if (data == null || banners == null) return NONE;
            for (FactionRecord faction : data.allFactions()) {
                if (faction == null || !key.equals(ResourceRepository.territoryKey(faction.name()))) continue;
                Identifier banner = banners.bannerFor(faction).orElse(null);
                if (banner != null) return banner;
            }
        } catch (RuntimeException | LinkageError error) {
            // Older core versions or missing data: the chosen symbol is shown instead.
        }
        return NONE;
    }
}
