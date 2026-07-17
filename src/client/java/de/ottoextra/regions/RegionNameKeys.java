package de.ottoextra.regions;
import java.util.Locale;
public final class RegionNameKeys {
    private RegionNameKeys() {
    }
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String s = foldUmlauts(value);
        s = s.toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9]", "");
    }
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
                .replace("ÃÂ¤", "ae")
                .replace("ÃÂ¶", "oe")
                .replace("ÃÂ¼", "ue")
                .replace("ÃÂ", "ss")
                .replace("Ã¤", "ae")
                .replace("Ã¶", "oe")
                .replace("Ã¼", "ue")
                .replace("Ã", "ss")
                .replace("Ã", "Ae")
                .replace("Ã", "Oe")
                .replace("Ã", "Ue")
                .replace("ä", "ae").replace("Ä", "Ae")
                .replace("ö", "oe").replace("Ö", "Oe")
                .replace("ü", "ue").replace("Ü", "Ue")
                .replace("ß", "ss");
    }
}
