package de.ottoextra.resources;

/**
 * The single pending proposal created by {@code /ottoextra resources create point}. It is only
 * a suggestion held in memory until confirmed; it expires after {@link #LIFETIME_MILLIS} without
 * activity. Pure state machine, no game access.
 */
final class ResourceProposals {
    static final long LIFETIME_MILLIS = 120_000L;
    static final int DEFAULT_RADIUS = 3;

    record Proposal(String type, String name, String dimension, int x, int y, int z, int radius) {
        ResourceZone zone() {
            return ResourceZone.point(x, y, z, radius);
        }

        Proposal withRadius(int value) {
            return new Proposal(type, name, dimension, x, y, z, Math.clamp(value, 1, 64));
        }
    }

    private Proposal current;
    private long lastActivity;
    private boolean editing;

    /** Replaces any pending proposal with a new one at the default radius. */
    Proposal create(String type, String name, String dimension, int x, int y, int z, long now) {
        current = new Proposal(type, name, dimension, x, y, z, DEFAULT_RADIUS);
        lastActivity = now;
        editing = false;
        return current;
    }

    /** The pending proposal, or null when there is none or it ran out. */
    Proposal current(long now) {
        if (current == null || !editing && now - lastActivity > LIFETIME_MILLIS) return null;
        return current;
    }

    /** Clears an expired proposal; true exactly once so the caller can tell the player. */
    boolean expireIfDue(long now) {
        if (current != null && !editing && now - lastActivity > LIFETIME_MILLIS) {
            current = null;
            return true;
        }
        return false;
    }

    /** Sets the radius (clamped to 1..64) and restarts the lifetime; null without a proposal. */
    Proposal adjustRadius(int radius, long now) {
        if (current(now) == null) return null;
        current = current.withRadius(radius);
        lastActivity = now;
        return current;
    }

    /** Takes the proposal for saving: returns it and clears the state, or null. */
    Proposal confirm(long now) {
        Proposal proposal = current(now);
        current = null;
        editing = false;
        return proposal;
    }

    /** Drops the proposal; true when there was one. */
    boolean discard() {
        boolean had = current != null;
        current = null;
        editing = false;
        return had;
    }

    /** The editor works on the proposal: it does not expire meanwhile. Null without a proposal. */
    Proposal beginEdit(long now) {
        Proposal proposal = current(now);
        if (proposal != null) {
            editing = true;
            lastActivity = now;
        }
        return proposal;
    }

    /** Editor closed: a saved proposal is done, otherwise it is pending again. */
    void endEdit(boolean saved, long now) {
        if (!editing) return;
        editing = false;
        if (saved) current = null;
        else lastActivity = now;
    }
}
