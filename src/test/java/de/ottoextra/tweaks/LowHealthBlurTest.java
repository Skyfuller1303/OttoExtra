package de.ottoextra.tweaks;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.tweaks.lowhealth.LowHealthState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LowHealthBlurTest {
    @Test
    void mainSwitchDisablesBlurImmediatelyAtLowHealth() {
        var config = new OttoExtraConfig.Tweaks.LowHealth();
        config.reducedMotion = false;
        var state = new LowHealthState();
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 4));
        config.enabled = true;
        assertTrue(TweaksModule.lowHealthBlurPasses(state, config, 4) > 0);
        config.enabled = false;
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 4));
    }

    @Test
    void explicitPreviewCannotBypassMainSwitchAndReducedMotion() {
        var config = new OttoExtraConfig.Tweaks.LowHealth();
        config.reducedMotion = false;
        var state = new LowHealthState();
        config.enabled = true;
        state.setForced(1);
        for (int i = 0; i < 30; i++) state.update(null, config);
        assertTrue(TweaksModule.lowHealthBlurPasses(state, config, 20) > 0);
        config.reducedMotion = true;
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 20));
        config.reducedMotion = false;
        config.blurEnabled = false;
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 20));
        config.blurEnabled = true;
        config.enabled = false;
        state.update(null, config);
        assertEquals(0, state.intensity());
        assertFalse(state.isForced());
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 4));
    }
}
