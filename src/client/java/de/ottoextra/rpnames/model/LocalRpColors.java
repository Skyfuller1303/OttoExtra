package de.ottoextra.rpnames.model;

/**
 * Optionale Farb-Overrides eines Spielers (Hex-Strings "#RRGGBB", null = von
 * der Titelgruppe erben). GSON-direkt, daher mutable Felder.
 */
public final class LocalRpColors {
    public String chatTitleColor;
    public String chatNameColor;
    public String tabTitleColor;
    public String tabNameColor;
    public String nametagTitleColor;
    public String nametagNameColor;

    public boolean isEmpty() {
        return chatTitleColor == null && chatNameColor == null
                && tabTitleColor == null && tabNameColor == null
                && nametagTitleColor == null && nametagNameColor == null;
    }
}
