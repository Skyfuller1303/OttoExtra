package de.ottoextra.rpnames;

import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpNamesServicesTest {

    @Test
    void leaderToastAcceptsLocallyStoredApiImportedRpName() {
        LocalRpProfile profile = new LocalRpProfile();
        profile.rpName = "  Mönch Matthias  ";
        profile.knowledgeState = KnowledgeState.API_IMPORTED;
        profile.source = RpNameSource.API_IMPORTED;

        assertEquals("Mönch Matthias",
                RpNamesServices.localRpNameForLeader(profile).orElseThrow());
    }

    @Test
    void leaderToastRejectsMissingOrUnknownRpName() {
        assertTrue(RpNamesServices.localRpNameForLeader(null).isEmpty());

        LocalRpProfile profile = new LocalRpProfile();
        profile.rpName = "Unbekannt";
        assertTrue(RpNamesServices.localRpNameForLeader(profile).isEmpty());

        profile.rpName = "  ";
        assertTrue(RpNamesServices.localRpNameForLeader(profile).isEmpty());
    }
}
