package de.ottoextra.letter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LetterWriteItemGuardTest {

    @Test
    void onlyEarlierSupplyCanDivertCourierFromTheHeldSingleItem() {
        assertEquals(LetterWriteItemGuard.Result.HOLD_FIRST_SUPPLY,
                LetterWriteItemGuard.decide(false, false, 1, true));
        assertEquals(LetterWriteItemGuard.Result.ALLOW,
                LetterWriteItemGuard.decide(false, false, 1, false));
        assertEquals(LetterWriteItemGuard.Result.ALLOW,
                LetterWriteItemGuard.decide(true, true, 0, true));
    }

    @Test
    void missingOrStackedHeldMaterialKeepsItsOwnDiagnostic() {
        assertEquals(LetterWriteItemGuard.Result.HOLD_SINGLE_SUPPLY,
                LetterWriteItemGuard.decide(false, false, 0, true));
        assertEquals(LetterWriteItemGuard.Result.REMOVE_EXTRA_SUPPLIES,
                LetterWriteItemGuard.decide(false, false, 2, true));
    }

    @Test
    void freshLetterRequiresExactlyOneSupplyInTheActiveSlot() {
        assertEquals(LetterWriteItemGuard.Result.HOLD_SINGLE_SUPPLY,
                LetterWriteItemGuard.decide(false, false, 0));
        assertEquals(LetterWriteItemGuard.Result.ALLOW,
                LetterWriteItemGuard.decide(false, false, 1));
        assertEquals(LetterWriteItemGuard.Result.REMOVE_EXTRA_SUPPLIES,
                LetterWriteItemGuard.decide(false, false, 2));
    }

    @Test
    void continuationRequiresTheMatchingStartedLetterInMainHand() {
        assertEquals(LetterWriteItemGuard.Result.ALLOW,
                LetterWriteItemGuard.decide(true, true, 0));
        assertEquals(LetterWriteItemGuard.Result.HOLD_STARTED_LETTER,
                LetterWriteItemGuard.decide(true, false, 1));
        assertEquals(LetterWriteItemGuard.Result.ALLOW,
                LetterWriteItemGuard.decide(true, true, 2));
    }
}
