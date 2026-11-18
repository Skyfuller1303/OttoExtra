package de.ottoextra.ailments;

/**
 * State of the invisible menu check: the menu is opened with the hotbar book, but its screen is never
 * shown. The open-screen packet is taken over (see {@link #shouldIntercept}), the slots fill as usual,
 * then the check is closed again. Pure; the caller supplies the clock and does the actual packets.
 */
public final class HiddenCheckState {

    public enum Phase { IDLE, ARMED, OPEN }

    /** What the caller has to do now. */
    public enum Step { NONE, READ, TIMEOUT, ABORT }

    public static final long TIMEOUT_MS = 3_000L;
    /** The chat line may arrive slightly after the check ended. */
    public static final long CHAT_GRACE_MS = 1_000L;
    public static final String OPEN_LINE = "[Info] Du öffnest dein Menü.";

    private Phase phase = Phase.IDLE;
    private long armedAt;
    private long endedAt = Long.MIN_VALUE / 2;
    private int syncId = -1;
    private long lateUntil = Long.MIN_VALUE / 2;
    private long lateStart = Long.MIN_VALUE / 2;

    public Phase phase() {
        return phase;
    }

    public int syncId() {
        return syncId;
    }

    public boolean active() {
        return phase != Phase.IDLE;
    }

    /** The use of the menu item was sent; the menu is expected now. */
    public void arm(long now) {
        phase = Phase.ARMED;
        armedAt = now;
        syncId = -1;
    }

    /**
     * Decision for an incoming open-screen packet: take it over only while armed, inside the time
     * window, for a generic_9x6 with the menu's title glyph. Everything else is passed through.
     */
    public boolean shouldIntercept(long now, String handlerId, String title) {
        return phase == Phase.ARMED && now - armedAt <= TIMEOUT_MS
                && MenuReader.MENU_HANDLER.equals(handlerId)
                && title != null && title.contains(MenuReader.MENU_TITLE_GLYPH);
    }

    /** The packet was taken over and a hidden handler with this id is now current. */
    public void intercepted(int id) {
        phase = Phase.OPEN;
        syncId = id;
    }

    /**
     * @param screenOpen the user (or something else) has a screen open
     * @param handlerReplaced the current handler is no longer the hidden one
     * @param menuReady the hidden handler holds the menu contents (buttons arrived and settled)
     */
    public Step step(long now, boolean screenOpen, boolean handlerReplaced, boolean menuReady) {
        if (phase == Phase.IDLE) return Step.NONE;
        if (screenOpen || (phase == Phase.OPEN && handlerReplaced)) return Step.ABORT;
        if (phase == Phase.OPEN && menuReady) return Step.READ;
        if (now - armedAt > TIMEOUT_MS) return Step.TIMEOUT;
        return Step.NONE;
    }

    public void finish(long now) {
        finish(now, false);
    }

    /**
     * @param keepLate the menu was used but its packet has not been seen yet (abort or timeout while
     *                 armed): the packet may still arrive and would pop up visibly, so it is taken over and
     *                 closed again for another {@link #TIMEOUT_MS}
     */
    public void finish(long now, boolean keepLate) {
        if (keepLate && phase == Phase.ARMED) {
            lateStart = now;
            lateUntil = now + TIMEOUT_MS;
        }
        phase = Phase.IDLE;
        endedAt = now;
        syncId = -1;
    }

    /** When the late-intercept window started (for "user input since then" checks). */
    public long lateStart() {
        return lateStart;
    }

    /**
     * A matching open-screen packet arrives after the check was given up. Taken over (and closed at once by
     * the caller) only if the user did nothing since and no screen is open; otherwise it passes through.
     */
    public boolean shouldInterceptLate(long now, String handlerId, String title, boolean inputSinceEnd,
                                       boolean screenOpen) {
        return phase == Phase.IDLE && now <= lateUntil && !inputSinceEnd && !screenOpen
                && MenuReader.MENU_HANDLER.equals(handlerId)
                && title != null && title.contains(MenuReader.MENU_TITLE_GLYPH);
    }

    public void lateConsumed() {
        lateUntil = Long.MIN_VALUE / 2;
    }

    /**
     * Whether the close packet may be sent. The server closes whatever window is open, ignoring the id,
     * so it must not go out in a tick with another use/interact packet (the arming tick), while the user
     * presses use/inventory/attack, or while a screen is open.
     */
    public static boolean closeAllowed(boolean armedThisTick, boolean userInput, boolean screenOpen) {
        return !armedThisTick && !userInput && !screenOpen;
    }
    /** The "Du öffnest dein Menü." line is hidden while a check is in flight and shortly after. */
    public boolean shouldSuppressChat(long now, String plainText) {
        boolean window = phase != Phase.IDLE || now - endedAt <= CHAT_GRACE_MS || now <= lateUntil;
        return window && plainText != null && OPEN_LINE.equals(AilmentNames.plain(plainText));
    }
}
