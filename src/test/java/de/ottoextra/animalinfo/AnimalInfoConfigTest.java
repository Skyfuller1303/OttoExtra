package de.ottoextra.animalinfo;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalInfoConfigTest {

    @Test
    void featureIsEnabledAndAutomaticChatIsHiddenByDefault() {
        OttoExtraConfig config = new OttoExtraConfig();

        assertTrue(config.animalInfo.enabled);
        assertTrue(config.animalInfo.hideAutomaticChat);
        assertFalse(config.animalInfo.breederModeEnabled);
        assertEquals(100, config.animalInfo.hoverDelayMs);
        assertEquals(26, config.animalInfo.modelSize);
        assertEquals(0.68f, config.animalInfo.breedingScale);
    }

    @Test
    void migratesOnlyThePreviousDefaultHoverDelayToFastTiming() {
        OttoExtraConfig previousDefault = new OttoExtraConfig();
        previousDefault.restoreFrom("{\"animalInfo\":{\"hoverDelayMs\":300}}");

        OttoExtraConfig customDelay = new OttoExtraConfig();
        customDelay.restoreFrom("{\"animalInfo\":{\"hoverDelayMs\":550}}");

        OttoExtraConfig alreadyMigrated = new OttoExtraConfig();
        alreadyMigrated.restoreFrom("{\"animalInfo\":{\"hoverDelayMs\":300,"
                + "\"queryTimingVersion\":1}}");

        assertEquals(100, previousDefault.animalInfo.hoverDelayMs);
        assertEquals(550, customDelay.animalInfo.hoverDelayMs);
        assertEquals(300, alreadyMigrated.animalInfo.hoverDelayMs);
    }

    @Test
    void oldSnapshotKeepsAnimalInfoDefaults() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.restoreFrom("{\"chat\":{\"enabled\":true}}");

        assertTrue(config.animalInfo.enabled);
        assertTrue(config.animalInfo.hideAutomaticChat);
        assertFalse(config.animalInfo.breederModeEnabled);
        assertEquals(5_000, config.animalInfo.cacheDurationMs);
        assertTrue(config.animalInfo.showModel);
        assertTrue(config.animalInfo.showTimer);
        assertTrue(config.animalInfo.showHorseStats);
        assertEquals(28, config.animalInfo.titleX);
        assertTrue(config.animalInfo.layout("sheep").showGenderText);
        assertTrue(config.animalInfo.layout("horse").showHorseStats);
        assertTrue(config.animalInfo.layout("horse").showJumpHeight);
        assertTrue(config.animalInfo.layout("horse").showSpeed);
        assertEquals(28, config.animalInfo.layout("horse").jumpHeightY);
        assertEquals(28, config.animalInfo.layout("horse").speedY);
        assertEquals(76, config.animalInfo.layout("donkey").genderIconX);
    }

    @Test
    void layoutsAreIndependentPerAnimalType() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.animalInfo.layout("sheep").showTimer = false;

        assertTrue(config.animalInfo.layout("cow").showTimer);
        assertTrue(config.animalInfo.layout("donkey").showTimer);
    }

    @Test
    void everyAnimalTypeIsEnabledByDefault() {
        OttoExtraConfig config = new OttoExtraConfig();

        for (String animalType : OttoExtraConfig.AnimalInfo.ANIMAL_TYPES) {
            assertTrue(config.animalInfo.layout(animalType).enabled, animalType);
        }
    }

    @Test
    void disabledAnimalTypeIsPersistedWithoutAffectingOthers() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.restoreFrom("{\"animalInfo\":{\"layouts\":{"
                + "\"cow\":{\"enabled\":false}}}}" );

        assertFalse(config.animalInfo.layout("cow").enabled);
        assertTrue(config.animalInfo.layout("sheep").enabled);
        assertTrue(config.animalInfo.layout("pig").enabled);
    }

    @Test
    void resettingLayoutKeepsAnimalTypeDisabled() {
        OttoExtraConfig config = new OttoExtraConfig();
        OttoExtraConfig.AnimalInfoLayout cow = config.animalInfo.layout("cow");
        cow.enabled = false;
        cow.showTimer = false;

        config.animalInfo.resetLayout("cow");

        assertFalse(config.animalInfo.layout("cow").enabled);
        assertTrue(config.animalInfo.layout("cow").showTimer);
    }

    @Test
    void migratesCombinedHorseStatsIntoIndependentParts() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.restoreFrom("{\"animalInfo\":{\"layouts\":{\"horse\":{"
                + "\"showHorseStats\":false,\"horseStatsX\":44,"
                + "\"horseStatsY\":31,\"horseStatsScale\":0.75}}}}" );

        OttoExtraConfig.AnimalInfoLayout horse = config.animalInfo.layout("horse");
        assertFalse(horse.showJumpHeight);
        assertFalse(horse.showSpeed);
        assertEquals(44, horse.jumpHeightX);
        assertEquals(31, horse.jumpHeightY);
        assertEquals(116, horse.speedX);
        assertEquals(31, horse.speedY);
        assertEquals(0.75f, horse.jumpHeightScale);
        assertEquals(0.75f, horse.speedScale);
    }
}
