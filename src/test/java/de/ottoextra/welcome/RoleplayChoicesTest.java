package de.ottoextra.welcome;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.nametags.NameTagMode;
import org.junit.jupiter.api.Test;

import static de.ottoextra.welcome.RoleplayChoices.Option.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleplayChoicesTest {
    @Test
    void uneditedChoicesMatchEveryPresetExactly() {
        for (RoleplayPreset preset : RoleplayPreset.values()) {
            OttoExtraConfig expected = new OttoExtraConfig();
            OttoExtraConfig actual = new OttoExtraConfig();
            preset.apply(expected);
            new RoleplayChoices(preset).apply(actual);
            assertEquals(expected.snapshotJson(), actual.snapshotJson(), preset.name());
        }
    }

    @Test
    void draftsAreIndependentUntilExplicitlyApplied() {
        OttoExtraConfig config = new OttoExtraConfig();
        String before = config.snapshotJson();
        RoleplayChoices first = new RoleplayChoices(RoleplayPreset.BALANCED);
        RoleplayChoices other = new RoleplayChoices(RoleplayPreset.BALANCED);
        first.toggle(RP_CHAT);
        first.toggle(TAB_RP_NAMES);
        first.toggle(TAB_TITLES);
        assertTrue(other.isOn(RP_CHAT));
        assertTrue(other.isOn(TAB_RP_NAMES));
        assertEquals(before, config.snapshotJson());

        first.apply(config);
        assertFalse(config.rpnames.showInSprechen);
        assertFalse(config.rpnames.showInReden);
        assertFalse(config.rpnames.showInRufen);
        assertFalse(config.rpnames.showInBruellen);
        assertFalse(config.rpnames.showInFluestern);
        assertFalse(config.rpnames.showInMurmeln);
        assertFalse(config.rpnames.showInAllChannels);
        assertFalse(config.rpnames.showInOoc);
        assertFalse(config.rpnames.showInOfftopic);
        assertFalse(config.rpnames.showInHilfe);
        assertFalse(config.rpnames.tablistEnabled);
        assertFalse(config.rpnames.tablistShowTitle);
    }

    @Test
    void hiddenNametagsKeepTheirDetailsAndCannotAccidentallyBeReenabled() {
        RoleplayChoices choices = new RoleplayChoices(RoleplayPreset.BALANCED);
        choices.toggle(TAG_ACCOUNTS);
        choices.toggle(NAMETAGS);
        assertFalse(choices.isAvailable(LINE_OF_SIGHT));
        choices.toggle(LINE_OF_SIGHT);
        choices.toggle(TAG_ACCOUNTS);
        assertTrue(choices.isOn(LINE_OF_SIGHT));
        assertTrue(choices.isOn(TAG_ACCOUNTS));
        OttoExtraConfig config = new OttoExtraConfig();
        choices.apply(config);
        assertEquals(NameTagMode.HIDE_ALL, config.nametags.mode);
        assertTrue(config.nametags.showPlayerName);

        choices.toggle(NAMETAGS);
        choices.apply(config);
        assertEquals(NameTagMode.REALISTIC, config.nametags.mode);
        choices.toggle(LINE_OF_SIGHT);
        choices.toggle(TAG_RP_NAMES);
        choices.toggle(TAG_TITLES);
        choices.apply(config);
        assertEquals(NameTagMode.NORMAL, config.nametags.mode);
        assertFalse(config.nametags.showRpName);
        assertFalse(config.nametags.showTitle);
        assertTrue(config.nametags.showPlayerName);
    }

    @Test
    void introductionsAndAccountFallbackApplyTogether() {
        RoleplayChoices choices = new RoleplayChoices(RoleplayPreset.IMMERSIVE);
        choices.toggle(MEET_FIRST);
        choices.toggle(UNKNOWN_ACCOUNT);
        choices.toggle(INSPECT_ACCOUNT);
        OttoExtraConfig config = new OttoExtraConfig();
        choices.apply(config);
        assertFalse(config.rpnames.proactiveMeet);
        assertFalse(config.rpnames.showUnknownAsUnknown);
        assertTrue(config.rpnames.unknownShowAccount);
        assertTrue(config.rpnames.inspectShowAccount);
        choices.toggle(UNKNOWN_ACCOUNT);
        choices.apply(config);
        assertTrue(config.rpnames.showUnknownAsUnknown);
        assertFalse(config.rpnames.unknownShowAccount);
    }

    @Test
    void customChoicesPreserveUnrelatedOptionsAndSharedReferencesAcrossRestore() {
        OttoExtraConfig config = new OttoExtraConfig();
        var names = config.rpnames;
        var tags = config.nametags;
        de.ottoextra.privacy.ConsentPolicy.decide(config, de.ottoextra.privacy.ConsentPolicy.Purpose.EXTERNAL_IMAGES,
                true, de.ottoextra.privacy.ConsentPolicy.scope(config, de.ottoextra.privacy.ConsentPolicy.Purpose.EXTERNAL_IMAGES));
        config.tweaks.chatLog = false;
        config.rpnames.syncFromPublicApi = false;
        config.rpnames.unknownPlaceholder = "Fremder";
        config.rpnames.globalRpNameColor = "#123456";
        config.nametags.titleScale = 1.8f;
        var choices = new RoleplayChoices(RoleplayPreset.COMFORT);
        choices.toggle(TAB_RP_NAMES);
        choices.toggle(TAG_TITLES);
        choices.apply(config);
        config.restoreFrom(config.snapshotJson());
        assertSame(names, config.rpnames);
        assertSame(tags, config.nametags);
        assertTrue(config.rpnames.tablistEnabled);
        assertTrue(config.nametags.showTitle);
        assertEquals("Fremder", config.rpnames.unknownPlaceholder);
        assertEquals("#123456", config.rpnames.globalRpNameColor);
        assertFalse(config.rpnames.syncFromPublicApi);
        assertTrue(config.consent.externalImageDownloads);
        assertFalse(config.consent.communityRpNameUploads);
        assertFalse(config.tweaks.chatLog);
        assertEquals(1.8f, config.nametags.titleScale);
    }
}
