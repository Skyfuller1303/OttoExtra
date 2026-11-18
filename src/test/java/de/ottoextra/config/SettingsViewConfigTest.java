package de.ottoextra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsViewConfigTest {

    @TempDir
    Path directory;

    @Test
    void defaultsToBook() {
        assertEquals("book", new OttoExtraConfig().settingsView);
    }

    @Test
    void repairReplacesInvalidValuesWithBookAndKeepsClassic() {
        OttoExtraConfig config = new OttoExtraConfig();
        for (String bad : new String[]{null, "", "Book", "grid"}) {
            config.settingsView = bad;
            config.repair();
            assertEquals("book", config.settingsView, "value: " + bad);
        }
        config.settingsView = "classic";
        config.repair();
        assertEquals("classic", config.settingsView);
    }

    @Test
    void classicSurvivesSaveAndLoad() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.settingsView = "classic";
        Path file = directory.resolve("config.json");
        config.save(file);
        assertEquals("classic", OttoExtraConfig.load(file).settingsView);
    }

    @Test
    void invalidStoredValueIsRepairedOnLoad() throws Exception {
        Path file = directory.resolve("config.json");
        Files.writeString(file, "{\"settingsView\": \"sideways\"}", StandardCharsets.UTF_8);
        assertEquals("book", OttoExtraConfig.load(file).settingsView);
    }

    @Test
    void snapshotCarriesTheViewButRestoreKeepsTheCurrentPreference() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.settingsView = "classic";
        String snapshot = config.snapshotJson();
        assertTrue(snapshot.contains("\"settingsView\": \"classic\""));

        config.settingsView = "book";
        config.restoreFrom(snapshot);
        // The preference is not part of the restored settings: discarding must not undo a view switch.
        assertEquals("book", config.settingsView);
    }
}
