package de.ottoextra.ailments;

import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Debug tracing: finds out where the server shows a player's ailments (Leiden) so that a later feature
 * can read them. Looks at every opened container screen and, without any GUI, at the player's own
 * inventory. Local logging only (logger "ottoextra/leiden"); no API calls, no GUI is opened.
 */
public final class AilmentTrace {

    static final Logger LOG = LoggerFactory.getLogger("ottoextra/leiden");

    private static final int SETTLE_TICKS = 5;
    private static final int SCAN_INTERVAL_TICKS = 200;
    private static final int JOIN_SCAN_DELAY_TICKS = 60;
    private static final int MAX_DUMP_KEYS = 64;

    /** One matching item found somewhere. */
    private record Found(String location, AilmentInfo info, String itemId, int count) {
        String key() {
            return info.kind() + "@" + location;
        }
    }

    // Own inventory scan.
    private static int ticksSinceJoin = -1;
    private static Map<String, Found> ownFound = new LinkedHashMap<>();

    // Screen tracing.
    private static WeakReference<Screen> currentScreen = new WeakReference<>(null);
    private static int ticksOpen;
    private static int lastContentHash;
    private static boolean screenReported;
    private static final Set<String> dumpedKeys = new LinkedHashSet<>();

    // Where each ailment was seen last: kind -> description.
    private static final Map<String, String> lastSeen = new TreeMap<>();
    private static List<String> lastScreenDump = List.of();

