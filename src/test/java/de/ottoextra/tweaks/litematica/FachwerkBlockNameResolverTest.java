package de.ottoextra.tweaks.litematica;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FachwerkBlockNameResolverTest {

    private static final String[] INSTRUMENTS = {
            "basedrum", "snare", "bass", "flute", "bell", "guitar",
            "chime", "xylophone", "banjo", "didgeridoo", "bit"
    };

    @Test
    void mapsAllPluginStatesToItsTwentyEightParts() {
        Set<String> keys = new HashSet<>();
        int mappedStates = 0;
        for (String instrument : INSTRUMENTS) {
            for (int note = 0; note <= 24; note++) {
                String key = FachwerkBlockNameResolver.translationKey(
                        instrument, note, false);
                if (key != null) {
                    mappedStates++;
                    keys.add(key);
                }
            }
        }

        assertEquals(252, mappedStates);
        assertEquals(28, keys.size());
        assertEquals("ottoextra.litematica.fachwerk.weiss_balken",
                FachwerkBlockNameResolver.translationKey("basedrum", 0, false));
        assertEquals("ottoextra.litematica.fachwerk.grau_zierde",
                FachwerkBlockNameResolver.translationKey("bell", 6, false));
        assertEquals("ottoextra.litematica.fachwerk.rot_zierde",
                FachwerkBlockNameResolver.translationKey("bit", 9, false));

        assertEquals("ottoextra.litematica.fachwerk.backstein_zierde",
                FachwerkBlockNameResolver.translationKey("bass", 17, false));
        assertEquals("ottoextra.litematica.fachwerk.natur_zierde",
                FachwerkBlockNameResolver.translationKey("guitar", 18, false));
        assertEquals("ottoextra.litematica.fachwerk.gelb_zierde",
                FachwerkBlockNameResolver.translationKey("xylophone", 4, false));
        assertNull(FachwerkBlockNameResolver.translationKey("harp", 0, false));
        assertNull(FachwerkBlockNameResolver.translationKey("basedrum", 0, true));

        assertLanguageContains(keys, "de_de.json");
        assertLanguageContains(keys, "en_us.json");
    }

    @Test
    void inventoryNamesIgnoreCaseSpellingAndRepeatedSpaces() {
        assertTrue(FachwerkInventoryPicker.namesMatch(
                "Weiße Fachwerk Strebe", "  WEISSE   FACHWERK STREBE  "));
        assertTrue(FachwerkInventoryPicker.namesMatch(
                "Backstein Fachwerk Balken", "backstein fachwerk balken"));
        assertTrue(FachwerkInventoryPicker.namesMatch(
                "Gelbes Fachwerk Balken", "Gelb Fachwerk Balken"));
        assertTrue(FachwerkInventoryPicker.namesMatch(
                "Rotes Fachwerk Pfahl", "Rot Fachwerk Pfahl"));
        assertTrue(FachwerkInventoryPicker.namesMatch(
                "Braunes Fachwerk Pfahl", "Braun Fachwerk Pfahl"));
        assertTrue(FachwerkInventoryPicker.namesMatch(
                "Graue Fachwerk Strebe", "Grau Fachwerk Strebe"));
    }

    @Test
    void choosesShortestToolDirectionWithinPickedFachwerkPart() {
        assertTrue(FachwerkBlockNameResolver.isBackwardShorter(
                "basedrum", 0, "basedrum", 3));
        assertTrue(FachwerkBlockNameResolver.isBackwardShorter(
                "basedrum", 1, "basedrum", 0));
        assertTrue(FachwerkBlockNameResolver.isBackwardShorter(
                "basedrum", 20, "snare", 2));
        assertFalse(FachwerkBlockNameResolver.isBackwardShorter(
                "basedrum", 0, "basedrum", 2));
        assertFalse(FachwerkBlockNameResolver.isBackwardShorter(
                "basedrum", 0, "basedrum", 1));
        assertFalse(FachwerkBlockNameResolver.isBackwardShorter(
                "basedrum", 0, "bit", 10));
    }

    @Test
    void recognizesNamedFachwerkTool() {
        assertTrue(FachwerkAutoTuneFeature.isStemmeisenName("Stemmeisen"));
        assertTrue(FachwerkAutoTuneFeature.isStemmeisenName("Fachwerk-Stemmeisen"));
        assertFalse(FachwerkAutoTuneFeature.isStemmeisenName("Holzaxt"));
    }

    private static void assertLanguageContains(Set<String> keys, String file) {
        String path = "/assets/ottoextra/lang/" + file;
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                FachwerkBlockNameResolverTest.class.getResourceAsStream(path)),
                StandardCharsets.UTF_8)) {
            JsonObject language = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : keys) {
                assertTrue(language.has(key), () -> file + " fehlt " + key);
            }
        } catch (Exception error) {
            throw new AssertionError("Sprachdatei konnte nicht geprüft werden: " + file,
                    error);
        }
    }
}
