package de.ottoextra.crafting.werkstatt;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Werkstatt state on the client: queue, remembered recipes, the running job and everything that
 * talks to Minecraft (reading the station screen, clicking, reopening the station). Render thread
 * only. The pure logic lives in {@link JobRunner}, {@link CraftPlan}, {@link RecipeLore} and
 * {@link StopRules}.
 */
public final class Werkstatt {
    static final Logger LOG = LoggerFactory.getLogger("ottoextra/werkstatt");
    /** A station opened this long after using a block counts as freshly opened from that block (start page). */
    private static final long FRESH_OPEN_MS = 3000L;
    /** A navigation click counts for the next page shown within this time. */
    private static final long NAV_CLICK_MS = 4000L;
    private static final long GLOW_MS = 1500L;
    private static final long STOPPED_CARD_MS = 15000L;

    private static WerkstattStore store = WerkstattStore.memory();
    private static final FireClock FIRE = new FireClock();
    private static JobRunner runner;
    private static boolean runAll;
    private static int selected = -1;

    /** Block and exact hit (side, point) of the player's last use of any block. */
    private static BlockHitResult lastUseHit;
    private static Block lastUseBlock;
    private static long usedBlockAt = Long.MIN_VALUE / 2;
    /** Per station id: the block hit that opened it. Reopening reuses it as is, whatever the crosshair shows. */
    private static final Map<String, StationSpot> SPOTS = new HashMap<>();
    private static final StationSession SESSION = new StationSession();
    private static final StationMemory MEMORY = new StationMemory();
    private static StationSession.State sessionState = StationSession.State.NONE;
    private static int sessionScreen;
    private static long lastTickAt;
    private static JobRunner.Phase loggedPhase;
    private static final List<WerkstattJob.PathStep> currentPath = new ArrayList<>();
    private static WerkstattJob.PathStep pendingStep;
    private static long pendingAt;
    private static int lastStationScreen;
    private static JobRunner.ScreenKind lastKind = JobRunner.ScreenKind.NONE;

    private static Vec3d startPos;
    private static float lastHealth = -1;
    private static long glowUntil;
    private static long stoppedAt;
    private static long clickAt;
    /** Last click of ours; refusals only count shortly after it (other system messages must not stop a job). */
    private static long lastOwnClickAt;
    private static final long REFUSAL_WINDOW_MS = 5000L;
    private static int craftSeconds = JobRunner.DEFAULT_CRAFT_SECONDS;

    private Werkstatt() {
    }

    // ---------------------------------------------------------------- settings

    public static OttoExtraConfig.Crafting config() {
        OttoExtraConfig.Crafting c = OttoExtraConfig.active().crafting;
        return c == null ? new OttoExtraConfig.Crafting() : c;
    }

    public static boolean enabled() {
        try {
            return config().werkstattEnabled;
        } catch (Throwable t) {
            return false;
        }
    }

    public static StopRules.Settings stopSettings() {
        OttoExtraConfig.Crafting c = config();
        return new StopRules.Settings(c.werkstattStopOnMove, c.werkstattStopOnDamage, c.werkstattStopOnOtherGui,
                c.werkstattStopOnFull, c.werkstattStopOnUnfocus);
    }

    public static void saveConfig() {
        try {
            OttoExtraConfig.active().save();
        } catch (Throwable ignored) {
            // Config not writable: the change lasts for this session.
        }
    }

    // ---------------------------------------------------------------- session

    public static void onJoin(String server, String account) {
        store = WerkstattStore.open(OttoExtraPaths.root().resolve("werkstatt"), server, account);
        selected = queue().isEmpty() ? -1 : 0;
        FIRE.reset();
    }

    public static void onDisconnect() {
        stop(StopReason.DISCONNECTED);
        store.save();
        store = WerkstattStore.memory();
        SPOTS.clear();
        SESSION.reset();
        sessionState = StationSession.State.NONE;
        lastUseHit = null;
        lastUseBlock = null;
        currentPath.clear();
        pendingStep = null;
        selected = -1;
        FIRE.reset();
    }

