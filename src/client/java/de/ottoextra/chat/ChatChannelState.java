package de.ottoextra.chat;

import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Kanal-State für den Chat-Channel-Button:
 * aktueller Kanal + zuletzt genutzter RP-/OOC-Kanal, Klick-Logik
 * (Links = RP-Zyklus bzw. zurück aus OOC; Shift = OOC-Zyklus bzw. hinein;
 * Alt = ein Kanal zurück über alle Kanäle)
 * und Synchronisation mit manuell getippten Befehlen
 * ({@code /s /f /m /r /b /o /h /leave h /leave o}).
 *
 * <p>Der Kanalwechsel sendet den Serverbefehl; der angezeigte Prefix ist nur
 * UI und wird nie in die Nachricht eingefügt.</p>
 */
public final class ChatChannelState {

    public enum ChatChannel {
        SPRECHEN("[Sprechen]", "s", true),
        FLUESTERN("[Flüstern]", "f", true),
        MURMELN("[Murmeln]", "m", true),
        RUFEN("[Rufen]", "r", true),
        BRUELLEN("[Brüllen]", "b", true),
        OFFTOPIC("[Offtopic]", "o", false),
        HILFE("[Hilfe]", "h", false);

        public final String label;
        public final String command;
        public final boolean rp;

        ChatChannel(String label, String command, boolean rp) {
            this.label = label;
            this.command = command;
            this.rp = rp;
        }
    }

    /** Zyklus-Reihenfolge fürs Durchschalten (vor- und rückwärts). */
    private static final ChatChannel[] ORDER = {
            ChatChannel.MURMELN, ChatChannel.FLUESTERN, ChatChannel.SPRECHEN,
            ChatChannel.RUFEN, ChatChannel.BRUELLEN,
            ChatChannel.OFFTOPIC, ChatChannel.HILFE};

    private static volatile ChatChannel currentChannel = ChatChannel.SPRECHEN;
    private static volatile ChatChannel lastRpChannel = ChatChannel.SPRECHEN;
    private static volatile ChatChannel lastOocChannel = ChatChannel.OFFTOPIC;
    private static volatile BooleanSupplier onOttonien = () -> false;
    private static volatile de.ottoextra.config.OttoExtraConfig.Chat config;

    private ChatChannelState() {
    }

    public static void init(de.ottoextra.config.OttoExtraConfig.Chat cfg, BooleanSupplier ottonienGate) {
        config = cfg;
        onOttonien = ottonienGate;
    }

    public static de.ottoextra.config.OttoExtraConfig.Chat chatConfig() {
        return config;
    }

    /** Button sichtbar/aktiv? (Modul an + auf Ottonien) */
    public static boolean buttonActive() {
        return config != null && config.enabled && onOttonien.getAsBoolean();
    }

    /** Shift+Tab soll den Kanal wechseln (Modul an + auf Ottonien + Option an)? */
    public static boolean shiftTabCycleEnabled() {
        return buttonActive() && config.shiftTabCycleChannels;
    }

    public static ChatChannel current() {
        return currentChannel;
    }

    // ---- Klick-Logik -----------------------------------------------------------

    /** Direkt auf einen Kanal wechseln (für Hotkeys): State setzen + Befehl senden. */
    public static void selectChannel(ChatChannel channel) {
        if (channel != null) {
            switchToChannel(channel);
        }
    }

    /** Alle Kanäle der Reihe nach durchwechseln (inkl. OOC) — für Shift+Tab. */
    public static void cycleAllChannels() {
        switchToChannel(step(currentChannel, +1));
    }

    /** Einen Kanal in der Reihenfolge zurück — für Alt+Linksklick. */
    public static void cycleAllChannelsBackwards() {
        switchToChannel(step(currentChannel, -1));
    }

    /** Nachbar von {@code channel} in ORDER ({@code dir} = ±1, mit Umlauf). */
    private static ChatChannel step(ChatChannel channel, int dir) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == channel) {
                return ORDER[Math.floorMod(i + dir, ORDER.length)];
            }
        }
        return ChatChannel.SPRECHEN;
    }

    public static void clickChannelButton(boolean shiftDown, boolean altDown) {
        if (altDown) {
            cycleAllChannelsBackwards();
        } else if (shiftDown) {
            cycleOocChannel();
        } else {
            cycleRpChannelOrReturnFromOoc();
        }
    }

    private static void cycleRpChannelOrReturnFromOoc() {
        if (!currentChannel.rp) {
            switchToChannel(lastRpChannel != null ? lastRpChannel : ChatChannel.SPRECHEN);
            return;
        }
        // RP-Zyklus in ORDER-Reihenfolge: Murmeln → Flüstern → Sprechen → Rufen → Brüllen
        ChatChannel next = step(currentChannel, +1);
        if (!next.rp) {
            next = ORDER[0];
        }
        switchToChannel(next);
    }

    private static void cycleOocChannel() {
        if (currentChannel.rp) {
            switchToChannel(lastOocChannel != null ? lastOocChannel : ChatChannel.OFFTOPIC);
            return;
        }
        switchToChannel(currentChannel == ChatChannel.OFFTOPIC
                ? ChatChannel.HILFE : ChatChannel.OFFTOPIC);
    }

    private static void switchToChannel(ChatChannel channel) {
        setCurrentChannelFromExternal(channel);
        sendChannelCommand(channel);
    }

    private static void sendChannelCommand(ChatChannel channel) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        try {
            client.getNetworkHandler().sendChatCommand(channel.command);
        } catch (Throwable t) {
            OttoExtra.LOGGER.warn("[chat] Kanalbefehl /{} fehlgeschlagen: {}", channel.command, t.toString());
        }
    }

    // ---- Sync mit manuell getippten Befehlen -------------------------------------

    /** Vom sendChatCommand-Mixin gerufen (Befehl OHNE führenden Slash). */
    public static void handleOutgoingCommand(String command) {
        if (command == null) {
            return;
        }
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "s" -> setCurrentChannelFromExternal(ChatChannel.SPRECHEN);
            case "f" -> setCurrentChannelFromExternal(ChatChannel.FLUESTERN);
            case "m" -> setCurrentChannelFromExternal(ChatChannel.MURMELN);
            case "r" -> setCurrentChannelFromExternal(ChatChannel.RUFEN);
            case "b" -> setCurrentChannelFromExternal(ChatChannel.BRUELLEN);
            case "o" -> setCurrentChannelFromExternal(ChatChannel.OFFTOPIC);
            case "h" -> setCurrentChannelFromExternal(ChatChannel.HILFE);
            case "leave h", "leave o" -> setCurrentChannelFromExternal(
                    lastRpChannel != null ? lastRpChannel : ChatChannel.SPRECHEN);
            default -> {
            }
        }
    }

    private static void setCurrentChannelFromExternal(ChatChannel channel) {
        currentChannel = channel;
        if (channel.rp) {
            lastRpChannel = channel;
        } else {
            lastOocChannel = channel;
        }
    }
}
