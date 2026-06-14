package de.ottoextra.letter.format;

import java.util.LinkedHashSet;

/**
 * Zentrale Utility für Minecraft-Formatcodes in Briefen: Whitelist,
 * {@code §}↔{@code &}-Konvertierung und „aktiver Format-Prefix" für die
 * Live-Vorschau über automatische Zeilenumbrüche.
 *
 * <p>Bewusst stringbasiert (kein Rich-Text-Modell): Der Editor arbeitet intern
 * mit {@code §}, weil Minecrafts {@code TextRenderer} das live formatiert;
 * beim Senden werden gültige {@code §}-Codes zu {@code &}, die der Server
 * interpretiert. Ungültige {@code §x}/{@code &x}-Folgen bleiben unverändert.
 */
public final class LetterFormattingCodes {

    /** Section-Sign — im Editor genutzt (live formatiert vom TextRenderer). */
    public static final char SECTION = '§';
    /** Ampersand — auf der Leitung genutzt (Server interpretiert). */
    public static final char AMPERSAND = '&';

    private static final String COLORS = "0123456789abcdef";
    private static final String STYLES = "klmnor";
    /** Farben + Stile + Reset (k = obfuscated/magic). */
    private static final String VALID = COLORS + STYLES;

    private LetterFormattingCodes() {
    }

    /** Farbcode {@code 0-9 a-f}? */
    public static boolean isColor(char c) {
        return COLORS.indexOf(Character.toLowerCase(c)) >= 0;
    }

    /** Gültiger Formatcode insgesamt (Farbe, Stil oder Reset)? */
    public static boolean isValidCode(char c) {
        return VALID.indexOf(Character.toLowerCase(c)) >= 0;
    }

    /** Editor → Leitung: gültige {@code §}-Codes zu {@code &}. */
    public static String sectionToAmpersand(String input) {
        return convertPrefix(input, SECTION, AMPERSAND);
    }

    /** Leitung/Fremdtext → Editor: gültige {@code &}-Codes zu {@code §}. */
    public static String ampersandToSection(String input) {
        return convertPrefix(input, AMPERSAND, SECTION);
    }

    private static String convertPrefix(String input, char from, char to) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == from && i + 1 < input.length() && isValidCode(input.charAt(i + 1))) {
                out.append(to).append(Character.toLowerCase(input.charAt(++i)));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    /**
     * Aktive Formatierung (Farbe + Stile) unmittelbar vor {@code index} im
     * Dokument — als {@code §}-Prefix. Nötig, damit automatisch umgebrochene
     * Folgezeilen optisch weiterhin formatiert wirken, obwohl die zweite
     * VisualLine den Ursprungscode nicht mehr enthält.
     *
     * <p>Vanilla-Regeln: Ein Farbcode setzt aktive Stile zurück, {@code §r}
     * setzt alles zurück, Stilcodes sammeln sich.
     */
    public static String activePrefixBefore(String document, int index) {
        if (document == null || document.isEmpty()) {
            return "";
        }
        int limit = Math.min(index, document.length());
        String color = "";
        LinkedHashSet<Character> styles = new LinkedHashSet<>();
        for (int i = 0; i + 1 < limit; i++) {
            if (document.charAt(i) != SECTION) {
                continue;
            }
            char code = Character.toLowerCase(document.charAt(i + 1));
            if (!isValidCode(code)) {
                continue;
            }
            if (isColor(code)) {
                color = SECTION + String.valueOf(code);
                styles.clear();
            } else if (code == 'r') {
                color = "";
                styles.clear();
            } else {
                styles.add(code);
            }
            i++;
        }
        StringBuilder out = new StringBuilder(color);
        for (char style : styles) {
            out.append(SECTION).append(style);
        }
        return out.toString();
    }

    /**
     * Sichtbare Länge: {@code §}/{@code &} + gültiger Folgecode zählen nicht.
     * Für Layout-/Limitlogik, die nur die gerenderten Zeichen meinen soll.
     */
    public static int visibleLength(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == SECTION || c == AMPERSAND) && i + 1 < s.length()
                    && isValidCode(s.charAt(i + 1))) {
                i++;
                continue;
            }
            n++;
        }
        return n;
    }
}
