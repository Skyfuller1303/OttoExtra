package de.ottoextra.animalinfo;

import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class AnimalInfoModule implements OttoExtraModule {

    @Override
    public String id() {
        return "animalinfo";
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        AnimalInfoService.init(context.config().animalInfo, context::isOnOttonien);
        ClientTickEvents.END_CLIENT_TICK.register(AnimalInfoService::tick);
        HudRenderCallback.EVENT.register(AnimalInfoOverlay::render);
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() && hand == Hand.MAIN_HAND) {
                AnimalInfoService.onBlockInteraction();
            }
            return ActionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && hand == Hand.MAIN_HAND
                    && entity instanceof AnimalEntity animal) {
                AnimalInfoService.onAnimalInteraction(animal);
            }
            return ActionResult.PASS;
        });
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        AnimalInfoService.reset();
    }
}
