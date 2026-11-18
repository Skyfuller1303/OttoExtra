package de.ottoextra.tacticalfactions.mixin;

import de.ottoextra.tacticalfactions.FriendlyFireGuard;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the RELEASE_USE_ITEM packet for a drawn bow/trident aimed at an ally. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    private void tacticalFactions$protectRangedRelease(PlayerEntity player, CallbackInfo ci) {
        if (FriendlyFireGuard.shouldBlockRelease(player.getActiveItem())) {
            player.stopUsingItem();
            ci.cancel();
        }
    }
}
