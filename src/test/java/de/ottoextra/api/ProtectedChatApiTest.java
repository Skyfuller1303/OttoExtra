package de.ottoextra.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.ottoextra.api.auth.ApiAuthService;
import de.ottoextra.api.auth.ResponseVerifier;
import de.ottoextra.api.auth.SessionSnapshot;
import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedChatApiTest {
    private HttpServer server;
    private ExecutorService executor;
    private KeyPair signingKey;
    private HttpOttoExtraApiClient client;
    private final AtomicReference<String> postedBody = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        signingKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        executor = Executors.newFixedThreadPool(4);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/auth/challenge", exchange ->
                respond(exchange, 200, "{\"serverId\":\"challenge\"}"));
        server.createContext("/v2/auth/verify", exchange ->
                respond(exchange, 200,
                        "{\"token\":\"player-token\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}"));
        server.createContext("/v2/chat-translations", exchange -> {
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            if ("POST".equals(exchange.getRequestMethod())) {
                postedBody.set(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                respond(exchange, 201,
                        "{\"id\":\"share_12345678\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}");
            } else {
                respond(exchange, 405, "{}");
            }
        });
        server.createContext("/v2/chat-translations/share_12345678", exchange -> {
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200,
                    "{\"original\":\"Original auf Deutsch\",\"senderUuid\":\"069a79f4-44e9-4726-a5be-fca90e38aaf5\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}");
        });
        server.start();

        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        ResponseVerifier verifier = new ResponseVerifier(Map.of("k1",
                Base64.getEncoder().encodeToString(signingKey.getPublic().getEncoded())), () -> true);
        HttpClient http = HttpClient.newBuilder().executor(executor).build();
        SessionSnapshot session = new SessionSnapshot("Skyfuller1303",
                UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "mc-token");
        ApiAuthService auth = new ApiAuthService(http, Duration.ofSeconds(5),
                URI.create(base + "/v2/auth/challenge"),
                URI.create(base + "/v2/auth/verify"), executor,
                () -> session, (uuid, token, serverId) -> { }, verifier, Clock.systemUTC());
        OttoExtraConfig config = new OttoExtraConfig();
        config.api.baseUrl = base;
        client = new HttpOttoExtraApiClient(config, executor, http, verifier, auth);
    }

    @AfterEach
    void stop() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
    }

    @Test
    void createsShareAndFetchesOriginalWithIndividualBearerToken() {
        String id = client.createProtectedChatMessage(
                "Original auf Deutsch", List.of("Skyfuller1303")).join();
        assertEquals("share_12345678", id);
        assertTrue(postedBody.get().contains("\"access\":\"allowlist\""));
        assertTrue(postedBody.get().contains("Skyfuller1303"));

        assertEquals("Original auf Deutsch", client.protectedChatMessage(id).join());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(signingKey.getPrivate());
            signer.update(bytes);
            exchange.getResponseHeaders().set(ResponseVerifier.KEY_ID_HEADER, "k1");
            exchange.getResponseHeaders().set(ResponseVerifier.SIGNATURE_HEADER,
                    Base64.getEncoder().encodeToString(signer.sign()));
        } catch (Exception e) {
            throw new IOException(e);
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
