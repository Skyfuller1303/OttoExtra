package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalInfoIconResourcesTest {

    @Test
    void toastStatusTexturesAreSixteenPixelTransparentTintMasks() throws Exception {
        for (String name : List.of("gender_mare", "gender_stallion", "gender_unknown",
                "speed", "jump", "feeding", "breed_ready", "shearable", "milkable")) {
            String path = "assets/ottoextra/textures/gui/horse_overview/" + name + ".png";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                var image = ImageIO.read(stream);
                assertNotNull(image, path);
                assertEquals(16, image.getWidth(), path);
                assertEquals(16, image.getHeight(), path);
                boolean visible = false;
                boolean transparent = false;
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int pixel = image.getRGB(x, y);
                        int alpha = pixel >>> 24;
                        assertTrue(alpha == 0 || alpha == 255, path + " has smoothed edges");
                        if (alpha == 255) {
                            visible = true;
                            assertEquals(0xFFFFFF, pixel & 0xFFFFFF, path + " cannot be tinted reliably");
                        } else {
                            transparent = true;
                        }
                    }
                }
                assertTrue(visible && transparent, path + " must be a transparent icon");
            }
        }
    }

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
