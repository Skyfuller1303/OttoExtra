package de.ottoextra.api.auth;

import java.time.Duration;
import java.time.Instant;

/**
 * Kurzlebiges Bearer-Token der v2-API.
 *
 * <p>Lebt ausschliesslich im RAM des {@link ApiAuthService} — niemals in
 * Config, Cache-Dateien oder Logs. Der Inhalt ist für die Mod opak (JWT wird
 * nie geparst).</p>
 */
public record ApiToken(String token, Instant expiresAt) {

    /** Vorlauf für proaktive Erneuerung (Uhren-Drift). */
    public static final Duration RENEW_MARGIN = Duration.ofSeconds(120);

    /** Token noch nutzbar? Gilt ab {@code expiresAt - 120 s} als abgelaufen. */
    public boolean usable(Instant now) {
        return token != null && !token.isBlank()
                && expiresAt != null
                && now.isBefore(expiresAt.minus(RENEW_MARGIN));
    }

    /** Niemals den Token-Inhalt ausgeben. */
    @Override
    public String toString() {
        return "ApiToken[expiresAt=" + expiresAt + "]";
    }
}
