package de.ottoextra.mixin;
import de.ottoextra.chat.ChatChannelState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ChatCommandSyncMixin {
    @Inject(method = "sendChatCommand(Ljava/lang/String;)V", at = @At("HEAD"))
    private void ottoextra$syncChannelState(String command, CallbackInfo ci) {
        try {
            ChatChannelState.handleOutgoingCommand(command);
        } catch (Throwable ignored) {
        }
    }
}
