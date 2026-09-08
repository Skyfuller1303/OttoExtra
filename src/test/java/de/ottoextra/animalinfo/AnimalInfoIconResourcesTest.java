package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalInfoIconResourcesTest {

    private static final List<String> ICONS = List.of(
            "animals/sheep_ee200.png", "animals/cow_ee201.png",
            "animals/pig_ee202.png", "animals/horse_ee203.png",
            "animals/donkey_ee204.png", "animals/goat_ee205.png",
            "animals/chicken_ee206.png", "animals/wolf_ee207.png",
            "status/gender_ee400.png", "status/female_ee401.png",
            "status/male_ee402.png", "status/feeding_ee403.png",
            "status/feeding_loss_ee404.png", "status/shearable_ee405.png",
            "status/milkable_ee406.png", "status/breed_ready_ee407.png");

    @Test
    void bundlesEveryAnimalToastGlyphAndFontMapping() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        for (String icon : ICONS) {
            String path = "assets/mattis/textures/font/icons/clean/" + icon;
            try (InputStream stream = loader.getResourceAsStream(path)) {
                assertNotNull(stream, path);
                assertTrue(stream.readAllBytes().length > 0, path);
            }
        }

        try (InputStream stream = loader.getResourceAsStream(
                "assets/mattis/font/menu.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (int codepoint = 0xE200; codepoint <= 0xE207; codepoint++) {
                assertTrue(json.contains(String.valueOf((char) codepoint))
                        || json.contains(String.format("\\u%04X", codepoint)));
            }
            for (int codepoint = 0xE400; codepoint <= 0xE407; codepoint++) {
                assertTrue(json.contains(String.valueOf((char) codepoint))
                        || json.contains(String.format("\\u%04X", codepoint)));
            }
        }
    }
}
