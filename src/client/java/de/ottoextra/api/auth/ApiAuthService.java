package de.ottoextra.api.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;
import de.ottoextra.api.ApiProblem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ApiAuthService {

    static final Duration FAILURE_BACKOFF = Duration.ofMinutes(5);

    static final Duration NOT_DEPLOYED_BACKOFF = Duration.ofMinutes(30);

    private final HttpClient http;
    private final Duration requestTimeout;
    private final URI challengeUri;
    private final URI verifyUri;
    private final Executor executor;
    private final Supplier<SessionSnapshot> sessionSupplier;
    private final MojangSessionJoiner joiner;
    private final ResponseVerifier verifier;
    private final Clock clock;

    private volatile ApiToken token;

    private final AtomicReference<CompletableFuture<ApiToken>> pending = new AtomicReference<>();
    private volatile Instant backoffUntil = Instant.EPOCH;

    public ApiAuthService(HttpClient http,
                          Duration requestTimeout,
                          URI challengeUri,
                          URI verifyUri,
                          Executor executor,
                          Supplier<SessionSnapshot> sessionSupplier,
                          MojangSessionJoiner joiner,
                          ResponseVerifier verifier,
                          Clock clock) {
        this.http = http;
        this.requestTimeout = requestTimeout;
        this.challengeUri = challengeUri;
        this.verifyUri = verifyUri;
        this.executor = executor;
        this.sessionSupplier = sessionSupplier;
        this.joiner = joiner;
        this.verifier = verifier;
        this.clock = clock;
    }

    public CompletableFuture<ApiToken> tokenAsync() {
        ApiToken current = token;
        if (current != null && current.usable(clock.instant())) {
            return CompletableFuture.completedFuture(current);
        }
        if (clock.instant().isBefore(backoffUntil)) {
            return CompletableFuture.failedFuture(
                    ApiProblem.offline(challengeUri, "auth-backoff").toException());
        }
        CompletableFuture<ApiToken> running = pending.get();
        if (running != null) {
            return running;
        }
        CompletableFuture<ApiToken> attempt = new CompletableFuture<>();
        if (!pending.compareAndSet(null, attempt)) {
            return pending.get();
        }
        CompletableFuture
                .supplyAsync(this::handshakeWithRetry, executor)
                .whenComplete((result, error) -> {
                    pending.set(null);
                    if (error != null) {
                        backoffUntil = clock.instant().plus(backoffFor(error));
                        OttoExtra.LOGGER.info("[api/auth] Handshake fehlgeschlagen ({}) — Backoff bis {}",
                                summarize(error), backoffUntil);
                        attempt.completeExceptionally(error);
                    } else {
                        token = result;
                        OttoExtra.LOGGER.info("[api/auth] ANON→VERIFIED, Token gültig bis {}", result.expiresAt());
                        attempt.complete(result);
                    }
                });
        return attempt;
    }

    public void invalidate() {
        token = null;
    }

    public boolean backedOff() {
        return clock.instant().isBefore(backoffUntil);
    }

    private ApiToken handshakeWithRetry() {
        try {
            return handshakeOnce();
        } catch (ChallengeExpiredException first) {

            try {
                return handshakeOnce();
            } catch (ChallengeExpiredException second) {
                throw ApiProblem.httpStatus(verifyUri, 410).toException();
            }
        }
    }

    private ApiToken handshakeOnce() {
        SessionSnapshot session = sessionSupplier.get();
        if (session == null || !session.valid()) {

            OttoExtra.LOGGER.info("[api/auth] keine gültige Session — Offline-Modus");
            throw ApiProblem.badRequest("Keine gültige Minecraft-Session").toException();
        }

        JsonObject challengeRequest = new JsonObject();
        challengeRequest.addProperty("username", session.username());
        challengeRequest.addProperty("uuid", session.uuid().toString());
        JsonObject challenge = postJson(challengeUri, challengeRequest);
        String serverId = challenge.has("serverId") ? challenge.get("serverId").getAsString() : null;
        if (serverId == null || serverId.isBlank()) {
            throw ApiProblem.parse(challengeUri, "serverId fehlt").toException();
        }

        try {

            joiner.joinServer(session.uuid(), session.accessToken(), serverId);
        } catch (Exception e) {
            OttoExtra.LOGGER.info("[api/auth] keine gültige Session — Offline-Modus");
            throw ApiProblem.offline(challengeUri, "joinServer: " + e.getClass().getSimpleName()).toException();
        }

        JsonObject verifyRequest = new JsonObject();
        verifyRequest.addProperty("username", session.username());
        verifyRequest.addProperty("serverId", serverId);
        JsonObject verify = postJson(verifyUri, verifyRequest);
        String tokenValue = verify.has("token") ? verify.get("token").getAsString() : null;
        String expiresAt = verify.has("expiresAt") ? verify.get("expiresAt").getAsString() : null;
        if (tokenValue == null || tokenValue.isBlank() || expiresAt == null) {
            throw ApiProblem.parse(verifyUri, "token/expiresAt fehlt").toException();
        }
        try {
            return new ApiToken(tokenValue, Instant.parse(expiresAt));
        } catch (Exception e) {
            throw ApiProblem.parse(verifyUri, "expiresAt unlesbar").toException();
        }
    }

    private JsonObject postJson(URI uri, JsonObject body) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw ApiProblem.offline(uri, e.getClass().getSimpleName()).toException();
        }
        int status = response.statusCode();
        if (status == 410) {
            throw new ChallengeExpiredException();
        }
        if (status / 100 != 2) {
            throw ApiProblem.httpStatus(uri, status).toException();
        }
        byte[] raw = response.body();
        if (!verifier.accept(response.headers(), raw)) {
            throw ApiProblem.parse(uri, "Signatur ungültig").toException();
        }
        try {
            return JsonParser.parseString(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw ApiProblem.parse(uri, "Antwort kein JSON-Objekt").toException();
        }
    }

    private static Duration backoffFor(Throwable error) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        if (cause instanceof ApiProblem.ApiException api
                && api.problem().kind() == ApiProblem.Kind.HTTP_STATUS
                && "HTTP 404".equals(api.problem().message())) {
            return NOT_DEPLOYED_BACKOFF;
        }
        return FAILURE_BACKOFF;
    }

    private static String summarize(Throwable error) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        if (cause instanceof ApiProblem.ApiException api) {
            return api.problem().kind() + " " + api.problem().message();
        }
        return cause.getClass().getSimpleName();
    }

    private static final class ChallengeExpiredException extends RuntimeException {
    }
}
