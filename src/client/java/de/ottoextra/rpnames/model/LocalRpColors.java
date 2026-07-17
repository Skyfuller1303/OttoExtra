package de.ottoextra.rpnames.model;
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
