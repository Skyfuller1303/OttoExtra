package de.ottoextra.horses.ui;

import de.ottoextra.api.model.AnimalBreedingHorse;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static de.ottoextra.horses.ui.HorseOverviewView.SortOrder.*;

class HorseOverviewViewTest {

    @Test
    void sortsFarmGroupsBothWaysAndKeepsBlankFarmLast() {
        List<AnimalBreedingHorse> records = List.of(
                horse("5f4c96e3-8ef0-4fa0-a90c-efe5831b9a11",
                        "Boreas", "Nordmark", "Birkenhof", "BROWN", "NONE"),
                horse("32436c53-3054-4b04-8948-67e462810c71",
                        "Abendrot", "Südmark", "Apfelhof", "CHESTNUT", "WHITE_DOTS"),
                horse("4e48c721-e2ab-4169-b4ba-3aa2d15b3508",
                        "Freilauf", "Westen", "", "BLACK", "NONE"));

        assertEquals(List.of("Apfelhof", "Birkenhof", ""),
                HorseOverviewView.groups(records, "", FARMS_ASCENDING).stream()
                        .map(HorseOverviewView.FarmGroup::farm).toList());
        assertEquals(List.of("Birkenhof", "Apfelhof", ""),
                HorseOverviewView.groups(records, "", FARMS_DESCENDING).stream()
                        .map(HorseOverviewView.FarmGroup::farm).toList());
    }

    @Test
    void searchesNameFarmOriginAndUuid() {
        AnimalBreedingHorse record = horse(
                "32436c53-3054-4b04-8948-67e462810c71",
                "Abendrot", "Nordmark", "Apfelhof", "CHESTNUT", "WHITE_DOTS");

        for (String query : List.of("abend", "apfel", "nord", "32436c53")) {
            assertEquals(1, HorseOverviewView.groups(
                    List.of(record), query, FARMS_ASCENDING).size());
        }
        assertEquals(0, HorseOverviewView.groups(
                List.of(record), "fremdhof", FARMS_ASCENDING).size());
    }

    @Test
    void parsesStoredCoatAndFallsBackForUnknownValues() {
        HorseAppearance stored = HorseAppearance.from(horse(
                "32436c53-3054-4b04-8948-67e462810c71",
                "Abendrot", "Nordmark", "Apfelhof",
                "CHESTNUT", "WHITE_DOTS"));
        HorseAppearance unknown = HorseAppearance.from(horse(
                "5f4c96e3-8ef0-4fa0-a90c-efe5831b9a11",
                "Boreas", "Nordmark", "Birkenhof",
                "unbekannt", "unbekannt"));

        assertEquals(HorseColor.CHESTNUT, stored.color());
        assertEquals(HorseMarking.WHITE_DOTS, stored.marking());
        assertEquals(HorseColor.WHITE, unknown.color());
        assertEquals(HorseMarking.NONE, unknown.marking());
    }

    @ParameterizedTest
    @EnumSource(value = HorseOverviewView.SortOrder.class,
            names = {"SPEED_ASCENDING", "SPEED_DESCENDING", "JUMP_ASCENDING", "JUMP_DESCENDING"})
    void sortsNumericMetricsInBothDirectionsWithUnitsDecimalsAndStableTies(HorseOverviewView.SortOrder order) {
        var records = List.of(
                metricHorse(1, "Zora", "Apfelhof", "9,9 Blöcke/s", "1.8 Blöcke"),
                metricHorse(2, "Boreas", "Apfelhof", "14,2 Blöcke/s", "1,09 Blöcke"),
                metricHorse(3, "Abendrot", "Apfelhof", "14.20 Blöcke/s", "1.09 Blöcke"),
                metricHorse(4, "Fehlend", "Apfelhof", "–", "unbekannt"),
                metricHorse(5, "Weiterer Hof", "Birkenhof", "20", "2"),
                metricHorse(6, "Ohne Hof", "", "30", "3"));
        var groups = HorseOverviewView.groups(records, "", order);
        assertEquals(List.of("Apfelhof", "Birkenhof", ""), groups.stream()
                .map(HorseOverviewView.FarmGroup::farm).toList());
        var expected = switch (order) {
            case SPEED_ASCENDING, JUMP_DESCENDING -> List.of("Zora", "Abendrot", "Boreas", "Fehlend");
            default -> List.of("Abendrot", "Boreas", "Zora", "Fehlend");
        };
        assertEquals(expected, groups.getFirst().horses().stream()
                .map(AnimalBreedingHorse::registryName).toList());
        assertEquals(List.of("Boreas"), HorseOverviewView.groups(records, "bore", order)
                .getFirst().horses().stream().map(AnimalBreedingHorse::registryName).toList());
    }

    @Test
    void treatsBlankInvalidAndNegativeMetricsAsUnknownButKeepsZero() {
        var records = List.of(
                metricHorse(1, "A unbekannt", "Hof", "", ""),
                metricHorse(2, "B unbekannt", "Hof", "NaN", ""),
                metricHorse(3, "C unbekannt", "Hof", "-5", ""),
                metricHorse(4, "Null", "Hof", "0 Blöcke/s", ""),
                metricHorse(5, "Zehn", "Hof", "10 Blöcke/s", ""));
        assertEquals(List.of("Null", "Zehn", "A unbekannt", "B unbekannt", "C unbekannt"),
                HorseOverviewView.groups(records, "", SPEED_ASCENDING).getFirst().horses()
                        .stream().map(AnimalBreedingHorse::registryName).toList());
        assertEquals(List.of("Zehn", "Null", "A unbekannt", "B unbekannt", "C unbekannt"),
                HorseOverviewView.groups(records, "", SPEED_DESCENDING).getFirst().horses()
                        .stream().map(AnimalBreedingHorse::registryName).toList());
    }

