package de.ottoextra.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.ottoextra.api.auth.ApiAuthService;
import de.ottoextra.api.auth.ResponseVerifier;
import de.ottoextra.api.auth.SessionSnapshot;
import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.api.model.CompactPlayer;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpOttoExtraApiClientBootstrapTest {
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    private HttpServer server;
    private ExecutorService executor;
    private KeyPair signingKey;
    private HttpOttoExtraApiClient client;
    private final AtomicBoolean signBootstrap = new AtomicBoolean(true);
    private final AtomicInteger legacyRequests = new AtomicInteger();
    private final AtomicReference<String> bootstrapAuthorization = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        signingKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        executor = Executors.newFixedThreadPool(4);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/auth/challenge", exchange ->
                respond(exchange, 200, "{\"serverId\":\"challenge\"}", true));
        server.createContext("/v2/auth/verify", exchange ->
                respond(exchange, 200,
                        "{\"token\":\"player-token\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}",
                        true));
        server.createContext("/v2/bootstrap", exchange -> {
            bootstrapAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"ok\":true,\"sync_cursor\":1,\"regions\":[]}",
                    signBootstrap.get());
        });
        server.createContext("/v2/player-compact", exchange -> respond(exchange, 200,
                "{\"players\":[{\"entity_key\":\"player|1\","
                        + "\"uuid\":\"069a79f4-44e9-4726-a5be-fca90e38aaf5\","
                        + "\"name\":\"Visible\",\"minecraft_name\":\"Account\","
                        + "\"rp_name\":\"RP Name\",\"title\":\"Graf\","
                        + "\"rank\":\"LEADER\",\"state\":\"Adliger\","
                        + "\"faction\":\"faction-uuid\","
                        + "\"faction_name\":\"Faction\"}]}", true));
        server.createContext("/uploads/banner.png", exchange ->
                respond(exchange, 200, PNG_BYTES));
        server.createContext("/api/index.php", exchange -> {
            legacyRequests.incrementAndGet();
            respond(exchange, 200, "{\"ok\":true,\"regions\":[]}", false);
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
    void bootstrapUsesSignedAuthenticatedV2Endpoint() {
        ApiEnvelope envelope = client.bootstrap().join();

        assertTrue(envelope.ok());
        assertEquals("Bearer player-token", bootstrapAuthorization.get());
        assertEquals(0, legacyRequests.get());
    }

    @Test
    void compactPlayersMapsSnakeCaseAffiliationFields() {
        CompactPlayer player = client.compactPlayers().join().getFirst();

        assertEquals("player|1", player.entityKey());
        assertEquals("Account", player.minecraftName());
        assertEquals("RP Name", player.rpName());
        assertEquals("faction-uuid", player.faction());
        assertEquals("Faction", player.factionName());
        assertEquals("LEADER", player.rank());
    }

    @Test
    void invalidV2SignatureDoesNotFallBackToUnsignedLegacyEndpoint() {
        signBootstrap.set(false);

        CompletionException error = assertThrows(CompletionException.class,
                () -> client.bootstrap().join());

        assertTrue(error.getCause().getMessage().contains("Signatur ungültig"));
        assertEquals(0, legacyRequests.get());
    }

    @Test
    void unsignedSameOriginBannerIsAccepted() {
        byte[] bytes = client.downloadBinary(
                client.routes().resolveRelative("uploads/banner.png")).join();

        assertArrayEquals(PNG_BYTES, bytes);
    }

    @Test
    void crossOriginBannerIsRejectedBeforeDownload() {
        CompletionException error = assertThrows(CompletionException.class,
                () -> client.downloadBinary(URI.create("https://example.com/banner.png")).join());

        assertTrue(error.getCause().getMessage().contains("Nicht vertrauenswürdige Download-URI"));
    }

    private void respond(HttpExchange exchange, int status, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void respond(HttpExchange exchange, int status, String body, boolean signed)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (signed) {
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
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
