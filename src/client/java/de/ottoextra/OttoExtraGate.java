package de.ottoextra;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

public final class OttoExtraGate {

    private OttoExtraGate() {
    }

    public static boolean isOnOttonien(MinecraftClient client) {
        if (client == null) {
            return false;
        }
        ServerInfo entry = client.getCurrentServerEntry();
        if (entry == null || entry.address == null) {
            return false;
        }
        return entry.address.toLowerCase(Locale.ROOT).contains(OttoExtra.OTTONIEN_HOST_MARKER);
    }
}
