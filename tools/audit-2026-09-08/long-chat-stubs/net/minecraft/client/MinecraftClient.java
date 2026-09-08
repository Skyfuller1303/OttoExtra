package net.minecraft.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;

/** Test boundary only: no Minecraft lifecycle or queue behaviour is simulated. */
public final class MinecraftClient {
    private static MinecraftClient instance;
    private final ClientPlayNetworkHandler networkHandler;

    public MinecraftClient(ClientPlayNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        instance = this;
    }

    public static MinecraftClient getInstance() {
        return instance;
    }

    public ClientPlayNetworkHandler getNetworkHandler() {
        return networkHandler;
    }
}
