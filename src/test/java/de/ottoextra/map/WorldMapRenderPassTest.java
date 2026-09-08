package de.ottoextra.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapRenderPassTest {
    @Test
    void plainWorldMapKeepsLatePaintEveryFrame() {
        WorldMapRenderPass pass = new WorldMapRenderPass();
        Object screen = new Object();
        assertTrue(pass.shouldRenderLate(screen));
        pass.begin(screen);
        assertTrue(pass.shouldRenderLate(screen));
        pass.begin(screen);
        assertTrue(pass.shouldRenderLate(screen));
    }

    @Test
    void xaeroPlusPaintsOnlyOnceAndNeverOverItsFeatures() {
        WorldMapRenderPass pass = new WorldMapRenderPass();
        Object screen = new Object();
        pass.begin(screen);
        assertTrue(pass.claimEarly(screen));
        assertFalse(pass.claimEarly(screen));
        assertFalse(pass.shouldRenderLate(screen));
        // Repeated late element passes must not consume/reset the suppression.
        assertFalse(pass.shouldRenderLate(screen));
    }

    @Test
    void missingEarlyHookOnNextFrameDoesNotLeaveStaleSuppression() {
        WorldMapRenderPass pass = new WorldMapRenderPass();
        Object screen = new Object();
        pass.begin(screen);
        assertTrue(pass.claimEarly(screen));
        pass.begin(screen);
        assertTrue(pass.shouldRenderLate(screen));
        assertTrue(pass.claimEarly(screen));
    }

    @Test
    void differentScreensAndCallsOutsideMapRenderCannotClaimEarlyPass() {
        WorldMapRenderPass pass = new WorldMapRenderPass();
        Object first = new Object();
        Object second = new Object();
        assertFalse(pass.claimEarly(first));
        assertFalse(pass.claimEarly(null));
        pass.begin(first);
        assertFalse(pass.claimEarly(second));
        assertTrue(pass.claimEarly(first));
        assertTrue(pass.shouldRenderLate(second));
        pass.begin(second);
        assertTrue(pass.shouldRenderLate(second));
        assertTrue(pass.claimEarly(second));
    }
}
