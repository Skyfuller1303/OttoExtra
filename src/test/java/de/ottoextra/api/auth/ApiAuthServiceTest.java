package de.ottoextra.api.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Auth-Zustandsmaschine gegen einen lokalen Stub-Server:
 * Handshake, 410-Retry, joinServer-Fehler, Backoff, Singleton-Future.
 */
class ApiAuthServiceTest {

    private static final SessionSnapshot SESSION = new SessionSnapshot(
            "Skyfuller1303", UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "access-token");

    private HttpServer server;
    private ExecutorService executor;
    private HttpClient http;

    private final AtomicInteger challengeCalls = new AtomicInteger();
    private final AtomicInteger verifyCalls = new AtomicInteger();
    /** Antwort-Skript: Statuscode + Body pro Route, je Test konfiguriert. */
    private volatile int challengeStatus = 200;
    private volatile int verifyStatus = 200;
    private volatile int verify410Count;

    @BeforeEach
    void start() throws IOException {
        executor = Executors.newFixedThreadPool(3);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/auth/challenge", exchange -> {
            challengeCalls.incrementAndGet();
            respond(exchange, challengeStatus, "{\"serverId\":\"abc123\"}");
        });
        server.createContext("/v2/auth/verify", exchange -> {
            verifyCalls.incrementAndGet();
            if (verify410Count > 0) {
                verify410Count--;
                respond(exchange, 410, "{}");
                return;
            }
            respond(exchange, verifyStatus,
                    "{\"token\":\"jwt-value\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}");
        });
        server.start();
        http = HttpClient.newBuilder().executor(executor).build();
    }

    @AfterEach
    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private ApiAuthService service(MojangSessionJoiner joiner, Supplier<SessionSnapshot> session) {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new ApiAuthService(
                http,
                Duration.ofSeconds(5),
                base.resolve("/v2/auth/challenge"),
                base.resolve("/v2/auth/verify"),
                executor,
                session,
                joiner,
                new ResponseVerifier(Map.of(), () -> false),
                Clock.systemUTC());
    }

    @Test
    void successfulHandshakeYieldsToken() {
        AtomicReference<String> joinedServerId = new AtomicReference<>();
        ApiAuthService auth = service((uuid, token, serverId) -> {
            assertEquals(SESSION.uuid(), uuid);
            assertEquals("access-token", token);
            joinedServerId.set(serverId);
        }, () -> SESSION);

        ApiToken token = auth.tokenAsync().join();
        assertEquals("jwt-value", token.token());
        assertEquals(Instant.parse("2099-01-01T00:00:00Z"), token.expiresAt());
        assertEquals("abc123", joinedServerId.get());
        assertEquals(1, challengeCalls.get());
        assertEquals(1, verifyCalls.get());

        // Zweiter Aufruf: Token aus dem RAM, kein neuer Handshake
        assertSame(token, auth.tokenAsync().join());
        assertEquals(1, challengeCalls.get());
    }

    @Test
    void joinServerFailureSkipsVerifyAndBacksOff() {
        ApiAuthService auth = service((uuid, token, serverId) -> {
            throw new Exception("ungültige Session");
        }, () -> SESSION);

        assertThrows(CompletionException.class, () -> auth.tokenAsync().join());
        assertEquals(0, verifyCalls.get(), "verify darf nach joinServer-Fehler nicht aufgerufen werden");
        assertTrue(auth.backedOff());
        // Im Backoff: sofortiger Fehler, kein weiterer Challenge-Call
        int before = challengeCalls.get();
        assertThrows(CompletionException.class, () -> auth.tokenAsync().join());
        assertEquals(before, challengeCalls.get());
    }

    @Test
    void expiredChallengeRetriesExactlyOnce() {
        verify410Count = 1; // erste Verify-Antwort 410, zweite ok
        ApiAuthService auth = service((uuid, token, serverId) -> {
        }, () -> SESSION);

        ApiToken token = auth.tokenAsync().join();
        assertNotNull(token);
        assertEquals(2, challengeCalls.get(), "410 ⇒ genau EIN kompletter Neuversuch");
        assertEquals(2, verifyCalls.get());
    }

    @Test
    void missingSessionFailsWithoutHttpCalls() {
        ApiAuthService auth = service((uuid, token, serverId) -> {
        }, () -> null);

        assertThrows(CompletionException.class, () -> auth.tokenAsync().join());
        assertEquals(0, challengeCalls.get());
        assertEquals(0, verifyCalls.get());
        assertTrue(auth.backedOff());
    }

    @Test
    void concurrentCallsShareSingleHandshake() throws Exception {
        ApiAuthService auth = service((uuid, token, serverId) -> Thread.sleep(150), () -> SESSION);

        CompletableFuture<ApiToken> first = auth.tokenAsync();
        CompletableFuture<ApiToken> second = auth.tokenAsync();
        first.join();
        second.join();
        assertEquals(1, challengeCalls.get(), "parallele Aufrufer hängen am selben Handshake");
    }

    @Test
    void invalidateForcesNewHandshake() {
        ApiAuthService auth = service((uuid, token, serverId) -> {
        }, () -> SESSION);

        auth.tokenAsync().join();
        auth.invalidate();
        auth.tokenAsync().join();
        assertEquals(2, challengeCalls.get());
    }
}
