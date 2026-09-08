package de.ottoextra.tweaks.combat;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Server-announced combat techniques; no client-side mode cycling. */
public enum CombatMode {
    HALBSCHWERTTECHNIK(WeaponFamily.BLADE, "halbschwerttechnik", "Halbschwerttechnik"),
    FLINKER_SCHNITT(WeaponFamily.BLADE, "flinker_schnitt", "Flinker Schnitt"),
    ADERLASS(WeaponFamily.BLADE, "aderlass", "Aderlass"),
    WUCHTHAU(WeaponFamily.BLUNT, "wuchthau", "Wuchthau"),
    DRESCHEN(WeaponFamily.BLUNT, "dreschen", "Dreschen"),
    GLOCKENSCHLAG(WeaponFamily.BLUNT, "glockenschlag", "Glockenschlag"),
    ENTSATTELN(WeaponFamily.POLEARM, "entsatteln", "Entsatteln"),
    KUEHNER_STICH(WeaponFamily.POLEARM, "kuehner_stich", "Kühner Stich"),
    PFAEHLEN(WeaponFamily.POLEARM, "pfaehlen", "Pfählen"),
    SEHNENSCHUSS(WeaponFamily.RANGED, "sehnenschuss", "Sehnenschuss"),
    PFEILHAGEL(WeaponFamily.RANGED, "pfeilhagel", "Pfeilhagel"),
    INS_SCHWARZE(WeaponFamily.RANGED, "ins_schwarze", "Ins Schwarze");

    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-orx]");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");
    private final WeaponFamily family;
    private final String icon;
    private final String displayName;

    CombatMode(WeaponFamily family, String icon, String displayName) {
        this.family = family;
        this.icon = icon;
        this.displayName = displayName;
    }

    public WeaponFamily family() { return family; }
    public String icon() { return icon; }
    public String displayName() { return displayName; }

    /** Requires one unambiguous technique; unrelated actionbar text is ignored. */
    public static Optional<CombatMode> parse(String message) {
        String value = normalize(message);
        if (value.isBlank()) return Optional.empty();
        String padded = " " + value + " ";
        if (padded.contains(" nicht erlernt ") || padded.contains(" nicht gelernt ")
                || padded.contains(" ungelernt ") || padded.contains(" nicht freigeschaltet ")
                || padded.contains(" nicht verfuegbar ")) return Optional.empty();
        CombatMode found = null;
        for (CombatMode mode : values()) {
            if (padded.contains(" " + mode.icon.replace('_', ' ') + " ")) {
                if (found != null) return Optional.empty();
                found = mode;
            }
        }
        return Optional.ofNullable(found);
    }

    static String normalize(String value) {
        if (value == null) return "";
        String plain = FORMATTING.matcher(value).replaceAll("");
        plain = Normalizer.normalize(plain, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        return SEPARATORS.matcher(plain).replaceAll(" ").trim();
    }
}
