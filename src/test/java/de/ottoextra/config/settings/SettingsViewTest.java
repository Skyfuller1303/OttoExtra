package de.ottoextra.config.settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsViewTest {

    private static final Path LANG_DIR = Path.of("src/main/resources/assets/ottoextra/lang");

    private static final List<String> VIEW_KEYS = List.of(
            "ottoextra.settings.view.book",
            "ottoextra.settings.view.classic",
            "ottoextra.settings.view.tooltip",
            "ottoextra.settings.view.tooSmall",
            "ottoextra.settings.level.basic",
            "ottoextra.settings.level.expert",
            "ottoextra.settings.help.hint1",
            "ottoextra.settings.help.hint2",
            "ottoextra.settings.reset.tooltip",
            "ottoextra.settings.search.book");

    @Test
    void bookIsChosenOnlyWhenPreferredAndLargeEnough() {
        assertEquals("book", SettingsView.choose("book", 440, 240));
        assertEquals("book", SettingsView.choose("book", 854, 480));
        assertEquals("classic", SettingsView.choose("book", 439, 240));
        assertEquals("classic", SettingsView.choose("book", 440, 239));
        assertEquals("classic", SettingsView.choose("classic", 854, 480));
    }

    @Test
    void unknownPreferenceCountsAsBook() {
        assertEquals("book", SettingsView.choose(null, 854, 480));
        assertEquals("book", SettingsView.choose("grid", 854, 480));
        assertEquals("classic", SettingsView.choose(null, 300, 200));
        assertEquals("book", SettingsView.normalize("nonsense"));
        assertEquals("classic", SettingsView.normalize("classic"));
    }

    @Test
    void otherViewFlipsBetweenTheTwo() {
        assertEquals("classic", SettingsView.other("book"));
        assertEquals("book", SettingsView.other("classic"));
        assertEquals("classic", SettingsView.other(null));
    }

    @Test
    void bookFitsUsesBothDimensions() {
        assertTrue(SettingsView.bookFits(SettingsView.BOOK_MIN_WIDTH, SettingsView.BOOK_MIN_HEIGHT));
        assertFalse(SettingsView.bookFits(SettingsView.BOOK_MIN_WIDTH - 1, 1000));
        assertFalse(SettingsView.bookFits(1000, SettingsView.BOOK_MIN_HEIGHT - 1));
    }

    @Test
    void viewKeysExistInBothLanguages() throws IOException {
        for (String file : List.of("de_de.json", "en_us.json")) {
            JsonObject lang;
            try (Reader r = Files.newBufferedReader(LANG_DIR.resolve(file), StandardCharsets.UTF_8)) {
                lang = JsonParser.parseReader(r).getAsJsonObject();
            }
            for (String key : VIEW_KEYS) {
                assertTrue(lang.has(key), file + " missing " + key);
            }
        }
    }
}
