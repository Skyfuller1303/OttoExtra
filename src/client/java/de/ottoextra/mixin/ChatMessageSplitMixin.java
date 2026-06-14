package de.ottoextra.mixin;

import de.ottoextra.chat.LongChatSender;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lange Chatnachrichten splitten: ist eine Nachricht länger als das Chunk-Limit,
 * wird sie an Wortgrenzen geteilt und gestaffelt gesendet ({@link LongChatSender}).
 * Die einzelnen Teilstücke (≤ Limit) laufen erneut durch diese Methode, lösen die
 * Bedingung aber nicht aus -> keine Rekursion. Ziel: {@link ClientPlayNetworkHandler}.
 */
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
            LongChatSender.configureMs(cfg.longChatDelayMs);
            LongChatSender.enqueue(
                    LongChatSender.split(content, cfg.longChatChunk, cfg.longChatMarker));
            ci.cancel(); // Original (zu lange) Nachricht nicht senden
        } catch (Throwable ignored) {
            // Chat darf nie brechen -> im Zweifel Vanilla senden lassen
        }
    }
}
