package de.ottoextra.letter.paste;

/**
 * Eingehenden Fremdtext säubern: Zeilenenden vereinheitlichen,
 * Sonder-Whitespace zu normalen Leerzeichen, Steuerzeichen raus,
 * Formatierungscodes (§) entfernen. Inhalt wird NIE gekürzt.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\r' -> {
                    // CRLF/CR -> LF (folgt LF direkt, hier nichts anhängen)
                    if (i + 1 >= raw.length() || raw.charAt(i + 1) != '\n') {
                        out.append('\n');
                    }
                }
                case '\t' -> out.append("    ");
                case '\u00a0', '\u2007', '\u202f' -> out.append(' '); // NBSP
                case '§' -> i++; // §x Formatierungscode komplett entfernen
                default -> {
                    if (c == '\n' || c >= ' ') {
                        out.append(c);
                    }
                    // andere Steuerzeichen verwerfen
                }
            }
        }
        return out.toString();
    }
}
