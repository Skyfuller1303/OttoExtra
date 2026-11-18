package de.ottoextra.ailments;

import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.privacy.BoundedBody;
import de.ottoextra.privacy.ConsentHttp;
import de.ottoextra.privacy.ConsentPermit;
import de.ottoextra.privacy.ConsentPolicy;
import de.ottoextra.verify.AccountLinkCredentials;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reads the player's conditions from the server menu (passively whenever it is opened, and by a
 * brief automatic check), keeps them for the HUD and name tags, and shares them if the player
 * chose to. Protocol: docs/zustand-protokoll.md of the server. Log: logger "ottoextra/zustand".
 */
public final class ConditionsModule implements OttoExtraModule {

    static final Logger LOG = LoggerFactory.getLogger("ottoextra/zustand");

    private static final long JOIN_DELAY_MS = 10_000L;
    private static final long RETRY_MS = 60_000L;
    private static final int SETTLE_TICKS = 4;
    private static final long CHECK_TIMEOUT_MS = 3_000L;
    private static final long EXPECT_MENU_MS = 3_000L;
    private static final long TOKEN_CACHE_MS = 30_000L;
    private static final long UNAUTHORIZED_BACKOFF_MS = 5 * 60_000L;

    private enum Phase { IDLE, WAITING }

    /** The one module instance; the network mixin reaches it through the static hooks below. */
    private static volatile ConditionsModule instance;

    private OttoExtraContext context;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final SharePlanner planner = new SharePlanner();

    // Menu reading.
    private WeakReference<Screen> menuScreen = new WeakReference<>(null);
    private int menuTicks;
    private int menuHash;
    private int appliedHash;
    private long expectMenuUntil;

    // Automatic check.
    private Phase phase = Phase.IDLE;
    private long nextCheckAt;
    private long checkStartedAt;
    private int previousSlot = -1;
    private boolean checkRequestedClose;

    // Hidden check (menu read without showing a screen).
    private final HiddenCheckState hidden = new HiddenCheckState();
    private ScreenHandler hiddenHandler;
    private String hiddenTitle = "";
    private int hiddenTicks;

    // Combat tracking.
    private float lastHealth = -1f;
    private long lastDamageAt = Long.MIN_VALUE / 2;
    private long lastAttackAt = Long.MIN_VALUE / 2;

    // Sharing.
    private volatile boolean inFlight;
    private volatile boolean queryInFlight;
    private volatile long tokenBlockedUntil;
    private AccountLinkCredentials cachedCredentials;
    private long credentialsAt;

    @Override
    public String id() {
        return "conditions";
    }

