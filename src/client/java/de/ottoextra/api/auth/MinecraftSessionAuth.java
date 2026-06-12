package de.ottoextra.api.auth;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.util.function.Supplier;

/**
 * Produktiv-Anbindung des Auth-Handshakes an den laufenden Minecraft-Client.
 *
 * <p>Einzige Klasse im auth-Paket mit Minecraft-Imports — alles andere bleibt
 * MC-frei und damit unit-testbar.</p>
 */
public final class MinecraftSessionAuth {

    private MinecraftSessionAuth() {
    }

    /** Liefert die aktuelle Session als Snapshot, {@code null} wenn keine da ist. */
    public static Supplier<SessionSnapshot> sessionSupplier() {
        return () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return null;
            }
            Session session = client.getSession();
            if (session == null) {
                return null;
            }
            return new SessionSnapshot(
                    session.getUsername(),
                    session.getUuidOrNull(),
                    session.getAccessToken());
        };
    }

    /**
     * {@code joinServer} über den Session-Service der Client-Instanz.
     * Der Access-Token geht ausschliesslich an sessionserver.mojang.com.
     */
    public static MojangSessionJoiner joiner() {
        return (uuid, accessToken, serverId) -> MinecraftClient.getInstance()
                .getApiServices()
                .sessionService()
                .joinServer(uuid, accessToken, serverId);
    }
}
