package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSnapshotRestoreTest {
    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path directory;

    @Test
    void restoreChatLoggingInBothDirectionsAndKeepItWhenSavedAgain() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            OttoExtraConfig config = new OttoExtraConfig();
            config.repair();
            var tweaks = config.tweaks;
            config.tweaks.chatLog = enabled;
            String snapshot = config.snapshotJson();
            config.tweaks.chatLog = !enabled;

            config.restoreFrom(snapshot);

            assertSame(tweaks, config.tweaks);
            assertEquals(enabled, config.tweaks.chatLog);
            assertEquals(snapshot, config.snapshotJson());
            java.nio.file.Path file = directory.resolve("config-" + enabled + ".json");
            config.save(file);
            assertEquals(enabled, OttoExtraConfig.load(file).tweaks.chatLog);
        }
    }

    @Test
    void restoreFromCopiesAllConsentValuesWithoutReplacingSharedReference() {
        OttoExtraConfig config = new OttoExtraConfig();
        var consent = config.consent;
        OttoExtraConfig restored = new OttoExtraConfig();
        restored.consent.onboardingVersion = 2;
        restored.consent.automaticResourcePackDownloads = true;
        restored.consent.externalImageDownloads = true;
        restored.consent.updateChecks = true;
        restored.consent.communityRpNameUploads = true;

        config.restoreFrom(restored.snapshotJson());

        assertSame(consent, config.consent);
        assertEquals(2, consent.onboardingVersion);
        assertTrue(consent.automaticResourcePackDownloads);
        assertTrue(consent.externalImageDownloads);
        assertTrue(consent.updateChecks);
        assertTrue(consent.communityRpNameUploads);

        config.restoreFrom("{}");

        assertSame(consent, config.consent);
        assertEquals(0, consent.onboardingVersion);
        assertFalse(consent.automaticResourcePackDownloads);
        assertFalse(consent.externalImageDownloads);
        assertFalse(consent.updateChecks);
        assertFalse(consent.communityRpNameUploads);
    }
}
