package de.ottoextra.crafting.werkstatt;

import de.ottoextra.crafting.werkstatt.JobRunner.Action;
import de.ottoextra.crafting.werkstatt.JobRunner.ScreenKind;
import de.ottoextra.crafting.werkstatt.JobRunner.SlotView;
import de.ottoextra.crafting.werkstatt.JobRunner.View;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobRunnerTest {
    private static final List<SlotView> ROOT = List.of(
            new SlotView(10, "Zeugschmieden", List.of("Klicke, um die Kategorie zu öffnen")),
            new SlotView(12, "Schmelzen", List.of()));
    private static final List<SlotView> RECIPES = List.of(
            new SlotView(0, "Zurück", List.of()),
            new SlotView(3, "Feuer machen", RecipeLoreTest.FEUER),
            new SlotView(15, "Eisenbarren", RecipeLoreTest.EISENBARREN));

    private static WerkstattJob job(int quantity) {
        WerkstattJob j = new WerkstattJob();
        j.name = "Eisenbarren";
        j.lore = new ArrayList<>(RecipeLoreTest.EISENBARREN);
        j.path = new ArrayList<>(List.of(new WerkstattJob.PathStep(10, "Zeugschmieden")));
        j.slot = 15;
        j.quantity = quantity;
        j.fuel = "Stock";
        return j;
    }

    /**
     * A tiny fake of the server: answers our actions like Ottonien does (loading menu between pages,
     * the station closes after a recipe click, start and completion messages later).
     */
    private static final class FakeServer {
        final Map<String, Integer> inventory = new HashMap<>(Map.of("eisenerz", 96, "steinkohle", 44, "stock", 30));
        ScreenKind kind = ScreenKind.NONE;
        List<SlotView> page = List.of();
        int id = 1;
        int loadingTicks;
        List<SlotView> after;
        int craftTicks = -1;
        boolean refuseRecipe;
        boolean silent;
        boolean neverCompletes;
        /** The next this many uses of the station block are lost (no answer at all). */
        int dropOpens;
        /** The next this many crafts end with "Die Herstellung ist schiefgegangen". */
        int failNext;
        /** The next this many crafts start but never get a result message. */
        int noResultNext;
        boolean failing;
        int loadingLength = 2;
        /** After the loading menu a page shows no slots for this many ticks (contents not sent yet). */
        int emptyTicks;
        int contentTicks;
        int opens;
        int fireClicks;
        int recipeClicks;
        final List<Action> actions = new ArrayList<>();

        View view() {
            return new View(kind, id, kind == ScreenKind.STATION && contentTicks <= 0 ? page : List.of(), inventory,
                    true);
        }

        void apply(Action a, JobRunner runner) {
            actions.add(a);
            if (a instanceof JobRunner.Open) {
                opens++;
                if (dropOpens > 0) {
                    dropOpens--;
                } else {
                    load(ROOT);
                }
            } else if (a instanceof JobRunner.Close) {
                kind = ScreenKind.NONE;
            } else if (a instanceof JobRunner.Click c) {
                if (page == ROOT && c.slot() == 10) {
                    load(RECIPES);
                } else if (page == RECIPES && c.slot() == 3) {
                    fireClicks++;
                    inventory.merge("stock", -3, Integer::sum);
                } else if (page == RECIPES && c.slot() == 15) {
                    recipeClicks++;
                    kind = ScreenKind.NONE;
                    if (refuseRecipe) {
                        runner.onRefused();
                    } else if (silent) {
                        // No message at all.
                    } else {
                        inventory.merge("eisenerz", -4, Integer::sum);
                        inventory.merge("steinkohle", -2, Integer::sum);
                        runner.onStarted();
                        failing = failNext > 0;
                        if (failing) {
                            failNext--;
                        }
                        if (noResultNext > 0) {
                            noResultNext--;
                            craftTicks = -1;
                        } else {
                            craftTicks = neverCompletes ? -1 : 20;
                        }
                    }
                }
            }
        }

        void load(List<SlotView> next) {
            kind = ScreenKind.LOADING;
            id++;
            loadingTicks = loadingLength;
            after = next;
        }

        void tick(JobRunner runner) {
            if (contentTicks > 0 && --contentTicks == 0) {
                id++;
            }
            if (kind == ScreenKind.LOADING && --loadingTicks <= 0) {
                kind = ScreenKind.STATION;
                page = after;
                id++;
                contentTicks = emptyTicks;
            }
            if (craftTicks > 0 && --craftTicks == 0) {
                if (failing) {
                    runner.onFailed();
                } else {
                    runner.onCompleted();
                }
            }
        }
    }

    private static FakeServer run(JobRunner runner, FakeServer server, int ticks) {
        for (int t = 0; t < ticks && runner.active(); t++) {
            long now = t * 100L;
            server.tick(runner);
            Action a = runner.tick(server.view(), now);
            if (!(a instanceof JobRunner.None)) {
                server.apply(a, runner);
            }
        }
        return server;
    }

    @Test void continuousJobOpensNavigatesRefuelsAndCraftsUntilDone() {
        WerkstattJob job = job(3);
        FakeServer server = new FakeServer();
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 2000);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertEquals(3, job.done);
        assertEquals(WerkstattJob.Status.DONE, job.status);
        assertEquals(3, server.opens, "station reopened for every craft");
        assertEquals(3, server.recipeClicks);
        assertTrue(server.fireClicks >= 1, "fire lit before the first heat craft");
    }

    @Test void actionsAreNeverCloserThanTheFixedGap() {
        FakeServer server = new FakeServer();
        JobRunner runner = new JobRunner(job(2), true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        long last = Long.MIN_VALUE / 2;
        for (int t = 0; t < 2000 && runner.active(); t++) {
            long now = t * 50L;
            server.tick(runner);
            Action a = runner.tick(server.view(), now);
            if (!(a instanceof JobRunner.None)) {
                assertTrue(now - last >= JobRunner.ACTION_GAP_MS, "two actions within " + (now - last) + " ms");
                last = now;
                server.apply(a, runner);
            }
        }
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
    }

    @Test void serverRefusalStopsTheJobWithReason() {
        FakeServer server = new FakeServer();
        server.refuseRecipe = true;
        WerkstattJob job = job(5);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 500);
        assertEquals(JobRunner.Phase.STOPPED, runner.phase());
        assertEquals(StopReason.SERVER_REFUSED, runner.stopReason());
        assertEquals(StopReason.SERVER_REFUSED, job.stopReason);
        assertEquals(0, job.done);
    }

    @Test void missingMaterialStopsBeforeTheRecipeClick() {
        FakeServer server = new FakeServer();
        server.inventory.put("eisenerz", 3);
        JobRunner runner = new JobRunner(job(1), true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 500);
        assertEquals(StopReason.MISSING_MATERIAL, runner.stopReason());
        assertEquals(0, server.recipeClicks);
    }

    @Test void missingFuelStopsBeforeTheFireClick() {
        FakeServer server = new FakeServer();
        server.inventory.put("stock", 2);
        JobRunner runner = new JobRunner(job(1), true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 500);
        assertEquals(StopReason.MISSING_FUEL, runner.stopReason());
        assertEquals(0, server.fireClicks);
    }

    @Test void oneShotOnTheOpenRecipePageNeverReopensTheStation() {
        FakeServer server = new FakeServer();
        server.kind = ScreenKind.STATION;
        server.page = RECIPES;
        WerkstattJob job = job(10);
        JobRunner runner = new JobRunner(job, false, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 500);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertEquals(1, job.done);
        assertEquals(0, server.opens);
        assertEquals(1, server.recipeClicks);
        assertEquals(WerkstattJob.Status.WAITING, job.status, "9 crafts still to do");
    }

    @Test void oneShotWithoutTheRecipeOnScreenStopsAtOnce() {
        JobRunner runner = new JobRunner(job(1), false, new FireClock(), true, 10);
        runner.start(View.none(Map.of()), 0);
        assertEquals(StopReason.RECIPE_NOT_FOUND, runner.stopReason());
    }

    @Test void missingNavigationButtonStopsWithPathNotFound() {
        FakeServer server = new FakeServer();
        WerkstattJob job = job(1);
        job.path = new ArrayList<>(List.of(new WerkstattJob.PathStep(20, "Gibt es nicht")));
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 500);
        assertEquals(StopReason.PATH_NOT_FOUND, runner.stopReason());
    }

    @Test void pagesWithoutContentYetAreWaitedFor() {
        FakeServer server = new FakeServer();
        server.emptyTicks = 8;
        WerkstattJob job = job(2);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 3000);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase(), "slots arriving 800 ms late do not break the path");
        assertEquals(2, job.done);
    }

    @Test void pageThatNeverGetsContentStopsAfterTheWait() {
        FakeServer server = new FakeServer();
        server.emptyTicks = 100_000;
        WerkstattJob job = job(1);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        long[] stoppedAt = {-1};
        for (int t = 0; t < 500 && runner.active(); t++) {
            long now = t * 100L;
            server.tick(runner);
            Action a = runner.tick(server.view(), now);
            if (!(a instanceof JobRunner.None)) {
                server.apply(a, runner);
            }
            if (!runner.active()) {
                stoppedAt[0] = now;
            }
        }
        assertEquals(StopReason.PATH_NOT_FOUND, runner.stopReason());
        assertTrue(stoppedAt[0] >= JobRunner.CONTENT_WAIT_MS, "no click on an empty page before the wait is over");
        assertEquals(1, server.opens);
        assertTrue(server.actions.stream().noneMatch(a -> a instanceof JobRunner.Click));
    }

    @Test void navigationPrefersTheNameOverTheStoredSlot() {
        View view = new View(ScreenKind.STATION, 1, List.of(new SlotView(11, "Zeugschmieden", List.of())), Map.of(), true);
        assertEquals(11, JobRunner.findByName(view, 10, "Zeugschmieden"));
        assertEquals(-1, JobRunner.findByName(view, 10, "Schmelzen"));
    }

    @Test void noStartMessageStopsWithNoStart() {
        FakeServer server = new FakeServer();
        server.silent = true;
        JobRunner runner = new JobRunner(job(1), true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 2000);
        assertEquals(StopReason.NO_START, runner.stopReason());
    }

    @Test void resultThatNeverComesCountsAsFailedAndTheJobGoesOn() {
        FakeServer server = new FakeServer();
        server.noResultNext = 1;
        WerkstattJob job = job(2);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 3000);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertEquals(1, job.done);
        assertEquals(1, job.failed);
        assertEquals(2, server.recipeClicks);
    }

    @Test void resultsThatNeverComeStopAfterTheFailureLimit() {
        FakeServer server = new FakeServer();
        server.neverCompletes = true;
        WerkstattJob job = job(5);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 5000);
        assertEquals(StopReason.TOO_MANY_FAILURES, runner.stopReason());
        assertEquals(JobRunner.MAX_CONSECUTIVE_FAILURES, job.failed);
        assertEquals(0, job.done);
    }

    @Test void otherScreenWhileOpeningStops() {
        WerkstattJob job = job(1);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(View.none(Map.of()), 0);
        assertInstanceOf(JobRunner.Open.class, runner.tick(View.none(Map.of()), 0));
        runner.tick(new View(ScreenKind.OTHER, 5, List.of(), Map.of(), true), 100);
        assertEquals(StopReason.OTHER_GUI, runner.stopReason());
    }

    @Test void pauseHoldsAndResumeContinues() {
        FakeServer server = new FakeServer();
        WerkstattJob job = job(2);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        runner.pause(0);
        assertEquals(JobRunner.Phase.PAUSED, runner.phase());
        assertInstanceOf(JobRunner.None.class, runner.tick(server.view(), 1000));
        assertEquals(WerkstattJob.Status.PAUSED, job.status);
        runner.resume(1000);
        assertEquals(JobRunner.Phase.OPENING, runner.phase());
        run(runner, server, 2000);
        assertEquals(2, job.done);
    }

    @Test void recipeIsVerifiedByIngredientsNotOnlyByName() {
        WerkstattJob j = job(1);
        JobRunner runner = new JobRunner(j, false, new FireClock(), true, 10);
        View sameNameOtherRecipe = new View(ScreenKind.STATION, 1, List.of(new SlotView(15, "Eisenbarren",
                List.of("Herstellen:", "9 x Gold"))), Map.of(), true);
        assertEquals(-1, runner.findRecipe(sameNameOtherRecipe));
        View moved = new View(ScreenKind.STATION, 1, List.of(new SlotView(16, "Eisenbarren", RecipeLoreTest.EISENBARREN)),
                Map.of(), true);
        assertEquals(16, runner.findRecipe(moved), "slot is only a hint");
    }

    @Test void stopRules() {
        StopRules.Settings all = StopRules.Settings.ALL;
        assertNull(StopRules.check(all, new StopRules.Observation(1.9, false, false, true, false)));
        assertEquals(StopReason.MOVED, StopRules.check(all, new StopRules.Observation(2.1, false, false, true, false)));
        assertEquals(StopReason.DAMAGE, StopRules.check(all, new StopRules.Observation(5, true, true, false, true)));
        assertEquals(StopReason.OTHER_GUI, StopRules.check(all, new StopRules.Observation(0, false, true, true, false)));
        assertEquals(StopReason.UNFOCUSED, StopRules.check(all, new StopRules.Observation(0, false, false, false, false)));
        assertEquals(StopReason.INVENTORY_FULL, StopRules.check(all, new StopRules.Observation(0, false, false, true, true)));
        StopRules.Settings none = new StopRules.Settings(false, false, false, false, false);
        assertNull(StopRules.check(none, new StopRules.Observation(9, true, true, false, true)));
        assertEquals(5, all.activeCount());
        assertNull(StopRules.check(null, null));
    }

    @Test void continuousModeReopensTheStationAfterEachResult() {
        FakeServer server = new FakeServer();
        WerkstattJob job = job(2);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        long now = 0;
        boolean sawBetween = false;
        boolean reopenedAfterResult = false;
        for (int t = 0; t < 2000 && runner.active(); t++) {
            now = t * 100L;
            server.tick(runner);
            JobRunner.Phase before = runner.phase();
            Action a = runner.tick(server.view(), now);
            sawBetween |= before == JobRunner.Phase.BETWEEN;
            if (a instanceof JobRunner.Open && job.done == 1) {
                reopenedAfterResult = true;
                assertEquals(ScreenKind.NONE, server.kind, "reopened only with nothing on screen");
            }
            if (!(a instanceof JobRunner.None)) {
                server.apply(a, runner);
            }
        }
        assertTrue(sawBetween);
        assertTrue(reopenedAfterResult, "station used again after the first result");
        assertEquals(2, job.done);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
    }

    @Test void lostStationUseIsRetried() {
        FakeServer server = new FakeServer();
        server.dropOpens = 1;
        WerkstattJob job = job(1);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 2000);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertEquals(2, server.opens, "one lost use, one retry");
        assertEquals(1, job.done);
    }

    @Test void stationThatNeverOpensStopsAfterAllAttempts() {
        FakeServer server = new FakeServer();
        server.dropOpens = 99;
        JobRunner runner = new JobRunner(job(1), true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 2000);
        assertEquals(StopReason.STATION_NOT_OPENED, runner.stopReason());
        assertEquals(JobRunner.OPEN_ATTEMPTS, server.opens);
    }

    @Test void slowLoadingMenuIsNotAnsweredWithASecondUse() {
        FakeServer server = new FakeServer();
        server.loadingLength = 30; // 3 s, longer than the retry delay
        WerkstattJob job = job(1);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 2000);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertEquals(1, server.opens);
    }

    @Test void holdFreezesTimeouts() {
        WerkstattJob job = job(1);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(View.none(Map.of()), 0);
        assertInstanceOf(JobRunner.Open.class, runner.tick(View.none(Map.of()), 0));
        // Chat open for 30 s: no retry, no timeout during or right after it.
        runner.hold(30_000L);
        assertInstanceOf(JobRunner.None.class, runner.tick(View.none(Map.of()), 30_000L));
        assertTrue(runner.active());
        assertEquals(JobRunner.Phase.OPENING, runner.phase());
        assertInstanceOf(JobRunner.Open.class, runner.tick(View.none(Map.of()), 30_000L + JobRunner.OPEN_RETRY_MS),
                "retry counts from the end of the hold");
    }

    @Test void viewLockFollowsTheJobLifecycle() {
        FakeServer server = new FakeServer();
        WerkstattJob job = job(1);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        assertFalse(runner.locksView(), "not before the start");
        runner.start(server.view(), 0);
        assertTrue(runner.locksView());
        runner.pause(0);
        assertFalse(runner.locksView(), "pause unlocks");
        runner.resume(0);
        assertTrue(runner.locksView());
        runner.stop(StopReason.KEY);
        assertFalse(runner.locksView(), "stop unlocks");

        JobRunner finishing = new JobRunner(job(1), true, new FireClock(), true, 10);
        finishing.start(server.view(), 0);
        run(finishing, new FakeServer(), 2000);
        assertEquals(JobRunner.Phase.FINISHED, finishing.phase());
        assertFalse(finishing.locksView(), "finish unlocks");

        FakeServer open = new FakeServer();
        open.kind = ScreenKind.STATION;
        open.page = RECIPES;
        JobRunner once = new JobRunner(job(1), false, new FireClock(), true, 10);
        once.start(open.view(), 0);
        assertTrue(once.active());
        assertFalse(once.locksView(), "a single click never locks");
    }

    @Test void failedCraftCountsAsAPassAndTheJobContinues() {
        FakeServer server = new FakeServer();
        server.failNext = 1;
        WerkstattJob job = job(2);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        boolean doneUnchangedAfterFailure = false;
        for (int t = 0; t < 3000 && runner.active(); t++) {
            server.tick(runner);
            int failedBefore = job.failed;
            int doneBefore = job.done;
            Action a = runner.tick(server.view(), t * 100L);
            if (job.failed > failedBefore) {
                doneUnchangedAfterFailure = job.done == doneBefore;
                assertEquals(JobRunner.Phase.BETWEEN, runner.phase(), "goes on like after a success");
            }
            if (!(a instanceof JobRunner.None)) {
                server.apply(a, runner);
            }
        }
        assertTrue(doneUnchangedAfterFailure, "a failure never counts as done");
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertEquals(WerkstattJob.Status.DONE, job.status);
        assertEquals(1, job.done);
        assertEquals(1, job.failed);
        assertEquals(2, server.recipeClicks, "a failed pass is never made up");
        assertEquals(2, server.opens);
    }

    @Test void threeFailuresInARowStopTheJob() {
        FakeServer server = new FakeServer();
        server.failNext = 99;
        WerkstattJob job = job(10);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 5000);
        assertEquals(StopReason.TOO_MANY_FAILURES, runner.stopReason());
        assertEquals(JobRunner.MAX_CONSECUTIVE_FAILURES, job.failed);
        assertEquals(0, job.done);
        assertEquals(JobRunner.MAX_CONSECUTIVE_FAILURES, server.recipeClicks);
    }

    @Test void successResetsTheFailureStreak() {
        FakeServer server = new FakeServer();
        WerkstattJob job = job(6);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        // fail, fail, success, fail, fail, success: never three in a row.
        int[] plan = {1, 1, 0, 1, 1, 0};
        int craft = 0;
        for (int t = 0; t < 10000 && runner.active(); t++) {
            server.tick(runner);
            Action a = runner.tick(server.view(), t * 100L);
            if (a instanceof JobRunner.Click c && c.slot() == 15) {
                server.failNext = craft < plan.length ? plan[craft] : 0;
                craft++;
            }
            if (!(a instanceof JobRunner.None)) {
                server.apply(a, runner);
            }
            if (runner.failStreak() == 0 && job.failed == 2) {
                assertEquals(1, job.done, "streak reset by the success after two failures");
            }
        }
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertNull(runner.stopReason());
        assertEquals(2, job.done);
        assertEquals(4, job.failed);
    }

    @Test void runningOutOfMaterialAfterAFailureStopsWithMissingMaterial() {
        FakeServer server = new FakeServer();
        server.inventory.put("eisenerz", 4); // enough for exactly one craft
        server.failNext = 1;
        WerkstattJob job = job(2);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 3000);
        assertEquals(StopReason.MISSING_MATERIAL, runner.stopReason());
        assertEquals(0, job.done);
        assertEquals(1, job.failed);
    }

    @Test void stationWithoutFireButtonsCraftsUnknownHeatRecipesWithoutFire() {
        // Schreibstube-like page: no "Feuer machen" button, recipe lore without a heat line.
        WerkstattJob j = new WerkstattJob();
        j.name = "Brief";
        j.station = "schreibstube";
        j.lore = new ArrayList<>(List.of("Gewöhnlich", "Herstellen:", "1 x Papier"));
        j.slot = 4;
        j.quantity = 1;
        View page = new View(ScreenKind.STATION, 1, List.of(new SlotView(4, "Brief", j.lore)),
                Map.of("papier", 3), true);
        JobRunner runner = new JobRunner(j, false, new FireClock(), true, 10);
        runner.start(page, 0);
        Action a = JobRunner.NONE;
        for (int t = 0; t < 20 && !(a instanceof JobRunner.Click); t++) {
            a = runner.tick(page, t * 100L);
        }
        assertInstanceOf(JobRunner.Click.class, a);
        assertEquals(4, ((JobRunner.Click) a).slot(), "recipe clicked, no fire needed");
        assertNull(runner.stopReason());
    }

    @Test void quantityCountsPassesSoFailuresFinishTheJob() {
        FakeServer server = new FakeServer();
        server.failNext = 1;
        WerkstattJob job = job(3);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 3000);
        assertEquals(JobRunner.Phase.FINISHED, runner.phase());
        assertEquals(WerkstattJob.Status.DONE, job.status, "all passes made counts as finished");
        assertNull(runner.stopReason());
        assertEquals(2, job.done);
        assertEquals(1, job.failed);
        assertEquals(3, server.recipeClicks, "no pass made up");
        assertEquals(0, job.remaining());
    }


    @Test void missingMaterialBeforeThePlannedPassesStillStops() {
        FakeServer server = new FakeServer();
        server.inventory.put("eisenerz", 8); // two passes of three
        WerkstattJob job = job(3);
        JobRunner runner = new JobRunner(job, true, new FireClock(), true, 10);
        runner.start(server.view(), 0);
        run(runner, server, 3000);
        assertEquals(StopReason.MISSING_MATERIAL, runner.stopReason());
        assertEquals(2, job.done);
        assertEquals(WerkstattJob.Status.STOPPED, job.status);
    }
}
