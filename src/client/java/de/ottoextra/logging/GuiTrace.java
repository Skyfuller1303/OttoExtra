package de.ottoextra.logging;

import de.ottoextra.OttoExtra;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import com.mojang.serialization.JsonOps;

import java.lang.ref.WeakReference;

/**
 * Development aid: logs every screen that opens (class, title as JSON) and, for container screens,
 * the handler type and the non-empty slots with name, item, model and lore. Slot contents arrive
 * after the screen opens, so they are logged once they settle and again whenever they change.
 * Active only in the development environment; output goes to latest.log with prefix [gui-trace].
 */
public final class GuiTrace {

    private static final int SETTLE_TICKS = 5;

    private static WeakReference<Screen> current = new WeakReference<>(null);
    private static int ticksOpen;
    private static int lastHash;

    private GuiTrace() {
    }

    public static void register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (current.get() == screen) return; // resize re-init
            // Helper screens of other mods (e.g. ReplayMod) also fire AFTER_INIT; only trace the
            // screen that is actually shown.
            if (client.currentScreen != screen) return;
            current = new WeakReference<>(screen);
            ticksOpen = 0;
            lastHash = 0;
            String handler = "";
            if (screen instanceof HandledScreen<?> hs) {
                ScreenHandler sh = hs.getScreenHandler();
                handler = " handler=" + handlerId(sh) + " slots=" + sh.slots.size();
            }
            OttoExtra.LOGGER.info("[gui-trace] open {} title={}{}", screen.getClass().getName(),
                    json(screen.getTitle()), handler);
        });
        ClientTickEvents.END_CLIENT_TICK.register(GuiTrace::tick);
        // Action bar (overlay) messages, e.g. the server's crafting progress bar. Consecutive
        // identical messages are logged once.
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) return;
            String js = json(message);
            if (js.equals(lastOverlay)) return;
            lastOverlay = js;
            OttoExtra.LOGGER.info("[gui-trace] actionbar {}", js);
        });
    }

    private static String lastOverlay = "";

    private static void tick(MinecraftClient client) {
        Screen screen = client.currentScreen;
        if (screen == null || screen != current.get()) return;
        if (!(screen instanceof HandledScreen<?> hs)) return;
        if (++ticksOpen < SETTLE_TICKS) return;
        ScreenHandler sh = hs.getScreenHandler();
        int hash = 1;
        for (var slot : sh.slots) {
            ItemStack st = slot.getStack();
            hash = 31 * hash + (st.isEmpty() ? 0 : ItemStack.hashCode(st) * 31 + st.getCount());
        }
        if (hash == lastHash) return;
        lastHash = hash;
        OttoExtra.LOGGER.info("[gui-trace] contents {} ({} slots):", handlerId(sh), sh.slots.size());
        for (int i = 0; i < sh.slots.size(); i++) {
            ItemStack st = sh.slots.get(i).getStack();
            if (st.isEmpty()) continue;
            // Skip the player's own inventory in container screens; in the inventory screen itself
            // every slot is of interest (e.g. a server-provided head).
            if (!(screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)
                    && sh.slots.get(i).inventory == client.player.getInventory()) continue;
            var lore = st.get(DataComponentTypes.LORE);
            var profile = st.get(DataComponentTypes.PROFILE);
            OttoExtra.LOGGER.info("[gui-trace]  slot {}: {}x{} name={} model={} cmd={} lore={} profile={}", i,
                    st.getCount(), Registries.ITEM.getId(st.getItem()), json(st.getName()),
                    st.get(DataComponentTypes.ITEM_MODEL), st.get(DataComponentTypes.CUSTOM_MODEL_DATA),
                    lore == null ? "[]" : lore.lines().stream().map(GuiTrace::json).toList(), profile);
        }
    }

    private static String handlerId(ScreenHandler sh) {
        try {
            return String.valueOf(Registries.SCREEN_HANDLER.getId(sh.getType()));
        } catch (UnsupportedOperationException e) {
            return "player"; // the player inventory handler has no type
        }
    }

    private static String json(Text text) {
        try {
            return TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow().toString();
        } catch (RuntimeException e) {
            return '"' + text.getString() + '"';
        }
    }
}
