package de.ottoextra.rpnames.chat;

import de.ottoextra.mixin.ChatHudAccessor;
import de.ottoextra.rpnames.RpNamesServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Aktualisiert bereits sichtbare Chatzeilen nach manuellen RP-Profiländerungen. */
public final class ChatHistoryRefresh {

    private static final Map<Text, Text> ORIGINAL_MESSAGES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ChatHistoryRefresh() {
    }

    /** Merkt den unveränderten Servertext für spätere, verlustfreie Aktualisierungen. */
    public static Text remember(Text original, Text displayed) {
        if (original != null && displayed != null && original != displayed) {
            if (displayed != original) {
                ORIGINAL_MESSAGES.put(displayed, original);
            }
        }
        return displayed;
    }

    public static void request() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(ChatHistoryRefresh::refreshNow);
        }
    }

    private static void refreshNow() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.inGameHud == null) {
            return;
        }

        ChatHud hud = client.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) (Object) hud;
        var messages = accessor.ottoextra$messages();
        int changed = 0;

        for (int index = 0; index < messages.size(); index++) {
            ChatHudLine line = messages.get(index);
            Text original = ORIGINAL_MESSAGES.getOrDefault(line.content(), line.content());
            Text displayed = RpNamesServices.rewriteChatDisplay(original);
            displayed = de.ottoextra.chat.RpChatFormatter.format(displayed);
            if (displayed == null || displayed.equals(line.content())) {
                continue;
            }

            ORIGINAL_MESSAGES.put(displayed, original);
            messages.set(index, new ChatHudLine(
                    line.creationTick(),
                    displayed,
                    line.signature(),
                    line.indicator()));
            changed++;
        }

        if (changed > 0) {
            accessor.ottoextra$refresh();
            de.ottoextra.OttoExtra.LOGGER.info(
                    "[rpnames] {} bestehende Chatzeile(n) aktualisiert.", changed);
        }
    }
}
