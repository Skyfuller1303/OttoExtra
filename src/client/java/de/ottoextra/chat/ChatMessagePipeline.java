package de.ottoextra.chat;

import de.ottoextra.rpnames.RpNamesServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ChatMessagePipeline {

    private static final Deque<PendingMessage> PENDING = new ArrayDeque<>();
    private static final ThreadLocal<PreparedMessage> PREPARED = new ThreadLocal<>();

    private ChatMessagePipeline() {
    }

    public static Text formatIncoming(Text original,
                                      @Nullable MessageSignatureData signature,
                                      @Nullable MessageIndicator indicator) {
        PreparedMessage prepared = PREPARED.get();
        if (prepared != null && prepared.original() == original) {
            PREPARED.remove();
            return prepared.displayed();
        }
        return format(original, signature, indicator, true);
    }

    public static Text formatHistory(Text original,
                                     @Nullable MessageSignatureData signature,
                                     @Nullable MessageIndicator indicator) {
        return format(original, signature, indicator, false);
    }

    private static Text format(Text original,
                               @Nullable MessageSignatureData signature,
                               @Nullable MessageIndicator indicator,
                               boolean incoming) {
        ChatMessageContextParts parts = prepare(original, signature, indicator, incoming);
        return de.ottoextra.addon.OttoExtraAddons.processChatMessage(
                new de.ottoextra.addon.ChatMessageContext(
                        original, parts.displayed(), parts.speakerKind(), incoming));
    }

    /**
     * Haelt eine eingehende Zeile bei Bedarf zurueck. Fertige Zeilen werden
     * in Server-Reihenfolge erneut in dasselbe ChatHud eingespeist.
     */
    public static boolean deferIncoming(Text original,
                                        @Nullable MessageSignatureData signature,
                                        @Nullable MessageIndicator indicator,
                                        Runnable replay) {
        if (isReplay(original)) return false;

        ChatMessageContextParts parts = prepare(original, signature, indicator, true);
        de.ottoextra.addon.ChatMessageContext context =
                new de.ottoextra.addon.ChatMessageContext(
                        original, parts.displayed(), parts.speakerKind(), true);
        if (!de.ottoextra.addon.OttoExtraAddons.delaysIncomingChat(context)
                && PENDING.isEmpty()) {
            return false;
        }

        PendingMessage pending = new PendingMessage(original, parts.displayed(), replay);
        PENDING.addLast(pending);
        de.ottoextra.addon.OttoExtraAddons.processChatMessageAsync(context)
                .whenComplete((displayed, error) -> runOnClient(() -> {
                    pending.displayed = error == null && displayed != null
                            ? displayed : parts.displayed();
                    pending.ready = true;
                    releaseReady();
                }));
        return true;
    }

    /** Distinguishes our delayed replay from a genuinely repeated incoming message. */
    public static boolean isReplay(Text original) {
        PreparedMessage prepared = PREPARED.get();
        return prepared != null && prepared.original() == original;
    }

    private static ChatMessageContextParts prepare(
            Text original,
            @Nullable MessageSignatureData signature,
            @Nullable MessageIndicator indicator,
            boolean incoming) {
        ChatSpeakerClassifier.SpeakerKind speakerKind =
                ChatSpeakerClassifier.classify(original, signature, indicator);
        Text displayed = original;
        if (speakerKind == ChatSpeakerClassifier.SpeakerKind.PLAYER) {
            Text renamed = incoming
                    ? RpNamesServices.processChatMessage(original)
                    : RpNamesServices.rewriteChatDisplay(original);
            displayed = keepOnlyIfBodyPreserved(original, renamed);
        }
        displayed = RpChatCorruptionRepair.repair(displayed, signature, indicator);
        Text colored = ChatChannelFormatter.format(displayed, speakerKind);
        displayed = RpChatFormatter.keepOnlyIfTextPreserved(displayed, colored);
        displayed = RpChatFormatter.format(displayed);
        return new ChatMessageContextParts(displayed, speakerKind);
    }

    /** RP-Namensersetzung darf ausschließlich den Kopf vor dem Nachrichtentext ändern. */
    static Text keepOnlyIfBodyPreserved(Text original, Text rewritten) {
        if (original == null || rewritten == null) {
            return original;
        }
        String originalPlain = RpChatFormatter.visibleString(original);
        String rewrittenPlain = RpChatFormatter.visibleString(rewritten);
        int originalBody = RpChatFormatter.findBodyStart(originalPlain);
        int rewrittenBody = RpChatFormatter.findBodyStart(rewrittenPlain);
        if (originalBody < 0 || rewrittenBody < 0) {
            return originalPlain.equals(rewrittenPlain) ? rewritten : original;
        }
        return originalPlain.substring(originalBody).equals(
                rewrittenPlain.substring(rewrittenBody)) ? rewritten : original;
    }

    private static void releaseReady() {
        while (!PENDING.isEmpty() && PENDING.peekFirst().ready) {
            PendingMessage pending = PENDING.removeFirst();
            PREPARED.set(new PreparedMessage(pending.original, pending.displayed));
            try {
                pending.replay.run();
            } finally {
                PREPARED.remove();
            }
        }
    }

    private static void runOnClient(Runnable action) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.isOnThread()) {
            action.run();
        } else {
            client.execute(action);
        }
    }

    private record ChatMessageContextParts(
            Text displayed, ChatSpeakerClassifier.SpeakerKind speakerKind) { }

    private record PreparedMessage(Text original, Text displayed) { }

    private static final class PendingMessage {
        private final Text original;
        private final Runnable replay;
        private Text displayed;
        private boolean ready;

        private PendingMessage(Text original, Text displayed, Runnable replay) {
            this.original = original;
            this.displayed = displayed;
            this.replay = replay;
        }
    }
}
