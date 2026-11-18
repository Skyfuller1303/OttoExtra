package de.ottoextra.ailments;

/**
 * Decides whether the automatic menu check may run right now. Pure; the caller collects the inputs.
 * Returns the reason for skipping, or null if the check is allowed.
 */
public final class AutoCheckGuard {

    /** Combat is over after this long without damage or attack. */
    public static final long COMBAT_COOLDOWN_MS = 10_000L;

    private AutoCheckGuard() {
    }

    public record Inputs(boolean inWorld, boolean onOttonien, boolean combatBossBar, long msSinceDamage,
                         long msSinceAttack, boolean screenOpen, boolean werkstattRunning, boolean moving,
                         boolean sprinting, boolean flying, boolean riding, boolean windowFocused,
                         boolean inputKeys, boolean usingItem, boolean itemCooldown) {

        public Inputs(boolean inWorld, boolean onOttonien, boolean combatBossBar, long msSinceDamage,
                      long msSinceAttack, boolean screenOpen, boolean werkstattRunning, boolean moving,
                      boolean sprinting, boolean flying, boolean riding, boolean windowFocused,
                      boolean inputKeys) {
            this(inWorld, onOttonien, combatBossBar, msSinceDamage, msSinceAttack, screenOpen, werkstattRunning,
                    moving, sprinting, flying, riding, windowFocused, inputKeys, false, false);
        }

        public Inputs(boolean inWorld, boolean onOttonien, boolean combatBossBar, long msSinceDamage,
                      long msSinceAttack, boolean screenOpen, boolean werkstattRunning, boolean moving,
                      boolean sprinting, boolean flying, boolean riding, boolean windowFocused) {
            this(inWorld, onOttonien, combatBossBar, msSinceDamage, msSinceAttack, screenOpen, werkstattRunning,
                    moving, sprinting, flying, riding, windowFocused, false, false, false);
        }
    }

    public static String blockReason(Inputs in) {
        if (!in.inWorld()) return "not in world";
        if (!in.onOttonien()) return "not on Ottonien";
        if (in.combatBossBar()) return "combat boss bar";
        if (in.msSinceDamage() < COMBAT_COOLDOWN_MS) return "recent damage";
        if (in.msSinceAttack() < COMBAT_COOLDOWN_MS) return "recent attack";
        if (in.screenOpen()) return "screen open";
        if (in.werkstattRunning()) return "Werkstatt job running";
        if (in.moving()) return "moving";
        if (in.sprinting()) return "sprinting";
        if (in.flying()) return "flying";
        if (in.riding()) return "riding";
        if (!in.windowFocused()) return "window unfocused";
        if (in.inputKeys()) return "inventory/use key pressed";
        if (in.usingItem()) return "using an item (fishing rod, bow, shield, ...)";
        if (in.itemCooldown()) return "item cooldown";
        return null;
    }
}
