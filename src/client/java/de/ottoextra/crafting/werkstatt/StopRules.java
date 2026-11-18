package de.ottoextra.crafting.werkstatt;

/**
 * Stop conditions that are checked every tick while a job runs. Material, fuel, server refusal,
 * the stop command and the stop key always stop and are handled where they happen.
 */
public final class StopRules {
    /** Moving further than this many blocks from the start position stops the job. */
    public static final double MAX_DISTANCE = 2.0;

    /** Which optional rules are on. */
    public record Settings(boolean moved, boolean damage, boolean otherGui, boolean inventoryFull,
                           boolean unfocused) {
        public static final Settings ALL = new Settings(true, true, true, true, true);

        public int activeCount() {
            return (moved ? 1 : 0) + (damage ? 1 : 0) + (otherGui ? 1 : 0) + (inventoryFull ? 1 : 0)
                    + (unfocused ? 1 : 0);
        }
    }

    /**
     * One tick's facts.
     *
     * @param distanceFromStart blocks between the player and where the job started
     * @param damaged           the player lost health since the last tick
     * @param foreignScreen     a screen is open that is neither the station nor its loading menu
     * @param windowFocused     the game window has the focus
     * @param inventoryFull     no free inventory slot for the product
     */
    public record Observation(double distanceFromStart, boolean damaged, boolean foreignScreen,
                              boolean windowFocused, boolean inventoryFull) {
        /**
         * Facts while the chat is open: the chat is no foreign GUI and typing needs no focus check, but
         * damage and moving away still count (death is checked separately).
         */
        public static Observation whileChatting(double distanceFromStart, boolean damaged) {
            return new Observation(distanceFromStart, damaged, false, true, false);
        }
    }

    private StopRules() {
    }

    /** The first rule that applies, or null to keep going. */
    public static StopReason check(Settings settings, Observation o) {
        if (settings == null || o == null) {
            return null;
        }
        if (settings.damage() && o.damaged()) {
            return StopReason.DAMAGE;
        }
        if (settings.moved() && o.distanceFromStart() > MAX_DISTANCE) {
            return StopReason.MOVED;
        }
        if (settings.otherGui() && o.foreignScreen()) {
            return StopReason.OTHER_GUI;
        }
        if (settings.unfocused() && !o.windowFocused()) {
            return StopReason.UNFOCUSED;
        }
        if (settings.inventoryFull() && o.inventoryFull()) {
            return StopReason.INVENTORY_FULL;
        }
        return null;
    }
}
