package de.ottoextra.tacticalfactions;

public enum PoliticalEntityType {
    API, FIEF, FOLLOWING;

    public String translationKey() {
        return "ottoextra_tacticalfactions.entity_type." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
