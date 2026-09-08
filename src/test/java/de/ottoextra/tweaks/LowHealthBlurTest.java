package de.ottoextra.tweaks;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.tweaks.lowhealth.LowHealthState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LowHealthBlurTest {
    @Test
    void mainSwitchDisablesBlurImmediatelyAtLowHealth() {
        var config = new OttoExtraConfig.Tweaks.LowHealth();
        var state = new LowHealthState();
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 4));
        config.enabled = true;
        assertTrue(TweaksModule.lowHealthBlurPasses(state, config, 4) > 0);
        config.enabled = false;
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 4));
    }

    @Test
    void explicitPreviewCanBypassMainSwitchUntilStopped() {
        var config = new OttoExtraConfig.Tweaks.LowHealth();
        var state = new LowHealthState();
        state.setForced(1);
        for (int i = 0; i < 30; i++) state.update(null, config);
        assertTrue(TweaksModule.lowHealthBlurPasses(state, config, 20) > 0);
        config.blurEnabled = false;
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 20));
        config.blurEnabled = true;
        state.clearForced();
        assertEquals(0, TweaksModule.lowHealthBlurPasses(state, config, 4));
    }
}
