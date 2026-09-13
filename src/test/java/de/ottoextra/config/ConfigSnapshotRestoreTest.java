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
    void restorePreservesCurrentChatLoggingChoiceAndKeepsItWhenSavedAgain() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            OttoExtraConfig config = new OttoExtraConfig();
            config.repair();
            var tweaks = config.tweaks;
            de.ottoextra.privacy.LocalPrivacy.decideChat(config, enabled, "OTTONIEN");
            String snapshot = config.snapshotJson();
            de.ottoextra.privacy.LocalPrivacy.decideChat(config, !enabled, "OTTONIEN");

            config.restoreFrom(snapshot);

            assertSame(tweaks, config.tweaks);
            assertEquals(!enabled, config.tweaks.chatLog);
            java.nio.file.Path file = directory.resolve("config-" + enabled + ".json");
            config.save(file);
            assertEquals(!enabled, OttoExtraConfig.load(file).tweaks.chatLog);
        }
    }

    @Test
    void restoreNeverImportsHistoricalConsentAndPreservesCurrentDecisions() throws Exception {
        OttoExtraConfig config = new OttoExtraConfig();
        var consent = config.consent;
        OttoExtraConfig restored = new OttoExtraConfig();
        for (var purpose : de.ottoextra.privacy.ConsentPolicy.Purpose.values()) {
            de.ottoextra.privacy.ConsentPolicy.decide(restored, purpose, true,
                    de.ottoextra.privacy.ConsentPolicy.scope(restored, purpose));
        }
        restored.consent.onboardingVersion = 3;
        config.restoreFrom(restored.snapshotJson());
        assertSame(consent, config.consent);
        assertEquals(0, consent.onboardingVersion);
        for (var purpose : de.ottoextra.privacy.ConsentPolicy.Purpose.values()) {
            assertFalse(de.ottoextra.privacy.ConsentPolicy.allowed(config, purpose));
        }
        var images = de.ottoextra.privacy.ConsentPolicy.Purpose.EXTERNAL_IMAGES;
        de.ottoextra.privacy.ConsentPolicy.decide(config, images, true,
                de.ottoextra.privacy.ConsentPolicy.scope(config, images));
        config.restoreFrom("{}");
        assertTrue(de.ottoextra.privacy.ConsentPolicy.allowed(config, images));
        var file = directory.resolve("consent.json");
        config.save(file);
        var loaded = OttoExtraConfig.load(file);
        assertTrue(de.ottoextra.privacy.ConsentPolicy.allowed(loaded, images));
        assertFalse(de.ottoextra.privacy.ConsentPolicy.allowed(loaded,
                de.ottoextra.privacy.ConsentPolicy.Purpose.ONLINE_SERVICES));
    }
}
