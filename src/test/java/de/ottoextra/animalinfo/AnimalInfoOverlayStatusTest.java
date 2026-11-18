package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalInfoOverlayStatusTest {

    @Test
    void breedingIsGreenOnlyForExplicitReadyValues() {
        assertTrue(AnimalInfoOverlay.isReady("Ja"));
        assertTrue(AnimalInfoOverlay.isReady("Bereit"));
        assertTrue(AnimalInfoOverlay.isReady("Zuchtbereit"));
        assertTrue(AnimalInfoOverlay.isReady("Jetzt | Mast 30/30"));
        assertFalse(AnimalInfoOverlay.isReady("Mast 0/30"), "Real server: Zuchtbereit: Mast 0/100 means not ready");
        assertFalse(AnimalInfoOverlay.isReady("Mast 0/100"));
        assertFalse(AnimalInfoOverlay.isReady("Mast 29/30"));
        assertTrue(AnimalInfoOverlay.isReady("Mast 30/30"));
        assertTrue(AnimalInfoOverlay.isReady("Mast 100/100"));
        assertTrue(AnimalInfoOverlay.isReady("ja"));
        assertFalse(AnimalInfoOverlay.isReady("nein"));
        assertFalse(AnimalInfoOverlay.isReady("UNDEFINED"));
        assertFalse(AnimalInfoOverlay.isReady("in 11h 59m | Mast 30/30"));
        assertFalse(AnimalInfoOverlay.isReady("Mast 1/100"));
        assertFalse(AnimalInfoOverlay.isReady("Mast 71/100"));
        assertFalse(AnimalInfoOverlay.isReady("Nein"));
        assertFalse(AnimalInfoOverlay.isReady("in 9h 6m | Mast 21/100"));
        assertFalse(AnimalInfoOverlay.isReady("Nicht bereit"));
    }

    @Test
    void optionalAnimalStatusesUseSameExplicitRule() {
        assertTrue(AnimalInfoOverlay.isPositive("Yes"));
        assertFalse(AnimalInfoOverlay.isPositive("Mast 0/20"));
        assertFalse(AnimalInfoOverlay.isPositive("Nein"));
    }

    @Test
    void extractsBreedingCooldownBeforeMastRequirement() {
        assertTrue(AnimalInfoOverlay.breedingCooldown(
                "in 9h 6m | Mast 21/100").equals("in 9h 6m"));
        assertTrue(AnimalInfoOverlay.breedingCooldown("Mast 0/30").isEmpty());
        assertTrue(AnimalInfoOverlay.breedingCooldown("Bereit").isEmpty());
    }

    @Test
    void genderTextNormalizesHorseTerms() {
        assertEquals("Weiblich", AnimalInfoOverlay.genderLabel("Stute"));
        assertEquals("Männlich", AnimalInfoOverlay.genderLabel("Hengst"));
        assertEquals("Männlich", AnimalInfoOverlay.genderLabel("Wallach"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Stute (Weiblich)", "Schaf (Weiblich)", "Sau (Weiblich)",
            "Henne (Weiblich)", "Neue Tierart (Weiblich)"})
    void newFemaleGenderValuesUseFemaleHud(String value) {
        assertEquals("Weiblich", AnimalInfoOverlay.genderLabel(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hengst (Männlich)", "Bulle (Männlich)", "Bock (Männlich)",
            "Eber (Männlich)", "Hahn (Männlich)", "Neue Tierart (Male)"})
    void newMaleGenderValuesUseMaleHud(String value) {
        assertEquals("Männlich", AnimalInfoOverlay.genderLabel(value));
    }

    @Test
    void conflictingGenderAndFemaleOnlyMilkStatusStayUnknownOrUnavailable() {
        assertEquals("Stute (Männlich)", AnimalInfoOverlay.genderLabel("Stute (Männlich)"));
        assertEquals("Unbekannt", AnimalInfoOverlay.genderLabel("Unbekannt"));
        assertFalse(AnimalInfoOverlay.isPositive("Nur weiblich"));
    }

    @Test
    void editorUsesServerAnimalNames() {
        assertEquals("Zaupel", AnimalInfoOverlay.editorSample("sheep").animalName());
        assertEquals("Rotvieh Rind", AnimalInfoOverlay.editorSample("cow").animalName());
        assertEquals("Hausschwein", AnimalInfoOverlay.editorSample("pig").animalName());
        assertEquals("Brakel", AnimalInfoOverlay.editorSample("chicken").animalName());
        assertEquals("Packesel", AnimalInfoOverlay.editorSample("donkey").animalName());
        assertEquals("Streitross", AnimalInfoOverlay.editorSample("streitross").animalName());
        assertEquals("Reitpferd", AnimalInfoOverlay.editorSample("reitpferd").animalName());
        assertEquals("Ackergaul", AnimalInfoOverlay.editorSample("ackergaul").animalName());
    }

    @Test
    void serverHorseNamesResolvePackAnimalsAsDonkeys() {
        assertEquals("donkey", AnimalInfoOverlay.horseType("Packesel"));
        assertEquals("donkey", AnimalInfoOverlay.horseType("Lastesel des Händlers"));
        assertEquals("donkey", AnimalInfoOverlay.horseType("Maultier"));
        assertEquals("donkey", AnimalInfoOverlay.horseType("Donkey"));
        assertEquals("donkey", AnimalInfoOverlay.horseType("Mule"));
        assertEquals("horse", AnimalInfoOverlay.horseType("Pferd"));
    }

    @Test
    void keepsHorseValuesAndUnitsCompactBesideTheirIcons() {
        AnimalInfoSnapshot snapshot = new AnimalInfoSnapshot(
                "Pferd", "Hengst", "", "Ja", "", "",
                "3,42 Blöcke", "11,87 Blöcke/s");

        assertEquals("3,42 Blöcke",
                AnimalInfoOverlay.jumpHeightLabel(snapshot));
        assertEquals("11,87 Blöcke/s",
                AnimalInfoOverlay.speedLabel(snapshot));
        assertEquals("", AnimalInfoOverlay.jumpHeightLabel(new AnimalInfoSnapshot(
                "Pferd", "Hengst", "", "Ja", "", "",
                "", "9,5 Blöcke/s")));
    }
}
