package de.ottoextra.resources;

/** Colour per resource kind, shared by the editor and the world map zone drawing. */
public final class ResourceZoneColors {
    private ResourceZoneColors() { }

    /** Opaque RGB colour of a type; the same hues as the kind tiles of the editor. */
    public static int typeColor(String type) {
        String base = ResourcePolicy.baseOf(type);
        if (base == null) return 0xFF9AA6B5;
        return switch (base) {
            case "IRON" -> 0xFFD6A27A;
            case "COAL" -> 0xFF6B7280;
            case "SALT" -> 0xFFF2F2F2;
            case "PEAT" -> 0xFF8B5E34;
            case "CLAY" -> 0xFF9FB4C7;
            case "GARDEN" -> 0xFF5FC25F;
            case "APIARY" -> 0xFFF2C94C;
            default -> 0xFFB06FD6;
        };
    }

    /** Same colour with the given alpha (0..255). */
    public static int withAlpha(int argb, int alpha) {
        return (alpha & 0xFF) << 24 | argb & 0x00FFFFFF;
    }
}
