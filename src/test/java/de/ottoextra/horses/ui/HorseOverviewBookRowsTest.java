package de.ottoextra.horses.ui;

import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.horses.ui.HorseOverviewView.BookSort;
import de.ottoextra.horses.ui.HorseOverviewView.SortSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HorseOverviewBookRowsTest {
    private static final AnimalBreedingHorse BERTA = horse(1, "Berta", "Eichengrund", "Rotvieh Rind", "");
    private static final AnimalBreedingHorse DONAR = horse(2, "Donar", "Eichengrund", "Reitpferd", "12");
    private static final AnimalBreedingHorse WOLLE = horse(3, "Wolle", "Mühlbach", "Zaupel", "");
    private static final AnimalBreedingHorse ANON = horse(4, "", "", "Hausschwein", "");
    private static final AnimalBreedingHorse KASTANIE = horse(5, "Kastanie", "Eichengrund", "Reitpferd", "14");
    private static final List<AnimalBreedingHorse> ALL = List.of(BERTA, DONAR, WOLLE, ANON, KASTANIE);
    /** Status ranks as the screen would compute them: Wolle ready, Donar waiting, rest unknown. */
    private static final Map<String, Integer> RANK = Map.of("Wolle", 0, "Donar", 2);

    private static List<String> names(List<AnimalBreedingHorse> rows) {
        return rows.stream().map(HorseOverviewView::displayName).toList();
    }

    private static List<AnimalBreedingHorse> rows(String query, String farm, BookSort sort, SortSelection selection) {
        return HorseOverviewView.bookRows(ALL, query, farm, sort, selection,
                horse -> RANK.getOrDefault(horse.registryName(), 6), horse -> 0L);
    }

    @Test
    void farmOrderGroupsFarmsAndPutsAnimalsWithoutFarmLast() {
        assertEquals(List.of("Berta", "Donar", "Kastanie", "Wolle", "???"),
                names(rows("", null, BookSort.FARM, new SortSelection())));
        SortSelection descending = new SortSelection();
        descending.select(HorseOverviewView.SortOrder.FARMS_DESCENDING);
        assertEquals(List.of("Wolle", "Berta", "Donar", "Kastanie", "???"),
                names(rows("", null, BookSort.FARM, descending)));
    }

    @Test
    void nameOrderIgnoresFarmsAndPutsUnnamedLast() {
        assertEquals(List.of("Berta", "Donar", "Kastanie", "Wolle", "???"),
                names(rows("", null, BookSort.NAME, new SortSelection())));
    }

    @Test
    void stateOrderUsesTheRankThenTheName() {
        assertEquals(List.of("Wolle", "Donar", "Berta", "Kastanie", "???"),
                names(rows("", null, BookSort.STATE, new SortSelection())));
    }

    @Test
    void farmFilterAndSearchCombine() {
        assertEquals(List.of("Berta", "Donar", "Kastanie"),
                names(rows("", "eichengrund", BookSort.NAME, new SortSelection())));
        assertEquals(List.of("???"), names(rows("", "", BookSort.NAME, new SortSelection())));
        assertEquals(List.of("Donar", "Kastanie"), names(rows("reitpferd", "Eichengrund", BookSort.STATE,
                new SortSelection())));
        assertEquals(List.of("Wolle"), names(rows("mühl", null, BookSort.FARM, new SortSelection())));
        assertEquals(List.of("???"), names(rows("0000000004", null, BookSort.NAME, new SortSelection())));
    }

    @Test
    void speedCriterionComesBeforeTheChosenOrder() {
        SortSelection speed = new SortSelection();
        speed.cycle(true);
        assertEquals(List.of("Kastanie", "Donar", "Berta", "Wolle", "???"),
                names(rows("", null, BookSort.NAME, speed)));
    }

    private static AnimalBreedingHorse horse(int id, String name, String farm, String type, String speed) {
        return new AnimalBreedingHorse(String.format("00000000-0000-0000-0000-%012d", id), name, "", farm, type,
                "", "", "", speed, "", "", 0L, 1L, 0L, "", "");
    }
}
