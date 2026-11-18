package de.ottoextra.regions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BannerSourcePolicyTest {

    @Test
    void changedKnownSourceNeedsImmediateRefresh() {
        assertTrue(BannerSourcePolicy.needsImmediateRefresh("uploads/old.png", "uploads/new.png"));
    }

    @Test
    void sameSourceIgnoringWhitespaceDoesNotRefresh() {
        assertFalse(BannerSourcePolicy.needsImmediateRefresh("uploads/a.png", " uploads/a.png\n"));
    }

    @Test
    void unknownSourceKeepsTimedBehaviour() {
        assertFalse(BannerSourcePolicy.needsImmediateRefresh(null, "uploads/new.png"));
        assertFalse(BannerSourcePolicy.needsImmediateRefresh("  ", "uploads/new.png"));
    }

    @Test
    void blankCurrentPathNeverRefreshes() {
        assertFalse(BannerSourcePolicy.needsImmediateRefresh("uploads/old.png", null));
        assertFalse(BannerSourcePolicy.needsImmediateRefresh("uploads/old.png", " "));
    }

    @Test
    void normalizeTrimsAndMapsNullToEmpty() {
        assertEquals("", BannerSourcePolicy.normalize(null));
        assertEquals("a/b.png", BannerSourcePolicy.normalize("  a/b.png "));
    }
}
