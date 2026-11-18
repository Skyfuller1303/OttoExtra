package de.ottoextra.config.settings;

import de.ottoextra.config.OttoExtraBackupService;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * State and behaviour shared by the classic and the book settings screen: the registry, defaults,
 * the discard snapshot, selection, backup actions and text resolution. Switching the view builds a
 * new screen around the same session, so nothing the user did is lost.
 */
final class SettingsSession {

    /** Expert mode survives reopening the screen during a session. */
    static boolean expertMode;

    final Screen parent;
    final OttoExtraConfig config;
    final SettingsRegistry registry;
    final List<SettingsModel.Cat> allCats;
    final Map<String, String> defaults = new HashMap<>();
    final Map<String, Boolean> expanded = new HashMap<>();

    /** Config state from before the settings were opened; restored by "discard". */
    private String snapshot;
    /** View preference at snapshot time; a view switch alone is not an unsaved change. */
    private String snapshotView;
    /** View preference as persisted so far. */
    private String persistedView;

    int selectedModule;
    int selectedTab;
    String query = "";
    Path pendingRestore;
    String statusMessage = "";
    boolean statusWarn;

    /** The screen currently showing this session; sub-screens return to it. */
    Screen owner;

    SettingsSession(Screen parent) {
        this.parent = parent;
        this.config = OttoExtraConfig.active();
        this.snapshot = config.snapshotJson();
        this.snapshotView = config.settingsView;
        this.persistedView = config.settingsView;
        this.registry = SettingsRegistry.build(config,
                () -> open(new de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen(owner)),
                () -> open(new de.ottoextra.config.MapGroupColorsScreen(owner, config)),
                this::openMinimapBannerEditor,
                () -> open(new de.ottoextra.letter.ui.LetterEditorScreen(owner, config)),
                () -> open(new de.ottoextra.config.RegionThemeScreen(owner, config)),
                () -> open(new de.ottoextra.regions.ActivityHudEditScreen(owner, config)),
                () -> open(new de.ottoextra.config.FollowingScreen(owner, config)),
                () -> open(new de.ottoextra.config.ChatChannelColorsScreen(owner, config)),
                () -> open(new de.ottoextra.animalinfo.AnimalInfoEditScreen(owner, config)));
        de.ottoextra.ui.hudlayout.HudLayoutSettings.register(this.registry,
                () -> open(new de.ottoextra.ui.hudlayout.HudLayoutEditorScreen(owner)));
        de.ottoextra.addon.OttoExtraAddons.registerSettings(this.registry);
        this.allCats = SettingsModel.build(this.registry, true);
        SettingsRegistry defaultRegistry = SettingsRegistry.build(new OttoExtraConfig(),
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { });
        de.ottoextra.addon.OttoExtraAddons.registerDefaultSettings(defaultRegistry);
        for (SettingsRegistry.ModulePage m : defaultRegistry.modules()) {
            for (SettingsRegistry.Tab t : m.tabs()) {
                for (SettingsRegistry.Card c : t.cards()) {
                    for (SettingsRegistry.Option o : c.options()) {
                        defaults.put(o.configKey, o.get.get());
                    }
                }
            }
        }
    }

    // ---- navigation ----

    void open(Screen s) {
        MinecraftClient.getInstance().setScreen(s);
    }

    void saveAndClose() {
        config.save();
        open(parent);
    }

    void discardAndClose() {
        boolean oldNobilityColor = config.rpnames.nobilityColorForAllTitles;
        String view = config.settingsView;
        config.restoreFrom(snapshot);
        config.settingsView = view;
        if (oldNobilityColor != config.rpnames.nobilityColorForAllTitles) {
            de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
        }
        if (!view.equals(persistedView)) {
            config.save();
            persistedView = view;
        }
        open(parent);
    }

    /** Stores the preferred view (unsaved until save or discard) and opens it. */
    void switchView(String target) {
        config.settingsView = SettingsView.normalize(target);
        openView(config.settingsView);
    }

    /** Opens a view around this session without touching the stored preference. */
    void openView(String view) {
        open(SettingsView.BOOK.equals(view)
                ? new OttoExtraSettingsBookScreen(this) : new OttoExtraSettingsScreen(this));
    }

    /** Takes the current config as the new discard state (after a backup restore). */
    void markClean() {
        snapshot = config.snapshotJson();
        snapshotView = config.settingsView;
    }

    boolean isDirty() {
        String current = config.settingsView;
        config.settingsView = snapshotView;
        try {
            return !config.snapshotJson().equals(snapshot);
        } finally {
            config.settingsView = current;
        }
    }

    // ---- status and actions ----

    void status(String key, boolean warn) {
        statusMessage = Text.translatable(key).getString();
        statusWarn = warn;
    }

    private void openMinimapBannerEditor() {
        if (!de.ottoextra.map.MinimapBannerScreenFactory.isAvailable()) {
            statusMessage = Text.translatable("ottoextra.map.bannerEdit.requiresMinimap").getString();
            statusWarn = true;
            return;
        }
        open(de.ottoextra.map.MinimapBannerScreenFactory.create(owner, config));
    }

