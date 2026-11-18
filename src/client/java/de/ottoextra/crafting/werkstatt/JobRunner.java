package de.ottoextra.crafting.werkstatt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * State machine of one job, free of Minecraft. Every tick it gets a {@link View} of what is on
 * screen and returns at most one {@link Action}. Timings are fixed (no randomisation): one action
 * per {@link #ACTION_GAP_MS} at most, and every wait has a timeout that stops the job with a reason.
 *
 * <p>Continuous mode (Dauerbetrieb): open the station, follow the stored path, refuel if needed,
 * click the recipe, wait for the server's result, repeat until the quantity is reached. One-shot
 * mode: the station is already open on the recipe page; refuel if needed and click the recipe once.
 */
public final class JobRunner {
    public static final long ACTION_GAP_MS = 350L;
    public static final long OPEN_TIMEOUT_MS = 6000L;
    /** The station did not open (and no loading menu showed): use the block again after this long. */
    public static final long OPEN_RETRY_MS = 2000L;
    /** Uses of the station block per opening, the first one included. */
    public static final int OPEN_ATTEMPTS = 3;
    public static final long PAGE_TIMEOUT_MS = 4000L;
    public static final long START_TIMEOUT_MS = 6000L;
    public static final long RESULT_TIMEOUT_MS = 60000L;
    /**
     * After the start message, no result within this long counts as an unknown result: the craft is
     * counted as failed and the job goes on (a craft takes about 10 s).
     */
    public static final long RESULT_UNKNOWN_MS = 25000L;
    /** This many failed crafts in a row stop the job ({@link StopReason#TOO_MANY_FAILURES}). */
    public static final int MAX_CONSECUTIVE_FAILURES = 3;
    public static final long BETWEEN_CRAFTS_MS = 700L;
    /** After a fire click on a page that stays open, wait this long before the recipe click. */
    public static final long FIRE_SETTLE_MS = 800L;
    /**
     * The server sends a fresh screen's slots a moment after opening it: a station screen without any
     * slot is waited for this long before path, fire button or recipe are looked up.
     */
    public static final long CONTENT_WAIT_MS = 1500L;
    /** Assumed fire use per craft when nothing better is known. */
    public static final int DEFAULT_CRAFT_SECONDS = 10;

    public enum Phase { IDLE, CLOSING, OPENING, NAVIGATING, REFUELING, CLICKING, WAITING_RESULT, BETWEEN, PAUSED,
        STOPPED, FINISHED }

    /** The step shown in the panel's checklist. */
    public enum Step { OPEN, REFUEL, CLICK, RESULT }

    public enum ScreenKind { NONE, LOADING, STATION, OTHER }

    public record SlotView(int slot, String name, List<String> lore) {
        public RecipeLore recipe() {
            return RecipeLore.parse(lore);
        }
    }

    /**
     * What is on screen this tick.
     *
     * @param screenId     changes whenever a new screen (or new contents) is shown
     * @param slots        station slots (not the player inventory), empty unless {@code STATION}
     * @param inventory    player inventory, normalised name to count
     * @param cursorEmpty  nothing is held on the cursor
     */
    public record View(ScreenKind kind, int screenId, List<SlotView> slots, Map<String, Integer> inventory,
                       boolean cursorEmpty) {
        public static View none(Map<String, Integer> inventory) {
            return new View(ScreenKind.NONE, 0, List.of(), inventory, true);
        }
    }

    public sealed interface Action permits None, Open, Close, Click {
    }

    public record None() implements Action {
    }

    /** Use the station block again (only if in reach; the caller checks and never moves the player). */
    public record Open() implements Action {
    }

    /** Close the station screen. */
    public record Close() implements Action {
    }

    /** Left click on a station slot. {@code fireSeconds} is the burn time added if it is a fire button. */
    public record Click(int slot, int fireSeconds) implements Action {
    }

    public static final Action NONE = new None();

    private final WerkstattJob job;
    private final boolean continuous;
    private final FireClock fire;
    private final boolean autoRefuel;
    private final int craftSeconds;

    private Phase phase = Phase.IDLE;
    private Phase pausedFrom;
    private StopReason stopReason;
    private int pathIndex;
    private long phaseSince;
    private long lastActionAt = Long.MIN_VALUE / 2;
    private int screenAtAction;
    private boolean awaitingChange;
    /** Since when the station screen shows no slots; -1 while it has content. */
    private long emptySince = -1;
    private boolean started;
    private boolean completed;
    private boolean failedResult;
    private long startedAt = -1;
    private int failStreak;
    private boolean refused;
    private int firesThisCraft;
    private int openAttempts;
    private boolean openLoading;

    public JobRunner(WerkstattJob job, boolean continuous, FireClock fire, boolean autoRefuel, int craftSeconds) {
        this.job = job;
        this.continuous = continuous;
        this.fire = fire == null ? new FireClock() : fire;
        this.autoRefuel = autoRefuel;
        this.craftSeconds = craftSeconds > 0 ? craftSeconds : DEFAULT_CRAFT_SECONDS;
    }



    public WerkstattJob job() {
        return job;
    }

    public Phase phase() {
        return phase;
    }

    public StopReason stopReason() {
        return stopReason;
    }

    public boolean continuous() {
        return continuous;
    }

    public boolean active() {
        return phase != Phase.IDLE && phase != Phase.STOPPED && phase != Phase.FINISHED;
    }

    /**
     * True while the view and position lock applies: a continuous job that is running and not paused.
     * Stopping, pausing and finishing release the lock at once.
     */
    public boolean locksView() {
        return continuous && active() && phase != Phase.PAUSED;
    }

    /**
     * Freezes every timer of the current phase for {@code ms} (the chat was open). Nothing times out
     * and no action is due earlier because of the hold.
     */
    public void hold(long ms) {
        if (ms <= 0 || !active()) {
            return;
        }
        phaseSince += ms;
        lastActionAt += ms;
    }

    /** Current step for the checklist. */
    public Step step() {
        return switch (phase) {
            case REFUELING -> Step.REFUEL;
            case CLICKING -> Step.CLICK;
            case WAITING_RESULT, BETWEEN -> Step.RESULT;
            default -> Step.OPEN;
        };
    }

    /** Starts the job. With the station open on a page showing the recipe, the path is skipped. */
    public void start(View view, long now) {
        stopReason = null;
        job.stopReason = null;
        started = false;
        startedAt = -1;
        completed = false;
        failedResult = false;
        refused = false;
        failStreak = 0;
        firesThisCraft = 0;
        job.status = WerkstattJob.Status.RUNNING;
        if (view != null && view.kind() == ScreenKind.STATION && findRecipe(view) >= 0) {
            enter(Phase.REFUELING, now);
        } else if (!continuous) {
            stop(StopReason.RECIPE_NOT_FOUND);
        } else if (view != null && view.kind() == ScreenKind.STATION) {
            enter(Phase.CLOSING, now);
        } else {
            enter(Phase.OPENING, now);
        }
    }

    public void pause(long now) {
        if (active() && phase != Phase.PAUSED) {
            pausedFrom = phase;
            phase = Phase.PAUSED;
            phaseSince = now;
            job.status = WerkstattJob.Status.PAUSED;
        }
    }

    /** Resumes a paused job; waiting for a result continues, everything else starts the craft over. */
    public void resume(long now) {
        if (phase != Phase.PAUSED) {
            return;
        }
        job.status = WerkstattJob.Status.RUNNING;
        enter(pausedFrom == Phase.WAITING_RESULT ? Phase.WAITING_RESULT : continuous ? Phase.OPENING : Phase.REFUELING,
                now);
    }

    public void stop(StopReason reason) {
        if (phase == Phase.STOPPED || phase == Phase.FINISHED) {
            return;
        }
        phase = Phase.STOPPED;
        stopReason = reason;
        job.status = WerkstattJob.Status.STOPPED;
        job.stopReason = reason;
    }

    /** "Du beginnst die Herstellung von ..." */
    public void onStarted() {
        started = true;
    }

    /** "Die Herstellung ist erfolgreich" */
    public void onCompleted() {
        completed = true;
    }

    /** "Die Herstellung ist schiefgegangen": the craft ran, the materials are gone, nothing was made. */
    public void onFailed() {
        failedResult = true;
    }

    /** Failed crafts in a row; a success resets it. */
    public int failStreak() {
        return failStreak;
    }

    /** A refusal or failure message of the server. */
    public void onRefused() {
        refused = true;
    }

    public Action tick(View view, long now) {
        if (view == null) {
            return NONE;
        }
        if (refused && active() && phase != Phase.PAUSED) {
            // A refusal after any of our clicks (recipe or fire) ends the job.
            stop(StopReason.SERVER_REFUSED);
            return NONE;
        }
        return switch (phase) {
            case IDLE, PAUSED, STOPPED, FINISHED -> NONE;
            case CLOSING -> closing(view, now);
            case OPENING -> opening(view, now);
            case NAVIGATING -> navigating(view, now);
            case REFUELING -> refueling(view, now);
            case CLICKING -> clicking(view, now);
            case WAITING_RESULT -> waitingResult(now);
            case BETWEEN -> {
                if (now - phaseSince >= BETWEEN_CRAFTS_MS) {
                    enter(view.kind() == ScreenKind.STATION ? Phase.CLOSING : Phase.OPENING, now);
                }
                yield NONE;
            }
        };
    }

    private Action closing(View view, long now) {
        if (view.kind() == ScreenKind.NONE) {
            enter(Phase.OPENING, now);
            return NONE;
        }
        if (!awaitingChange && gapOver(now)) {
            act(view, now);
            awaitingChange = true;
            return new Close();
        }
        if (now - phaseSince > OPEN_TIMEOUT_MS) {
            stop(StopReason.STATION_NOT_OPENED);
        }
        return NONE;
    }

    private Action opening(View view, long now) {
        if (view.kind() == ScreenKind.STATION && awaitingChange) {
            pathIndex = 0;
            enter(Phase.NAVIGATING, now);
            return NONE;
        }
        if (view.kind() == ScreenKind.OTHER) {
            stop(StopReason.OTHER_GUI);
            return NONE;
        }
        if (view.kind() == ScreenKind.LOADING && awaitingChange) {
            // The server answered; the loading menu turns into the station by itself.
            openLoading = true;
        }
        boolean retry = awaitingChange && !openLoading && openAttempts < OPEN_ATTEMPTS
                && now - lastActionAt >= OPEN_RETRY_MS;
        if ((!awaitingChange || retry) && view.kind() == ScreenKind.NONE && gapOver(now)) {
            act(view, now);
            awaitingChange = true;
            openAttempts++;
            return new Open();
        }
        if (now - phaseSince > OPEN_TIMEOUT_MS) {
            stop(StopReason.STATION_NOT_OPENED);
        }
        return NONE;
    }

    private Action navigating(View view, long now) {
        if (awaitingChange) {
            if (view.kind() == ScreenKind.STATION && view.screenId() != screenAtAction) {
                awaitingChange = false;
                pathIndex++;
                phaseSince = now;
            } else if (now - lastActionAt > PAGE_TIMEOUT_MS) {
                stop(StopReason.PAGE_TIMEOUT);
            }
            return NONE;
        }
        if (view.kind() == ScreenKind.LOADING) {
            return NONE;
        }
        if (view.kind() != ScreenKind.STATION) {
            if (now - phaseSince > PAGE_TIMEOUT_MS) {
                stop(StopReason.PAGE_TIMEOUT);
            }
            return NONE;
        }
        if (awaitingContent(view, now)) {
            return NONE;
        }
        if (pathIndex >= job.path.size()) {
            enter(Phase.REFUELING, now);
            return NONE;
        }
        if (!gapOver(now)) {
            return NONE;
        }
        WerkstattJob.PathStep step = job.path.get(pathIndex);
        int slot = findByName(view, step.slot, step.name);
        if (slot < 0) {
            stop(StopReason.PATH_NOT_FOUND);
            return NONE;
        }
        if (!view.cursorEmpty()) {
            stop(StopReason.CURSOR_BUSY);
            return NONE;
        }
        act(view, now);
        awaitingChange = true;
        return new Click(slot, 0);
    }

    private Action refueling(View view, long now) {
        if (awaitingChange) {
            // After a fire click the server may keep the page, reload it or close the station.
            if (view.kind() == ScreenKind.NONE) {
                awaitingChange = false;
                if (continuous) {
                    enter(Phase.OPENING, now);
                } else {
                    stop(StopReason.FIRE_CLOSED);
                }
                return NONE;
            }
            if (view.kind() == ScreenKind.LOADING) {
                return NONE;
            }
            if (view.kind() == ScreenKind.STATION && (view.screenId() != screenAtAction || now - lastActionAt >= FIRE_SETTLE_MS)) {
                if (awaitingContent(view, now)) {
                    return NONE;
                }
                awaitingChange = false;
                if (findRecipe(view) < 0) {
                    if (continuous) {
                        enter(Phase.CLOSING, now);
                    } else {
                        stop(StopReason.FIRE_CLOSED);
                    }
                }
                return NONE;
            }
            if (now - lastActionAt > PAGE_TIMEOUT_MS) {
                stop(StopReason.PAGE_TIMEOUT);
            }
            return NONE;
        }
        if (view.kind() != ScreenKind.STATION) {
            if (view.kind() == ScreenKind.NONE && !continuous) {
                stop(StopReason.FIRE_CLOSED);
            } else if (now - phaseSince > PAGE_TIMEOUT_MS) {
                stop(StopReason.PAGE_TIMEOUT);
            }
            return NONE;
        }
        if (awaitingContent(view, now)) {
            return NONE;
        }
        RecipeLore recipe = job.recipe();
        boolean heat = recipe.heat() == RecipeLore.Heat.YES
                || recipe.heat() == RecipeLore.Heat.UNKNOWN && firesThisCraft == 0 && fireButton(view, "") != null;
        if (!autoRefuel || !heat || !fire.needsRefuel(now)) {
            enter(Phase.CLICKING, now);
            return NONE;
        }
        if (!gapOver(now)) {
            return NONE;
        }
        SlotView button = fireButton(view, job.fuel);
        if (button == null) {
            stop(StopReason.NO_FIRE_BUTTON);
            return NONE;
        }
        RecipeLore cost = button.recipe();
        if (cost.known() && CraftPlan.simulate(new RecipeLore("", List.of(), cost.groups(), RecipeLore.Heat.NO, -1),
                view.inventory(), 1, null, 0, 0).crafts() < 1) {
            stop(StopReason.MISSING_FUEL);
            return NONE;
        }
        if (!view.cursorEmpty()) {
            stop(StopReason.CURSOR_BUSY);
            return NONE;
        }
        act(view, now);
        awaitingChange = true;
        firesThisCraft++;
        fire.add(cost.burnSeconds(), now);
        return new Click(button.slot(), cost.burnSeconds());
    }

    private Action clicking(View view, long now) {
        if (view.kind() != ScreenKind.STATION) {
            if (now - phaseSince > PAGE_TIMEOUT_MS) {
                stop(continuous ? StopReason.PAGE_TIMEOUT : StopReason.RECIPE_NOT_FOUND);
            }
            return NONE;
        }
        if (!gapOver(now) || awaitingContent(view, now)) {
            return NONE;
        }
        int slot = findRecipe(view);
        if (slot < 0) {
            stop(StopReason.RECIPE_NOT_FOUND);
            return NONE;
        }
        RecipeLore recipe = job.recipe();
        if (recipe.known() && CraftPlan.simulate(recipe, view.inventory(), 1, null, 0, 0).crafts() < 1) {
            stop(StopReason.MISSING_MATERIAL);
            return NONE;
        }
        if (!view.cursorEmpty()) {
            stop(StopReason.CURSOR_BUSY);
            return NONE;
        }
        act(view, now);
        started = false;
        startedAt = -1;
        completed = false;
        failedResult = false;
        refused = false;
        enter(Phase.WAITING_RESULT, now);
        return new Click(slot, 0);
    }

    private Action waitingResult(long now) {
        if (refused) {
            stop(StopReason.SERVER_REFUSED);
            return NONE;
        }
        if (started && startedAt < 0) {
            startedAt = now;
        }
        if (completed) {
            failStreak = 0;
            job.done = Math.min(job.quantity, job.done + 1);
            afterResult(now);
            return NONE;
        }
        if (failedResult || started && now - startedAt > RESULT_UNKNOWN_MS) {
            // A failed (or never answered) craft: not done, but the job goes on with the next one.
            job.failed++;
            failStreak++;
            if (failStreak >= MAX_CONSECUTIVE_FAILURES) {
                resetResult();
                stop(StopReason.TOO_MANY_FAILURES);
                return NONE;
            }
            afterResult(now);
            return NONE;
        }
        if (!started && now - phaseSince > START_TIMEOUT_MS) {
            stop(StopReason.NO_START);
        } else if (now - phaseSince > RESULT_TIMEOUT_MS) {
            stop(StopReason.RESULT_TIMEOUT);
        }
        return NONE;
    }

    private void afterResult(long now) {
        resetResult();
        if (!continuous || job.complete()) {
            phase = Phase.FINISHED;
            job.status = job.complete() ? WerkstattJob.Status.DONE : WerkstattJob.Status.WAITING;
        } else {
            enter(Phase.BETWEEN, now);
        }
    }

    private void resetResult() {
        completed = false;
        failedResult = false;
        started = false;
        startedAt = -1;
        firesThisCraft = 0;
    }

    /**
     * True while a station screen still shows no slots (its contents have not arrived), for at most
     * {@link #CONTENT_WAIT_MS}; afterwards the lookup runs and fails as usual.
     */
    private boolean awaitingContent(View view, long now) {
        if (!view.slots().isEmpty()) {
            emptySince = -1;
            return false;
        }
        if (emptySince < 0) {
            emptySince = now;
        }
        return now - emptySince < CONTENT_WAIT_MS;
    }

    private void enter(Phase next, long now) {
        phase = next;
        phaseSince = now;
        awaitingChange = false;
        openAttempts = 0;
        openLoading = false;
        job.status = WerkstattJob.Status.RUNNING;
    }

    private void act(View view, long now) {
        lastActionAt = now;
        screenAtAction = view.screenId();
    }

    private boolean gapOver(long now) {
        return now - lastActionAt >= ACTION_GAP_MS;
    }

    /** Slot of the job's recipe on this page: same name and, if readable, the same ingredients. */
    int findRecipe(View view) {
        String name = WerkstattText.normalize(job.name);
        RecipeLore wanted = job.recipe();
        List<SlotView> candidates = new ArrayList<>();
        for (SlotView s : view.slots()) {
            if (WerkstattText.normalize(s.name()).equals(name)) {
                RecipeLore here = s.recipe();
                if (!wanted.known() || here.known() && here.groups().equals(wanted.groups())) {
                    candidates.add(s);
                }
            }
        }
        for (SlotView s : candidates) {
            if (s.slot() == job.slot) {
                return s.slot();
            }
        }
        return candidates.isEmpty() ? -1 : candidates.get(0).slot();
    }

    /** Slot with this name; the stored slot wins when it still shows that name. */
    static int findByName(View view, int hint, String name) {
        String wanted = WerkstattText.normalize(name);
        int found = -1;
        for (SlotView s : view.slots()) {
            if (WerkstattText.normalize(s.name()).equals(wanted)) {
                if (s.slot() == hint) {
                    return hint;
                }
                if (found < 0) {
                    found = s.slot();
                }
            }
        }
        return found;
    }

    /** Fire button whose cost uses the chosen fuel; with no choice the first fire button. */
    static SlotView fireButton(View view, String fuel) {
        String wanted = WerkstattText.normalize(fuel);
        SlotView first = null;
        for (SlotView s : view.slots()) {
            RecipeLore r = s.recipe();
            if (!r.isFire()) {
                continue;
            }
            if (first == null) {
                first = s;
            }
            if (!wanted.isEmpty() && wanted.equals(fuelName(r))) {
                return s;
            }
        }
        return wanted.isEmpty() ? first : null;
    }

    /** Normalised name of the first cost item of a fire button, or "" if unreadable. */
    static String fuelName(RecipeLore fireLore) {
        if (fireLore == null || !fireLore.known() || fireLore.groups().isEmpty()) {
            return "";
        }
        return fireLore.groups().get(0).get(0).key();
    }
}
