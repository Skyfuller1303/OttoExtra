package de.ottoextra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LetterConfigDefaultsTest {
    @TempDir Path directory;

    @Test
    void pageIsTheDefaultSendModeAndSurvivesRepair() {
        OttoExtraConfig config = new OttoExtraConfig();

        assertEquals("PAGE", config.letter.sendMode);
        config.repair();
        assertEquals("PAGE", config.letter.sendMode);
    }

    @Test
    void announcementAutoOptimizeIsOptIn() {
        assertFalse(new OttoExtraConfig().letter.announcementAutoOptimizeEnabled);
    }

    @Test
    void letterExportSubfoldersDefaultOffAndMissingConfigFieldStaysOff() throws Exception {
        assertFalse(new OttoExtraConfig().letter.exportSubfoldersByPlayer);
        Path file = directory.resolve("old-config.json");
        Files.writeString(file, "{\"letter\":{\"sendMode\":\"PAGE\"}}");
        assertFalse(OttoExtraConfig.load(file, ignored -> { }).letter.exportSubfoldersByPlayer);
    }
}
