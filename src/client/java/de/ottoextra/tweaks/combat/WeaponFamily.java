package de.ottoextra.tweaks.combat;

public enum WeaponFamily {
    BLADE("klingenwaffenkampf"),
    BLUNT("stumpfwaffenkampf"),
    POLEARM("stangenwaffenkampf"),
    RANGED("fernkampf");

    private final String icon;

    WeaponFamily(String icon) { this.icon = icon; }

    public String icon() { return icon; }
}