    private AilmentTrace() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AilmentTrace::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetSession(true));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetSession(false));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("leiden")
                                .then(ClientCommandManager.literal("debug")
                                        .executes(context -> runDebugCommand(context.getSource())))
                                .then(ClientCommandManager.literal("check")
                                        .executes(context -> runCheckCommand(context.getSource()))))));
    }

    /** Info line under the logger ottoextra/leiden if the trace setting is on (used by the check scheduler). */
    static void trace(String format, Object... args) {
        if (enabled()) LOG.info(format, args);
    }

    private static boolean enabled() {
        OttoExtraConfig config = OttoExtraConfig.active();
        return config.debug == null || config.debug.ailmentTrace;
    }

    private static void resetSession(boolean joined) {
        ticksSinceJoin = joined ? 0 : -1;
        ownFound = new LinkedHashMap<>();
        currentScreen = new WeakReference<>(null);
        dumpedKeys.clear();
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || !enabled()) return;
        try {
            tickOwnInventory(client.player);
            tickScreen(client);
        } catch (RuntimeException e) {
            LOG.warn("Leiden-Trace fehlgeschlagen", e);
        }
    }

    // ---- own inventory ----

    private static void tickOwnInventory(ClientPlayerEntity player) {
        if (ticksSinceJoin < 0) ticksSinceJoin = 0; // mod loaded while already in a world
        ticksSinceJoin++;
        boolean first = ticksSinceJoin == JOIN_SCAN_DELAY_TICKS;
        if (!first && (ticksSinceJoin < JOIN_SCAN_DELAY_TICKS
                || (ticksSinceJoin - JOIN_SCAN_DELAY_TICKS) % SCAN_INTERVAL_TICKS != 0)) {
            return;
        }
        Map<String, Found> now = scanOwn(player);
        for (Found f : now.values()) {
            if (!ownFound.containsKey(f.key())) {
                LOG.info("own inventory: + {} at {} ({}x{}) effects={}", f.info().name(), f.location(),
                        f.count(), f.itemId(), f.info().effects());
            }
            lastSeen.put(f.info().kind(), "eigenes Inventar " + f.location());
        }
        for (Found f : ownFound.values()) {
            if (!now.containsKey(f.key())) {
                LOG.info("own inventory: - {} at {}", f.info().name(), f.location());
            }
        }
        if (first && now.isEmpty()) LOG.info("own inventory: no Leiden or Zustand items found (initial scan)");
        ownFound = now;
    }

    private static Map<String, Found> scanOwn(ClientPlayerEntity player) {
        Map<String, Found> out = new LinkedHashMap<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            add(out, "inventory[" + i + "]", inv.getStack(i));
        }
        // Crafting grid and result of the player screen handler; its other slots mirror the inventory.
        ScreenHandler psh = player.playerScreenHandler;
        for (int i = 0; i < psh.slots.size(); i++) {
            Slot slot = psh.slots.get(i);
            if (slot.inventory == inv) continue;
            add(out, "playerScreenHandler[" + i + "]", slot.getStack());
        }
        return out;
    }

    private static void add(Map<String, Found> out, String location, ItemStack stack) {
        Found f = describe(location, stack);
        if (f != null) out.put(f.key(), f);
    }

    private static Found describe(String location, ItemStack stack) {
        if (stack.isEmpty()) return null;
        var info = AilmentInfo.parse(stack.getName().getString(), loreOf(stack));
        return info.map(i -> new Found(location, i, String.valueOf(Registries.ITEM.getId(stack.getItem())),
                stack.getCount())).orElse(null);
    }

    private static List<String> loreOf(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return List.of();
        List<String> out = new ArrayList<>();
        for (Text line : lore.lines()) out.add(line.getString());
        return out;
    }

    // ---- screens ----

    private static void tickScreen(MinecraftClient client) {
        Screen screen = client.currentScreen;
        if (screen == null) {
            currentScreen = new WeakReference<>(null);
            return;
        }
        if (currentScreen.get() != screen) {
            currentScreen = new WeakReference<>(screen);
            ticksOpen = 0;
            lastContentHash = 0;
            screenReported = false;
        }
        if (!(screen instanceof HandledScreen<?> hs)) return;
        if (++ticksOpen < SETTLE_TICKS) return;
        ScreenHandler sh = hs.getScreenHandler();
        int hash = 1;
        for (Slot slot : sh.slots) {
            ItemStack st = slot.getStack();
            hash = 31 * hash + (st.isEmpty() ? 0 : ItemStack.hashCode(st) * 31 + st.getCount());
        }
        if (hash == lastContentHash && screenReported) return;
        lastContentHash = hash;
        dumpScreen(client, hs, false);
    }

    /** @param force log even if an identical dump was already written */
    private static List<String> dumpScreen(MinecraftClient client, HandledScreen<?> hs, boolean force) {
        ScreenHandler sh = hs.getScreenHandler();
        String title = AilmentNames.plain(hs.getTitle().getString());
        String header = "screen " + hs.getClass().getName() + " title='" + title + "' handler="
                + handlerId(sh) + " slots=" + sh.slots.size();
        List<String> lines = new ArrayList<>();
        int contentHash = 1;
        for (int i = 0; i < sh.slots.size(); i++) {
            Slot slot = sh.slots.get(i);
            String owner = slot.inventory == client.player.getInventory() ? "player-inventory" : "container";
            Found f = describe("slot " + i + " (" + owner + ", index " + slot.getIndex() + ")", slot.getStack());
            if (f == null) continue;
            AilmentInfo info = f.info();
            lines.add("  " + f.location() + ": " + f.count() + "x" + f.itemId() + " name='" + info.name()
                    + "' lore=" + info.lore() + " effects=" + info.effects());
            lastSeen.put(info.kind(), "Fenster '" + title + "' " + f.location());
            contentHash = 31 * contentHash + f.key().hashCode() + info.lore().hashCode();
        }
        boolean firstReport = !screenReported;
        screenReported = true;
        String dumpKey = hs.getClass().getName() + "|" + title + "|" + contentHash;
        if (!force && !dumpedKeys.add(dumpKey)) return lines;
        if (dumpedKeys.size() > MAX_DUMP_KEYS) dumpedKeys.remove(dumpedKeys.iterator().next());
        if (lines.isEmpty() && !force && !firstReport) return lines;
        LOG.info("{} matches={}", header, lines.size());
        for (String line : lines) LOG.info("{}", line);
        if (!lines.isEmpty()) lastScreenDump = new ArrayList<>(List.of(header));
        if (!lines.isEmpty()) lastScreenDump.addAll(lines);
        return lines;
    }

    private static String handlerId(ScreenHandler sh) {
        try {
            return String.valueOf(Registries.SCREEN_HANDLER.getId(sh.getType()));
        } catch (UnsupportedOperationException e) {
            return "player"; // the player inventory handler has no type
        }
    }

    // ---- command ----

    private static int runCheckCommand(FabricClientCommandSource source) {
        String problem = ConditionsModule.requestManualCheck();
        if (problem != null) {
            source.sendFeedback(Text.literal("§c[OttoExtra] " + problem));
            return 0;
        }
        trace("manual check requested");
        source.sendFeedback(Text.literal("§a[OttoExtra] §7Prüfe das Menü ..."));
        return 1;
    }

    private static int runDebugCommand(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            source.sendFeedback(Text.literal("§c[OttoExtra] Nicht im Spiel."));
            return 0;
        }
        // Full dump to the log, independent of the setting (explicit request).
        Map<String, Found> own = scanOwn(player);
        LOG.info("debug command: own inventory scan, {} match(es)", own.size());
        for (Found f : own.values()) {
            LOG.info("  {}: {}x{} name='{}' lore={} effects={}", f.location(), f.count(), f.itemId(),
                    f.info().name(), f.info().lore(), f.info().effects());
            lastSeen.put(f.info().kind(), "eigenes Inventar " + f.location());
        }
        if (client.currentScreen instanceof HandledScreen<?> hs) {
            dumpScreen(client, hs, true);
        } else if (!lastScreenDump.isEmpty()) {
            LOG.info("debug command: last screen dump");
            for (String line : lastScreenDump) LOG.info("{}", line);
        }
        LOG.info("debug command: lastSeen={}", lastSeen);

        source.sendFeedback(Text.literal("§a[OttoExtra] §7Leiden-Trace: " + (enabled() ? "§aAN" : "§cAUS")));
        if (lastSeen.isEmpty()) {
            source.sendFeedback(Text.literal("§7Noch nichts gesehen: nie (weder Inventar noch Fenster)."));
        } else {
            for (Map.Entry<String, String> e : lastSeen.entrySet()) {
                source.sendFeedback(Text.literal("§7" + e.getKey() + ": " + e.getValue()));
            }
        }
        long unseen = AilmentNames.ALL.stream().filter(n -> !lastSeen.containsKey(n)).count();
        source.sendFeedback(Text.literal("§7Nie gesehen: " + unseen + " von " + AilmentNames.ALL.size()
                + " Leiden. Details: latest.log, Logger ottoextra/leiden."));
        return 1;
    }
}
