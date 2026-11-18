package de.ottoextra.horses;

import java.util.List;

/**
 * Symbols of the farms (Höfe) in the animal book: twelve shapes times six colours, the same symbol set 2
 * as the resource lists of OttoExtra Resources. A symbol is stored as the key {@code <shape>_<colour>};
 * the textures live under {@code textures/gui/symbols/<key>.png}.
 */
public final class FarmSymbols {
    public static final List<String> SHAPES = List.of("marker", "schild", "burg", "flagge", "schwert", "schluessel",
            "krone", "spitzhacke", "kristall", "zelt", "haus", "beutel");
    public static final List<String> COLORS = List.of("blau", "rot", "gruen", "gold", "violett", "grau");
    public static final String DEFAULT_SHAPE = "haus";
    public static final String DEFAULT_COLOR = "gruen";
    public static final String DEFAULT = key(DEFAULT_SHAPE, DEFAULT_COLOR);

    private FarmSymbols() {
    }

    public static String key(String shape, String color) {
        return shape + "_" + color;
    }

    /** True for a key made of a known shape and a known colour. */
    public static boolean valid(String key) {
        if (key == null) return false;
        int split = key.lastIndexOf('_');
        return split > 0 && SHAPES.contains(key.substring(0, split)) && COLORS.contains(key.substring(split + 1));
    }

    /** The key itself when valid, otherwise the default symbol. */
    public static String canonical(String key) {
        return valid(key) ? key : DEFAULT;
    }

    /** Shape of a key; the default shape for an invalid key. */
    public static String shape(String key) {
        String known = canonical(key);
        return known.substring(0, known.lastIndexOf('_'));
    }

    /** Colour of a key; the default colour for an invalid key. */
    public static String color(String key) {
        String known = canonical(key);
        return known.substring(known.lastIndexOf('_') + 1);
    }

    /** Key with the shape replaced, keeping the colour. */
    public static String withShape(String key, String shape) {
        return SHAPES.contains(shape) ? key(shape, color(key)) : canonical(key);
    }

    /** Key with the colour replaced, keeping the shape. */
    public static String withColor(String key, String color) {
        return COLORS.contains(color) ? key(shape(key), color) : canonical(key);
    }
}
