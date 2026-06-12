package de.ottoextra.regions;

import java.util.Locale;

/**
 * Normalisierung von Regions-/Fraktionsnamen für robustes Matching
 * (Portierung der bewährten OttoRegions-Logik).
 *
 * <p>Die API liefert teils Mojibake-kodierte Umlaute (UTF-8 doppelt/dreifach
 * dekodiert, z. B. {@code Ã¤}). Alle Varianten werden auf ae/oe/ue/ss gefaltet,
 * dann lowercase, dann alles Nicht-Alphanumerische entfernt.</p>
 */
public final class RegionNameKeys {

    private RegionNameKeys() {
    }

    /** Matching-Schlüssel: umlautgefaltet, lowercase, nur [a-z0-9]. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String s = foldUmlauts(value);
        s = s.toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9]", "");
    }

    /** Dateistamm für Cache-Dateien: umlautgefaltet, [A-Za-z0-9._-], kollabierte '_'. */
    public static String sanitizeFileStem(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String s = foldUmlauts(value);
        s = s.replaceAll("[^A-Za-z0-9._-]+", "_");
        s = s.replaceAll("_{2,}", "_");
        s = s.replaceAll("^_+|_+$", "");
        return s.isBlank() ? "unknown" : s;
    }

    private static String foldUmlauts(String s) {
        return s
                // dreifach kodierte Mojibake zuerst (längste Sequenzen)
                .replace("ÃÂ¤", "ae")
                .replace("ÃÂ¶", "oe")
                .replace("ÃÂ¼", "ue")
                .replace("ÃÂ", "ss")
                // doppelt kodierte Mojibake
                .replace("Ã¤", "ae")
                .replace("Ã¶", "oe")
                .replace("Ã¼", "ue")
                .replace("Ã", "ss")
                .replace("Ã", "Ae")
                .replace("Ã", "Oe")
                .replace("Ã", "Ue")
                // echte Umlaute
                .replace("ä", "ae").replace("Ä", "Ae")
                .replace("ö", "oe").replace("Ö", "Oe")
                .replace("ü", "ue").replace("Ü", "Ue")
                .replace("ß", "ss");
    }
}
