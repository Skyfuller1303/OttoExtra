package de.ottoextra.resources.mixin;

import de.ottoextra.resources.OttoExtraResourcesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void ottoextraResources$detectBlockInteraction(CallbackInfo callbackInfo) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            OttoExtraResourcesClient.detectBlockInteraction(client, hit);
        }
    }
}
