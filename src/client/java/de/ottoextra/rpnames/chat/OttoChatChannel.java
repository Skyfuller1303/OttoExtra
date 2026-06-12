package de.ottoextra.rpnames.chat;

import de.ottoextra.config.OttoExtraConfig;

/**
 * Ottonien-Chatkanäle anhand des Zeilenpräfixes. Unbekannte
 * Formate bleiben OTHER und werden nie verändert.
 */
public enum OttoChatChannel {
    SPRECHEN,
    REDEN,
    RUFEN,
    FLUESTERN,
    HILFE,
    OFFTOPIC,
    OOC,
    SYSTEM,
    OTHER;

    public static OttoChatChannel fromMessage(String plain) {
        if (plain == null || plain.isEmpty() || plain.charAt(0) != '[') {
            return OTHER;
        }
        if (plain.startsWith("[Sprechen]")) {
            return SPRECHEN;
        }
        if (plain.startsWith("[Reden]")) {
            return REDEN;
        }
        if (plain.startsWith("[Rufen]")) {
            return RUFEN;
        }
        if (plain.startsWith("[Flüstern]") || plain.startsWith("[Fluestern]")) {
            return FLUESTERN;
        }
        if (plain.startsWith("[Hilfe]")) {
            return HILFE;
        }
        if (plain.startsWith("[Offtopic]")) {
            return OFFTOPIC;
        }
        if (plain.startsWith("[OOC]")) {
            return OOC;
        }
        if (plain.startsWith("[System]")) {
            return SYSTEM;
        }
        return OTHER;
    }

    /** Anzeige-Gate je Config. */
    public boolean shouldReplace(OttoExtraConfig.RpNames cfg) {
        if (cfg.showInAllChannels) {
            return this != OTHER && this != SYSTEM;
        }
        return switch (this) {
            case SPRECHEN -> cfg.showInSprechen;
            case REDEN -> cfg.showInReden;
            case RUFEN -> cfg.showInRufen;
            case FLUESTERN -> cfg.showInFluestern;
            case HILFE -> cfg.showInHilfe;
            case OFFTOPIC -> cfg.showInOfftopic;
            case OOC -> cfg.showInOoc;
            default -> false;
        };
    }

    /** Lern-Gate: Hover gilt als server-bestätigt — RP-Kanäle + Hilfe. */
    public boolean shouldLearn() {
        return switch (this) {
            case SPRECHEN, REDEN, RUFEN, FLUESTERN, HILFE -> true;
            default -> false;
        };
    }
}
