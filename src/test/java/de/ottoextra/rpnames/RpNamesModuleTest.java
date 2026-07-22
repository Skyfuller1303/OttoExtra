package de.ottoextra.rpnames;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpNamesModuleTest {

    @Test
    void combatBossBarMatchesCaseInsensitively() {
        assertTrue(RpNamesModule.isCombatBossBarName("IM KAMPF"));
        assertTrue(RpNamesModule.isCombatBossBarName("im kampf"));
        assertTrue(RpNamesModule.isCombatBossBarName("Im KaMpF"));
    }

    @Test
    void combatBossBarMatchesInsideVisibleText() {
        assertTrue(RpNamesModule.isCombatBossBarName("Status: IM KAMPF!"));
    }

    @Test
    void localMeetPrefillAvoidsUnnecessaryApiLookup() {
        de.ottoextra.rpnames.model.LocalRpProfile profile =
                new de.ottoextra.rpnames.model.LocalRpProfile();
        assertFalse(RpNamesModule.hasLocalMeetPrefill(profile, null));

        profile.rpName = "Krämerin Ada";
        assertTrue(RpNamesModule.hasLocalMeetPrefill(profile, null));

        profile.rpName = "Unbekannt";
        assertTrue(RpNamesModule.hasLocalMeetPrefill(profile,
                new RpNamesServices.MeetSuggestion("Botin Ulrike", "")));
        assertFalse(RpNamesModule.hasLocalMeetPrefill(profile,
                new RpNamesServices.MeetSuggestion("  ", "Bote")));
    }

    @Test
    void unrelatedOrIncompleteBossBarsDoNotMatch() {
        assertFalse(RpNamesModule.isCombatBossBarName(null));
        assertFalse(RpNamesModule.isCombatBossBarName(""));
        assertFalse(RpNamesModule.isCombatBossBarName("Kampf beginnt"));
        assertFalse(RpNamesModule.isCombatBossBarName("IM  KAMPF"));
        assertFalse(RpNamesModule.isCombatBossBarName("IM KÄMPFERLAGER"));
    }
}
