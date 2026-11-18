package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Field values taken from real Ottonien answers in the client logs. */
class RealServerFormatsTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final UUID ID = UUID.randomUUID();

    private static AnimalStatus.Status status(String name, String mast, String breeding, String shear) {
        AnimalLiveState state = AnimalLiveState.empty(ID).withInfo(
                new AnimalInfoSnapshot(name, "Stute (Weiblich)", mast, breeding, shear, ""), NOW);
        return AnimalStatus.compute(state, AnimalSpecies.byName(name), false, NOW);
    }

    @Test
    void zuchtbereitMast0Of100IsMissingFeedNotReady() {
        AnimalStatus.Status s = status("Reitpferd", "0/100", "Mast 0/100", "");
        assertEquals(AnimalStatus.Kind.NEED_MAST, s.kind());
        assertEquals(0, s.mast());
        assertEquals(100, s.mastNeeded());
    }

    @Test
    void feedProgressReachingTheMaximumIsReady() {
        assertEquals(AnimalStatus.Kind.READY, status("Zaupel", "30/30", "Mast 30/30", "").kind());
        assertEquals(AnimalStatus.Kind.NEED_MAST, status("Zaupel", "29/30", "Mast 29/30", "").kind());
    }

    @Test
    void yesNoAndReadyWords() {
        assertEquals(AnimalStatus.Kind.READY, status("Zaupel", "", "Bereit", "").kind());
        assertEquals(AnimalStatus.Kind.READY, status("Zaupel", "", "ja", "").kind());
        assertEquals(AnimalStatus.Kind.UNKNOWN, status("Wolf", "", "UNDEFINED", "").kind());
        assertEquals(AnimalStatus.Kind.UNKNOWN, status("Wolf", "", "nein", "").kind());
    }

    @Test
    void durationWithFeedProgressWaits() {
        AnimalStatus.Status s = status("Reitpferd", "8/100", "in 2h 53m | Mast 8/100", "");
        assertEquals(AnimalStatus.Kind.WAITING, s.kind());
        assertEquals((2 * 60 + 53) * 60_000L, s.remainingMs());
        assertEquals(AnimalStatus.Kind.WAITING,
                status("Zaupel", "15/30", "in 11h 59m | Mast 15/30", "").kind());
    }

    @Test
    void shearAndMilkFeedProgressIsNotReadyAndNeverSetsATimer() {
        for (String shear : new String[]{"Mast 0/20", "Mast 15/20", "UNDEFINED", "nein"}) {
            AnimalLiveState state = AnimalLiveState.empty(ID).withInfo(
                    new AnimalInfoSnapshot("Zaupel", "Weiblich", "0/20", "Mast 0/20", shear, ""), NOW);
            assertEquals(0, AnimalStatus.shearWaitUntil(state), shear);
            assertFalse(AnimalInfoOverlay.isPositive(shear), shear);
        }
        assertTrue(AnimalInfoOverlay.isPositive("Bereit"));
        assertTrue(AnimalInfoOverlay.isPositive("ja"));
        assertFalse(AnimalInfoOverlay.isPositive("Nur weiblich"));
    }
}
