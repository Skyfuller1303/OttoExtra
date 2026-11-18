package de.ottoextra.crafting.werkstatt;

import de.ottoextra.crafting.CraftingStation;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text helpers for the Werkstatt: name normalisation, station titles and server refusals. */
public final class WerkstattText {
    private static final Pattern FORMAT = Pattern.compile("§[0-9A-FK-ORa-fk-or]");
    /**
     * Station screen titles: every {@link CraftingStation} (Zeugtisch, Rüstbank, Amboss, Bognertisch,
     * Kochstelle, Schreibstube, Laboratorium), the agent's "Zeugstisch" spelling, and an optional
     * bracketed suffix such as "(Seite 2)". Group 1 is the station id (normalised name).
     */
    private static final Pattern STATION = Pattern.compile("(" + stationAlternatives()
            + ")(?:\\s*\\([^\\r\\n]*\\))?");
    /** Known sub-menu titles of the stations ("Mehr Sonstiges", "Mehr Pigmente", ...). */
    private static final Pattern SUB_MENU = Pattern.compile("mehr\\s+\\S.*");
    /** Words of the server that mean a craft or fire click was refused (same list as the agent's rules). */
    static final List<String> REFUSALS = List.of("nicht bereit", "fehlende zutaten", "zu wenig", "nicht genug",
            "kein feuer", "feuer erloschen");
    /** Prefixes of the server's own chat notices; player chat never starts with them. */
    private static final Pattern SERVER_PREFIX = Pattern.compile("^\\[(?:info|fehler|hinweis|werkstatt)\\]\\s*");
    /**
     * A refusal sentence as a whole line: optional server prefix, at most three short lead words
     * ("Du hast", "Die Station ist"), then a refusal phrase, and no chat markers (":" after a name,
     * "<name>", brackets) anywhere. A chat line that only mentions a phrase does not match.
     */
    private static final Pattern REFUSAL = Pattern.compile("^(?:\\[(?:info|fehler|hinweis|werkstatt)\\]\\s*)?"
            + "(?:[^\\s:<>\\[\\]]+\\s){0,3}(?:" + String.join("|", REFUSALS.stream().map(Pattern::quote).toList())
            + ")(?:[\\s,.!…]|$)[^:<>\\[\\]]*$");

    private WerkstattText() {
    }

    /** Lower case, no colour codes, umlauts folded, single spaces. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(FORMAT.matcher(value).replaceAll(""), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replace('ä', 'a').replace('ö', 'o').replace('ü', 'u').replace("ß", "ss")
                .replaceAll("\\s+", " ").trim();
    }

    private static String stationAlternatives() {
        StringBuilder b = new StringBuilder("zeugstisch");
        for (CraftingStation s : CraftingStation.ALL) {
            b.append('|').append(Pattern.quote(normalize(s.title())));
        }
        return b.toString();
    }

    /** True for the title of a crafting station screen. */
    public static boolean isStationTitle(String title) {
        return stationId(title) != null;
    }

    /**
     * Station id of a screen title: the normalised station name without suffix ("zeugtisch",
     * "rustbank", ...); "Zeugstisch" counts as "zeugtisch". Null for any other title.
     */
    public static String stationId(String title) {
        Matcher m = STATION.matcher(normalize(title));
        if (!m.matches()) {
            return null;
        }
        String id = m.group(1);
        return id.equals("zeugstisch") ? "zeugtisch" : id;
    }

    /** True for a known sub-menu title of a station ("Mehr Sonstiges", "Mehr Pigmente"). */
    public static boolean isSubMenuTitle(String title) {
        return SUB_MENU.matcher(normalize(title)).matches();
    }

    /** Display name of a station id ("rustbank" gives "Rüstbank"); jobs without a station are Zeugtisch jobs. */
    public static String stationName(String id) {
        String key = id == null || id.isBlank() ? "zeugtisch" : id;
        for (CraftingStation s : CraftingStation.ALL) {
            if (normalize(s.title()).equals(key)) {
                return s.title();
            }
        }
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    /** True if a message is a whole refusal sentence of the server (anchored, see {@link #REFUSAL}). */
    public static boolean isRefusal(String message) {
        String m = normalize(message);
        return !m.isEmpty() && REFUSAL.matcher(m).matches();
    }

    /**
     * True if a game message comes from the server itself: the action bar ({@code overlay}), or a chat
     * line with the server's notice prefix ("[Info] ..."). Player chat, also when a plugin resends it as
     * a system message, has neither.
     */
    public static boolean isServerNotice(String message, boolean overlay) {
        return overlay || SERVER_PREFIX.matcher(normalize(message)).lookingAt();
    }

    /** A server notice that refuses the last click: an anchored refusal sentence or a failure notice. */
    public static boolean isServerRefusal(String message, boolean overlay) {
        return isServerNotice(message, overlay)
                && (isRefusal(message) || de.ottoextra.crafting.CraftingParser.isFailure(message));
    }

    /** "m:ss" for a number of seconds (negative counts as 0). */
    public static String clock(long seconds) {
        long s = Math.max(0, seconds);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
