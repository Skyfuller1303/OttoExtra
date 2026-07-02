package de.ottoextra.chat;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
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

    /** Hotkeys je Kanal (Standard unbelegt — manuell in Steuerung binden). */
    private KeyBinding keySprechen;
    private KeyBinding keyFluestern;
    private KeyBinding keyRufen;
    private KeyBinding keyOfftopic;
    private KeyBinding keyHilfe;

    private KeyBinding channelKey(String id) {
        KeyBinding key = new KeyBinding(id, InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(key);
        return key;
    }

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

        keySprechen = channelKey("key.ottoextra.channel_sprechen");
        keyFluestern = channelKey("key.ottoextra.channel_fluestern");
        keyRufen = channelKey("key.ottoextra.channel_rufen");
        keyOfftopic = channelKey("key.ottoextra.channel_offtopic");
        keyHilfe = channelKey("key.ottoextra.channel_hilfe");

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
                boolean alt = (click.modifiers() & GLFW.GLFW_MOD_ALT) != 0;
                ChatChannelState.clickChannelButton(shift, alt);
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
                    handleChannelHotkeys();
                    LongChatSender.tick(client);
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

    /** Kanal-Hotkeys abfragen; nur auf Ottonien (Button aktiv) wirksam. */
    private void handleChannelHotkeys() {
        boolean active = ChatChannelState.buttonActive();
        pollChannelKey(keySprechen, ChatChannelState.ChatChannel.SPRECHEN, active);
        pollChannelKey(keyFluestern, ChatChannelState.ChatChannel.FLUESTERN, active);
        pollChannelKey(keyRufen, ChatChannelState.ChatChannel.RUFEN, active);
        pollChannelKey(keyOfftopic, ChatChannelState.ChatChannel.OFFTOPIC, active);
        pollChannelKey(keyHilfe, ChatChannelState.ChatChannel.HILFE, active);
    }

    private void pollChannelKey(KeyBinding key, ChatChannelState.ChatChannel channel,
                                boolean active) {
        if (key == null) {
            return;
        }
        while (key.wasPressed()) {
            if (active) {
                ChatChannelState.selectChannel(channel);
            }
        }
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
