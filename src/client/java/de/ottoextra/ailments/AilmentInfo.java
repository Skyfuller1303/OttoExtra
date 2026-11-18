package de.ottoextra.ailments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure extraction of the readable parts of a Leid or "Zustand" item: plain name, plain lore lines and
 * the effect lines (lines starting with "- " directly after the "Effekte:" line).
 */
public record AilmentInfo(String kind, boolean zustand, String name, List<String> lore, List<String> effects) {

    private static final String EFFECTS_HEADER = "effekte:";

    /** Empty if neither a known Leid nor the "Zustand" item. */
    public static Optional<AilmentInfo> parse(String itemName, List<String> loreLines) {
        String name = AilmentNames.plain(itemName);
        Optional<String> leid = AilmentNames.match(name);
        boolean zustand = AilmentNames.isZustand(name);
        if (leid.isEmpty() && !zustand) return Optional.empty();
        List<String> lore = new ArrayList<>();
        if (loreLines != null) {
            for (String line : loreLines) lore.add(AilmentNames.plain(line));
        }
        return Optional.of(new AilmentInfo(leid.orElse(AilmentNames.ZUSTAND), zustand, name,
                List.copyOf(lore), effects(lore)));
    }

    /** Lines after "Effekte:" that start with "- ", without the dash; stops at the first other line. */
    public static List<String> effects(List<String> plainLore) {
        List<String> out = new ArrayList<>();
        boolean inEffects = false;
        for (String raw : plainLore) {
            String line = AilmentNames.plain(raw);
            if (!inEffects) {
                inEffects = line.toLowerCase(java.util.Locale.ROOT).equals(EFFECTS_HEADER);
            } else if (line.startsWith("- ")) {
                out.add(line.substring(2).trim());
            } else {
                break;
            }
        }
        return List.copyOf(out);
    }
}
