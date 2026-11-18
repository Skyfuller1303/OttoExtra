package de.ottoextra.config.settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLayoutTest {

    private static final Path LANG_DIR =
            Path.of("src/main/resources/assets/ottoextra/lang");

    private static JsonObject lang(String file) throws IOException {
        try (Reader r = Files.newBufferedReader(LANG_DIR.resolve(file), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static SettingsRegistry coreRegistry() {
        Runnable noop = () -> { };
        SettingsRegistry registry = SettingsRegistry.build(new OttoExtraConfig(), noop, noop,
                noop, noop, noop, noop, noop, noop, noop);
        de.ottoextra.ui.hudlayout.HudLayoutSettings.register(registry, noop);
        return registry;
    }

    private static List<SettingsRegistry.Option> options(SettingsRegistry registry) {
        List<SettingsRegistry.Option> all = new ArrayList<>();
        for (var m : registry.modules()) {
            for (var t : m.tabs()) {
                for (var c : t.cards()) {
                    all.addAll(c.options());
                }
            }
        }
        return all;
    }

    private static List<String> layoutKeys() {
        List<String> keys = new ArrayList<>();
        for (var c : SettingsLayout.CATEGORIES) {
            for (var p : c.pages()) {
                for (var s : p.sections()) {
                    for (var e : s.entries()) {
                        keys.add(e.configKey());
                    }
                }
            }
        }
        return keys;
    }

    @Test
    void noConfigKeyAppearsTwiceInLayout() {
        Set<String> seen = new HashSet<>();
        for (String key : layoutKeys()) {
            assertTrue(seen.add(key), "duplicate layout entry: " + key);
        }
    }

    @Test
    void everyCoreOptionHasALayoutEntry() {
        Set<String> keys = new HashSet<>(layoutKeys());
        for (var o : options(coreRegistry())) {
            assertTrue(keys.contains(o.configKey), "not in layout: " + o.configKey);
        }
    }

    @Test
    void layoutEntriesHaveGermanTexts() throws IOException {
        JsonObject de = lang("de_de.json");
        for (String key : layoutKeys()) {
            assertTrue(de.has(SettingsLayout.labelKey(key)), "missing label: " + key);
            assertTrue(de.has(SettingsLayout.helpKey(key)), "missing help: " + key);
        }
        for (var o : options(coreRegistry())) {
            if (o.type == SettingsRegistry.Type.ACTION) {
                assertTrue(de.has(SettingsLayout.buttonKey(o.configKey)),
                        "missing button: " + o.configKey);
            }
            if (o.type == SettingsRegistry.Type.CYCLE) {
                for (String v : o.cycleValues) {
                    assertTrue(de.has(SettingsLayout.valueKey(o.configKey, v)),
                            "missing value: " + o.configKey + " = " + v);
                }
            }
        }
    }

    @Test
    void structureTitlesExistInBothLanguages() throws IOException {
        for (String file : List.of("de_de.json", "en_us.json")) {
            JsonObject lang = lang(file);
            for (var c : SettingsLayout.CATEGORIES) {
                assertTrue(lang.has(c.titleKey()), file + " missing " + c.titleKey());
                assertTrue(lang.has(c.descKey()), file + " missing " + c.descKey());
                for (var p : c.pages()) {
                    if (p.titleKey() != null) {
                        assertTrue(lang.has(p.titleKey()), file + " missing " + p.titleKey());
                    }
                    for (var s : p.sections()) {
                        assertTrue(lang.has(s.titleKey()), file + " missing " + s.titleKey());
                        assertTrue(lang.has(s.descKey()), file + " missing " + s.descKey());
                    }
                }
            }
        }
    }

    private static SettingsModel.Item item(String key, SettingsLayout.Level level) {
        return new SettingsModel.Item(SettingsRegistry.Option.action("l", key, () -> { }), level);
    }

    private static List<String> keys(SettingsModel.Visible v) {
        return v.shown().stream().map(i -> i.option().configKey).toList();
    }

    @Test
    void basicRowsAreAlwaysShownAndMoreRowsNeedExpansion() {
        var items = List.of(item("a", SettingsLayout.Level.BASIC),
                item("b", SettingsLayout.Level.MORE), item("c", SettingsLayout.Level.MORE));
        var collapsed = SettingsModel.visible(items, false, SettingsModel.isExpanded(items, null));
        assertEquals(List.of("a"), keys(collapsed));
        assertTrue(collapsed.toggle());
        assertEquals(2, collapsed.hidden());
        assertFalse(collapsed.expanded());

        var open = SettingsModel.visible(items, false, SettingsModel.isExpanded(items, true));
        assertEquals(List.of("a", "b", "c"), keys(open));
        assertTrue(open.toggle());
        assertTrue(open.expanded());
    }

    @Test
    void expertRowsNeedExpertModeAndExpansion() {
        var items = List.of(item("a", SettingsLayout.Level.BASIC),
                item("b", SettingsLayout.Level.MORE), item("x", SettingsLayout.Level.EXPERT));
        assertEquals(List.of("a"), keys(SettingsModel.visible(items, true, false)));
        assertEquals(2, SettingsModel.visible(items, true, false).hidden());
        assertEquals(List.of("a", "b"), keys(SettingsModel.visible(items, false, true)));
        assertEquals(List.of("a", "b", "x"), keys(SettingsModel.visible(items, true, true)));
    }

    @Test
    void sectionWithoutBasicRowsIsExpandedAndHasNoToggle() {
        var items = List.of(item("b", SettingsLayout.Level.MORE),
                item("x", SettingsLayout.Level.EXPERT));
        assertTrue(SettingsModel.isExpanded(items, null));
        var v = SettingsModel.visible(items, false, true);
        assertEquals(List.of("b"), keys(v));
        assertFalse(v.toggle());
    }

    @Test
    void noToggleWhenNothingCanBeHidden() {
        var items = List.of(item("a", SettingsLayout.Level.BASIC),
                item("x", SettingsLayout.Level.EXPERT));
        var v = SettingsModel.visible(items, false, false);
        assertEquals(List.of("a"), keys(v));
        assertFalse(v.toggle());
    }

    @Test
    void voteTweaksAreSkippedOnlyWhenVoteAddonIsPresent() {
        assertFalse(SettingsModel.voteAddonInstalled(Set.of("tweaks.vote.x", "hud.a")));
        assertTrue(SettingsModel.voteAddonInstalled(Set.of("vote.enabled")));
        assertTrue(SettingsModel.skipped("tweaks.vote.x", true));
        assertFalse(SettingsModel.skipped("tweaks.vote.x", false));
        assertFalse(SettingsModel.skipped("vote.enabled", true));
    }

    @Test
    void coreRegistryBuildsCategoriesAndKeepsBackupsCategory() {
        var registry = coreRegistry();
        var cats = SettingsModel.build(registry, false);
        assertTrue(cats.stream().anyMatch(c -> c.id().equals(SettingsLayout.BACKUPS_CATEGORY)));
        Set<String> shown = new LinkedHashSet<>();
        for (var c : SettingsModel.build(registry, true)) {
            for (var p : c.pages()) {
                for (var s : p.sections()) {
                    for (var i : s.items()) {
                        assertTrue(shown.add(i.option().configKey),
                                "listed twice: " + i.option().configKey);
                    }
                }
            }
        }
        assertFalse(cats.stream().anyMatch(SettingsModel.Cat::other));
    }
}
