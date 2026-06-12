package de.ottoextra.rpnames.model;

/**
 * Lokal bekannter Spieler.
 *
 * <p>UUID ist Primärschlüssel, Accountname Pflicht-Fallback — Einträge können
 * ohne UUID entstehen (Chat-Hover) und werden beim ersten GameProfile-Kontakt
 * "geupgradet". Zeitstempel als epochMillis (0 = unbekannt). GSON-direkt.</p>
 */
public final class LocalRpProfile {

    public static final String UNKNOWN_NAME = "Unbekannt";

    public String uuid;             // optional
    public String accountName;      // Pflicht-Fallback (Original-Schreibweise)
    public String rpName = UNKNOWN_NAME;
    public String title = "";
    public String titleGroup = "";
    public KnowledgeState knowledgeState = KnowledgeState.SEEN;
    public RpNameSource source = RpNameSource.SEEN_ONLINE;
    public boolean locked = false;
    public boolean favorite = false;
    public String notes = "";
    /** API-Wert, der von einem lokalen Wert abweicht (nur Anzeige, nie auto-übernommen). */
    public String apiConflict;

    public LocalRpColors colors = new LocalRpColors();

    public boolean showInChat = true;
    public boolean showInTablist = true;
    public boolean showInNametag = true;

    public long firstSeenAt;
    public long lastSeenAt;
    public long firstHeardAt;
    public long lastUpdatedAt;

    public boolean hasRpName() {
        return rpName != null && !rpName.isBlank() && !UNKNOWN_NAME.equalsIgnoreCase(rpName);
    }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    /** Anzeige-Name: RP-Name oder "Unbekannt". */
    public String displayRpName() {
        return hasRpName() ? rpName : UNKNOWN_NAME;
    }

    /** Defensive Defaults nach GSON-Load (alte/fremde Dateien). */
    public void repair() {
        if (rpName == null || rpName.isBlank()) {
            rpName = UNKNOWN_NAME;
        }
        if (title == null) {
            title = "";
        }
        if (titleGroup == null) {
            titleGroup = "";
        }
        if (knowledgeState == null) {
            knowledgeState = hasRpName() ? KnowledgeState.HEARD_NAME : KnowledgeState.SEEN;
        }
        if (source == null) {
            source = RpNameSource.SEEN_ONLINE;
        }
        if (colors == null) {
            colors = new LocalRpColors();
        }
        if (notes == null) {
            notes = "";
        }
    }
}
