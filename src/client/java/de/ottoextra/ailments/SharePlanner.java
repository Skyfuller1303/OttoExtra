package de.ottoextra.ailments;

import java.util.List;

/**
 * Timing rules of sharing (docs/zustand-protokoll.md): send on change and at the latest every 5
 * minutes, never more than 6 writes a minute (10 s spacing), DELETE when switched off, query at most
 * every 60 s. Pure; the caller passes the clock and records what it did.
 */
public final class SharePlanner {

    public static final long HEARTBEAT_MS = 5 * 60_000L;
    public static final long MIN_WRITE_GAP_MS = 10_000L;
    public static final long QUERY_GAP_MS = 60_000L;
    public static final long RETRY_AFTER_ERROR_MS = 60_000L;

    public enum Action { NONE, PUT, DELETE }

    private List<Condition> lastSent;
    private long lastWriteAt = Long.MIN_VALUE / 2;
    private long lastQueryAt = Long.MIN_VALUE / 2;
    private boolean remoteExists;
    private long blockedUntil;

    /**
     * @param active  sharing is on and allowed (setting, consent, linked account, on Ottonien)
     * @param current own conditions, or null if the menu was never read
     */
    public Action next(long now, boolean active, List<Condition> current) {
        if (!active) return Action.NONE; // removal is handled by deleteDue, which also works off Ottonien
        if (current == null || now < blockedUntil || now - lastWriteAt < MIN_WRITE_GAP_MS) return Action.NONE;
        boolean changed = lastSent == null || !lastSent.equals(current);
        boolean heartbeat = now - lastWriteAt >= HEARTBEAT_MS;
        return changed || heartbeat ? Action.PUT : Action.NONE;
    }

    public void wrote(long now, List<Condition> sent) {
        lastWriteAt = now;
        lastSent = List.copyOf(sent);
        remoteExists = true;
    }

    public void deleted(long now) {
        lastWriteAt = now;
        lastSent = null;
        remoteExists = false;
    }

    private long lastDeleteAttemptAt = Long.MIN_VALUE / 2;

    /**
     * Off means delete: due while a copy may exist on the server and sharing is not (or no longer)
     * possible, no matter which server the player is on. Retried at most once a minute.
     */
    public boolean deleteDue(long now, boolean serverCopy, boolean shareOn, boolean consentOk) {
        if (!serverCopy || (shareOn && consentOk)) return false;
        return now - lastDeleteAttemptAt >= RETRY_AFTER_ERROR_MS && now >= blockedUntil;
    }

    public void deleteAttempted(long now) {
        lastDeleteAttemptAt = now;
    }

    public void failed(long now) {
        blockedUntil = now + RETRY_AFTER_ERROR_MS;
    }

    public boolean queryDue(long now) {
        return now - lastQueryAt >= QUERY_GAP_MS && now >= blockedUntilQuery;
    }

    private long blockedUntilQuery;

    public void queried(long now) {
        lastQueryAt = now;
    }

    public void queryFailed(long now) {
        blockedUntilQuery = now + RETRY_AFTER_ERROR_MS;
    }

    /** Forget everything sent, e.g. after a disconnect (the server entry expires on its own). */
    public void reset() {
        lastSent = null;
        lastWriteAt = Long.MIN_VALUE / 2;
        lastQueryAt = Long.MIN_VALUE / 2;
        remoteExists = false;
        blockedUntil = 0L;
        blockedUntilQuery = 0L;
    }
}
