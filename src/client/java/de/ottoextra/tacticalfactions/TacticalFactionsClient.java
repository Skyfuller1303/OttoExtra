package de.ottoextra.tacticalfactions;

import de.ottoextra.OttoExtraGate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class TacticalFactionsClient implements ClientModInitializer {
    private static KeyBinding combatKey;
    private static KeyBinding squadHudKey;
    private static long nextRefresh;

    @Override
    public void onInitializeClient() {
        TacticalFactions.config();
        combatKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra_tacticalfactions.combat", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));
        squadHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra_tacticalfactions.squad_hud", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        FriendlyFireGuard.register();
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(SquadActivityHud::render);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            nextRefresh = System.currentTimeMillis() + 1000L;
            TacticalFactions.politics().resetRefreshTimer();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            nextRefresh = 0L;
            RetaliationMarkers.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (combatKey.wasPressed()) {
                setCombatMode(!TacticalFactions.config().combatMode);
                if (client.player != null) client.player.sendMessage(Text.translatable(
                        TacticalFactions.config().combatMode ? "ottoextra_tacticalfactions.combat.on"
                                : "ottoextra_tacticalfactions.combat.off"), true);
            }
            while (squadHudKey.wasPressed()) {
                TacticalConfig config = TacticalFactions.config();
                config.squadHudEnabled = !config.squadHudEnabled;
                config.save();
                SquadActivityHud.invalidate();
                if (client.player != null) client.player.sendMessage(Text.translatable(
                        config.squadHudEnabled ? "ottoextra_tacticalfactions.squad_hud.on"
                                : "ottoextra_tacticalfactions.squad_hud.off"), true);
            }
            SquadActivityHud.tick(client);
            long now = System.currentTimeMillis();
            // nextRefresh only delays the first refresh after joining; the service throttles itself
            // (5 min after success, 30 s retry while data is missing).
            if (nextRefresh > 0L && now >= nextRefresh && OttoExtraGate.isOnOttonien(client)) {
                TacticalFactions.politics().refreshIfDue(now);
            }
        });
        de.ottoextra.logging.DebugLog.debug("[tacticalfactions] bereit.");
    }

    /** Localized name of the key currently bound to combat mode. */
    static Text combatKeyName() {
        return combatKey != null ? combatKey.getBoundKeyLocalizedText() : Text.literal("K");
    }

    /** Shared by the K key and the combat tab of the book. */
    static void setCombatMode(boolean on) {
        TacticalConfig config = TacticalFactions.config();
        config.combatMode = on;
        config.save();
        if (!on) RetaliationMarkers.clear();
    }
}
