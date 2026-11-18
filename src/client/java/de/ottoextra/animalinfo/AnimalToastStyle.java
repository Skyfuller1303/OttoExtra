package de.ottoextra.animalinfo;

import java.util.Arrays;
import java.util.Locale;

/** Visual style of the animal info toast. */
public enum AnimalToastStyle {
    /** Slim bar: status stripe, portrait, name and status line, feeding on the right. */
    COMPACT,
    /** Seal card: status header band, portrait, name, progress bar. */
    DETAILED,
    /** The freely editable layout of the layout editor. */
    CUSTOM;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static String[] ids() {
        return Arrays.stream(values()).map(AnimalToastStyle::id).toArray(String[]::new);
    }

    public static boolean isKnown(String id) {
        return id != null && Arrays.asList(ids()).contains(id.trim().toLowerCase(Locale.ROOT));
    }

    /** Unknown or missing values resolve to the detailed style. */
    public static AnimalToastStyle parse(String id) {
        return isKnown(id) ? valueOf(id.trim().toUpperCase(Locale.ROOT)) : DETAILED;
    }
}
