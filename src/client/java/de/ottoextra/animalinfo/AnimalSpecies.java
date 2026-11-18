package de.ottoextra.animalinfo;

/*
 * Behaviour adapted from Viehzucht by yuyukey (MIT), https://github.com/yuyukey/OttonienUtils
 * See THIRD_PARTY_NOTICES.md in the repository root.
 */

import java.util.Locale;
import java.util.Map;

/**
 * Breeding values per animal species from the Ottonien table. They are used when the server has not
 * (yet) told us anything about a specific animal: estimating the wait after an observed mating,
 * the growth time of a newborn and the feeding requirement.
 */
public record AnimalSpecies(String name, long cooldownMs, long growMs, int breedMast) {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;

    /** A sheep can be sheared again six hours after it was sheared. */
    public static final long SHEAR_COOLDOWN_MS = 6 * HOUR;

    private static final Map<String, AnimalSpecies> ALL = Map.ofEntries(
            entry(new AnimalSpecies("Brakel", 6 * HOUR, 12 * HOUR, 20)),
            entry(new AnimalSpecies("Graue Bergziege", 12 * HOUR, 24 * HOUR, 30)),
            entry(new AnimalSpecies("Zaupel", 12 * HOUR, 24 * HOUR, 30)),
            entry(new AnimalSpecies("Hausschwein", 18 * HOUR, 36 * HOUR, 50)),
            entry(new AnimalSpecies("Rotvieh Rind", 24 * HOUR, 48 * HOUR, 80)),
            entry(new AnimalSpecies("Ackergaul", 24 * HOUR, 48 * HOUR, 100)),
            entry(new AnimalSpecies("Reitpferd", 24 * HOUR, 48 * HOUR, 100)),
            entry(new AnimalSpecies("Streitross", 24 * HOUR, 48 * HOUR, 100)),
            entry(new AnimalSpecies("Packesel", 24 * HOUR, 48 * HOUR, 100)),
            entry(new AnimalSpecies("Merciapferd", 2 * HOUR, 6 * HOUR, 30)),
            entry(new AnimalSpecies("Standardpferd", 40 * MINUTE, 20 * MINUTE, 7)));

    private static Map.Entry<String, AnimalSpecies> entry(AnimalSpecies species) {
        return Map.entry(key(species.name), species);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT).replace('-', ' ').replaceAll("\\s+", " ").trim();
    }

    /** Species by the name shown above the animal ("Rotvieh Rind", "Rotvieh-Rind", ...); null if unknown. */
    public static AnimalSpecies byName(String name) {
        return name == null || name.isBlank() ? null : ALL.get(key(name));
    }

    /** Prefers the species stored with the animal, then falls back to the name shown in the world. */
    public static AnimalSpecies resolve(String storedSpecies, String visibleName) {
        AnimalSpecies species = byName(storedSpecies);
        return species != null ? species : byName(visibleName);
    }
}
