package de.ottoextra.crafting.werkstatt;

/**
 * Estimated fire of the station. The server does not show the fire, so this only counts what our
 * own fire clicks added and lets it burn down in real time. It starts empty: the first craft that
 * needs heat lights a fire.
 */
public final class FireClock {
    /** Refuel when the estimate is below this many seconds. */
    public static final int REFUEL_BELOW_SECONDS = 3;

    private long burnsUntilMs;

    /** Seconds of fire left at {@code nowMs}. */
    public int secondsLeft(long nowMs) {
        return (int) Math.max(0, (burnsUntilMs - nowMs) / 1000L);
    }

    /** A fire click added {@code seconds}; adds on top of a fire still burning. */
    public void add(int seconds, long nowMs) {
        if (seconds <= 0) {
            return;
        }
        burnsUntilMs = Math.max(burnsUntilMs, nowMs) + seconds * 1000L;
    }

    public boolean needsRefuel(long nowMs) {
        return secondsLeft(nowMs) < REFUEL_BELOW_SECONDS;
    }

    public void reset() {
        burnsUntilMs = 0;
    }
}
