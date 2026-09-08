package de.ottoextra.config;

import de.ottoextra.config.settings.SettingsRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimalOverviewConfigTest {
    @TempDir Path directory;

    @Test
    void optionalAnimalTypesAreIndependentAndPersistThroughSettings() throws Exception {
        var config = new OttoExtraConfig();
        var registry = SettingsRegistry.build(config, null, null, null, null, null, null, null, null, null);
        var options = registry.modules().stream().filter(m -> m.id().equals("animalinfo"))
                .flatMap(m -> m.tabs().stream()).flatMap(t -> t.cards().stream())
                .flatMap(c -> c.options().stream()).toList();
        var names = List.of("overviewShowCows", "overviewShowChickens", "overviewShowPigs",
                "overviewShowGoats", "overviewShowSheep");
        for (String name : names) {
            var field = OttoExtraConfig.AnimalInfo.class.getField(name);
            assertFalse(field.getBoolean(config.animalInfo));
            options.stream().filter(o -> o.configKey.equals("animalInfo." + name)).findFirst().orElseThrow()
                    .set.accept("true");
            config.save(directory.resolve("animals.json"));
            var loaded = OttoExtraConfig.load(directory.resolve("animals.json"));
            assertTrue(field.getBoolean(loaded.animalInfo));
            for (String other : names) {
                if (!other.equals(name)) assertFalse(OttoExtraConfig.AnimalInfo.class.getField(other).getBoolean(loaded.animalInfo));
            }
            String snapshot = config.snapshotJson();
            field.setBoolean(config.animalInfo, false);
            config.restoreFrom(snapshot);
            assertTrue(field.getBoolean(config.animalInfo));
            config.restoreFrom("{\"animalInfo\":{}}");
            assertFalse(field.getBoolean(config.animalInfo));
        }
    }
}
