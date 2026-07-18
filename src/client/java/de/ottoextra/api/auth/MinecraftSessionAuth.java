package de.ottoextra.api.auth;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.util.function.Supplier;

public final class MinecraftSessionAuth {

    private MinecraftSessionAuth() {
    }

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

    public static MojangSessionJoiner joiner() {
        return (uuid, accessToken, serverId) -> MinecraftClient.getInstance()
                .getApiServices()
                .sessionService()
                .joinServer(uuid, accessToken, serverId);
    }
}