    public static List<WerkstattJob> queue() {
        return store.data().queue;
    }

    public static List<WerkstattJob> saved() {
        return store.data().saved;
    }

    /** The selected job, if it belongs to the given station (null: any station). */
    public static WerkstattJob selected(String station) {
        List<WerkstattJob> q = queue();
        if (selected < 0 || selected >= q.size()) {
            return null;
        }
        WerkstattJob job = q.get(selected);
        return station == null || station.equals(job.station) ? job : null;
    }

    public static WerkstattJob selected() {
        return selected(null);
    }

    /** Jobs of one station, in queue order. */
    public static List<WerkstattJob> queueFor(String station) {
        List<WerkstattJob> out = new ArrayList<>();
        for (WerkstattJob j : queue()) {
            if (station == null || station.equals(j.station)) {
                out.add(j);
            }
        }
        return out;
    }

    /** Remembered recipes of one station. */
    public static List<WerkstattJob> savedFor(String station) {
        List<WerkstattJob> out = new ArrayList<>();
        for (WerkstattJob j : saved()) {
            if (station == null || station.equals(j.station)) {
                out.add(j);
            }
        }
        return out;
    }

    static StationMemory memory() {
        return MEMORY;
    }

    /** Station of the open screen (sub-menus included), or null. */
    public static String currentStation() {
        return MinecraftClient.getInstance().currentScreen == null ? null : sessionState.station();
    }

    public static void select(int index) {
        selected = index >= 0 && index < queue().size() ? index : -1;
    }

    public static JobRunner runner() {
        return runner;
    }

    public static boolean running() {
        return runner != null && runner.active();
    }

