package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalPrivacyTest {
    @Test void defaultAndHistoricalTrueAreNotAChoice() {
        var config = new OttoExtraConfig();
        assertFalse(LocalPrivacy.chatAllowed(config, true));
        config.tweaks.chatLog = true;
        LocalPrivacy.reconcile(config);
        assertFalse(config.tweaks.chatLog);
    }
    @Test void scopeChangesRequireAnotherDecisionAndWithdrawalSurvivesRestore() {
        var config = new OttoExtraConfig();
        LocalPrivacy.decideChat(config, true, "OTTONIEN");
        assertTrue(LocalPrivacy.chatAllowed(config, true));
        assertFalse(LocalPrivacy.chatAllowed(config, false));
        config.tweaks.chatLogScope = "ALL";
        LocalPrivacy.reconcile(config);
        assertFalse(LocalPrivacy.chatAllowed(config, true));
        LocalPrivacy.decideChat(config, true, "ALL");
        assertTrue(LocalPrivacy.chatAllowed(config, false));
        String old = config.snapshotJson();
        LocalPrivacy.decideChat(config, false, "ALL");
        config.restoreFrom(old);
        assertFalse(LocalPrivacy.chatAllowed(config, true));
    }
    @Test void oldPackPreferenceCannotOverrideManualDisable() {
        var config = new OttoExtraConfig();
        config.resourcepack.respectUserDisable = false;
        LocalPrivacy.reconcile(config);
        assertTrue(config.resourcepack.respectUserDisable);
    }
    @Test void automaticGenderOutlinesStayOffWithoutOperatorApprovedControl() {
        assertFalse(de.ottoextra.horses.AnimalOutlinePolicy.serverAuthorized());
    }
}
