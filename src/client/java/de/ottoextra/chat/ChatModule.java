package de.ottoextra.chat;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Modul: Chat-Kanal-Button.
 *
 * <p>Fester Kanal-Prefix-Button links unten im Chat-Screen: Linksklick
 * zykliert RP-Kanäle (bzw. zurück aus OOC), Shift-Klick zykliert OOC.
 * Kanalwechsel sendet den Serverbefehl; der Prefix ist reines UI. State
 * synchronisiert sich mit manuell getippten Befehlen (ChatCommandSyncMixin);
 * das Eingabefeld rückt über ChatScreenMixin ein. Nur auf Ottonien aktiv.</p>
 */
public final class ChatModule implements OttoExtraModule {

    /** Ticks bis zum Auto-/s nach Join (Spieler + Server bereit). */
    private static final int AUTO_SPRECHEN_DELAY_TICKS = 60;

    private int joinCountdown = -1;

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public boolean enabled(OttoExtraConfig config) {
        return config.chat.enabled;
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        ChatChannelState.init(context.config().chat, context::isOnOttonien);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof ChatScreen)) {
                return;
            }
            // Klick VOR dem Chatfeld abfangen (allow=false blockt Weitergabe)
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !ChatChannelState.buttonActive()) {
                    return true;
                }
                MinecraftClient mc = MinecraftClient.getInstance();
                if (!ChatChannelButton.contains(mc, s.height, click.x(), click.y())) {
                    return true;
                }
                boolean shift = (click.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
                ChatChannelState.clickChannelButton(shift);
                return false;
            });
            ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
                if (ChatChannelState.buttonActive()) {
                    ChatChannelButton.render(drawContext, MinecraftClient.getInstance(),
                            s.height, mouseX, mouseY);
                }
            });
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
                .register(client -> {
                    if (joinCountdown > 0 && client.player != null) {
                        if (--joinCountdown == 0 && ChatChannelState.buttonActive()
                                && context.config().chat.autoSprechenOnJoin
                                && client.getNetworkHandler() != null) {
                            client.getNetworkHandler().sendChatCommand("s");
                        }
                    }
                });

        OttoExtra.LOGGER.info("[chat] initialisiert (Kanal-Button: Sprechen/Flüstern/Rufen + OOC).");
    }

    @Override
    public void onServerJoin(de.ottoextra.OttoExtraContext context) {
        // Standardkanal Sprechen: kurz nach Join /s senden (einstellbar)
        joinCountdown = AUTO_SPRECHEN_DELAY_TICKS;
    }

    @Override
    public void onDisconnect(de.ottoextra.OttoExtraContext context) {
        joinCountdown = -1;
    }
}
