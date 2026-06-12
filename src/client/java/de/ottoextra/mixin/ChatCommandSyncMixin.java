package de.ottoextra.mixin;

import de.ottoextra.chat.ChatChannelState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kanal-State mit manuell getippten Befehlen synchron halten
 *: /s /f /r /o /h /leave h /leave o
 * aktualisieren den Button-Zustand — egal ob per Button oder Hand gesendet.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ChatCommandSyncMixin {

    @Inject(method = "sendChatCommand(Ljava/lang/String;)V", at = @At("HEAD"))
    private void ottoextra$syncChannelState(String command, CallbackInfo ci) {
        try {
            ChatChannelState.handleOutgoingCommand(command);
        } catch (Throwable ignored) {
            // Sync ist best effort
        }
    }
}
