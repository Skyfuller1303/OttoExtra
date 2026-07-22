package de.ottoextra.regions;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionNotificationOverlayTest {

    @Test
    void leaderRpNameDisplayIsEnabledByDefaultAndSurvivesOldSnapshot() {
        OttoExtraConfig config = new OttoExtraConfig();
        assertTrue(config.regions.showLeaderRpName);

        config.restoreFrom("{\"regions\":{\"showLeader\":true}}");

        assertTrue(config.regions.showLeaderRpName);
    }

    @Test
    void explicitDisabledLeaderRpNameSettingIsPreserved() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.restoreFrom("{\"regions\":{\"showLeaderRpName\":false}}");

        assertFalse(config.regions.showLeaderRpName);
    }

    @Test
    void knownVisibleRpNameWins() {
        assertEquals("Mönch Matthias", RegionNotificationOverlay.leaderDisplayName(
                "Skyfuller1303", true,
                account -> Optional.of("Mönch Matthias")));
    }

    @Test
    void missingOrHiddenRpNameFallsBackToAccount() {
        assertEquals("Skyfuller1303", RegionNotificationOverlay.leaderDisplayName(
                " Skyfuller1303 ", true, account -> Optional.empty()));
        assertEquals("Skyfuller1303", RegionNotificationOverlay.leaderDisplayName(
                "Skyfuller1303", true, account -> Optional.of("  ")));
    }

    @Test
    void disabledRpNameSettingReturnsAccountWithoutResolverCall() {
        AtomicBoolean called = new AtomicBoolean();

        String displayed = RegionNotificationOverlay.leaderDisplayName(
                "Skyfuller1303", false, account -> {
                    called.set(true);
                    return Optional.of("Mönch Matthias");
                });

        assertEquals("Skyfuller1303", displayed);
        assertFalse(called.get());
    }

    @Test
    void missingLeaderProducesNoLeaderLine() {
        AtomicBoolean called = new AtomicBoolean();

        String displayed = RegionNotificationOverlay.leaderDisplayName(
                "  ", true, account -> {
                    called.set(true);
                    return Optional.of("Nicht verwenden");
                });

        assertEquals("", displayed);
        assertFalse(called.get());
    }
}
