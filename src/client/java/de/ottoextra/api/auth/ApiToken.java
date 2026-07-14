package de.ottoextra.api.auth;

import java.time.Duration;
import java.time.Instant;

public record ApiToken(String token, Instant expiresAt) {

    public static final Duration RENEW_MARGIN = Duration.ofSeconds(120);

    public boolean usable(Instant now) {
        return token != null && !token.isBlank()
                && expiresAt != null
                && now.isBefore(expiresAt.minus(RENEW_MARGIN));
    }

    @Override
    public String toString() {
        return "ApiToken[expiresAt=" + expiresAt + "]";
    }
}
