package de.ottoextra.api;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.ottoextra.api.auth.*;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.privacy.ConsentPermit;
import de.ottoextra.privacy.ConsentPolicy;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RpNameAuthenticatedTransportTest {
    HttpServer server;
    ExecutorService executor;
    HttpOttoExtraApiClient client;
    OttoExtraConfig config;
    KeyPair key;
    String base;
    final AtomicInteger posts = new AtomicInteger();
    final AtomicInteger logins = new AtomicInteger();
    final CountDownLatch posted = new CountDownLatch(1), release = new CountDownLatch(1);
    volatile boolean retry, delay, badSignature;
    volatile String body, bearer;

    @BeforeEach void start() throws Exception {
        executor = Executors.newFixedThreadPool(6);
        key = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/v2/auth/challenge", e -> respond(e, 200, "{\"serverId\":\"challenge\"}", false));
        server.createContext("/v2/auth/verify", e -> {
            int n = logins.incrementAndGet();
            respond(e, 200, "{\"token\":\"actor-token-" + n + "\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}", false);
        });
        server.createContext("/v2/rp-names/observations", e -> {
            int n = posts.incrementAndGet();
            bearer = e.getRequestHeaders().getFirst("Authorization");
            body = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            posted.countDown();
            if (delay) {
                try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
            respond(e, retry && n == 1 ? 401 : 202, "{\"ok\":true,\"message\":\"proposal\"}", badSignature);
        });
        server.start(); base = "http://127.0.0.1:" + server.getAddress().getPort();
        config = new OttoExtraConfig(); config.api.baseUrl = base;
        ConsentPolicy.decide(config, ConsentPolicy.Purpose.ONLINE_SERVICES, true, ConsentPolicy.scope(config, ConsentPolicy.Purpose.ONLINE_SERVICES));
        ConsentPolicy.decide(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS, true, ConsentPolicy.scope(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS));
        ResponseVerifier verifier = new ResponseVerifier(Map.of("qa", Base64.getEncoder().encodeToString(key.getPublic().getEncoded())), () -> true);
        HttpClient http = HttpClient.newBuilder().executor(executor).followRedirects(HttpClient.Redirect.NEVER).build();
        var auth = new ApiAuthService(http, Duration.ofSeconds(4), URI.create(base + "/v2/auth/challenge"),
                URI.create(base + "/v2/auth/verify"), executor,
                () -> new SessionSnapshot("AliceQa", UUID.fromString("11111111-1111-4111-8111-111111111111"), "qa-minecraft-token"),
                (u, t, s) -> {}, verifier, Clock.systemUTC(), () -> ConsentPolicy.capture(config, ConsentPolicy.Purpose.ONLINE_SERVICES));
        client = new HttpOttoExtraApiClient(config, executor, http, verifier, auth);
    }

    @AfterEach void stop() {
        release.countDown(); if (server != null) server.stop(0);
        if (client != null) client.close(); if (executor != null) executor.shutdownNow();
    }

    JsonObject payload() {
        JsonObject body = new JsonObject(); body.addProperty("target_uuid", "22222222-2222-4222-8222-222222222222");
        body.addProperty("rp_name", "Bob Roleplay"); return body;
    }

    CompletableFuture<JsonObject> transport(ConsentPermit permit) {
        return client.submitRpNameObservation(payload(), permit);
    }

    ConsentPermit uploadPermit() {
        return ConsentPolicy.capture(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS);
    }

    void decide(ConsentPolicy.Purpose purpose, boolean allow) {
        ConsentPolicy.decide(config, purpose, allow, ConsentPolicy.scope(config, purpose));
    }

    @Test void signedUploadUsesVerifiedBearerAndNoActorClaim() throws Exception {
        assertTrue(transport(uploadPermit()).get(5, TimeUnit.SECONDS).get("ok").getAsBoolean());
        assertEquals("Bearer actor-token-1", bearer);
        assertFalse(body.contains("actor_uuid")); assertFalse(body.contains("player_name"));
        assertEquals(1, posts.get());
    }

    @Test void missingUploadConsentCannotBeBypassedByCallerPermit() {
        decide(ConsentPolicy.Purpose.COMMUNITY_UPLOADS, false);
        assertThrows(Exception.class, () -> client.submitRpNameObservation(payload(), new ConsentPermit.Gate().capture(() -> true)).get(2, TimeUnit.SECONDS));
        assertEquals(0, logins.get()); assertEquals(0, posts.get());
    }

    @Test void uploadConsentWithoutOnlineConsentNeverStartsAuthentication() {
        decide(ConsentPolicy.Purpose.ONLINE_SERVICES, false);
        assertThrows(Exception.class, () -> transport(uploadPermit()).get(2, TimeUnit.SECONDS));
        assertEquals(0, logins.get()); assertEquals(0, posts.get());
    }

    @Test void regrantDoesNotReactivateAnEarlierUploadJob() {
        var permit = uploadPermit();
        decide(ConsentPolicy.Purpose.COMMUNITY_UPLOADS, false);
        decide(ConsentPolicy.Purpose.COMMUNITY_UPLOADS, true);
        assertThrows(Exception.class, () -> transport(permit).get(2, TimeUnit.SECONDS));
        assertEquals(0, logins.get()); assertEquals(0, posts.get());
    }

    @Test void revokedGrantNeverStartsAuthentication() throws Exception {
        var gate = new ConsentPermit.Gate(); var permit = gate.capture(() -> true); gate.invalidate();
        assertThrows(Exception.class, () -> transport(permit).get(2, TimeUnit.SECONDS));
        assertEquals(0, logins.get()); assertEquals(0, posts.get());
    }

    @Test void rejectedTokenRefreshesOnce() throws Exception {
        retry = true;
        assertTrue(transport(uploadPermit()).get(5, TimeUnit.SECONDS).get("ok").getAsBoolean());
        assertEquals(2, posts.get()); assertEquals(2, logins.get()); assertEquals("Bearer actor-token-2", bearer);
    }

    @Test void revocationDuringUploadPreventsRefreshAndRetry() throws Exception {
        retry = true; delay = true;
        var future = transport(uploadPermit());
        assertTrue(posted.await(5, TimeUnit.SECONDS));
        decide(ConsentPolicy.Purpose.COMMUNITY_UPLOADS, false); release.countDown();
        assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals(1, posts.get()); assertEquals(1, logins.get());
    }

    @Test void onlineRevocationDuringUploadPreventsRefreshAndRetry() throws Exception {
        retry = true; delay = true;
        var future = transport(uploadPermit());
        assertTrue(posted.await(5, TimeUnit.SECONDS));
        decide(ConsentPolicy.Purpose.ONLINE_SERVICES, false); release.countDown();
        assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals(1, posts.get()); assertEquals(1, logins.get());
    }

    @Test void forgedSuccessSignatureIsRejected() throws Exception {
        badSignature = true;
        assertThrows(Exception.class, () -> transport(uploadPermit()).get(5, TimeUnit.SECONDS));
        assertEquals(1, posts.get());
    }

    @Test void forgedUnauthorizedResponseCannotTriggerAnotherLogin() throws Exception {
        retry = true; badSignature = true;
        assertThrows(Exception.class, () -> transport(uploadPermit()).get(5, TimeUnit.SECONDS));
        assertEquals(1, posts.get()); assertEquals(1, logins.get());
    }

    void respond(HttpExchange e, int status, String body, boolean tamper) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            Signature signer = Signature.getInstance("Ed25519"); signer.initSign(key.getPrivate()); signer.update(bytes);
            byte[] signature = signer.sign(); if (tamper) signature[0] ^= 1;
            e.getResponseHeaders().set(ResponseVerifier.KEY_ID_HEADER, "qa");
            e.getResponseHeaders().set(ResponseVerifier.SIGNATURE_HEADER, Base64.getEncoder().encodeToString(signature));
            e.sendResponseHeaders(status, bytes.length); e.getResponseBody().write(bytes);
        } catch (GeneralSecurityException ex) { throw new java.io.IOException(ex); }
        finally { e.close(); }
    }
}
