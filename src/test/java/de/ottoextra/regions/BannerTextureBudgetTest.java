package de.ottoextra.regions;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BannerTextureBudgetTest {
    @Test void allCurrentSmallBannersFitBeyondTheOldThirtyTwoTextureCutoff() {
        var budget = new BannerTextureBudget();
        for (int i = 0; i < 200; i++) assertTrue(budget.retain("banner-" + i, 16 * 16 * 4).isEmpty());
        assertTrue(budget.retain("large", 220 * 220 * 4).isEmpty());
    }

    @Test void countLimitEvictsLeastRecentlyUsedInsteadOfBlockingNewImages() {
        var budget = new BannerTextureBudget(3, 100);
        budget.retain("a", 1); budget.retain("b", 1); budget.retain("c", 1);
        budget.touch("a");
        assertEquals(List.of("b"), budget.retain("d", 1));
        // An evicted banner may be requested again from disk; the cache never locks up.
        assertEquals(List.of("c"), budget.retain("b", 1));
    }

    @Test void decodedPixelBudgetCanEvictSeveralTexturesEvenBelowCountLimit() {
        var budget = new BannerTextureBudget(20, 12);
        budget.retain("a", 4); budget.retain("b", 4); budget.retain("c", 4);
        budget.touch("a");
        assertEquals(List.of("b", "c"), budget.retain("d", 8));
    }

    @Test void refreshReplacesItsWeightWithoutDoubleCountingOrEvictingTheFreshTexture() {
        var budget = new BannerTextureBudget(20, 10);
        budget.retain("a", 4); budget.retain("b", 4);
        assertTrue(budget.retain("a", 6).isEmpty());
        assertEquals(List.of("b"), budget.retain("a", 8));
        assertTrue(budget.retain("b", 2).isEmpty());
    }

    @Test void bundledTexturesUseNoOwnedPixelBudgetButStillCountTowardsEntryLimit() {
        var budget = new BannerTextureBudget(2, 10);
        budget.retain("bundled-a", 0); budget.retain("bundled-b", 0);
        assertEquals(List.of("bundled-a"), budget.retain("dynamic", 10));
    }

    @Test void rejectedOversizedImageDoesNotDisplaceExistingTextures() {
        var budget = new BannerTextureBudget(2, 10);
        budget.retain("a", 4); budget.retain("b", 4);
        assertThrows(IllegalArgumentException.class, () -> budget.retain("a", 11));
        assertEquals(List.of("a"), budget.retain("c", 4));
    }
}
