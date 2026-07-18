package de.ottoextra.api.auth;

import java.util.UUID;

@FunctionalInterface
public interface MojangSessionJoiner {

    void joinServer(UUID profileUuid, String accessToken, String serverId) throws Exception;
}
