package de.ottoextra.nametags;

/**
 * Sichtbarkeitsmodi für Namensschilder.
 */
public enum NameTagMode {

    /** Vanilla-/OttoExtra-Nametag nach normalen Regeln. */
    NORMAL("ottoextra.nametags.mode.normal"),

    /** Nur sichtbar, wenn eine freie Sichtlinie zum Spieler besteht (Drei-Punkt-Raycast). */
    REALISTIC("ottoextra.nametags.mode.realistic"),

    /** Alle Namensschilder ausblenden. */
    HIDE_ALL("ottoextra.nametags.mode.hide_all");

    private final String translationKey;

    NameTagMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    /** Nächster Modus im Zyklus (für den Toggle-Key). */
    public NameTagMode next() {
        NameTagMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
