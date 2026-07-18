package de.ottoextra.api.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTokenTest {

    private static final Instant NOW = Instant.parse("2026-06-13T12:00:00Z");

    @Test
    void usableWellBeforeExpiry() {
        ApiToken token = new ApiToken("t", NOW.plus(Duration.ofHours(1)));
        assertTrue(token.usable(NOW));
    }

    @Test
    void expiredWithinRenewMargin() {
        ApiToken token = new ApiToken("t", NOW.plus(Duration.ofSeconds(119)));
        assertFalse(token.usable(NOW));
    }

    @Test
    void usableJustOutsideRenewMargin() {
        ApiToken token = new ApiToken("t", NOW.plus(Duration.ofSeconds(121)));
        assertTrue(token.usable(NOW));
    }

    @Test
    void neverUsableWithoutTokenValue() {
        assertFalse(new ApiToken(null, NOW.plus(Duration.ofHours(1))).usable(NOW));
        assertFalse(new ApiToken("  ", NOW.plus(Duration.ofHours(1))).usable(NOW));
        assertFalse(new ApiToken("t", null).usable(NOW));
    }

    @Test
    void toStringNeverLeaksTokenValue() {
        ApiToken token = new ApiToken("super-geheimes-token", NOW);
        assertFalse(token.toString().contains("super-geheimes-token"));
    }
}
