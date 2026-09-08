package de.ottoextra.welcome;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.nametags.NameTagMode;

import java.util.EnumMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** A local draft: neither choosing a preset nor editing a switch touches the active config. */
final class RoleplayChoices {
    enum Group {
        CHAT("chat"), NAMETAGS("nametags"), MEETING("meeting");

        final String key;
        Group(String key) { this.key = key; }
        String translationKey() { return "ottoextra.setup.rp.group." + key; }
    }

    enum Option {
        RP_CHAT(Group.CHAT, "rp_chat", c -> c.rpnames.showInSprechen, (c, on) -> {
            c.rpnames.showInSprechen = on;
            c.rpnames.showInReden = on;
            c.rpnames.showInRufen = on;
            c.rpnames.showInBruellen = on;
            c.rpnames.showInFluestern = on;
            c.rpnames.showInMurmeln = on;
        }),
        TAB_RP_NAMES(Group.CHAT, "tab_names", c -> c.rpnames.tablistEnabled,
                (c, on) -> c.rpnames.tablistEnabled = on),
        TAB_TITLES(Group.CHAT, "tab_titles", c -> c.rpnames.tablistShowTitle,
                (c, on) -> c.rpnames.tablistShowTitle = on),
        // Visibility is applied before its line-of-sight refinement below.
        NAMETAGS(Group.NAMETAGS, "tags", c -> c.nametags.mode != NameTagMode.HIDE_ALL,
                (c, on) -> c.nametags.mode = on ? NameTagMode.NORMAL : NameTagMode.HIDE_ALL),
        LINE_OF_SIGHT(Group.NAMETAGS, "sight", c -> c.nametags.mode != NameTagMode.NORMAL,
                (c, on) -> {
                    if (c.nametags.mode != NameTagMode.HIDE_ALL)
                        c.nametags.mode = on ? NameTagMode.REALISTIC : NameTagMode.NORMAL;
                }),
        TAG_RP_NAMES(Group.NAMETAGS, "tag_rp", c -> c.nametags.showRpName,
                (c, on) -> c.nametags.showRpName = on),
        TAG_ACCOUNTS(Group.NAMETAGS, "tag_account", c -> c.nametags.showPlayerName,
                (c, on) -> c.nametags.showPlayerName = on),
        TAG_TITLES(Group.NAMETAGS, "tag_title", c -> c.nametags.showTitle,
                (c, on) -> c.nametags.showTitle = on),
        MEET_FIRST(Group.MEETING, "meet", c -> c.rpnames.proactiveMeet,
                (c, on) -> c.rpnames.proactiveMeet = on),
        UNKNOWN_ACCOUNT(Group.MEETING, "unknown_account", c -> c.rpnames.unknownShowAccount,
                (c, on) -> c.rpnames.unknownShowAccount = on),
        INSPECT_ACCOUNT(Group.MEETING, "inspect_account", c -> c.rpnames.inspectShowAccount,
                (c, on) -> c.rpnames.inspectShowAccount = on);

        final Group group;
        private final String key;
        private final Predicate<OttoExtraConfig> read;
        private final BiConsumer<OttoExtraConfig, Boolean> write;

        Option(Group group, String key, Predicate<OttoExtraConfig> read,
               BiConsumer<OttoExtraConfig, Boolean> write) {
            this.group = group;
            this.key = key;
            this.read = read;
            this.write = write;
        }

        String translationKey() { return "ottoextra.setup.rp.option." + key; }
    }

    private final RoleplayPreset preset;
    private final EnumMap<Option, Boolean> values = new EnumMap<>(Option.class);

    RoleplayChoices(RoleplayPreset preset) {
        this.preset = preset;
        OttoExtraConfig defaults = new OttoExtraConfig();
        preset.apply(defaults);
        for (Option option : Option.values()) values.put(option, option.read.test(defaults));
    }

    boolean isOn(Option option) { return values.get(option); }

    boolean isAvailable(Option option) {
        return option.group != Group.NAMETAGS || option == Option.NAMETAGS || isOn(Option.NAMETAGS);
    }

    void toggle(Option option) {
        if (isAvailable(option)) values.put(option, !isOn(option));
    }

    void apply(OttoExtraConfig config) {
        preset.apply(config);
        for (Option option : Option.values()) option.write.accept(config, isOn(option));
        config.rpnames.showUnknownAsUnknown = isOn(Option.MEET_FIRST) || !isOn(Option.UNKNOWN_ACCOUNT);
    }
}
