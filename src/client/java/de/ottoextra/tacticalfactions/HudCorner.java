package de.ottoextra.tacticalfactions;

/** Screen corner of the squad HUD. */
public enum HudCorner {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

    public boolean right() { return this == TOP_RIGHT || this == BOTTOM_RIGHT; }

    public boolean bottom() { return this == BOTTOM_LEFT || this == BOTTOM_RIGHT; }

    public HudCorner next() { return values()[(ordinal() + 1) % values().length]; }

    /** Same side, other vertical edge. */
    public HudCorner flipVertical() {
        return switch (this) {
            case TOP_LEFT -> BOTTOM_LEFT;
            case TOP_RIGHT -> BOTTOM_RIGHT;
            case BOTTOM_LEFT -> TOP_LEFT;
            case BOTTOM_RIGHT -> TOP_RIGHT;
        };
    }

    public String translationKey() {
        return "ottoextra_tacticalfactions.corner." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
