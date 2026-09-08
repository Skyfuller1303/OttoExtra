package de.ottoextra.rpnames;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpNamesBackupRestoreTest {

    @Test
    void throwingReloadReturnsFailureAfterConfigChangeAndStillRunsOtherReloads() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.chat.enabled = true;
        String beforeRestore = config.snapshotJson();
        AtomicBoolean remainingReloadRan = new AtomicBoolean();

        boolean restored = RpNamesServices.restoreAndReload(() -> {
            config.restoreFrom("{\"chat\":{\"enabled\":false}}");
            return true;
        }, () -> {
            throw new IllegalStateException("defekter Titelkatalog");
        }, () -> remainingReloadRan.set(true));

        assertFalse(restored);
        assertTrue(remainingReloadRan.get());
        assertFalse(config.chat.enabled);
        assertNotEquals(beforeRestore, config.snapshotJson());
    }

    @Test
    void throwingFileRestoreStillRunsEveryReloadAndReturnsFailure() {
        AtomicBoolean titlesReloaded = new AtomicBoolean();
        AtomicBoolean catalogReloaded = new AtomicBoolean();

        assertFalse(RpNamesServices.restoreAndReload(() -> {
            throw new IllegalStateException("teilweise kopiert");
        }, () -> titlesReloaded.set(true), () -> catalogReloaded.set(true)));

        assertTrue(titlesReloaded.get());
        assertTrue(catalogReloaded.get());
    }
}
