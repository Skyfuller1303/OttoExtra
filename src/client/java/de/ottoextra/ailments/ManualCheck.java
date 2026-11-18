package de.ottoextra.ailments;

/** Request and run state of the manual check ({@code /ottoextra leiden check}). Pure. */
public final class ManualCheck {

    private boolean pending;
    private boolean running;

    /** @return false if a manual check is already requested or running */
    public boolean request() {
        if (pending || running) return false;
        pending = true;
        return true;
    }

    public boolean pending() {
        return pending;
    }

    public boolean running() {
        return running;
    }

    public boolean any() {
        return pending || running;
    }

    /** The scheduler took the request and started the check. */
    public void started() {
        running = pending;
        pending = false;
    }

    public void done() {
        pending = false;
        running = false;
    }

    /** Disconnect or reset: nothing may survive into the next session. */
    public void reset() {
        done();
    }
}
