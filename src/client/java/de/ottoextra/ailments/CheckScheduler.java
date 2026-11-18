package de.ottoextra.ailments;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Pure decision of the check scheduler: what happens at this tick, and why. */
public final class CheckScheduler {

    public enum Kind { DISABLED, UNSCHEDULED, WAIT, BLOCKED, NO_MENU_ITEM, GO }

    public record Decision(Kind kind, String reason) {
        /** True if the decision is worth a log line (everything except the idle states). */
        public boolean loggable() {
            return kind == Kind.BLOCKED || kind == Kind.NO_MENU_ITEM || kind == Kind.GO;
        }
    }

    private CheckScheduler() {
    }

    /**
     * @param manual      started by the player ({@code /ottoextra leiden check}): ignores setting and interval,
     *                    but never the safety guards
     * @param nextCheckAt 0 if nothing is scheduled
     * @param guard       reason from {@link AutoCheckGuard#blockReason}, null if the check is allowed; only asked
     *                    when the check is due
     */
    public static Decision decide(boolean manual, boolean autoEnabled, long now, long nextCheckAt,
                                  Supplier<String> guard, BooleanSupplier hasMenuItem) {
        if (!manual) {
            if (!autoEnabled) return new Decision(Kind.DISABLED, "autoCheck off");
            if (nextCheckAt == 0L) return new Decision(Kind.UNSCHEDULED, "not scheduled (no join seen)");
            if (now < nextCheckAt) return new Decision(Kind.WAIT, "due in " + (nextCheckAt - now) + " ms");
        }
        String blocked = guard.get();
        if (blocked != null) return new Decision(Kind.BLOCKED, blocked);
        if (!hasMenuItem.getAsBoolean()) return new Decision(Kind.NO_MENU_ITEM, "no Menü item in hotbar");
        return new Decision(Kind.GO, manual ? "manual" : "due");
    }
}
