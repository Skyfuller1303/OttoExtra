package de.ottoextra.api.auth;

import java.util.UUID;

/**
 * Momentaufnahme der Minecraft-Session für den Auth-Handshake.
 *
 * <p>Der Access-Token verlässt den Client ausschliesslich Richtung Mojang
 * ({@code joinServer}) — er wird NIE an die Regions-API geschickt und nie
 * geloggt.</p>
 */
public record SessionSnapshot(String username, UUID uuid, String accessToken) {

    public boolean valid() {
        return username != null && !username.isBlank()
                && uuid != null
                && accessToken != null && !accessToken.isBlank();
    }

    /** Niemals den Access-Token ausgeben. */
    @Override
    public String toString() {
        return "SessionSnapshot[" + username + "/" + uuid + "]";
    }
}