    private static AnimalBreedingHorse metricHorse(int id, String name, String farm, String speed, String jump) {
        return new AnimalBreedingHorse(new java.util.UUID(0, id).toString(), name, "", farm,
                "Reitpferd", "Stute", "BROWN", "NONE", "24/30", jump, speed,
                "72/100", "in 4h", 1_000, 2_000, 7, "", "", true, true, "", "");
    }

    @Test
    void appliesSecondaryMetricBeforeNameAndAllowsReorderingPriority() {
        var records = List.of(
                metricHorse(1, "Alpha", "Hof", "14,2 Blöcke/s", "1.1 Blöcke"),
                metricHorse(2, "Zulu", "Hof", "14.20 Blöcke/s", "1,8 Blöcke"),
                metricHorse(3, "Schnell", "Hof", "15", "1.4"),
                metricHorse(4, "Springer", "Hof", "13", "2"));
        var sorting = new HorseOverviewView.SortSelection();
        sorting.select(SPEED_DESCENDING);
        sorting.select(JUMP_DESCENDING);
        assertEquals(List.of("Schnell", "Zulu", "Alpha", "Springer"), sortedNames(records, sorting));
        sorting.promote(JUMP_DESCENDING);
        assertEquals(List.of("Springer", "Zulu", "Schnell", "Alpha"), sortedNames(records, sorting));
        assertEquals(1, sorting.priority(JUMP_DESCENDING));
        assertEquals(2, sorting.priority(SPEED_DESCENDING));
    }

    @Test
    void combinesIndependentDirectionsAndKeepsUnknownMetricsLastAtEachLevel() {
        var records = List.of(
                metricHorse(1, "Alpha", "Hof", "14", "1.1"),
                metricHorse(2, "Zulu", "Hof", "14", "1.8"),
                metricHorse(3, "Sprung unbekannt", "Hof", "14", ""),
                metricHorse(4, "Tempo unbekannt", "Hof", "", "9.9"),
                metricHorse(5, "Langsam", "Hof", "9.9", "0"));
        var sorting = new HorseOverviewView.SortSelection();
        sorting.select(SPEED_DESCENDING);
        sorting.select(JUMP_ASCENDING);
        assertEquals(List.of("Alpha", "Zulu", "Sprung unbekannt", "Langsam", "Tempo unbekannt"),
                sortedNames(records, sorting));
        sorting.select(SPEED_ASCENDING);
        assertEquals(List.of("Langsam", "Alpha", "Zulu", "Sprung unbekannt", "Tempo unbekannt"),
                sortedNames(records, sorting));
        assertEquals(List.of(SPEED_ASCENDING, JUMP_ASCENDING), sorting.criteria());
    }

    @Test
    void addingChangingRemovingAndResettingCriteriaPreservesIndependentFarmOrder() {
        var sorting = new HorseOverviewView.SortSelection();
        sorting.select(FARMS_DESCENDING);
        sorting.select(JUMP_DESCENDING);
        sorting.select(SPEED_DESCENDING);
        sorting.select(JUMP_ASCENDING);
        assertEquals(List.of(JUMP_ASCENDING, SPEED_DESCENDING), sorting.criteria());
        assertEquals(FARMS_DESCENDING, sorting.farmOrder());
        sorting.select(JUMP_ASCENDING);
        assertEquals(List.of(SPEED_DESCENDING), sorting.criteria());
        assertEquals(1, sorting.priority(SPEED_DESCENDING));
        var records = List.of(metricHorse(1, "A", "Apfelhof", "14", "1"),
                metricHorse(2, "B", "Birkenhof", "13", "2"), metricHorse(3, "C", "", "15", "3"));
        assertEquals(List.of("Birkenhof", "Apfelhof", ""), HorseOverviewView.groups(records, "", sorting)
                .stream().map(HorseOverviewView.FarmGroup::farm).toList());
        sorting.reset();
        assertEquals(List.of(), sorting.criteria());
        assertEquals(FARMS_ASCENDING, sorting.farmOrder());
    }

    @Test
    void headerCycleDoesNotReplaceOtherCriteria() {
        var sorting = new HorseOverviewView.SortSelection();
        sorting.cycle(true);
        sorting.cycle(false);
        assertEquals(List.of(SPEED_DESCENDING, JUMP_DESCENDING), sorting.criteria());
        sorting.cycle(true);
        assertEquals(List.of(SPEED_ASCENDING, JUMP_DESCENDING), sorting.criteria());
        sorting.cycle(true);
        assertEquals(List.of(JUMP_DESCENDING), sorting.criteria());
        sorting.cycle(true);
        assertEquals(List.of(JUMP_DESCENDING, SPEED_DESCENDING), sorting.criteria());
    }

    private static List<String> sortedNames(List<AnimalBreedingHorse> records, HorseOverviewView.SortSelection sorting) {
        return HorseOverviewView.groups(records, "", sorting).getFirst().horses()
                .stream().map(AnimalBreedingHorse::registryName).toList();
    }

    private static AnimalBreedingHorse horse(String uuid, String name,
                                              String origin, String farm,
                                              String color, String marking) {
        return new AnimalBreedingHorse(uuid, name, origin, farm,
                "Reitpferd", "Stute", color, marking,
                "24/30", "1,42 Blöcke", "14,2 Blöcke/s",
                "72/100", "in 4h", 1_000, 2_000, 7,
                "", "", true, true, "", "");
    }
}
