package de.ottoextra.rpnames.model;
import java.util.regex.Pattern;
public final class LocalRpProfile {
    public static final String UNKNOWN_NAME = "Unbekannt";
    private static final Pattern OBJECT_TEXT_TOKEN = Pattern.compile(
            "\\[[a-z0-9_.-]+:[^\\]\\r\\n]+@[^\\]\\r\\n]+\\]\\s*",
            Pattern.CASE_INSENSITIVE);
    public String uuid;
    public String accountName;
    public String rpName = UNKNOWN_NAME;
    public String title = "";
    public String titleGroup = "";
    public KnowledgeState knowledgeState = KnowledgeState.SEEN;
    public RpNameSource source = RpNameSource.SEEN_ONLINE;
    public boolean locked = false;
    public boolean titleLocked = false;
    public boolean favorite = false;
    public String notes = "";
    public String apiConflict;
    public String apiRpName;
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
    public String displayRpName() {
        return hasRpName() ? rpName : UNKNOWN_NAME;
    }
    public void repair() {
        if (rpName == null || rpName.isBlank()) {
            rpName = UNKNOWN_NAME;
        }
        if (title == null) {
            title = "";
        } else {
            title = cleanLegacyObjectTokens(title);
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
    private static String cleanLegacyObjectTokens(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return OBJECT_TEXT_TOKEN.matcher(value)
                .replaceAll("")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }
}
