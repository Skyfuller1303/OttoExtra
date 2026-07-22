package de.ottoextra.chat;

import de.ottoextra.rpnames.RpNamesServices;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class ChatMessagePipeline {

    private ChatMessagePipeline() {
    }

    public static Text formatIncoming(Text original,
                                      @Nullable MessageSignatureData signature,
                                      @Nullable MessageIndicator indicator) {
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
        ChatSpeakerClassifier.SpeakerKind speakerKind =
                ChatSpeakerClassifier.classify(original, signature, indicator);

        Text displayed = original;
        if (speakerKind == ChatSpeakerClassifier.SpeakerKind.PLAYER) {
            displayed = incoming
                    ? RpNamesServices.processChatMessage(original)
                    : RpNamesServices.rewriteChatDisplay(original);
        }
        displayed = ChatChannelFormatter.format(displayed, speakerKind);
        displayed = RpChatFormatter.format(displayed);
        return de.ottoextra.addon.OttoExtraAddons.processChatMessage(displayed);
    }
}
