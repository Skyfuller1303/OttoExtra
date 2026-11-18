package de.ottoextra.tacticalfactions;

public record Resolution(Relation relation, String source) {
    public Resolution {
        if (relation == null || relation == Relation.INHERIT) relation = Relation.NEUTRAL;
        if (source == null) source = "";
    }
}
