package de.ottoextra.rpnames.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalRpProfileTest {

    @Test
    void migratesKnownLegacyTitleGroupState() {
        LocalRpProfile profile = new LocalRpProfile();
        profile.apiState = null;
        profile.titleGroup = "  AdLiGe  ";

        profile.repair();

        assertEquals("AdLiGe", profile.apiState);
        assertEquals("  AdLiGe  ", profile.titleGroup);
    }

    @Test
    void doesNotMigrateOrdinaryLegacyTitleGroup() {
        LocalRpProfile profile = new LocalRpProfile();
        profile.apiState = null;
        profile.titleGroup = "Handwerk";

        profile.repair();

        assertEquals("", profile.apiState);
    }
}
