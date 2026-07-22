package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.text.object.PlayerTextObjectContents;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatSpeakerClassifier {

    private static final Pattern HEAD_ACCOUNT = Pattern.compile(
            "\\[([A-Za-z0-9_]{3,16})\\s+(?:head|kopf)\\]",
            Pattern.CASE_INSENSITIVE);

    public enum SpeakerKind {
        PLAYER,
        NPC,
        UNKNOWN
    }

    private ChatSpeakerClassifier() {
    }

    public static SpeakerKind classify(Text message,
                                       @Nullable MessageSignatureData signature,
                                       @Nullable MessageIndicator indicator) {
        return classify(message, signature != null, indicator == MessageIndicator.system(),
                ChatSpeakerClassifier::isOnlineAccount);
    }

    static SpeakerKind classify(Text message, boolean signaturePresent,
                                boolean systemIndicator, Predicate<String> isOnlineAccount) {
        if (signaturePresent) {
            return SpeakerKind.PLAYER;
        }

        String componentAccount = playerComponentAccount(message);
        if (componentAccount != null && isOnlineAccount.test(componentAccount)) {
            return SpeakerKind.PLAYER;
        }

        String account = headAccountInSpeaker(message);
        if (account != null && isOnlineAccount.test(account)) {
            return SpeakerKind.PLAYER;
        }

        return systemIndicator ? SpeakerKind.NPC : SpeakerKind.UNKNOWN;
    }

    private static String playerComponentAccount(Text node) {
        if (node == null) {
            return null;
        }
        if (node.getContent() instanceof ObjectTextContent object
                && object.contents() instanceof PlayerTextObjectContents player) {
            String name = player.player().getName().orElse(null);
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        if (node.getContent() instanceof TranslatableTextContent translated) {
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Text text) {
                    String name = playerComponentAccount(text);
                    if (name != null) {
                        return name;
                    }
                }
            }
        }
        for (Text sibling : node.getSiblings()) {
            String name = playerComponentAccount(sibling);
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    private static String headAccountInSpeaker(Text message) {
        if (message == null) {
            return null;
        }
        String plain = message.getString();
        int bodyStart = RpChatFormatter.findBodyStart(plain);
        int channelEnd = plain.indexOf(']');
        if (channelEnd < 0 || bodyStart <= channelEnd + 1) {
            return null;
        }
        Matcher matcher = HEAD_ACCOUNT.matcher(plain.substring(channelEnd + 1, bodyStart));
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean isOnlineAccount(String account) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return false;
        }
        return client.getNetworkHandler().getPlayerList().stream()
                .map(entry -> entry.getProfile().name())
                .anyMatch(name -> name != null && name.equalsIgnoreCase(account));
    }
}
