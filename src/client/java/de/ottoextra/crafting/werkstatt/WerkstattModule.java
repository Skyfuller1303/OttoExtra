package de.ottoextra.crafting.werkstatt;

import de.ottoextra.OttoExtraContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

/** Wires the Werkstatt into the game: station screens, ticks, messages, command, key and HUD. */
public final class WerkstattModule {
    private static KeyBinding stopKey;
    private static boolean registered;
    /** Panel of the open station screen; drawn by the screen mixin before the deferred tooltip. */
    private static Screen panelScreen;
    private static WerkstattPanel panel;

    private WerkstattModule() {
    }

    public static void register(OttoExtraContext context) {
        if (registered) {
            return;
        }
        registered = true;
        stopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.ottoextra.werkstatt_stop",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen && Werkstatt.viewLocked()) {
                // Esc while the view is locked ends the job. Focus loss opens this menu too; then the
                // focus rule decides.
                if (client.isWindowFocused()) {
                    Werkstatt.stop(StopReason.ESCAPE);
                } else if (Werkstatt.config().werkstattStopOnUnfocus) {
                    Werkstatt.stop(StopReason.UNFOCUSED);
                }
                return;
            }
            if (!Werkstatt.enabled() || !context.isOnOttonien() || !(screen instanceof HandledScreen<?> hs)) {
                return;
            }
            String station = Werkstatt.updateSession(screen).station();
            if (station == null) {
                return;
            }
            WerkstattPanel stationPanel = new WerkstattPanel(hs, station);
            panelScreen = screen;
            panel = stationPanel;
            ScreenEvents.remove(screen).register(s -> {
                if (panelScreen == s) {
                    panelScreen = null;
                    panel = null;
                }
            });
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                try {
                    return stationPanel.mouseClicked(click);
                } catch (Throwable ignored) {
                    return true;
                }
            });
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key) -> {
                if (key.isEscape() && Werkstatt.running() && Werkstatt.runner().continuous()) {
                    // Esc in the station during continuous mode: stop, then close as usual.
                    Werkstatt.stop(StopReason.ESCAPE);
                }
                return true;
            });
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() && hit != null && Werkstatt.enabled()) {
                Werkstatt.onUseBlock(hit, world.getBlockState(hit.getBlockPos()).getBlock());
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (stopKey != null) {
                while (stopKey.wasPressed()) {
                    Werkstatt.stop(StopReason.KEY);
                }
            }
            if (!context.isOnOttonien()) {
                return;
            }
            try {
                Werkstatt.tick(client);
            } catch (Throwable t) {
                de.ottoextra.OttoExtra.LOGGER.warn("[werkstatt] Fehler, Auftrag gestoppt: {}", t.toString());
                Werkstatt.stop(StopReason.DISABLED);
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (Werkstatt.enabled()) {
                Werkstatt.onMessage(message.getString(), overlay);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var server = client.getCurrentServerEntry();
            if (server != null) {
                Werkstatt.onJoin(server.address, client.getSession().getUsername());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Werkstatt.onDisconnect());

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            try {
                WerkstattHud.render(ctx);
            } catch (Throwable ignored) {
                // HUD card is optional.
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("werkstatt")
                                .then(ClientCommandManager.literal("stop").executes(c -> {
                                    boolean was = Werkstatt.running();
                                    Werkstatt.stop(StopReason.COMMAND);
                                    c.getSource().sendFeedback(Text.translatable(was
                                            ? "ottoextra.werkstatt.command.stopped"
                                            : "ottoextra.werkstatt.command.idle"));
                                    return 1;
                                })))));
    }

    /**
     * Draws the station panel. Called by the screen mixin right before the deferred tooltips, so
     * item tooltips of the station stay on top of the panel.
     */
    public static void renderPanel(Screen screen, DrawContext ctx, int mouseX, int mouseY) {
        WerkstattPanel p = panel;
        if (p == null || screen != panelScreen) {
            return;
        }
        try {
            ctx.createNewRootLayer();
            p.render(ctx, mouseX, mouseY);
        } catch (Throwable ignored) {
            // Never break the station screen.
        }
    }

    /** Key hint for the HUD card, e.g. "[K]", or "" while unbound. */
    static String stopKeyLabel() {
        if (stopKey == null || stopKey.isUnbound()) {
            return "";
        }
        return "[" + stopKey.getBoundKeyLocalizedText().getString() + "]";
    }

    static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }
}
