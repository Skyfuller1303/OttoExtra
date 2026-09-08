package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSnapshotRestoreTest {

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
