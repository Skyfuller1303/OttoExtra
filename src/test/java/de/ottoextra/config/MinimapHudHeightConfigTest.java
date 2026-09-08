package de.ottoextra.config;

import de.ottoextra.config.settings.SettingsRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinimapHudHeightConfigTest {
    @TempDir Path directory;

    @Test
    void independentHeightFieldsSurviveSaveLoadAndRestoreWithoutChangingScale() throws Exception {
        var config = new OttoExtraConfig();
        var options = SettingsRegistry.build(config, null, null, null, null, null, null, null, null, null)
                .modules().stream().filter(m -> m.id().equals("map")).flatMap(m -> m.tabs().stream())
                .flatMap(t -> t.cards().stream()).flatMap(c -> c.options().stream()).toList();
        int height = 15;
        for (String name : List.of("nameHudHeight", "stateHudHeight", "factionHudHeight")) {
            assertEquals(0, OttoExtraConfig.Map.class.getField(name).getInt(config.map));
            options.stream().filter(o -> o.configKey.equals("map." + name)).findFirst().orElseThrow()
                    .set.accept(Integer.toString(height));
            height += 5;
        }
        Path file = directory.resolve("hud.json");
        config.save(file);
        var loaded = OttoExtraConfig.load(file);
        assertEquals(15, loaded.map.nameHudHeight);
        assertEquals(20, loaded.map.stateHudHeight);
        assertEquals(25, loaded.map.factionHudHeight);
        assertEquals(config.map.stateHudScale, loaded.map.stateHudScale);
        var reference = loaded.map;
        String snapshot = loaded.snapshotJson();
        loaded.map.stateHudHeight = 100;
        loaded.restoreFrom(snapshot);
        assertSame(reference, loaded.map);
        assertEquals(20, reference.stateHudHeight);
        loaded.restoreFrom("{\"map\":{\"stateHudScale\":1.5}}");
        assertEquals(0, reference.stateHudHeight);
        assertEquals(1.5F, reference.stateHudScale);
    }
}
