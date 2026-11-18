package de.ottoextra.tacticalfactions;

public final class FriendlyFirePolicy {
    public enum TargetKind { ONLINE_PLAYER, NPC, OTHER }
    public enum WeaponKind { MELEE, BOW, CROSSBOW, TRIDENT, HARMFUL_POTION, OTHER }

    private FriendlyFirePolicy() { }

    public static boolean blocks(boolean combatMode, boolean onOttonien, boolean protectionEnabled,
                                 boolean protectNeutralPlayers, boolean retaliationAllowed,
                                 Relation relation, TargetKind target, WeaponKind weapon) {
        if (!combatMode || !onOttonien || !protectionEnabled
                || retaliationAllowed || target != TargetKind.ONLINE_PLAYER) return false;
        if (relation != Relation.ALLIED && !(protectNeutralPlayers && relation == Relation.NEUTRAL)) return false;
        return weapon == WeaponKind.MELEE || weapon == WeaponKind.BOW
                || weapon == WeaponKind.CROSSBOW || weapon == WeaponKind.TRIDENT
                || weapon == WeaponKind.HARMFUL_POTION;
    }
}
