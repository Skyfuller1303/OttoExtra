package de.ottoextra.letter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LetterWriteItemGuardTest {

    @Test
    void rejectsMatchingSupplyElsewhereBecauseCourierRemovesInventoryInSlotOrder() {
        assertEquals(LetterWriteItemGuard.Result.REMOVE_EXTRA_SUPPLIES,
                LetterWriteItemGuard.decide(false, false, 1, 2));
        assertEquals(LetterWriteItemGuard.Result.ALLOW,
                LetterWriteItemGuard.decide(false, false, 1, 1));
        assertEquals(LetterWriteItemGuard.Result.ALLOW,
                LetterWriteItemGuard.decide(true, true, 0, 4));
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
