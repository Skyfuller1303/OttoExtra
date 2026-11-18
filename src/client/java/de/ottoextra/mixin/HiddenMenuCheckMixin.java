package de.ottoextra.mixin;

import de.ottoextra.ailments.ConditionsModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the automatic menu check read the server menu without showing it: the matching open-screen
 * packet is taken over, and the "Du öffnest dein Menü." line is hidden while a check runs.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class HiddenMenuCheckMixin {

    private static final String FORCE_MAIN_THREAD =
            "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread("
                    + "Lnet/minecraft/network/packet/Packet;"
                    + "Lnet/minecraft/network/listener/PacketListener;"
                    + "Lnet/minecraft/network/PacketApplyBatcher;)V";

    @Inject(method = "onOpenScreen", at = @At(
            value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void ottoextra$hiddenOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (ConditionsModule.interceptOpenScreen(packet.getSyncId(), packet.getScreenHandlerType(),
                packet.getName())) {
            ci.cancel();
        }
    }

    @Inject(method = "onGameMessage", at = @At(
            value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void ottoextra$hiddenMenuChat(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (!packet.overlay() && ConditionsModule.suppressChat(packet.content())) {
            ci.cancel();
        }
    }
}
