package de.ottoextra.config;

import de.ottoextra.config.settings.SettingsRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnimalGlowConfigTest {
    @TempDir Path directory;

    @Test
    void oldConfigurationsDoNotEnableGlow() {
        var config = new OttoExtraConfig();
        assertFalse(config.animalInfo.genderGlowEnabled);
        config.animalInfo.genderGlowEnabled = true;
        config.restoreFrom("{\"animalInfo\":{\"nametagGenderColor\":true}}");
        assertFalse(config.animalInfo.genderGlowEnabled);
        assertTrue(config.animalInfo.nametagGenderColor);
    }

    @Test
    void menuOptInPersistsAndSnapshotRestoreKeepsTheConfigReference() {
        var config = new OttoExtraConfig();
        var registry = SettingsRegistry.build(config, null, null, null, null, null, null, null, null, null);
        var option = registry.modules().stream().filter(m -> m.id().equals("animalinfo"))
                .flatMap(m -> m.tabs().stream()).flatMap(t -> t.cards().stream())
                .flatMap(c -> c.options().stream()).filter(o -> o.configKey.equals("animalInfo.genderGlowEnabled"))
                .findFirst().orElseThrow();
        option.set.accept("true");
        assertTrue(config.animalInfo.genderGlowEnabled);
        assertFalse(config.animalInfo.nametagGenderColor);
        assertFalse(config.animalInfo.nametagIncludeUnregistered);
        Path file = directory.resolve("glow.json");
        config.save(file);
        var loaded = OttoExtraConfig.load(file);
        assertTrue(loaded.animalInfo.genderGlowEnabled);
        String snapshot = loaded.snapshotJson();
        var reference = loaded.animalInfo;
        loaded.animalInfo.genderGlowEnabled = false;
        loaded.restoreFrom(snapshot);
        assertSame(reference, loaded.animalInfo);
        assertTrue(reference.genderGlowEnabled);
        option.set.accept("false");
        config.save(file);
        assertFalse(OttoExtraConfig.load(file).animalInfo.genderGlowEnabled);
    }
}
