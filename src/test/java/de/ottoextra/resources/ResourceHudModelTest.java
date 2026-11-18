package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceHudModelTest {
    private static final String OVERWORLD = "minecraft:overworld";

    private static ResourceSnapshot entry(String name, String territory, int secondsUntilFull) {
        return entry(name, territory, secondsUntilFull, "IRON_MINE", OVERWORLD, 0, 0);
    }

    private static ResourceSnapshot entry(String name, String territory, int secondsUntilFull,
                                          String type, String dimension, int x, int z) {
        String status = secondsUntilFull == 0 ? "READY" : "REGENERATING";
        return new ResourceSnapshot(name, name, type, territory, dimension,
                ResourceZone.point(x, 64, z, 3), status, secondsUntilFull == 0 ? 100 : 0, "", "",
                -1, secondsUntilFull);
    }

    private static List<String> names(ResourceHudModel.Result result) {
        return result.shown().stream().map(ResourceSnapshot::name).toList();
    }

    @Test
    void defaultModeShowsOnlyTheLehenOfThePlayerFromAllLists() {
        List<ResourceSnapshot> all = List.of(
                entry("Eigen A", "Holdstewik", 600),
                entry("Verbuendet", "Nachbarlehen", 100),
                entry("Eigen B", "Holdstewik", 0),
                entry("Ohne Lehen", "", 5));
        var result = ResourceHudModel.build(all, OVERWORLD, "Holdstewik", false, 0, 0, 8);
        assertEquals(List.of("Eigen B", "Eigen A"), names(result));
        assertEquals(0, result.more());
    }

    @Test
    void lehenNamesAreComparedIgnoringCaseAndSpelling() {
        List<ResourceSnapshot> all = List.of(entry("A", "Hold Stewik", 10), entry("B", "holdstewik", 20));
        var result = ResourceHudModel.build(all, OVERWORLD, "HOLDSTEWIK", false, 0, 0, 8);
        assertEquals(2, result.shown().size());
    }

    @Test
    void unknownLehenOfThePlayerShowsNothingInDefaultMode() {
        List<ResourceSnapshot> all = List.of(entry("A", "Holdstewik", 10), entry("B", "", 10));
        assertTrue(ResourceHudModel.build(all, OVERWORLD, "", false, 0, 0, 8).shown().isEmpty());
    }

    @Test
    void otherDimensionsAndMarkersNeverShow() {
        List<ResourceSnapshot> all = List.of(
                entry("Nether", "Holdstewik", 10, "IRON_MINE", "minecraft:the_nether", 0, 0),
                entry("Marker", "Holdstewik", 0, "MARKER", OVERWORLD, 0, 0),
                entry("Ok", "Holdstewik", 10));
        assertEquals(List.of("Ok"), names(ResourceHudModel.build(all, OVERWORLD, "Holdstewik", true, 0, 0, 8)));
        assertEquals(List.of("Ok"), names(ResourceHudModel.build(all, OVERWORLD, "Holdstewik", false, 0, 0, 8)));
    }

    @Test
    void allModeShowsEveryLehenAndTruncatesWithTheRemainingCount() {
        List<ResourceSnapshot> all = List.of(
                entry("E", "Fern", 500), entry("C", "Holdstewik", 300), entry("A", "Holdstewik", 100),
                entry("D", "Nachbar", 400), entry("B", "Nachbar", 200));
        var result = ResourceHudModel.build(all, OVERWORLD, "Holdstewik", true, 0, 0, 3);
        assertEquals(List.of("A", "B", "C"), names(result));
        assertEquals(2, result.more());
        var full = ResourceHudModel.build(all, OVERWORLD, "Holdstewik", true, 0, 0, 5);
        assertEquals(0, full.more());
    }

    @Test
    void entriesWithoutLehenShowInAllModeOnlyWithinTwoHundredFiftySixBlocks() {
        List<ResourceSnapshot> all = List.of(
                entry("Nah", "", 10, "IRON_MINE", OVERWORLD, 256, 0),
                entry("Fern", "", 10, "IRON_MINE", OVERWORLD, 257, 0),
                entry("Diagonal", "", 10, "IRON_MINE", OVERWORLD, 200, 200));
        var result = ResourceHudModel.build(all, OVERWORLD, "Holdstewik", true, 0, 0, 8);
        assertEquals(List.of("Nah"), names(result));
    }

    @Test
    void readyAndUpcomingCoverAllMatchingEntriesEvenBeyondTheRowLimit() {
        List<ResourceSnapshot> all = List.of(
                entry("Spaet", "X", 900), entry("Bereit", "X", 0), entry("Bald", "X", 120),
                entry("Mitte", "X", 400), entry("Fremd", "Y", 0));
        var result = ResourceHudModel.build(all, OVERWORLD, "X", false, 0, 0, 1);
        assertEquals(List.of("Bereit"), names(result));
        assertEquals(3, result.more());
        assertEquals(List.of("Bereit"), result.ready().stream().map(ResourceSnapshot::name).toList());
        assertEquals(List.of("Bald", "Mitte", "Spaet"),
                result.upcoming().stream().map(ResourceSnapshot::name).toList());
        assertEquals(120, result.nextSeconds());
        var none = ResourceHudModel.build(List.of(entry("Bereit", "X", 0)), OVERWORLD, "X", false, 0, 0, 8);
        assertEquals(-1, none.nextSeconds());
    }

    @Test
    void equalTimesAreOrderedByNameAndRowLimitIsAtLeastOne() {
        List<ResourceSnapshot> all = List.of(entry("b", "X", 10), entry("A", "X", 10));
        var result = ResourceHudModel.build(all, OVERWORLD, "X", false, 0, 0, 0);
        assertEquals(List.of("A"), names(result));
        assertEquals(1, result.more());
    }
}
