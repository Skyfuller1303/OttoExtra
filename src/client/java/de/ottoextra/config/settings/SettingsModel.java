package de.ottoextra.config.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves {@link SettingsLayout} against the options that are actually registered and decides
 * which rows of a section are visible. Free of Minecraft classes so it can be unit-tested.
 */
public final class SettingsModel {

    /** Id of the extra category that collects options the layout does not know. */
    public static final String OTHER_ID = "otherAddons";
    public static final String OTHER_TITLE_KEY = "ottoextra.settings.otherAddons";
    public static final String OTHER_DESC_KEY = "ottoextra.settings.otherAddons.desc";

    public record Item(SettingsRegistry.Option option, SettingsLayout.Level level) {
    }

    public record Sec(String titleKey, String descKey, List<Item> items) {
    }

    public record Pg(String titleKey, List<Sec> sections) {
    }

    public record Cat(String id, String titleKey, String descKey, List<Pg> pages) {
        public boolean other() {
            return OTHER_ID.equals(id);
        }
    }

    /** Rows of one section that are shown right now. */
    public record Visible(List<Item> shown, int hidden, boolean toggle, boolean expanded) {
    }

    private SettingsModel() {
    }

    /** First registered option per config key. */
    public static Map<String, SettingsRegistry.Option> index(SettingsRegistry registry) {
        Map<String, SettingsRegistry.Option> index = new LinkedHashMap<>();
        for (SettingsRegistry.ModulePage m : registry.modules()) {
            for (SettingsRegistry.Tab t : m.tabs()) {
                for (SettingsRegistry.Card c : t.cards()) {
                    for (SettingsRegistry.Option o : c.options()) {
                        index.putIfAbsent(o.configKey, o);
                    }
                }
            }
        }
        return index;
    }

    /** True when the Vote addon is present, which replaces the built-in vote tweaks. */
    public static boolean voteAddonInstalled(Collection<String> configKeys) {
        for (String key : configKeys) {
            if (key.startsWith("vote.")) {
                return true;
            }
        }
        return false;
    }

    public static boolean skipped(String configKey, boolean voteAddon) {
        return voteAddon && configKey.startsWith("tweaks.vote.");
    }

    /**
     * Builds the categories. Sections whose entries are all expert-level are left out while
     * {@code expert} is off; empty sections, pages and categories are dropped.
     */
    public static List<Cat> build(SettingsRegistry registry, boolean expert) {
        Map<String, SettingsRegistry.Option> index = index(registry);
        boolean vote = voteAddonInstalled(index.keySet());
        Set<String> known = new LinkedHashSet<>();
        List<Cat> result = new ArrayList<>();
        for (SettingsLayout.Category category : SettingsLayout.CATEGORIES) {
            List<Pg> pages = new ArrayList<>();
            for (SettingsLayout.Page page : category.pages()) {
                List<Sec> sections = new ArrayList<>();
                for (SettingsLayout.Section section : page.sections()) {
                    List<Item> items = new ArrayList<>();
                    for (SettingsLayout.Entry entry : section.entries()) {
                        known.add(entry.configKey());
                        SettingsRegistry.Option o = index.get(entry.configKey());
                        if (o != null && !skipped(entry.configKey(), vote)) {
                            items.add(new Item(o, entry.level()));
                        }
                    }
                    if (!items.isEmpty() && (expert || !allExpert(items))) {
                        sections.add(new Sec(section.titleKey(), section.descKey(), items));
                    }
                }
                if (!sections.isEmpty()) {
                    pages.add(new Pg(page.titleKey(), sections));
                }
            }
            boolean backups = SettingsLayout.BACKUPS_CATEGORY.equals(category.id());
            if (!pages.isEmpty() || backups) {
                if (pages.isEmpty()) {
                    pages.add(new Pg(null, new ArrayList<>()));
                }
                result.add(new Cat(category.id(), category.titleKey(), category.descKey(),
                        pages));
            }
        }

        List<Sec> other = new ArrayList<>();
        for (SettingsRegistry.ModulePage m : registry.modules()) {
            for (SettingsRegistry.Tab t : m.tabs()) {
                for (SettingsRegistry.Card c : t.cards()) {
                    List<Item> items = new ArrayList<>();
                    for (SettingsRegistry.Option o : c.options()) {
                        if (!known.contains(o.configKey) && index.get(o.configKey) == o) {
                            items.add(new Item(o, SettingsLayout.Level.BASIC));
                        }
                    }
                    if (!items.isEmpty()) {
                        other.add(new Sec(c.titleKey(), c.descKey(), items));
                    }
                }
            }
        }
        if (!other.isEmpty()) {
            List<Pg> pages = new ArrayList<>();
            pages.add(new Pg(null, other));
            result.add(new Cat(OTHER_ID, OTHER_TITLE_KEY, OTHER_DESC_KEY, pages));
        }
        return result;
    }

    private static boolean allExpert(List<Item> items) {
        for (Item i : items) {
            if (i.level() != SettingsLayout.Level.EXPERT) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a section starts expanded: only when it has no basic rows. {@code override} is the
     * state the user chose, or null.
     */
    public static boolean isExpanded(List<Item> items, Boolean override) {
        if (countLevel(items, SettingsLayout.Level.BASIC) == 0) {
            return true;
        }
        return override != null && override;
    }

    /** Rows to show for a section given expert mode and expanded state. */
    public static Visible visible(List<Item> items, boolean expert, boolean expanded) {
        boolean noBasic = countLevel(items, SettingsLayout.Level.BASIC) == 0;
        boolean open = expanded || noBasic;
        List<Item> shown = new ArrayList<>();
        int hideable = 0;
        for (Item i : items) {
            switch (i.level()) {
                case BASIC -> shown.add(i);
                case MORE -> {
                    hideable++;
                    if (open) {
                        shown.add(i);
                    }
                }
                case EXPERT -> {
                    if (expert) {
                        hideable++;
                        if (open) {
                            shown.add(i);
                        }
                    }
                }
            }
        }
        boolean toggle = !noBasic && hideable > 0;
        return new Visible(shown, open ? 0 : hideable, toggle, open);
    }

    private static int countLevel(List<Item> items, SettingsLayout.Level level) {
        int n = 0;
        for (Item i : items) {
            if (i.level() == level) {
                n++;
            }
        }
        return n;
    }
}