    @Override
    public void onInitializeClient(OttoExtraContext ctx) {
        this.context = ctx;
        instance = this;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && MenuReader.isOpenMessage(message.getString())) {
                expectMenuUntil = System.currentTimeMillis() + EXPECT_MENU_MS;
            }
        });
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            try {
                ConditionsHud.render(drawContext, cfg());
            } catch (Throwable ignored) {
                // the HUD must never break the frame
            }
        });
    }

    @Override
    public void onServerJoin(OttoExtraContext ctx) {
        reset();
        joinSeen = true;
        AilmentTrace.trace("check scheduler: join seen (onOttonien={}, hidden={}, autoCheck={})",
                ctx.isOnOttonien(), cfg().hiddenCheck, cfg().autoCheck);
        schedule(System.currentTimeMillis(), JOIN_DELAY_MS, "after join");
    }

    @Override
    public void onDisconnect(OttoExtraContext ctx) {
        reset();
        Conditions.clearAll();
    }

    private final ManualCheck manual = new ManualCheck();
    private long tickCount;
    private long armedTick = -1;
    private long lastInputAt = Long.MIN_VALUE / 2;
    private int menuSlotSelected = -1;
    private boolean joinSeen;

    private boolean onOttonien(MinecraftClient client) {
        return context.isOnOttonien() || de.ottoextra.OttoExtraGate.isOnOttonien(client);
    }

    /** Sets the next check time and says so in the trace. */
    private void schedule(long now, long delayMs, String why) {
        nextCheckAt = now + delayMs;
        AilmentTrace.trace("check scheduler: next check in {} s ({})", delayMs / 1000, why);
    }

    /** {@code /ottoextra leiden check}: one check at the next tick, ignoring interval and setting. */
    public static String requestManualCheck() {
        ConditionsModule m = instance;
        if (m == null) return "Zustandsmodul nicht aktiv.";
        if (m.hidden.active() || m.phase != Phase.IDLE || !m.manual.request()) return "Eine Prüfung läuft bereits.";
        return null;
    }

    private void report(MinecraftClient client, String message) {
        manual.done();
        AilmentTrace.trace("manual check result: {}", message);
        if (client.player != null) client.player.sendMessage(Text.literal("§a[OttoExtra] §7" + message), false);
    }

    private static String describe(List<Condition> conditions) {
        if (conditions.isEmpty()) return "Keine Zustände gelesen.";
        return conditions.size() + (conditions.size() == 1 ? " Zustand" : " Zustände") + " gelesen: "
                + String.join(", ", conditions.stream().map(Condition::name).toList());
    }

    private void reset() {
        hidden.finish(System.currentTimeMillis());
        hiddenHandler = null;
        phase = Phase.IDLE;
        previousSlot = -1;
        menuScreen = new WeakReference<>(null);
        planner.reset();
        nextCheckAt = 0L;
        joinSeen = false;
        manual.reset();
        armedTick = -1;
        menuSlotSelected = -1;
    }

    private OttoExtraConfig.Conditions cfg() {
        OttoExtraConfig config = OttoExtraConfig.active();
        if (config.conditions == null) config.conditions = new OttoExtraConfig.Conditions();
        return config.conditions;
    }

    // ---- tick ----

    private void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        long now = System.currentTimeMillis();
        tickCount++;
        try {
            tickShareCleanup(now);
        } catch (RuntimeException e) {
            LOG.warn("share cleanup failed", e);
        }
        if (player == null || context == null) return;
        if (userInputNow(client)) lastInputAt = now;
        if (!onOttonien(client)) {
            if (manual.any()) report(client, "Nicht auf Ottonien: keine Prüfung.");
            return;
        }
        if (!joinSeen && nextCheckAt == 0L) {
            // The join event did not reach us (mod loaded late, or the server entry was unknown then).
            joinSeen = true;
            AilmentTrace.trace("check scheduler: no join event seen, scheduling from the first tick on Ottonien");
            schedule(now, JOIN_DELAY_MS, "first tick on Ottonien");
        }
        try {
            trackCombat(client, player, now);
            if (hidden.active()) tickHidden(client, player, now);
            else tickMenu(client, player, now);
            if (phase == Phase.IDLE && !hidden.active()) tickAutoCheck(client, player, now);
            tickSharing(client, player, now);
        } catch (RuntimeException e) {
            LOG.warn("Zustands-Modul fehlgeschlagen", e);
            abortCheck(client, player);
            if (hidden.active()) endHidden(client, player, now, true, "error");
            if (manual.any()) report(client, "Prüfung fehlgeschlagen (siehe Log).");
        }
    }

    /** Use, inventory or attack key pressed, or any screen open: the user is doing something. */
    private static boolean userInputNow(MinecraftClient client) {
        var o = client.options;
        return client.currentScreen != null || o.inventoryKey.isPressed() || o.useKey.isPressed()
                || o.attackKey.isPressed();
    }

    private void trackCombat(MinecraftClient client, ClientPlayerEntity player, long now) {
        float health = player.getHealth();
        if (lastHealth >= 0 && health < lastHealth) lastDamageAt = now;
        lastHealth = health;
        // Only a held attack key while playing counts as attacking. handSwinging is also true for
        // mining, item use (including our own Menü-item use) and server-side swing animations, and
        // blocked every check.
        if (client.currentScreen == null && client.options.attackKey.isPressed()) lastAttackAt = now;
    }

    // ---- reading ----

    private static List<MenuReader.SlotView> views(ScreenHandler handler, ClientPlayerEntity player) {
        List<MenuReader.SlotView> out = new ArrayList<>();
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            boolean container = slot.inventory != player.getInventory();
            if (stack.isEmpty()) {
                out.add(new MenuReader.SlotView(i, container, "", "", List.of()));
                continue;
            }
            out.add(new MenuReader.SlotView(i, container, String.valueOf(Registries.ITEM.getId(stack.getItem())),
                    stack.getName().getString(), loreOf(stack)));
        }
        return out;
    }

    private static List<String> loreOf(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return List.of();
        List<String> out = new ArrayList<>();
        for (Text line : lore.lines()) out.add(line.getString());
        return out;
    }

    private static String handlerId(ScreenHandler handler) {
        try {
            return String.valueOf(Registries.SCREEN_HANDLER.getId(handler.getType()));
        } catch (UnsupportedOperationException e) {
            return "player";
        }
    }

    private static boolean isMenuContainer(ScreenHandler handler) {
        return handler instanceof GenericContainerScreenHandler g && g.getRows() == 6;
    }

    /** Reads the menu while it is open: passive for any opening, and the target of the visible auto check. */
    private void tickMenu(MinecraftClient client, ClientPlayerEntity player, long now) {
        Screen screen = client.currentScreen;
        boolean waitingTimeout = phase == Phase.WAITING && now - checkStartedAt > CHECK_TIMEOUT_MS;
        // Cheap type check first: the player inventory and other screens never need a slot list.
        if (!(screen instanceof HandledScreen<?> hs) || !isMenuContainer(hs.getScreenHandler())) {
            menuScreen = new WeakReference<>(null);
            if (waitingTimeout) {
                LOG.info("auto check: menu did not open within {} ms", CHECK_TIMEOUT_MS);
                finishCheck(client, player, now, false);
                if (manual.running()) report(client, "Zeitüberschreitung: das Menü hat nicht geantwortet.");
            }
            return;
        }
        ScreenHandler handler = hs.getScreenHandler();
        List<MenuReader.SlotView> views = views(handler, player);
        if (!MenuReader.isMenu(MenuReader.MENU_HANDLER, hs.getTitle().getString(), views, now < expectMenuUntil)) {
            if (waitingTimeout) {
                LOG.info("auto check: another screen opened ({}), leaving it open", screen.getClass().getSimpleName());
                finishCheck(client, player, now, false); // not the menu: never close a foreign window
                if (manual.running()) report(client, "Prüfung abgebrochen: anderes Fenster geöffnet.");
            }
            return;
        }
        if (menuScreen.get() != screen) {
            menuScreen = new WeakReference<>(screen);
            menuTicks = 0;
            menuHash = 0;
            appliedHash = 0;
        }
        List<Condition> conditions = MenuReader.readConditions(views);
        int hash = conditions.hashCode();
        if (hash != menuHash) {
            menuHash = hash; // contents still arriving: settle again
            menuTicks = 0;
        } else {
            menuTicks++;
        }
        if (menuTicks >= SETTLE_TICKS && (hash != appliedHash || phase == Phase.WAITING)) {
            appliedHash = hash;
            boolean viaCheck = phase == Phase.WAITING;
            apply(player, conditions, now, viaCheck ? "auto check" : "menu opened");
            if (viaCheck) {
                finishCheck(client, player, now, true);
                if (manual.running()) report(client, describe(conditions));
            }
        }
        if (phase == Phase.WAITING && now - checkStartedAt > CHECK_TIMEOUT_MS) {
            finishCheck(client, player, now, true);
            if (manual.running()) report(client, "Zeitüberschreitung: das Menü hat nicht geantwortet.");
        }
    }

    private void apply(ClientPlayerEntity player, List<Condition> conditions, long now, String source) {
        boolean changed = Conditions.setOwn(player.getUuid(), conditions, now);
        if (changed) {
            LOG.info("{}: {} condition(s): {}", source, conditions.size(), conditions);
        } else {
            LOG.debug("{}: unchanged ({} condition(s))", source, conditions.size());
        }
    }

    // ---- automatic check ----

    private void tickAutoCheck(MinecraftClient client, ClientPlayerEntity player, long now) {
        OttoExtraConfig.Conditions c = cfg();
        boolean isManual = manual.pending();
        int[] found = {-1};
        CheckScheduler.Decision d = CheckScheduler.decide(isManual, c.autoCheck, now, nextCheckAt,
                () -> AutoCheckGuard.blockReason(inputs(client, player, now)),
                () -> (found[0] = findMenuSlot(player)) >= 0);
        if (d.loggable()) AilmentTrace.trace("check scheduler: {} ({}){}", d.kind(), d.reason(), isManual ? " [manual]" : "");
        switch (d.kind()) {
            case BLOCKED, NO_MENU_ITEM -> {
                if (isManual) report(client, "Prüfung nicht möglich: " + d.reason());
                else schedule(now, RETRY_MS, "retry after " + d.reason());
                return;
            }
            case GO -> manual.started();
            default -> {
                return;
            }
        }
        int slot = found[0];
        previousSlot = player.getInventory().getSelectedSlot();
        menuSlotSelected = slot;
        player.getInventory().setSelectedSlot(slot);
        if (cfg().hiddenCheck) {
            // Switch, use and switch back within this tick: the hotbar never shows the book selected, and
            // the held item tooltip (compared once per tick) does not fire.
            hidden.arm(now);
            armedTick = tickCount;
            hiddenHandler = null;
            hiddenTicks = 0;
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            restoreSlot(player);
            AilmentTrace.LOG.info("hidden check: armed, used Menü item from hotbar slot {} (switched back in the same tick)", slot);
            AilmentTrace.trace("check scheduler: sent use of Menü item (hidden mode)");
            return;
        }
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        phase = Phase.WAITING;
        checkStartedAt = now;
        LOG.info("auto check: using Menü item in hotbar slot {}", slot);
        AilmentTrace.trace("check scheduler: sent use of Menü item (visible mode), slot {}", slot);
    }

    private AutoCheckGuard.Inputs inputs(MinecraftClient client, ClientPlayerEntity player, long now) {
        var o = client.options;
        boolean keys = o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed()
                || o.rightKey.isPressed() || o.jumpKey.isPressed();
        var v = player.getVelocity();
        boolean moving = keys || v.x * v.x + v.z * v.z > 1.0e-4 || !player.isOnGround();
        boolean werkstatt;
        try {
            var runner = de.ottoextra.crafting.werkstatt.Werkstatt.runner();
            werkstatt = runner != null && runner.active();
        } catch (Throwable t) {
            werkstatt = false;
        }
        return new AutoCheckGuard.Inputs(true, onOttonien(client),
                de.ottoextra.rpnames.RpNamesModule.isCombatBossBarActive(client), now - lastDamageAt,
                now - lastAttackAt, client.currentScreen != null, werkstatt, moving, player.isSprinting(),
                player.getAbilities().flying || player.isGliding(), player.hasVehicle(), client.isWindowFocused(),
                o.inventoryKey.isPressed() || o.useKey.isPressed(),
                player.fishHook != null || player.isUsingItem(), itemCooldown(player));
    }

    /** The selected item or the menu book is cooling down. */
    private static boolean itemCooldown(ClientPlayerEntity player) {
        var cooldowns = player.getItemCooldownManager();
        if (cooldowns.isCoolingDown(player.getMainHandStack())) return true;
        int slot = findMenuSlot(player);
        return slot >= 0 && cooldowns.isCoolingDown(player.getInventory().getStack(slot));
    }

    // ---- hidden check ----

    /** Called by the network mixin for every open-screen packet; true = taken over, no screen. */
    public static boolean interceptOpenScreen(int syncId, ScreenHandlerType<?> type, Text title) {
        ConditionsModule m = instance;
        if (m == null) return false;
        try {
            return m.takeOver(syncId, type, title);
        } catch (RuntimeException e) {
            AilmentTrace.LOG.warn("hidden check: take-over failed, passing the packet through", e);
            return false;
        }
    }

    /** Called by the network mixin for chat lines; true = hide it. */
    public static boolean suppressChat(Text content) {
        ConditionsModule m = instance;
        if (m == null) return false;
        boolean hide = m.hidden.shouldSuppressChat(System.currentTimeMillis(), content.getString());
        if (hide) AilmentTrace.LOG.info("hidden check: suppressed chat line '{}'", AilmentNames.plain(content.getString()));
        return hide;
    }

    private boolean takeOver(int syncId, ScreenHandlerType<?> type, Text title) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || type == null) return false;
        long now = System.currentTimeMillis();
        String handlerId = String.valueOf(Registries.SCREEN_HANDLER.getId(type));
        String plainTitle = title == null ? "" : title.getString();
        if (!hidden.active()) {
            // The check was given up, but its menu may still arrive: take it over and close it at once.
            boolean inputSince = lastInputAt >= hidden.lateStart() || userInputNow(client);
            if (!hidden.shouldInterceptLate(now, handlerId, plainTitle, inputSince, client.currentScreen != null)) {
                return false;
            }
            hidden.lateConsumed();
            if (player.networkHandler != null) player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
            AilmentTrace.LOG.info("hidden check: late menu packet syncId={} taken over and closed again", syncId);
            return true;
        }
        if (!hidden.shouldIntercept(now, handlerId, plainTitle)) {
            AilmentTrace.LOG.info("hidden check: open-screen {} '{}' is not the menu, passed through", handlerId, plainTitle);
            return false;
        }
        ScreenHandler handler = type.create(syncId, player.getInventory());
        player.currentScreenHandler = handler;
        hiddenHandler = handler;
        hiddenTitle = plainTitle;
        hiddenTicks = 0;
        hidden.intercepted(syncId);
        AilmentTrace.LOG.info("hidden check: took over open-screen syncId={} {} '{}', no screen shown",
                syncId, handlerId, plainTitle);
        return true;
    }

    private void tickHidden(MinecraftClient client, ClientPlayerEntity player, long now) {
        boolean replaced = hiddenHandler != null && player.currentScreenHandler != hiddenHandler;
        boolean ready = false;
        List<MenuReader.SlotView> views = null;
        if (hiddenHandler != null && !replaced) {
            hiddenTicks++;
            views = views(hiddenHandler, player);
            ready = hiddenTicks >= SETTLE_TICKS
                    && MenuReader.isMenu(MenuReader.MENU_HANDLER, hiddenTitle, views, true);
        }
        boolean userInput = userInputNow(client);
        boolean screenOpen = client.currentScreen != null;
        boolean combat = now - lastDamageAt < AutoCheckGuard.COMBAT_COOLDOWN_MS;
        HiddenCheckState.Step step = hidden.step(now, userInput || combat, replaced, ready);
        // Close only if nothing else may have opened; after user input just drop the handler locally.
        boolean mayClose = HiddenCheckState.closeAllowed(armedTick == tickCount, userInput, screenOpen);
        switch (step) {
            case READ -> {
                List<Condition> conditions = MenuReader.readConditions(views);
                AilmentTrace.LOG.info("hidden check: read {} condition(s) from slots {}-{}: {}", conditions.size(),
                        MenuReader.FIRST_CONDITION_SLOT, MenuReader.LAST_CONDITION_SLOT, conditions);
                apply(player, conditions, now, "hidden check");
                endHidden(client, player, now, mayClose, "read");
                if (manual.running()) report(client, describe(conditions));
            }
            case TIMEOUT -> {
                endHidden(client, player, now, mayClose, "timeout");
                if (manual.running()) report(client, "Zeitüberschreitung: das Menü hat nicht geantwortet.");
            }
            case ABORT -> {
                endHidden(client, player, now, false, "aborted (screen/input/combat/handler change)");
                if (manual.running()) report(client, "Prüfung abgebrochen (Fenster, Eingabe, Kampf oder anderes Menü).");
            }
            default -> { }
        }
    }

    private void endHidden(MinecraftClient client, ClientPlayerEntity player, long now, boolean closeHandler,
                           String why) {
        int syncId = hidden.syncId();
        boolean send = hiddenHandler != null && closeHandler && syncId >= 0 && player.networkHandler != null;
        if (send) player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
        if (hiddenHandler != null && player.currentScreenHandler == hiddenHandler) {
            player.currentScreenHandler = player.playerScreenHandler;
        }
        AilmentTrace.LOG.info("hidden check: finished ({}), syncId={}, close packet {}", why, syncId,
                send ? "sent" : "not sent (dropped locally)");
        hiddenHandler = null;
        boolean aborted = why.startsWith("aborted") || why.equals("error") || why.equals("timeout");
        hidden.finish(now, aborted); // a menu packet that has not arrived yet is still intercepted
        schedule(now, aborted ? RETRY_MS : Math.max(1, Math.min(60, cfg().intervalMinutes)) * 60_000L,
                "after hidden check: " + why);
    }

    private static int findMenuSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (MenuReader.isMenuItem(String.valueOf(Registries.ITEM.getId(stack.getItem())),
                    stack.getName().getString(), loreOf(stack))) {
                return i;
            }
        }
        return -1;
    }

    /** Closes the open screen only if it really is the menu; a foreign window stays untouched. */
    private void closeMenuScreen(MinecraftClient client, ClientPlayerEntity player) {
        if (!(client.currentScreen instanceof HandledScreen<?> hs) || !isMenuContainer(hs.getScreenHandler())) return;
        if (MenuReader.isMenu(MenuReader.MENU_HANDLER, hs.getTitle().getString(),
                views(hs.getScreenHandler(), player), true)) {
            player.closeHandledScreen();
        }
    }

    private void finishCheck(MinecraftClient client, ClientPlayerEntity player, long now, boolean closeMenu) {
        if (closeMenu) closeMenuScreen(client, player);
        restoreSlot(player);
        phase = Phase.IDLE;
        schedule(now, Math.max(1, Math.min(60, cfg().intervalMinutes)) * 60_000L, "after visible check");
    }

    private void abortCheck(MinecraftClient client, ClientPlayerEntity player) {
        if (phase != Phase.WAITING) return;
        closeMenuScreen(client, player);
        restoreSlot(player);
        phase = Phase.IDLE;
        nextCheckAt = System.currentTimeMillis() + RETRY_MS;
    }

    /** Puts the old hotbar slot back, unless the player has switched slots in the meantime. */
    private void restoreSlot(ClientPlayerEntity player) {
        if (previousSlot >= 0 && player.getInventory().getSelectedSlot() == menuSlotSelected) {
            player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
        menuSlotSelected = -1;
    }

    // ---- sharing ----

    private AccountLinkCredentials credentials(long now) {
        if (cachedCredentials == null || now - credentialsAt > TOKEN_CACHE_MS) {
            cachedCredentials = AccountLinkCredentials.load();
            credentialsAt = now;
        }
        return cachedCredentials;
    }

    private void tickSharing(MinecraftClient client, ClientPlayerEntity player, long now) {
        OttoExtraConfig config = OttoExtraConfig.active();
        OttoExtraConfig.Conditions c = cfg();
        boolean online = ConsentPolicy.allowed(config, ConsentPolicy.Purpose.ONLINE_SERVICES);
        boolean usable = online && credentials(now).linked() && now >= tokenBlockedUntil
                && ConditionEndpoint.base(c.baseUrl).isPresent();
        boolean active = c.share && usable;
        if (!inFlight && planner.next(now, active, Conditions.own()) == SharePlanner.Action.PUT) {
            put(config, c, Conditions.own(), now);
        }
        if (c.showOthers && usable && !queryInFlight && planner.queryDue(now)) query(client, player, config, c, now);
    }

    /**
     * Off means delete: while a copy may exist on the server and sharing is switched off or consent is gone,
     * send DELETE whenever a token is available, on any server and even in the main menu.
     */
    private void tickShareCleanup(long now) {
        OttoExtraConfig config = OttoExtraConfig.active();
        OttoExtraConfig.Conditions c = cfg();
        boolean consent = ConsentPolicy.allowed(config, ConsentPolicy.Purpose.ONLINE_SERVICES);
        if (inFlight || !planner.deleteDue(now, c.serverCopy, c.share, consent)) return;
        if (now < tokenBlockedUntil || !credentials(now).linked() || ConditionEndpoint.base(c.baseUrl).isEmpty()) return;
        planner.deleteAttempted(now);
        delete(config, c, now);
    }

    private HttpRequest.Builder request(OttoExtraConfig.Conditions c, String path) {
        URI uri = ConditionEndpoint.resolve(c.baseUrl, path).orElseThrow();
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "OttoExtra")
                .header("Authorization", "Bearer " + credentials(System.currentTimeMillis()).deviceToken());
    }

    private void put(OttoExtraConfig config, OttoExtraConfig.Conditions c, List<Condition> list, long now) {
        inFlight = true;
        long observedAt = Conditions.ownReadAt();
        HttpRequest req = request(c, "/api/v1/conditions/me").header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(ConditionWire.putBody(list, observedAt))).build();
        send(config, req, "PUT", status -> {
            if (status / 100 == 2) {
                planner.wrote(System.currentTimeMillis(), list);
                setServerCopy(c, true);
                LOG.info("share: PUT ok ({} condition(s))", list.size());
            } else {
                fail("PUT", status);
            }
        });
    }

    private void delete(OttoExtraConfig config, OttoExtraConfig.Conditions c, long now) {
        inFlight = true;
        HttpRequest req = request(c, "/api/v1/conditions/me").DELETE().build();
        // Removing data stays possible after consent was withdrawn (like unlinking the account).
        sendWith(new ConsentPermit.Gate().capture(() -> true), req, "DELETE", status -> {
            if (status / 100 == 2 || status == 401 || status == 404) {
                planner.deleted(System.currentTimeMillis());
                setServerCopy(c, false);
                LOG.info("share: DELETE done (HTTP {})", status);
            } else {
                fail("DELETE", status);
            }
        });
    }

    private void fail(String what, int status) {
        long now = System.currentTimeMillis();
        planner.failed(now);
        if (status == 401 || status == 403) tokenBlockedUntil = now + UNAUTHORIZED_BACKOFF_MS;
        LOG.info("share: {} failed, HTTP {}", what, status);
    }

    private void setServerCopy(OttoExtraConfig.Conditions c, boolean value) {
        if (c.serverCopy == value) return;
        c.serverCopy = value;
        OttoExtraConfig.active().save();
    }

    private void send(OttoExtraConfig config, HttpRequest req, String what, java.util.function.IntConsumer onStatus) {
        sendWith(ConsentPolicy.capture(config, ConsentPolicy.Purpose.ONLINE_SERVICES), req, what, onStatus);
    }

    private void sendWith(ConsentPermit permit, HttpRequest req, String what,
                          java.util.function.IntConsumer onStatus) {
        ConsentHttp.send(http, req, BoundedBody.text(64 * 1024), permit).whenComplete((response, error) ->
                MinecraftClient.getInstance().execute(() -> {
                    inFlight = false;
                    if (error != null) {
                        planner.failed(System.currentTimeMillis());
                        LOG.info("share: {} failed: {}", what, error.toString());
                        return;
                    }
                    onStatus.accept(response.statusCode());
                }));
    }

    private void query(MinecraftClient client, ClientPlayerEntity player, OttoExtraConfig config,
                       OttoExtraConfig.Conditions c, long now) {
        Set<UUID> uuids = new LinkedHashSet<>(Conditions.watched());
        if (client.world != null) {
            for (var p : client.world.getPlayers()) {
                if (!p.getUuid().equals(player.getUuid())) uuids.add(p.getUuid());
            }
        }
        uuids.remove(player.getUuid());
        planner.queried(now);
        if (uuids.isEmpty()) return;
        List<UUID> asked = new ArrayList<>(uuids);
        if (asked.size() > ConditionWire.MAX_UUIDS_PER_QUERY) {
            asked = asked.subList(0, ConditionWire.MAX_UUIDS_PER_QUERY);
        }
        List<UUID> queried = asked;
        queryInFlight = true;
        HttpRequest req = request(c, "/api/v1/conditions?uuids=" + ConditionWire.uuidQuery(queried)).GET().build();
        var permit = ConsentPolicy.capture(config, ConsentPolicy.Purpose.ONLINE_SERVICES);
        ConsentHttp.send(http, req, BoundedBody.text(512 * 1024), permit).whenComplete((response, error) ->
                MinecraftClient.getInstance().execute(() -> {
                    queryInFlight = false;
                    long done = System.currentTimeMillis();
                    if (error != null || response.statusCode() / 100 != 2) {
                        planner.queryFailed(done);
                        if (response != null && (response.statusCode() == 401 || response.statusCode() == 403)) {
                            tokenBlockedUntil = done + UNAUTHORIZED_BACKOFF_MS;
                        }
                        LOG.debug("query failed: {}", error != null ? error.toString() : response.statusCode());
                        return;
                    }
                    var players = ConditionWire.parseGet(response.body());
                    for (UUID uuid : queried) {
                        var remote = players.get(uuid);
                        if (remote == null) Conditions.removeRemote(uuid);
                        else Conditions.putRemote(uuid, remote.conditions(), remote.updatedAt());
                    }
                    LOG.debug("query: {} of {} player(s) share conditions", players.size(), queried.size());
                }));
    }
}
