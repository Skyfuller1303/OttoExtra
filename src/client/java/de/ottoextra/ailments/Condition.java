package de.ottoextra.ailments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One condition of a player as shown in the server menu: a Leid (known ailment) or any other status.
 * Limits follow docs/zustand-protokoll.md of the server: name 1-40 characters, at most 8 effects of
 * 1-40 characters each.
 */
public record Condition(String name, List<String> effects, String kind) {

    public static final String KIND_LEIDEN = "leiden";
    public static final String KIND_STATUS = "status";
    public static final int MAX_TEXT = 40;
    public static final int MAX_EFFECTS = 8;

    /** Builds a condition from an item's name and lore; empty if the name is blank. */
    public static Optional<Condition> fromItem(String itemName, List<String> lore) {
        String name = clip(AilmentNames.plain(itemName));
        if (name.isEmpty()) return Optional.empty();
        List<String> plainLore = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) plainLore.add(AilmentNames.plain(line));
        }
        List<String> effects = new ArrayList<>();
        for (String effect : AilmentInfo.effects(plainLore)) {
            String e = clip(effect);
            if (!e.isEmpty() && effects.size() < MAX_EFFECTS) effects.add(e);
        }
        return Optional.of(new Condition(name, List.copyOf(effects), kindOf(name)));
    }

    /** "leiden" for one of the known ailment names, otherwise "status". */
    public static String kindOf(String name) {
        return AilmentNames.match(name).isPresent() ? KIND_LEIDEN : KIND_STATUS;
    }

    public boolean isLeiden() {
        return KIND_LEIDEN.equals(kind);
    }

    private static String clip(String text) {
        String t = text == null ? "" : text.trim();
        return t.length() <= MAX_TEXT ? t : t.substring(0, MAX_TEXT).trim();
    }
}
