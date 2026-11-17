package de.ottoextra.crafting;

/**
 * Time driven state of the crafting HUD. All methods take the current time in milliseconds so the logic
 * stays independent of the game and testable.
 */
final class CraftingState {

    enum Result { NONE, SUCCESS, FAILURE }

    /** No progress update for this long counts as a failed craft (the server sends one per second). */
    static final long TIMEOUT_MS = 3000;
    /** How long the green or red end state stays visible. */
    static final long HOLD_MS = 800;
    /** Time constant of the exponential approach to the server's latest step. */
    private static final float SMOOTH_MS = 350.0f;

    private CraftingStation station;
    private String itemName;
    private boolean active;
    private Result result = Result.NONE;
    private float target;
    private float shown;
    private long lastUpdate;
    private long lastFrame;
    private long resultUntil;

    /** Chat line "Du beginnst die Herstellung von ...": a new craft, starts from empty. */
    void onStart(CraftingStation stationOfLastScreen, String item, long now) {
        station = stationOfLastScreen;
        itemName = item;
        target = 0.0f;
        shown = 0.0f;
        active = true;
        result = Result.NONE;
        lastUpdate = now;
        lastFrame = now;
        resultUntil = 0;
    }

    void onProgress(float fraction, CraftingStation stationOfLastScreen, long now) {
        if (!active || result != Result.NONE) {
            // Progress without a running craft: start fresh with the latest screen.
            onStart(stationOfLastScreen, null, now);
        } else if (fraction < target) {
            shown = 0.0f; // the only way the bar moves backwards: a reset
        }
        target = Math.max(0.0f, Math.min(1.0f, fraction));
        lastUpdate = now;
    }

    /** Completion message: bar full and green. Wins over a failure that was just assumed. Returns true if applied. */
    boolean onComplete(long now) {
        if (!visible(now) || result == Result.SUCCESS) return false;
        result = Result.SUCCESS;
        target = 1.0f;
        shown = 1.0f;
        resultUntil = now + HOLD_MS;
        return true;
    }

    /** Failure message while crafting: bar red at its current fill. Returns true if applied. */
    boolean onFailure(long now) {
        if (!active || result != Result.NONE) return false;
        result = Result.FAILURE;
        resultUntil = now + HOLD_MS;
        return true;
    }

    /** Detects the timeout; returns true exactly once when it turns the craft into a failure. */
    boolean tick(long now) {
        return active && result == Result.NONE && now > lastUpdate + TIMEOUT_MS && onFailure(now);
    }

    void clear() {
        active = false;
        result = Result.NONE;
        resultUntil = 0;
    }

    boolean visible(long now) {
        if (!active) return false;
        if (result != Result.NONE) return now < resultUntil;
        return now <= lastUpdate + TIMEOUT_MS;
    }

    Result result() {
        return result;
    }

    /** Advances the smoothing to {@code now} and returns the displayed fraction. */
    float advance(long now) {
        long dt = Math.max(0, now - lastFrame);
        lastFrame = now;
        if (result == Result.NONE && shown < target) {
            float k = 1.0f - (float) Math.exp(-dt / SMOOTH_MS);
            shown = Math.min(target, shown + (target - shown) * k);
        }
        return shown;
    }

    CraftingStation station() {
        return station;
    }

    String itemName() {
        return itemName;
    }
}
