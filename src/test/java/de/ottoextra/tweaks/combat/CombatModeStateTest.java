package de.ottoextra.tweaks.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatModeStateTest {
    @Test
    void remembersModesSeparatelyAndShowsNoneForAnEmptyHand() {
        CombatModeState state = new CombatModeState();
        assertTrue(state.mode(WeaponFamily.BLUNT).isEmpty());
        state.observe(WeaponFamily.BLADE, "Flinker Schnitt");
        state.observe(WeaponFamily.BLUNT, "Glockenschlag");
        state.observe(WeaponFamily.RANGED, "Ins Schwarze");

        assertEquals(CombatMode.FLINKER_SCHNITT, state.mode(WeaponFamily.BLADE).orElseThrow());
        assertEquals(CombatMode.GLOCKENSCHLAG, state.mode(WeaponFamily.BLUNT).orElseThrow());
        assertEquals(CombatMode.INS_SCHWARZE, state.mode(WeaponFamily.RANGED).orElseThrow());
        assertTrue(state.mode(null).isEmpty());
        assertTrue(state.mode(WeaponFamily.POLEARM).isEmpty());
    }

    @Test
    void ignoresUnrelatedActionbarsAndMessagesForADifferentWeapon() {
        CombatModeState state = new CombatModeState();
        state.observe(WeaponFamily.BLUNT, "Wuchthau");
        state.observe(WeaponFamily.BLUNT, "Region: Nordmark");
        state.observe(WeaponFamily.BLUNT, "Aderlass");
        state.observe(null, "Dreschen");
        state.observe(WeaponFamily.BLUNT, "Glockenschlag ist nicht erlernt.");
        assertEquals(CombatMode.WUCHTHAU, state.mode(WeaponFamily.BLUNT).orElseThrow());
        assertTrue(state.mode(WeaponFamily.BLADE).isEmpty());
    }

    @Test
    void replacesPreviousSelectionAndForgetsItAtSessionEnd() {
        CombatModeState state = new CombatModeState();
        state.observe(WeaponFamily.POLEARM, "Entsatteln");
        state.observe(WeaponFamily.POLEARM, "Pfählen");
        assertEquals(CombatMode.PFAEHLEN, state.mode(WeaponFamily.POLEARM).orElseThrow());
        state.clear();
        for (WeaponFamily family : WeaponFamily.values()) assertTrue(state.mode(family).isEmpty());
    }
}
