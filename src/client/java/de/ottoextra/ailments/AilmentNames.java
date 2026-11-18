package de.ottoextra.ailments;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure matcher for the names of the Ottonien ailments ("Leiden") and the "Zustand" item.
 * Formatting codes are stripped and the comparison ignores case.
 */
public final class AilmentNames {

    /** Canonical spelling of all known Leiden (wiki.ottonien.com/w/Kategorie:Leiden). */
    public static final List<String> ALL = List.of(
            "Heimsuchung", "Lausbefall", "Magenleiden", "Pocken", "Prellung", "Spielsucht",
            "Trinksucht", "Trunken", "Unterkühlung", "Verdreckt", "Verwundet", "Wundbrand",
            "Übermüdet");

    public static final String ZUSTAND = "Zustand";

    private AilmentNames() {
    }

    /** Removes legacy section-sign formatting codes and trims the result. */
    public static String plain(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                i++; // skip the code character as well
                continue;
            }
            out.append(c);
        }
        return out.toString().trim();
    }

    private static String key(String text) {
        return Normalizer.normalize(plain(text), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    /** The canonical Leid name for the given (possibly formatted) item name, if it is one. */
    public static Optional<String> match(String name) {
        String k = key(name);
        if (k.isEmpty()) return Optional.empty();
        for (String known : ALL) {
            if (key(known).equals(k)) return Optional.of(known);
        }
        return Optional.empty();
    }

    public static boolean isZustand(String name) {
        return key(name).equals(key(ZUSTAND));
    }
}
