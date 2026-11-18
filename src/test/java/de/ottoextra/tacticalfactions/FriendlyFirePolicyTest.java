package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;

import static de.ottoextra.tacticalfactions.FriendlyFirePolicy.*;
import static org.junit.jupiter.api.Assertions.*;

class FriendlyFirePolicyTest {
    @Test void blocksSupportedWeaponsAgainstOnlineAllies() {
        for (WeaponKind weapon : new WeaponKind[]{WeaponKind.MELEE, WeaponKind.BOW,
                WeaponKind.CROSSBOW, WeaponKind.TRIDENT, WeaponKind.HARMFUL_POTION}) {
            assertTrue(blocks(true, true, true, false, false,
                    Relation.ALLIED, TargetKind.ONLINE_PLAYER, weapon));
        }
    }

    @Test void neverBlocksOutsideCompleteSafetyMatrix() {
        assertFalse(blocks(false, true, true, false, false, Relation.ALLIED, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
        assertFalse(blocks(true, false, true, false, false, Relation.ALLIED, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
        assertFalse(blocks(true, true, false, false, false, Relation.ALLIED, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
        assertFalse(blocks(true, true, true, false, false, Relation.NEUTRAL, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
        assertFalse(blocks(true, true, true, true, false, Relation.HOSTILE, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
        assertFalse(blocks(true, true, true, false, false, Relation.ALLIED, TargetKind.NPC, WeaponKind.MELEE));
        assertFalse(blocks(true, true, true, false, false, Relation.ALLIED, TargetKind.OTHER, WeaponKind.MELEE));
        assertFalse(blocks(true, true, true, false, false, Relation.ALLIED, TargetKind.ONLINE_PLAYER, WeaponKind.OTHER));
    }

    @Test void optionallyProtectsNeutralPlayers() {
        assertTrue(blocks(true, true, true, true, false,
                Relation.NEUTRAL, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
        assertFalse(blocks(true, true, true, false, false,
                Relation.NEUTRAL, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
    }

    @Test void markedAttackersCanBeHitRegardlessOfProtectedRelation() {
        assertFalse(blocks(true, true, true, true, true,
                Relation.ALLIED, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
        assertFalse(blocks(true, true, true, true, true,
                Relation.NEUTRAL, TargetKind.ONLINE_PLAYER, WeaponKind.MELEE));
    }
}
