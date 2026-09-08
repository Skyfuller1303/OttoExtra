package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.text.object.PlayerTextObjectContents;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
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
                ChatSpeakerClassifier::isOnlineAccount, localAccount());
    }

    static SpeakerKind classify(Text message, boolean signaturePresent,
                                 boolean systemIndicator, Predicate<String> isOnlineAccount) {
        return classify(message, signaturePresent, systemIndicator, isOnlineAccount, null);
    }

    static SpeakerKind classify(Text message, boolean signaturePresent,
                                boolean systemIndicator, Predicate<String> isOnlineAccount,
                                @Nullable String localAccount) {
        if (signaturePresent) {
            return SpeakerKind.PLAYER;
        }

        String componentAccount = speakerComponentAccount(message);
        if (isPlayerAccount(componentAccount, localAccount, isOnlineAccount)) {
            return SpeakerKind.PLAYER;
        }

        String account = headAccountInSpeaker(message);
        if (isPlayerAccount(account, localAccount, isOnlineAccount)) {
            return SpeakerKind.PLAYER;
        }

        return systemIndicator ? SpeakerKind.NPC : SpeakerKind.UNKNOWN;
    }

    public static @Nullable String speakerComponentAccount(Text message) {
        if (message == null) {
            return null;
        }
        String plain = message.getString();
        int channelEnd = plain.indexOf(']');
        int bodyStart = RpChatFormatter.findBodyStart(plain);
        if (channelEnd < 0 || bodyStart <= channelEnd + 1) {
            return null;
        }
        return speakerComponentAccount(message, new int[]{0}, channelEnd + 1, bodyStart);
    }

    private static @Nullable String speakerComponentAccount(
            Text node, int[] flatPosition, int speakerStart, int speakerEnd) {
        if (node == null) {
            return null;
        }
        if (node.getContent() instanceof TranslatableTextContent translated
                && "%s".equals(translated.getKey())) {
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Text text) {
                    String account = speakerComponentAccount(
                            text, flatPosition, speakerStart, speakerEnd);
                    if (account != null) {
                        return account;
                    }
                } else if (argument != null) {
                    flatPosition[0] += argument.toString().length();
                }
            }
            for (Text sibling : node.getSiblings()) {
                String account = speakerComponentAccount(
                        sibling, flatPosition, speakerStart, speakerEnd);
                if (account != null) {
                    return account;
                }
            }
            return null;
        }

        String own = ownText(node);
        int nodeStart = flatPosition[0];
        int nodeEnd = nodeStart + own.length();
        flatPosition[0] = nodeEnd;
        if (node.getContent() instanceof ObjectTextContent object
                && object.contents() instanceof PlayerTextObjectContents player
                && nodeStart < speakerEnd && nodeEnd > speakerStart) {
            String name = player.player().getName().orElse(null);
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        for (Text sibling : node.getSiblings()) {
            String account = speakerComponentAccount(
                    sibling, flatPosition, speakerStart, speakerEnd);
            if (account != null) {
                return account;
            }
        }
        return null;
    }

    private static String ownText(Text node) {
        StringBuilder plain = new StringBuilder();
        node.getContent().visit(part -> {
            plain.append(part);
            return Optional.empty();
        });
        return plain.toString();
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

    private static boolean isPlayerAccount(@Nullable String account,
                                           @Nullable String localAccount,
                                           Predicate<String> isOnlineAccount) {
        if (account == null || account.isBlank()) {
            return false;
        }
        return localAccount != null && account.equalsIgnoreCase(localAccount)
                || isOnlineAccount.test(account);
    }

    private static @Nullable String localAccount() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        if (client.player != null && client.player.getGameProfile() != null) {
            String name = client.player.getGameProfile().name();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        if (client.getSession() == null) {
            return null;
        }
        String name = client.getSession().getUsername();
        return name == null || name.isBlank() ? null : name;
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
