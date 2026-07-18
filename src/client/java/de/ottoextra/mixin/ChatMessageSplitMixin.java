package de.ottoextra.mixin;

import de.ottoextra.chat.ChatChannelState;
import de.ottoextra.chat.LongChatSender;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMessageSplitMixin {

    @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void ottoextra$splitLongChat(String content, CallbackInfo ci) {
        try {
            OttoExtraConfig.Chat cfg = OttoExtraConfig.active().chat;
            if (cfg == null || !cfg.enabled || !cfg.longChatEnabled || content == null) {
                return;
            }
            if (content.startsWith("/") || content.length() <= cfg.longChatChunk) {
                return;
            }
            boolean preserveRpSyntax = cfg.rpFormattingEnabled
                    && ChatChannelState.current().rp;
            LongChatSender.configureMs(cfg.longChatDelayMs);
            LongChatSender.enqueue(LongChatSender.split(
                    content,
                    cfg.longChatChunk,
                    cfg.longChatMarker,
                    preserveRpSyntax));
            ci.cancel();
        } catch (Throwable ignored) {

        }
    }
}
