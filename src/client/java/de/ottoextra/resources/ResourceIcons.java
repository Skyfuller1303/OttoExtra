package de.ottoextra.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Icons shipped with the addon: the server's item pictures for the resource kinds and an own map
 * pin for markers (textures/gui/resources). Should a texture be missing, markers fall back to a
 * vanilla item texture and the book to the coloured gem; nothing is downloaded.
 */
final class ResourceIcons {
    private static final Identifier MARKER_FALLBACK = Identifier.of(
            "minecraft", "textures/item/map.png");
    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();

    private ResourceIcons() { }

    /** Icon of an entry; a marker shows its own symbol or the default marker (see {@link MarkerSymbols}). */
    static Identifier iconFor(ResourceSnapshot resource) {
        if (resource.markerOnly()) {
            return ResourceListIcons.markerTexture(resource.iconPath());
        }
        return iconFor(resource.type());
    }

    static Identifier iconFor(String type) {
        Identifier cached = CACHE.get(type);
        if (cached != null) return cached;
        Identifier icon = ResourcesModule.bundledResourceIcon(type);
        if (icon == null && ResourcePolicy.isMarker(type)
                && MinecraftClient.getInstance().getResourceManager()
                .getResource(MARKER_FALLBACK).isPresent()) {
            icon = MARKER_FALLBACK;
        }
        if (icon != null) CACHE.put(type, icon);
        return icon;
    }

    /** Draws the icon of a type at 16x16; without a texture the coloured gem is drawn centred. */
    static void draw(DrawContext context, String type, int x, int y) {
        Identifier icon = iconFor(type);
        if (icon != null) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, 16, 16, 16, 16);
        } else {
            ResourceGem.draw(context, x + 4, y + 4, type, 2);
        }
    }

    /** Called after resource reloads so changed packs are picked up. */
    static void clear() {
        CACHE.clear();
    }
}
