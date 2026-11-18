package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnimalBookStatusTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long MINUTE = 60_000L;
    private final UUID uuid = UUID.randomUUID();

    private AnimalBookStatus.View register(String species, String mast, String breeding, long observedAt, Boolean baby) {
        return AnimalBookStatus.of(null, uuid, species, mast, breeding, observedAt, baby, NOW);
    }

    @Test
    void readyAnimalIsGreenWithFullBar() {
        var view = register("Reitpferd", "100/100", "Bereit", NOW - 5 * MINUTE, false);
        assertEquals(AnimalStatus.Kind.READY, view.status().kind());
        assertEquals(AnimalBookStatus.Tone.READY, view.tone());
        assertEquals(1f, view.progress());
        assertEquals("ottoextra.horses.book.status.ready", AnimalBookStatus.statusKey(view.status()));
        assertEquals(0, AnimalBookStatus.rank(view));
        assertEquals(5 * MINUTE, view.ageMs());
    }

    @Test
    void registerWaitingTimeCountsDownFromTheObservation() {
        // Observed two hours ago with three hours left: one hour remains now.
        var view = register("Reitpferd", "100/100", "in 3h | Mast 100/100", NOW - 120 * MINUTE, false);
        assertEquals(AnimalStatus.Kind.WAITING, view.status().kind());
        assertEquals(60 * MINUTE, view.status().remainingMs());
        assertEquals(AnimalBookStatus.Tone.WAITING, view.tone());
        assertEquals("ottoextra.horses.book.status.waiting_timer", AnimalBookStatus.statusKey(view.status()));
        assertArrayEquals(new Object[]{"1h 0m"}, AnimalBookStatus.statusArgs(view.status()));
        // 24 h table wait, 1 h left.
        assertEquals(23f / 24f, view.progress(), 0.001f);
    }

    @Test
    void shortWaitIsSoonAndExpiredWaitIsNoLongerWaiting() {
        var soon = register("Reitpferd", "100/100", "in 20m", NOW - 10 * MINUTE, false);
        assertEquals(AnimalBookStatus.Tone.SOON, soon.tone());
        var over = register("Reitpferd", "100/100", "in 20m", NOW - 30 * MINUTE, false);
        assertNotEquals(AnimalStatus.Kind.WAITING, over.status().kind());
    }

    @Test
    void missingFeedShowsNeedWithThresholdOnAHundredScale() {
        var view = register("Zaupel", "15/30", "Mast 15/30", NOW - MINUTE, false);
        assertEquals(AnimalStatus.Kind.NEED_MAST, view.status().kind());
        assertEquals(AnimalBookStatus.Tone.NEED_MAST, view.tone());
        assertEquals(15, view.mast());
        assertEquals(30, view.mastNeed());
        assertEquals(100, view.mastScale());
        assertFalse(view.mastEnough());
        assertEquals("ottoextra.horses.book.status.need_mast", AnimalBookStatus.statusKey(view.status()));
        assertArrayEquals(new Object[]{15, 30}, AnimalBookStatus.statusArgs(view.status()));
    }

    @Test
    void noDataIsUnknownButKeepsTheTableSpecies() {
        var view = register("Rotvieh Rind", "", "", 0L, null);
        assertEquals(AnimalBookStatus.Tone.UNKNOWN, view.tone());
        assertEquals("ottoextra.horses.book.status.unknown", AnimalBookStatus.statusKey(view.status()));
        assertNotNull(view.species());
        assertEquals(80, view.species().breedMast());
        assertEquals("1d 0h", AnimalBookStatus.tableWait(view.species()));
        assertNull(view.ageMs());
        assertEquals(AnimalBookStatus.AgeKind.UNKNOWN, view.age().kind());
        assertEquals("ottoextra.horses.book.age.unknown", AnimalBookStatus.ageKey(view.age()));
    }

    @Test
    void newerLiveAnswerWinsOverOlderRegisterValues() {
        AnimalLiveState live = AnimalLiveState.empty(uuid)
                .withInfo(new AnimalInfoSnapshot("Reitpferd", "Stute", "100/100", "Bereit", "", ""), NOW - MINUTE);
        var view = AnimalBookStatus.of(live, uuid, "Reitpferd", "10/100", "Mast 10/100", NOW - 60 * MINUTE, false, NOW);
        assertEquals(AnimalStatus.Kind.READY, view.status().kind());
        assertEquals(MINUTE, view.ageMs());
    }

    @Test
    void observedMatingAfterTheRegisterValuesGivesAnEstimatedWait() {
        AnimalLiveState live = AnimalLiveState.empty(uuid).withBred(NOW - 60 * MINUTE);
        var view = AnimalBookStatus.of(live, uuid, "Reitpferd", "100/100", "Bereit", NOW - 120 * MINUTE, false, NOW);
        assertEquals(AnimalStatus.Kind.WAITING, view.status().kind());
        assertTrue(view.status().estimated());
        assertArrayEquals(new Object[]{"~23h 0m"}, AnimalBookStatus.statusArgs(view.status()));
    }

    @Test
    void youngAnimalShowsTimeLeftWhileGrowingThenAdult() {
        AnimalLiveState born = AnimalLiveState.empty(uuid).withBorn(NOW - 60 * MINUTE);
        // Not loaded: a newborn seen an hour ago is still growing (Brakel: 12 h).
        var young = AnimalBookStatus.of(born, uuid, "Brakel", "", "", 0L, null, NOW);
        assertEquals(AnimalBookStatus.AgeKind.YOUNG, young.age().kind());
        assertEquals(11 * 60 * MINUTE, young.age().remainingMs());
        assertEquals("ottoextra.horses.book.age.young_timer", AnimalBookStatus.ageKey(young.age()));
        assertArrayEquals(new Object[]{"~11h 0m"}, AnimalBookStatus.ageArgs(young.age()));
        assertEquals(AnimalBookStatus.Tone.YOUNG, young.tone());

        var grown = AnimalBookStatus.of(AnimalLiveState.empty(uuid).withBorn(NOW - 13 * 60 * MINUTE), uuid,
                "Brakel", "", "", 0L, null, NOW);
        assertEquals(AnimalBookStatus.AgeKind.ADULT, grown.age().kind());
        assertEquals("ottoextra.horses.book.age.adult", AnimalBookStatus.ageKey(grown.age()));

        var loadedAdult = register("Brakel", "", "", 0L, false);
        assertEquals(AnimalBookStatus.AgeKind.ADULT, loadedAdult.age().kind());
        var loadedBaby = register("Brakel", "", "", 0L, true);
        assertEquals(AnimalBookStatus.AgeKind.YOUNG, loadedBaby.age().kind());
        assertEquals("ottoextra.horses.book.age.young", AnimalBookStatus.ageKey(loadedBaby.age()));
    }

    @Test
    void ranksPutReadyFirstAndUnknownLast() {
        var ready = register("Reitpferd", "100/100", "Bereit", NOW, false);
        var soon = register("Reitpferd", "100/100", "in 5m", NOW, false);
        var waiting = register("Reitpferd", "100/100", "in 5h", NOW, false);
        var need = register("Zaupel", "1/30", "Mast 1/30", NOW, false);
        var unknown = register("", "", "", 0L, null);
        assertTrue(AnimalBookStatus.rank(ready) < AnimalBookStatus.rank(soon));
        assertTrue(AnimalBookStatus.rank(soon) < AnimalBookStatus.rank(waiting));
        assertTrue(AnimalBookStatus.rank(waiting) < AnimalBookStatus.rank(need));
        assertTrue(AnimalBookStatus.rank(need) < AnimalBookStatus.rank(unknown));
    }
}
