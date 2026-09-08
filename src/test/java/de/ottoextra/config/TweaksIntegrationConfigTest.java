package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TweaksIntegrationConfigTest {

    @Test
    void combatModeHudDefaultsOnAndRestoresItsToggle() {
        OttoExtraConfig config = new OttoExtraConfig();
        assertTrue(config.tweaks.combatModeHud);
        OttoExtraConfig disabled = new OttoExtraConfig();
        disabled.tweaks.combatModeHud = false;
        config.restoreFrom(disabled.snapshotJson());
        assertFalse(config.tweaks.combatModeHud);
        config.restoreFrom("{}");
        assertTrue(config.tweaks.combatModeHud);
    }

    @Test
    void voteAndLitematicaIntegrationsHaveSafeDefaultsAfterRepair() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.tweaks.vote = null;
        config.tweaks.litematica = null;

        config.repair();

        assertNotNull(config.tweaks.vote);
        assertTrue(config.tweaks.vote.enabled);
        assertNotNull(config.tweaks.litematica);
        assertTrue(config.tweaks.litematica.localizedBlockNames);
        assertFalse(config.tweaks.litematica.fachwerkAutoTune);
    }
}
