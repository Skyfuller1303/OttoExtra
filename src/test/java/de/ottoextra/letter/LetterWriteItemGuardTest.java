package de.ottoextra.letter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LetterWriteItemGuardTest {

    @Test
    void screenshotSuppliesSelectFirstParchmentInsteadOfBlockingWrite() {
        // Slot 0 holds 35 leads, slots 1 and 2 each hold one identical parchment.
        int[] counts = {35, 1, 1, 0, 0, 0, 0, 1, 1};
        assertEquals(1, LetterWriteItemGuard.firstSingleSupplySlot(2, 1,
                slot -> slot == 1 || slot == 2, slot -> counts[slot]));
    }

    @Test
    void firstSingleSupplySelectionIgnoresOtherItemsAndLaterSupplies() {
        assertEquals(3, LetterWriteItemGuard.firstSingleSupplySlot(3, 1,
                slot -> slot == 3 || slot == 7, slot -> 1));
        assertEquals(0, LetterWriteItemGuard.firstSingleSupplySlot(8, 1,
                slot -> slot == 0 || slot == 4 || slot == 8, slot -> 1));
    }

    @Test
    void earlierStackMissingSupplyAndInvalidSelectionNeverAutoSelect() {
        assertEquals(-1, LetterWriteItemGuard.firstSingleSupplySlot(2, 1,
                slot -> slot == 1 || slot == 2, slot -> slot == 1 ? 8 : 1));
        assertEquals(-1, LetterWriteItemGuard.firstSingleSupplySlot(2, 1,
                slot -> false, slot -> 1));
        assertEquals(-1, LetterWriteItemGuard.firstSingleSupplySlot(2, 0,
                slot -> true, slot -> 1));
        assertEquals(-1, LetterWriteItemGuard.firstSingleSupplySlot(2, 8,
                slot -> true, slot -> 1));
        assertEquals(-1, LetterWriteItemGuard.firstSingleSupplySlot(-1, 1,
                slot -> true, slot -> 1));
        assertEquals(-1, LetterWriteItemGuard.firstSingleSupplySlot(9, 1,
                slot -> true, slot -> 1));
    }

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
