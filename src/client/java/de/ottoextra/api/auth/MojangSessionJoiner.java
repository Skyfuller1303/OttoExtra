package de.ottoextra.api.auth;

import java.util.UUID;

/**
 * Abstraktion über Mojangs {@code joinServer}, damit der
 * {@link ApiAuthService} ohne Minecraft-Klassen testbar bleibt.
 *
 * <p>Produktiv-Implementierung: {@link MinecraftSessionAuth#joiner()}.</p>
 */
@FunctionalInterface
public interface MojangSessionJoiner {

    /**
     * Meldet die laufende Session bei Mojang für die gegebene serverId an.
     * Blockierender HTTP-Call — nur auf dem API-Executor aufrufen.
     *
     * @throws Exception bei ungültiger Session (Offline-/Cracked-Client)
     *                   oder Mojang-Fehlern ({@code AuthenticationException})
     */
    void joinServer(UUID profileUuid, String accessToken, String serverId) throws Exception;
}
