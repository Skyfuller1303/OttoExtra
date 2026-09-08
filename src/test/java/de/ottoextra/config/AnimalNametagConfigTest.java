package de.ottoextra.config;

import de.ottoextra.config.settings.SettingsRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnimalNametagConfigTest {
    @TempDir
    Path directory;

    @Test
    void allCombinationsSurviveSaveLoadAndSnapshotRestore() {
        for (boolean show : new boolean[]{false, true}) {
            for (boolean tint : new boolean[]{false, true}) {
                for (boolean include : new boolean[]{false, true}) {
                    var config = new OttoExtraConfig();
                    config.animalInfo.nametagShowGender = show;
                    config.animalInfo.nametagGenderColor = tint;
                    config.animalInfo.nametagIncludeUnregistered = include;
                    Path file = directory.resolve(show + "-" + tint + "-" + include + ".json");
                    config.save(file);
                    var loaded = OttoExtraConfig.load(file);
                    assertEquals(show, loaded.animalInfo.nametagShowGender);
                    assertEquals(tint, loaded.animalInfo.nametagGenderColor);
                    assertEquals(include, loaded.animalInfo.nametagIncludeUnregistered);
                    String snapshot = loaded.snapshotJson();
                    var reference = loaded.animalInfo;
                    loaded.animalInfo.nametagShowGender = !show;
                    loaded.animalInfo.nametagGenderColor = !tint;
                    loaded.animalInfo.nametagIncludeUnregistered = !include;
                    loaded.restoreFrom(snapshot);
                    assertSame(reference, loaded.animalInfo);
                    assertEquals(show, reference.nametagShowGender);
                    assertEquals(tint, reference.nametagGenderColor);
                    assertEquals(include, reference.nametagIncludeUnregistered);
                }
            }
        }
    }

    @Test
    void existingConfigurationKeepsThePreviousNametagAppearance() {
        var config = new OttoExtraConfig();
        assertFalse(config.animalInfo.nametagShowGender);
        assertFalse(config.animalInfo.nametagGenderColor);
        assertFalse(config.animalInfo.nametagIncludeUnregistered);
        config.animalInfo.nametagShowGender = config.animalInfo.nametagGenderColor = true;
        config.animalInfo.nametagIncludeUnregistered = true;
        config.restoreFrom("{\"animalInfo\":{\"enabled\":false}}");
        assertFalse(config.animalInfo.nametagShowGender);
        assertFalse(config.animalInfo.nametagGenderColor);
        assertFalse(config.animalInfo.nametagIncludeUnregistered);
        assertFalse(config.animalInfo.enabled);
    }

    @Test
    void settingsMenuWritesEachOptionIndependently() {
        var config = new OttoExtraConfig();
        var registry = SettingsRegistry.build(config, null, null, null, null, null, null, null, null, null);
        var options = registry.modules().stream().filter(m -> m.id().equals("animalinfo"))
                .flatMap(m -> m.tabs().stream()).flatMap(t -> t.cards().stream())
                .flatMap(c -> c.options().stream()).toList();
        var show = options.stream().filter(o -> o.configKey.equals("animalInfo.nametagShowGender")).findFirst().orElseThrow();
        var tint = options.stream().filter(o -> o.configKey.equals("animalInfo.nametagGenderColor")).findFirst().orElseThrow();
        show.set.accept("true");
        assertTrue(config.animalInfo.nametagShowGender);
        assertFalse(config.animalInfo.nametagGenderColor);
        tint.set.accept("true");
        show.set.accept("false");
        assertFalse(config.animalInfo.nametagShowGender);
        assertTrue(config.animalInfo.nametagGenderColor);
        var include = options.stream().filter(o -> o.configKey.equals("animalInfo.nametagIncludeUnregistered")).findFirst().orElseThrow();
        include.set.accept("true");
        assertTrue(config.animalInfo.nametagIncludeUnregistered);
        assertFalse(config.animalInfo.nametagShowGender);
        assertTrue(config.animalInfo.nametagGenderColor);
    }
}
