package de.ottoextra.api.auth;
import java.util.UUID;
public record SessionSnapshot(String username, UUID uuid, String accessToken) {
    public boolean valid() {
        return username != null && !username.isBlank()
                && uuid != null
                && accessToken != null && !accessToken.isBlank();
    }
    @Override
    public String toString() {
        return "SessionSnapshot[" + username + "/" + uuid + "]";
    }
}
