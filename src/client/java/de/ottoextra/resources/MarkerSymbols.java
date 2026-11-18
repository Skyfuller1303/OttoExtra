package de.ottoextra.resources;

import java.util.List;

/**
 * Predefined symbols of markers: a shape and a colour, stored as {@code "<shape>_<colour>"}. Markers
 * use the same twelve shapes and six colours as the list symbols (symbol set 2, {@link ListMeta#ICONS}).
 * The choice is the optional zone field {@code symbol} of the list protocol and is shared with the
 * list. Old shape names are read as their new ones ({@link ListMeta#canonicalIcon}). Pure, so it can
 * be tested without the game.
 */
final class MarkerSymbols {
    static final String PIN = "marker";
    static final List<String> SHAPES = ListMeta.ICONS;
    static final List<String> COLORS = ListMeta.COLORS;
    static final String DEFAULT_COLOR = "violett";
    /** Symbol of a marker without an own choice; also the marker icon on the maps. */
    static final String DEFAULT = key(PIN, DEFAULT_COLOR);
    /** Folder of the 72 symbol textures inside the addon's assets. */
    static final String FOLDER = "textures/gui/symbols/";

    private MarkerSymbols() { }

    static String key(String shape, String color) {
        return shape + "_" + color;
    }

    /** The key with an old shape name replaced by the new one; null stays null. */
    static String canonical(String key) {
        if (key == null) return null;
        int split = key.lastIndexOf('_');
        if (split <= 0) return key;
        return key(ListMeta.canonicalIcon(key.substring(0, split)), key.substring(split + 1));
    }

    /** True for a known shape and colour of symbol set 2 (old shape names are not valid keys). */
    static boolean valid(String key) {
        if (key == null) return false;
        int split = key.lastIndexOf('_');
        return split > 0 && SHAPES.contains(key.substring(0, split)) && COLORS.contains(key.substring(split + 1));
    }

    /** Shape of a key; the marker shape for unknown keys. */
    static String shape(String key) {
        String known = canonical(key);
        return valid(known) ? known.substring(0, known.lastIndexOf('_')) : PIN;
    }

    /** Colour of a key; violet for unknown keys. */
    static String color(String key) {
        String known = canonical(key);
        return valid(known) ? known.substring(known.lastIndexOf('_') + 1) : DEFAULT_COLOR;
    }

    /** Texture path inside the addon's assets; unknown keys give the default marker. */
    static String texturePath(String key) {
        return FOLDER + key(shape(key), color(key)) + ".png";
    }
}
