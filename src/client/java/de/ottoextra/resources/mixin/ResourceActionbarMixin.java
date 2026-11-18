package de.ottoextra.resources.mixin;

import de.ottoextra.resources.OttoExtraResourcesClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe server packets on the client thread, including boundaries hidden by OttoExtra's region HUD.
 * The dedicated overlay packet bypasses Fabric's ClientReceiveMessageEvents in Minecraft 1.21.11.
 */
@Mixin(value = ClientPlayNetworkHandler.class, priority = 1100)
public abstract class ResourceActionbarMixin {
    private static final String FORCE_MAIN_THREAD =
            "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread("
                    + "Lnet/minecraft/network/packet/Packet;"
                    + "Lnet/minecraft/network/listener/PacketListener;"
                    + "Lnet/minecraft/network/PacketApplyBatcher;)V";

    @Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD,
            shift = At.Shift.AFTER))
    private void ottoextraResources$gameActionbar(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.overlay()) OttoExtraResourcesClient.receiveActionbar(packet.content());
    }

    @Inject(method = "onOverlayMessage", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD,
            shift = At.Shift.AFTER))
    private void ottoextraResources$overlayActionbar(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        OttoExtraResourcesClient.receiveActionbar(packet.text());
    }

    // Inspect the item before vanilla decrements its stack or removes the entity.
    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD,
            shift = At.Shift.AFTER))
    private void ottoextraResources$itemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        OttoExtraResourcesClient.receiveItemPickup(packet);
    }
}
