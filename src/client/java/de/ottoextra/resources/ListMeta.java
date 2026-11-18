package de.ottoextra.resources;

import java.util.List;

/** Header of a list: name, note and symbol. Shared with the list service (see "Listenkopf"). */
record ListMeta(String name, String note, String icon, String color, long updatedAt) {
    /**
     * The twelve shapes of symbol set 2 ("Symbolsatz 2"), shared by list symbols and marker symbols.
     * Textures: {@code textures/gui/symbols/<shape>_<colour>.png}, made by {@code tools/gen_symbols_v2.py}.
     */
    static final List<String> ICONS = List.of("marker", "schild", "burg", "flagge", "schwert", "schluessel",
            "krone", "spitzhacke", "kristall", "zelt", "haus", "beutel");
    static final List<String> COLORS = List.of("blau", "rot", "gruen", "gold", "violett", "grau");
    static final int NAME_MAX = 40;
    static final int NOTE_MAX = 200;
    static final String DEFAULT_ICON = "schild";
    static final String DEFAULT_COLOR = "blau";
    /** Symbol of a list joined by code that has no shared header yet. */
    static final String JOINED_ICON = "flagge";
    static final String JOINED_COLOR = "grau";

    ListMeta {
        icon = canonicalIcon(icon);
    }

    /**
     * Shape name of symbol set 2 for an old name: {@code turm} is {@code burg}, {@code fahne} is
     * {@code flagge} and {@code stecknadel} is {@code marker}. Other values are returned unchanged.
     * The service still accepts the old names; this client reads them and only sends the new ones.
     */
    static String canonicalIcon(String icon) {
        if (icon == null) return null;
        return switch (icon) {
            case "turm" -> "burg";
            case "fahne" -> "flagge";
            case "stecknadel" -> "marker";
            default -> icon;
        };
    }

    /** Null for a valid header, otherwise the name of the offending field. */
    static String problem(ListMeta meta) {
        if (meta == null) return "meta";
        if (meta.name().isEmpty() || meta.name().length() > NAME_MAX) return "name";
        if (meta.note().length() > NOTE_MAX) return "note";
        if (!ICONS.contains(meta.icon())) return "icon";
        if (!COLORS.contains(meta.color())) return "color";
        if (meta.updatedAt() < 0) return "updatedAt";
        return null;
    }

    /** Newer {@code updatedAt} wins; a tie or a missing remote header keeps the local one. */
    static ListMeta merge(ListMeta local, ListMeta remote) {
        if (remote == null) return local;
        if (local == null || remote.updatedAt() > local.updatedAt()) return remote;
        return local;
    }
}