    /**
     * View and position lock: on while a continuous job runs (not paused) and the setting is on.
     * Mouse look and movement keys are ignored then (see the mouse and keyboard input mixins).
     */
    public static boolean viewLocked() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            return runner != null && runner.locksView() && enabled() && config().werkstattLockView
                    && client.player != null && client.world != null && client.player.isAlive();
        } catch (Throwable t) {
            return false;
        }
    }

    public static FireClock fire() {
        return FIRE;
    }

    public static int craftSeconds() {
        return craftSeconds;
    }

    public static boolean glowing() {
        return System.currentTimeMillis() < glowUntil;
    }

    /** True while the stopped card should still show. */
    /** True while the finished card (summary) should still show. */
    public static boolean recentlyFinished() {
        return runner != null && runner.phase() == JobRunner.Phase.FINISHED
                && runner.job().status == WerkstattJob.Status.DONE
                && System.currentTimeMillis() - stoppedAt < STOPPED_CARD_MS;
    }

    /** Passes still to make (failed passes count as made). */
    public static int remaining(WerkstattJob job) {
        return job.remaining();
    }

    public static boolean recentlyStopped() {
        return runner != null && runner.phase() == JobRunner.Phase.STOPPED
                && System.currentTimeMillis() - stoppedAt < STOPPED_CARD_MS;
    }

    public static int waitingCount(String station) {
        int n = 0;
        for (WerkstattJob j : queueFor(station)) {
            if (j.status != WerkstattJob.Status.DONE) {
                n++;
            }
        }
        return n;
    }

    public static List<WerkstattJob.PathStep> currentPath() {
        return currentPath;
    }

    // ---------------------------------------------------------------- queue edits

    /** Right-click on a station slot: a new job for that button, selected. Returns the job or null. */
    public static WerkstattJob addFromStack(int slot, ItemStack stack, List<JobRunner.SlotView> page) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        WerkstattJob job = new WerkstattJob();
        job.name = stack.getName().getString();
        job.itemId = Registries.ITEM.getId(stack.getItem()).toString();
        Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
        job.itemModel = model == null ? "" : model.toString();
        job.outputCount = Math.max(1, stack.getCount());
        job.lore = loreOf(stack);
        job.slot = slot;
        String here = currentStation();
        job.station = here == null ? "zeugtisch" : here;
        job.path = new ArrayList<>();
        for (WerkstattJob.PathStep s : currentPath) {
            job.path.add(new WerkstattJob.PathStep(s.slot, s.name));
        }
        job.fuel = defaultFuel(page);
        if (job.recipe().isFire()) {
            return null;
        }
        List<WerkstattJob> q = queue();
        if (q.size() >= WerkstattStore.MAX_QUEUE) {
            q.removeIf(j -> j.status == WerkstattJob.Status.DONE);
            if (q.size() >= WerkstattStore.MAX_QUEUE) {
                return null;
            }
        }
        q.add(job);
        selected = q.size() - 1;
        glowUntil = System.currentTimeMillis() + GLOW_MS;
        store.save();
        return job;
    }

    /** Stick fire if the page has one, else the first fire button's fuel, else empty. */
    static String defaultFuel(List<JobRunner.SlotView> page) {
        String first = "";
        if (page != null) {
            for (JobRunner.SlotView s : page) {
                RecipeLore r = s.recipe();
                if (!r.isFire() || !r.known()) {
                    continue;
                }
                String fuel = r.groups().get(0).get(0).name();
                if (WerkstattText.normalize(fuel).equals("stock")) {
                    return fuel;
                }
                if (first.isEmpty()) {
                    first = fuel;
                }
            }
        }
        return first;
    }

    public static void remove(WerkstattJob job) {
        if (runner != null && runner.job() == job && runner.active()) {
            return;
        }
        queue().remove(job);
        select(Math.min(selected, queue().size() - 1));
        store.save();
    }

    /** Moves a job past its neighbour of the same station. */
    public static void move(WerkstattJob job, int delta) {
        List<WerkstattJob> q = queue();
        int i = q.indexOf(job);
        if (i < 0 || delta == 0) {
            return;
        }
        int j = i;
        do {
            j += Integer.signum(delta);
        } while (j >= 0 && j < q.size() && !q.get(j).station.equals(job.station));
        if (j < 0 || j >= q.size()) {
            return;
        }
        WerkstattJob other = q.get(j);
        q.set(j, job);
        q.set(i, other);
        if (selected == i) {
            selected = j;
        } else if (selected == j) {
            selected = i;
        }
        store.save();
    }

    public static boolean isSaved(WerkstattJob job) {
        String key = job.recipeKey();
        return saved().stream().anyMatch(s -> s.recipeKey().equals(key));
    }

    public static void toggleSaved(WerkstattJob job) {
        String key = job.recipeKey();
        if (!saved().removeIf(s -> s.recipeKey().equals(key))) {
            WerkstattJob copy = job.copyFresh();
            saved().add(0, copy);
            while (saved().size() > WerkstattStore.MAX_SAVED) {
                saved().remove(saved().size() - 1);
            }
        }
        store.save();
    }

    public static void forget(WerkstattJob savedJob) {
        saved().remove(savedJob);
        store.save();
    }

    /** "Neu" on a remembered recipe: a fresh queued job for it. */
    public static void queueSaved(WerkstattJob savedJob) {
        if (queue().size() >= WerkstattStore.MAX_QUEUE) {
            return;
        }
        WerkstattJob job = savedJob.copyFresh();
        queue().add(job);
        selected = queue().size() - 1;
        store.save();
    }

    public static void setQuantity(WerkstattJob job, int quantity) {
        job.quantity = Math.max(1, Math.min(CraftPlan.MAX_CRAFTS, quantity));
        job.done = Math.min(job.done, job.quantity);
        if (job.status == WerkstattJob.Status.DONE && !job.complete()) {
            job.status = WerkstattJob.Status.WAITING;
        }
        store.save();
    }

    public static void setFuel(WerkstattJob job, String fuel) {
        job.fuel = fuel == null ? "" : fuel;
        store.save();
    }

    // ---------------------------------------------------------------- running

    /** Starts one job: continuous with Dauerbetrieb, else a single recipe click on the open page. */
    public static void start(WerkstattJob job, boolean all) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (job == null || client.player == null || running()) {
            return;
        }
        boolean continuous = config().werkstattAutoRun;
        if (job.status == WerkstattJob.Status.DONE) {
            job.done = 0;
            job.failed = 0;
        }
        runAll = all && continuous;
        startPos = client.player.getEntityPos();
        lastHealth = client.player.getHealth();
        runner = new JobRunner(job, continuous, FIRE, config().werkstattAutoRefuel, craftSeconds);
        String here = currentStation();
        if (here != null && !here.equals(job.station)) {
            // The job belongs to another station: never click recipes of this one.
            runner.stop(StopReason.WRONG_STATION);
        } else {
            runner.start(view(client), now());
        }
        lastTickAt = now();
        LOG.info("Auftrag gestartet: {} ({}/{}), Dauerbetrieb={}, Phase {}", job.name, job.done, job.quantity,
                continuous, runner.phase());
        loggedPhase = runner.phase();
        if (runner.phase() == JobRunner.Phase.STOPPED) {
            stoppedAt = System.currentTimeMillis();
            LOG.info("Auftrag sofort gestoppt: {}", runner.stopReason());
        }
        store.save();
    }

    /** "Alle starten": the first job of this station that is not done. */
    public static void startAll(String station) {
        for (WerkstattJob j : queueFor(station)) {
            if (j.status != WerkstattJob.Status.DONE) {
                start(j, true);
                return;
            }
        }
    }

    public static void stop(StopReason reason) {
        if (runner != null && runner.active()) {
            runner.stop(reason);
            LOG.info("Auftrag gestoppt: {} ({}/{}), Grund {}", runner.job().name, runner.job().done,
                    runner.job().quantity, reason);
            loggedPhase = runner.phase();
            runAll = false;
            stoppedAt = System.currentTimeMillis();
            store.save();
        }
    }

    /** Hides a stopped job's card and panel view; the job stays in the queue with its reason. */
    public static void clearStopped() {
        if (runner != null && !runner.active()) {
            runner = null;
        }
        stoppedAt = 0;
    }

    public static void togglePause() {
        if (runner == null) {
            return;
        }
        if (runner.phase() == JobRunner.Phase.PAUSED) {
            runner.resume(now());
        } else {
            runner.pause(now());
        }
        LOG.info("Auftrag {}", runner.phase() == JobRunner.Phase.PAUSED ? "pausiert" : "fortgesetzt");
        loggedPhase = runner.phase();
    }

    /**
     * Game messages (system chat and, with {@code overlay}, the action bar). Start, success and failure
     * lines are matched as whole lines; a refusal only counts from a server notice (action bar or
     * "[Info]" line), never from player chat.
     */
    public static void onMessage(String plain, boolean overlay) {
        if (runner == null || !runner.active()) {
            return;
        }
        if (de.ottoextra.crafting.CraftingParser.itemName(plain).isPresent()) {
            LOG.info("Server: Herstellung begonnen (Phase {})", runner.phase());
            runner.onStarted();
        } else if (de.ottoextra.crafting.CraftingParser.isCompletion(plain)) {
            LOG.info("Server: Herstellung erfolgreich (Phase {})", runner.phase());
            if (clickAt > 0) {
                long seconds = (System.currentTimeMillis() - clickAt) / 1000L;
                if (seconds >= 1 && seconds <= 120) {
                    craftSeconds = (int) Math.max(1, Math.round(craftSeconds * 0.5 + seconds * 0.5));
                }
                clickAt = 0;
            }
            runner.onCompleted();
        } else if (de.ottoextra.crafting.CraftingParser.isCraftFailed(plain)) {
            LOG.info("Server: Herstellung schiefgegangen (Phase {}, \"{}\")", runner.phase(), plain);
            clickAt = 0;
            runner.onFailed();
        } else if (WerkstattText.isServerRefusal(plain, overlay)
                && System.currentTimeMillis() - lastOwnClickAt < REFUSAL_WINDOW_MS) {
            LOG.info("Server lehnt ab: \"{}\"", plain);
            runner.onRefused();
        }
    }

    /** The player used a block (client side, before the server answers). Any block can be a station. */
    public static void onUseBlock(BlockHitResult hit, Block block) {
        if (hit != null && block != null) {
            BlockPos pos = hit.getBlockPos().toImmutable();
            lastUseHit = new BlockHitResult(hit.getPos(), hit.getSide(), pos, hit.isInsideBlock());
            lastUseBlock = block;
            usedBlockAt = System.currentTimeMillis();
        }
    }

    /**
     * Updates the station session for the screen on display and returns its state. A station screen
     * opened right after using a block remembers that block for the station.
     */
    public static StationSession.State updateSession(Screen screen) {
        long now = System.currentTimeMillis();
        StationSession.ScreenType type;
        String title = "";
        boolean loading = false;
        int size = 0;
        if (screen == null) {
            type = StationSession.ScreenType.NONE;
        } else if (screen instanceof HandledScreen<?> hs) {
            type = StationSession.ScreenType.CONTAINER;
            title = hs.getTitle().getString();
            size = containerSize(hs.getScreenHandler());
            loading = title.isBlank() && hs.getScreenHandler() instanceof GenericContainerScreenHandler g
                    && loadingContents(g);
        } else {
            type = StationSession.ScreenType.OTHER;
        }
        String before = sessionState.station();
        StationSession.State state = SESSION.update(type, title, loading, size, now);
        int id = screen == null ? 0 : System.identityHashCode(screen);
        if (state.station() != null && id != sessionScreen) {
            sessionScreen = id;
            if (!state.subMenu() && now - usedBlockAt < FRESH_OPEN_MS && lastUseHit != null) {
                StationSpot old = SPOTS.put(state.station(), new StationSpot(lastUseHit, lastUseBlock));
                if (old == null || !old.hit().getBlockPos().equals(lastUseHit.getBlockPos())) {
                    LOG.info("Station {} an {} ({}) gemerkt", state.station(), lastUseHit.getBlockPos(),
                            Registries.BLOCK.getId(lastUseBlock));
                }
            }
            if (state.unknownTitle()) {
                LOG.info("Unbekannter Titel in Station {}: \"{}\" (als Untermenü behandelt)", state.station(), title);
            } else if (state.subMenu() && !loading) {
                LOG.info("Untermenü von {}: \"{}\"", state.station(), title);
            }
        } else if (state.station() == null && before != null && type != StationSession.ScreenType.NONE) {
            sessionScreen = 0;
        }
        sessionState = state;
        return state;
    }

    /** A left click on a station slot: navigation buttons are remembered as path steps. */
    public static void onStationLeftClick(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        RecipeLore r = RecipeLore.parse(loreOf(stack));
        if (r.isCraftable() || r.isFire()) {
            return;
        }
        pendingStep = new WerkstattJob.PathStep(slot, stack.getName().getString());
        pendingAt = System.currentTimeMillis();
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        JobRunner.View view = view(client);
        trackPath(view);
        String here = sessionState.station();
        if (here != null && view.kind() == JobRunner.ScreenKind.STATION) {
            for (JobRunner.SlotView s : view.slots()) {
                if (s.recipe().isFire()) {
                    MEMORY.markFire(here);
                    break;
                }
            }
        }
        if (runner != null && runner.active() && view.kind() == JobRunner.ScreenKind.STATION && here != null
                && !here.equals(runner.job().station)) {
            // Another station's screen: for this job it is a foreign GUI.
            view = new JobRunner.View(JobRunner.ScreenKind.OTHER, view.screenId(), List.of(), view.inventory(),
                    view.cursorEmpty());
        }
        long tickNow = now();
        long sinceLastTick = tickNow - lastTickAt;
        lastTickAt = tickNow;
        if (runner == null) {
            return;
        }
        if (!enabled() && runner.active()) {
            stop(StopReason.DISABLED);
        }
        if (runner.active() && !client.player.isAlive()) {
            stop(StopReason.DIED);
        }
        if (runner.active() && client.currentScreen instanceof ChatScreen) {
            // Chat stays usable during a job: the steps wait (timers included) until it closes, but
            // damage and moving away still stop a continuous job.
            runner.hold(Math.max(0, Math.min(sinceLastTick, 1000L)));
            float health = client.player.getHealth();
            boolean damaged = lastHealth >= 0 && health < lastHealth;
            lastHealth = health;
            if (runner.continuous() && runner.phase() != JobRunner.Phase.PAUSED) {
                double distance = startPos == null ? 0 : client.player.getEntityPos().distanceTo(startPos);
                StopReason reason = StopRules.check(stopSettings(),
                        StopRules.Observation.whileChatting(distance, damaged));
                if (reason != null) {
                    stop(reason);
                }
            }
            return;
        }
        if (runner.active() && runner.continuous() && runner.phase() != JobRunner.Phase.PAUSED) {
            // Stop rules guard the continuous mode; a single click needs no watching.
            float health = client.player.getHealth();
            boolean damaged = lastHealth >= 0 && health < lastHealth;
            lastHealth = health;
            double distance = startPos == null ? 0 : client.player.getEntityPos().distanceTo(startPos);
            boolean full = runner.phase() == JobRunner.Phase.CLICKING && inventoryFull(client);
            StopReason reason = StopRules.check(stopSettings(), new StopRules.Observation(distance, damaged,
                    view.kind() == JobRunner.ScreenKind.OTHER, client.isWindowFocused(), full));
            if (reason != null) {
                stop(reason);
            }
        } else {
            lastHealth = client.player.getHealth();
        }
        if (runner.active()) {
            JobRunner.Phase before = runner.phase();
            int failedBefore = runner.job().failed;
            execute(client, runner.tick(view, now()));
            if (runner.job().failed > failedBefore) {
                LOG.info("Durchgang fehlgeschlagen oder ohne Ergebnis: {}/{} fertig, {} fehlgeschlagen, {} in Folge",
                        runner.job().done, runner.job().quantity, runner.job().failed, runner.failStreak());
            }
            if (runner.phase() == JobRunner.Phase.WAITING_RESULT && before == JobRunner.Phase.CLICKING) {
                clickAt = System.currentTimeMillis();
            }
            logPhase(view);
            if (runner.phase() == JobRunner.Phase.STOPPED) {
                stoppedAt = System.currentTimeMillis();
                store.save();
                if (runAll && config().werkstattNextOnError) {
                    startNext(runner.job());
                } else {
                    runAll = false;
                }
            } else if (runner.phase() == JobRunner.Phase.FINISHED) {
                stoppedAt = System.currentTimeMillis();
                LOG.info("Auftrag fertig: {} von {} hergestellt, {} fehlgeschlagen", runner.job().done,
                        runner.job().quantity, runner.job().failed);
                store.save();
                if (runAll) {
                    startNext(runner.job());
                }
            }
        }
    }

    /** One INFO line per phase change of the runner (not per tick). */
    private static void logPhase(JobRunner.View view) {
        if (runner == null || runner.phase() == loggedPhase) {
            return;
        }
        WerkstattJob job = runner.job();
        if (runner.phase() == JobRunner.Phase.STOPPED) {
            LOG.info("Phase {} -> STOPPED ({}/{}), Grund {}, Bildschirm {}", loggedPhase, job.done, job.quantity,
                    runner.stopReason(), view.kind());
        } else {
            LOG.info("Phase {} -> {} ({}/{}), Bildschirm {}", loggedPhase, runner.phase(), job.done, job.quantity,
                    view.kind());
        }
        loggedPhase = runner.phase();
    }

    private static void startNext(WerkstattJob after) {
        List<WerkstattJob> q = queue();
        int from = q.indexOf(after);
        for (int i = from + 1; i < q.size(); i++) {
            WerkstattJob j = q.get(i);
            // Only jobs of the same station (same block) follow on; others wait for their station.
            if (j.status == WerkstattJob.Status.WAITING && j.station.equals(after.station)) {
                runner = null;
                start(j, true);
                return;
            }
        }
        runAll = false;
    }

    private static void execute(MinecraftClient client, JobRunner.Action action) {
        switch (action) {
            case JobRunner.Open open -> {
                if (!openStation(client, runner.job().station)) {
                    stop(StopReason.NOT_IN_REACH);
                }
            }
            case JobRunner.Close close -> {
                LOG.info("Schließe Zeugtisch");
                client.player.closeHandledScreen();
            }
            case JobRunner.Click click -> {
                if (client.currentScreen instanceof HandledScreen<?> hs && client.interactionManager != null) {
                    LOG.info("Klick auf Slot {}{}", click.slot(), click.fireSeconds() > 0 ? " (Feuer)" : "");
                    lastOwnClickAt = System.currentTimeMillis();
                    client.interactionManager.clickSlot(hs.getScreenHandler().syncId, click.slot(), 0,
                            SlotActionType.PICKUP, client.player);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Uses the job's station block again if it is in reach; never turns or moves the player. The hit
     * the player made when opening the station is reused as is (block, side, point), so the current
     * crosshair does not matter. The block must still be the same kind as then.
     */
    private static boolean openStation(MinecraftClient client, String station) {
        if (client.interactionManager == null || client.world == null) {
            LOG.info("Station öffnen: keine Welt");
            return false;
        }
        StationSpot spot = SPOTS.get(station);
        if (spot == null) {
            LOG.info("Station öffnen: {} noch nie selbst geöffnet, Block unbekannt", station);
            return false;
        }
        BlockHitResult hit = spot.hit();
        if (!client.world.getBlockState(hit.getBlockPos()).isOf(spot.block())) {
            LOG.info("Station öffnen: Block bei {} ist nicht mehr {}", hit.getBlockPos(),
                    Registries.BLOCK.getId(spot.block()));
            return false;
        }
        double reach = client.player.getBlockInteractionRange();
        double distance = client.player.getEyePos().distanceTo(Vec3d.ofCenter(hit.getBlockPos()));
        if (distance > reach + 0.5) {
            LOG.info("Station öffnen: außer Reichweite ({} > {})", String.format(java.util.Locale.ROOT, "%.2f",
                    distance), reach + 0.5);
            return false;
        }
        usedBlockAt = System.currentTimeMillis();
        var result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        LOG.info("Station {} öffnen bei {} ({}): {}", station, hit.getBlockPos(), hit.getSide(), result);
        if (result.isAccepted()) {
            client.player.swingHand(Hand.MAIN_HAND);
        }
        return true;
    }

    record StationSpot(BlockHitResult hit, Block block) {
    }

    private static void trackPath(JobRunner.View view) {
        long now = System.currentTimeMillis();
        if (view.kind() == JobRunner.ScreenKind.STATION) {
            // A sub-menu is never a fresh open, even if the server closed the GUI for a moment in between.
            boolean fresh = lastKind == JobRunner.ScreenKind.NONE && now - usedBlockAt < FRESH_OPEN_MS
                    && !sessionState.subMenu();
            if (fresh) {
                currentPath.clear();
                pendingStep = null;
            } else if (view.screenId() != lastStationScreen && pendingStep != null && now - pendingAt < NAV_CLICK_MS) {
                commit(pendingStep);
                pendingStep = null;
            }
            if (runner != null && runner.active() && (runner.phase() == JobRunner.Phase.REFUELING
                    || runner.phase() == JobRunner.Phase.CLICKING)) {
                currentPath.clear();
                currentPath.addAll(runner.job().path);
            }
            lastStationScreen = view.screenId();
        }
        if (view.kind() != JobRunner.ScreenKind.LOADING) {
            lastKind = view.kind();
        }
    }

    private static void commit(WerkstattJob.PathStep step) {
        String name = WerkstattText.normalize(step.name);
        if (name.equals("zuruck")) {
            if (!currentPath.isEmpty()) {
                currentPath.remove(currentPath.size() - 1);
            }
            return;
        }
        if (name.equals("vorherige seite") && !currentPath.isEmpty()
                && WerkstattText.normalize(currentPath.get(currentPath.size() - 1).name).equals("nachste seite")) {
            currentPath.remove(currentPath.size() - 1);
            return;
        }
        currentPath.add(step);
    }

    // ---------------------------------------------------------------- reading the screen

    /** True for a station screen or a screen opened from one (sub-menus, loading menu). */
    public static boolean isStationScreen(Screen screen) {
        return screen instanceof HandledScreen<?> && updateSession(screen).station() != null;
    }

    public static JobRunner.View view(MinecraftClient client) {
        Map<String, Integer> inventory = inventory(client);
        Screen screen = client.currentScreen;
        StationSession.State state = updateSession(screen);
        if (screen == null) {
            return JobRunner.View.none(inventory);
        }
        boolean cursorEmpty = client.player == null || client.player.currentScreenHandler.getCursorStack().isEmpty();
        if (screen instanceof HandledScreen<?> hs) {
            ScreenHandler handler = hs.getScreenHandler();
            if (hs.getTitle().getString().isBlank() && handler instanceof GenericContainerScreenHandler g
                    && loadingContents(g)) {
                return new JobRunner.View(JobRunner.ScreenKind.LOADING, System.identityHashCode(g), List.of(),
                        inventory, cursorEmpty);
            }
            if (state.station() != null) {
                List<JobRunner.SlotView> slots = stationSlots(handler);
                int id = System.identityHashCode(handler) * 31 + slots.hashCode();
                return new JobRunner.View(JobRunner.ScreenKind.STATION, id, slots, inventory, cursorEmpty);
            }
        }
        return new JobRunner.View(JobRunner.ScreenKind.OTHER, System.identityHashCode(screen), List.of(), inventory,
                cursorEmpty);
    }

    /** Number of container slots of a screen, without the player inventory (the station layout signature). */
    static int containerSize(ScreenHandler handler) {
        int n = 0;
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory)) {
                n++;
            }
        }
        return n;
    }

    /** Slots of the station (any container layout), without the player inventory; slot = handler slot id. */
    public static List<JobRunner.SlotView> stationSlots(ScreenHandler handler) {
        List<JobRunner.SlotView> slots = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                slots.add(new JobRunner.SlotView(slot.id, stack.getName().getString(), loreOf(stack)));
            }
        }
        return slots;
    }

    /** Ottonien's 9-slot loading menu: empty or one nameless structure void (see the agent's GuiSession). */
    private static boolean loadingContents(GenericContainerScreenHandler g) {
        var contents = g.getInventory();
        if (contents.isEmpty()) {
            return true;
        }
        if (contents.size() != 9) {
            return false;
        }
        int placeholders = 0;
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack stack = contents.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.isOf(Items.STRUCTURE_VOID) || stack.getCount() != 1 || !stack.getName().getString().isEmpty()
                    || ++placeholders > 1) {
                return false;
            }
        }
        return true;
    }

    public static List<String> loreOf(ItemStack stack) {
        List<String> out = new ArrayList<>();
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                out.add(line.getString());
            }
        }
        return out;
    }

    /** Player inventory (main and hotbar) by normalised display name. */
    public static Map<String, Integer> inventory(MinecraftClient client) {
        Map<String, Integer> counts = new HashMap<>();
        if (client.player == null) {
            return counts;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                counts.merge(WerkstattText.normalize(stack.getName().getString()), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    private static boolean inventoryFull(MinecraftClient client) {
        var inv = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Icon of a job: its item with the server's item model, if any. */
    public static ItemStack icon(WerkstattJob job) {
        try {
            Identifier id = Identifier.tryParse(job.itemId);
            ItemStack stack = new ItemStack(id == null ? Items.BARRIER : Registries.ITEM.get(id));
            Identifier model = job.itemModel == null || job.itemModel.isEmpty() ? null : Identifier.tryParse(job.itemModel);
            if (model != null) {
                stack.set(DataComponentTypes.ITEM_MODEL, model);
            }
            return stack;
        } catch (Throwable t) {
            return new ItemStack(Items.BARRIER);
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
