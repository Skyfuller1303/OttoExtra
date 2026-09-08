package de.ottoextra;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class OttoExtraGate {

    private static final String OTTONIEN_HOST = "ottonien.com";

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
        return isOttonienAddress(entry.address);
    }

    /**
     * Akzeptiert nur die offizielle Domain und echte Subdomains. Ein bloßer
     * Teilstring würde fremde Hosts wie "ottonien.example.org" freischalten.
     */
    static boolean isOttonienAddress(String address) {
        if (address == null) {
            return false;
        }
        String host = address.trim().toLowerCase(java.util.Locale.ROOT);
        int firstColon = host.indexOf(':');
        int lastColon = host.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon) {
            host = host.substring(0, firstColon);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host.equals(OTTONIEN_HOST) || host.endsWith("." + OTTONIEN_HOST);
    }
}
