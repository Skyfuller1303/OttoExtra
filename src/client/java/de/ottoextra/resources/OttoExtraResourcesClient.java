package de.ottoextra.resources;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;

public final class OttoExtraResourcesClient implements ClientModInitializer {
    private static ResourcesModule module;

    @Override
    public void onInitializeClient() {
        ResourcesConfig.active();
        module = new ResourcesModule();
        module.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> module.onServerJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> module.onDisconnect());
        OttoExtraResources.LOGGER.info("{} bereit.", OttoExtraResources.MOD_NAME);
    }

    public static void openCreateScreenFromSelection(Screen parent, BlockPos first, BlockPos second) {
        if (module == null) return;
        MinecraftClient.getInstance().setScreen(
                ResourceEditScreen.forCreate(parent, module, ResourceZone.cuboid(first, second)));
    }

    /** Opens the editor for a new point at a world map position. */
    public static void openCreateAt(Screen parent, int x, int z) {
        if (module == null) return;
        MinecraftClient.getInstance().setScreen(ResourceEditScreen.forCreateAt(parent, module, x, z));
    }

    public static boolean isFeatureAvailable() {
        return module != null && module.available();
    }

    public static void receiveActionbar(Text message) {
        if (module != null) module.onActionbar(message);
    }

    public static void receiveItemPickup(ItemPickupAnimationS2CPacket packet) {
        if (module != null) module.onItemPickup(packet);
    }

    public static void detectBlockInteraction(MinecraftClient client, BlockHitResult hit) {
        if (module == null || client.world == null) return;
        BlockPos pos = hit.getBlockPos();
        module.onInteraction(client, pos, client.world.getBlockState(pos), "BLOCK_INTERACT");
    }
}
