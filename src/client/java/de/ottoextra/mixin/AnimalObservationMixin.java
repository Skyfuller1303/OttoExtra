package de.ottoextra.mixin;

import de.ottoextra.animalinfo.AnimalObservationTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hearts above animals reach the client as entity status 18 or as heart particles. Fabric API has no event
 * for either, so the animal tracker listens here. TAIL runs after the packet was applied on the client thread.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class AnimalObservationMixin {

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void ottoextra$animalLoveStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        AnimalObservationTracker.onEntityStatus(packet.getEntity(client.world), packet.getStatus());
    }

    @Inject(method = "onParticle", at = @At("TAIL"))
    private void ottoextra$animalLoveParticles(ParticleS2CPacket packet, CallbackInfo ci) {
        AnimalObservationTracker.onParticle(packet.getParameters(), packet.getX(), packet.getY(), packet.getZ());
    }
}
