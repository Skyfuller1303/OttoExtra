package de.ottoextra;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

/**
 * Client-seitiges Server-Gate.
 *
 * <p>Erkennt anhand der Serveradresse, ob OttoExtra-Online-Features aktiv sein
 * sollen. Bewusst nur eine Komfort-Heuristik (Substring {@code "ottonien"}):
 * sie steuert ausschliesslich clientseitige Anzeige und API-Abrufe. Keine
 * Sicherheitsentscheidung hängt allein hieran (vgl. mod-family-map Warnung).</p>
 */
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
