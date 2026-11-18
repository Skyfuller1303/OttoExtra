package de.ottoextra.tacticalfactions.mixin;

import de.ottoextra.tacticalfactions.RetaliationMarkers;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityDamage", at = @At("TAIL"))
    private void tacticalFactions$markProtectedAttacker(EntityDamageS2CPacket packet, CallbackInfo ci) {
        RetaliationMarkers.onEntityDamage(packet);
    }
}
