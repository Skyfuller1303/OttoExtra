package de.ottoextra.tacticalfactions;

public enum Relation {
    INHERIT, ALLIED, NEUTRAL, HOSTILE;

    public String translationKey() {
        return "ottoextra_tacticalfactions.relation." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
