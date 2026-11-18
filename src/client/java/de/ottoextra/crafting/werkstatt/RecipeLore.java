package de.ottoextra.crafting.werkstatt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the lore of a station button says, read generically (no recipe list is built in).
 *
 * <p>Recipe lore: rarity in the first line, a description, then {@code Herstellen:} followed by
 * lines {@code N x Item}; alternatives are joined by {@code oder} ("2 x Steinkohle oder 1 x Torf").
 * {@code Benötigt: Hitze} marks a recipe that needs fire. Fire buttons say {@code + m:ss Brenndauer}
 * and list their cost under a bare {@code Benötigt:}. A blank line ends an ingredient block. If any
 * line of an ingredient block cannot be read, the ingredients count as unknown.
 */
public record RecipeLore(String rarity, List<String> description, List<List<Ingredient>> groups,
                         Heat heat, int burnSeconds) {

    public enum Heat { YES, NO, UNKNOWN }

    public record Ingredient(int count, String name) {
        public String key() {
            return WerkstattText.normalize(name);
        }
    }

    private static final Pattern AMOUNT = Pattern.compile("(\\d+)\\s*x\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BURN = Pattern.compile("\\+?\\s*(?:(\\d+):)?(\\d{1,2}):(\\d{2})\\s*brenndauer");

    public static final RecipeLore EMPTY = new RecipeLore("", List.of(), null, Heat.UNKNOWN, -1);

    /** True if the ingredients could be read. */
    public boolean known() {
        return groups != null;
    }

    /** True for a fire button ("Brenndauer" in the lore). */
    public boolean isFire() {
        return burnSeconds > 0;
    }

    /** True for anything that looks like a recipe or fire button (has an ingredient block). */
    public boolean isCraftable() {
        return groups != null && !groups.isEmpty();
    }

    public static RecipeLore parse(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return EMPTY;
        }
        String rarity = "";
        List<String> description = new ArrayList<>();
        List<List<Ingredient>> groups = new ArrayList<>();
        boolean unreadable = false;
        boolean sawBlock = false;
        boolean heat = false;
        int burn = -1;
        boolean inBlock = false;
        boolean beforeBlock = true;
        for (String raw : lore) {
            String line = raw == null ? "" : raw.strip();
            String norm = WerkstattText.normalize(line);
            if (norm.isEmpty()) {
                inBlock = false;
                continue;
            }
            Matcher burnMatch = BURN.matcher(norm);
            if (burnMatch.find()) {
                int h = burnMatch.group(1) == null ? 0 : Integer.parseInt(burnMatch.group(1));
                burn = h * 3600 + Integer.parseInt(burnMatch.group(2)) * 60 + Integer.parseInt(burnMatch.group(3));
                continue;
            }
            if (norm.startsWith("benotigt")) {
                String rest = norm.substring("benotigt".length()).replaceFirst("^\\s*:\\s*", "");
                if (rest.isEmpty()) {
                    inBlock = true;
                    sawBlock = true;
                    beforeBlock = false;
                } else {
                    heat |= rest.contains("hitze");
                    inBlock = false;
                }
                continue;
            }
            if (norm.startsWith("herstellen")) {
                inBlock = true;
                sawBlock = true;
                beforeBlock = false;
                continue;
            }
            if (inBlock) {
                List<Ingredient> alternatives = alternatives(line);
                if (alternatives == null) {
                    unreadable = true;
                } else {
                    groups.add(alternatives);
                }
                continue;
            }
            if (beforeBlock) {
                if (rarity.isEmpty() && description.isEmpty()) {
                    rarity = line;
                } else {
                    description.add(line);
                }
            }
        }
        List<List<Ingredient>> readGroups = sawBlock && !unreadable && !groups.isEmpty() ? List.copyOf(groups) : null;
        Heat h = heat ? Heat.YES : readGroups != null ? Heat.NO : Heat.UNKNOWN;
        return new RecipeLore(rarity, List.copyOf(description), readGroups, h, burn);
    }

    private static List<Ingredient> alternatives(String line) {
        List<Ingredient> out = new ArrayList<>();
        for (String part : line.split("\\s+oder\\s+")) {
            Matcher m = AMOUNT.matcher(part.strip());
            if (!m.matches()) {
                return null;
            }
            int count = Integer.parseInt(m.group(1));
            if (count <= 0) {
                return null;
            }
            out.add(new Ingredient(count, m.group(2).strip()));
        }
        return out.isEmpty() ? null : List.copyOf(out);
    }
}