    /** Applies the default of an option; false when there is none or it was rejected. */
    boolean reset(SettingsRegistry.Option option) {
        String def = defaults.get(option.configKey);
        if (def == null) {
            return false;
        }
        try {
            option.set.accept(def);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean isDefault(SettingsRegistry.Option option) {
        String def = defaults.get(option.configKey);
        return def == null || def.equals(option.get.get());
    }

    static void cycle(SettingsRegistry.Option o) {
        String value = o.get.get();
        String[] vals = o.cycleValues;
        int idx = 0;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i].equals(value)) {
                idx = i;
                break;
            }
        }
        o.set.accept(vals[(idx + 1) % vals.length]);
    }

    void runRpRepair() {
        var store = de.ottoextra.rpnames.RpNamesServices.store();
        if (store == null) {
            status("ottoextra.settings.backups.rpRepair.failed", true);
            return;
        }
        var result = store.repairData();
        switch (result.status()) {
            case HEALTHY -> status("ottoextra.settings.backups.rpRepair.healthy", false);
            case RESTORED -> {
                status("ottoextra.settings.backups.rpRepair.restored", false);
                de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
            }
            case NO_DATA -> status("ottoextra.settings.backups.rpRepair.noData", true);
            case NO_VALID_BACKUP -> status("ottoextra.settings.backups.rpRepair.noBackup", true);
            case FAILED -> status("ottoextra.settings.backups.rpRepair.failed", true);
        }
    }

    void createBackup() {
        try {
            OttoExtraBackupService.createBackup();
            status("ottoextra.settings.backups.created", false);
        } catch (Exception e) {
            status("ottoextra.settings.backups.failed", true);
        }
    }

    /**
     * First press arms the entry, second press restores it. Returns true when the status message
     * must survive the following rebuild (the entry was restored).
     */
    boolean pressRestore(OttoExtraBackupService.BackupEntry entry) {
        boolean confirming = entry.dir().equals(pendingRestore);
        if (confirming) {
            String beforeRestore = config.snapshotJson();
            boolean ok = OttoExtraBackupService.restoreBackup(entry.dir());
            if (ok || !beforeRestore.equals(config.snapshotJson())) {
                markClean();
                de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
            }
            status(ok ? "ottoextra.settings.backups.restored"
                    : "ottoextra.settings.backups.failed", !ok);
            pendingRestore = null;
        } else {
            pendingRestore = entry.dir();
        }
        return confirming;
    }

    // ---- text resolution ----

    /** Returns {@code key} if it is translated, otherwise {@code fallback}. */
    static String textKey(String key, String fallback) {
        return I18n.hasTranslation(key) ? key : fallback;
    }

    static Text labelText(SettingsRegistry.Option o) {
        return Text.translatable(textKey(SettingsLayout.labelKey(o.configKey), o.labelKey));
    }

    /** Help text of an option, or null when there is none. */
    static Text helpText(SettingsRegistry.Option o) {
        String key = textKey(SettingsLayout.helpKey(o.configKey), o.tooltipKey);
        return key == null ? null : Text.translatable(key);
    }

    static Text buttonText(SettingsRegistry.Option o) {
        return Text.translatable(textKey(SettingsLayout.buttonKey(o.configKey),
                o.labelKey + ".button"));
    }

    static boolean matches(SettingsRegistry.Option o, String lowerQuery) {
        Text help = helpText(o);
        String hay = (labelText(o).getString() + "\n" + (help == null ? "" : help.getString())
                + "\n" + Text.translatable(o.labelKey).getString() + "\n" + o.configKey)
                .toLowerCase(Locale.ROOT);
        return hay.contains(lowerQuery);
    }

    static Text boolLabel(SettingsRegistry.Option o) {
        boolean on = Boolean.parseBoolean(o.get.get());
        return on
                ? Text.translatable("ottoextra.settings.on").formatted(Formatting.GREEN)
                : Text.translatable("ottoextra.settings.off").formatted(Formatting.RED);
    }

    static Text defaultText(SettingsRegistry.Option o, String def) {
        return switch (o.type) {
            case BOOL -> Text.translatable(Boolean.parseBoolean(def)
                    ? "ottoextra.settings.on" : "ottoextra.settings.off");
            case CYCLE -> cycleLabel(o, def);
            default -> Text.literal(def);
        };
    }

    static Text cycleLabel(SettingsRegistry.Option option) {
        return cycleLabel(option, option.get.get());
    }

    static Text cycleLabel(SettingsRegistry.Option option, String value) {
        String valueKey = SettingsLayout.valueKey(option.configKey, value);
        if (I18n.hasTranslation(valueKey)) {
            return Text.translatable(valueKey);
        }
        if ("regions.activityHudPosition".equals(option.configKey)) {
            return Text.translatable("ottoextra.activityHud.position."
                    + value.toLowerCase(Locale.ROOT));
        }
        if ("animalInfo.toastStyle".equals(option.configKey)) {
            return Text.translatable("ottoextra.animalinfo.style." + value);
        }
        return Text.literal(value);
    }
}
