package de.ottoextra.api.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.api.ApiProblem;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.privacy.ConsentPermit;
import de.ottoextra.privacy.ConsentHttp;

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

    static final Duration FAILURE_BACKOFF = Duration.ofSeconds(60);

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
    private final Supplier<ConsentPermit> permits;
    private record CachedToken(ApiToken token, ConsentPermit permit) { }
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();
    private volatile Runnable unregisterToken = () -> { };

    private final AtomicReference<CompletableFuture<ApiToken>> pending = new AtomicReference<>();
    private final java.util.concurrent.atomic.AtomicLong authGeneration = new java.util.concurrent.atomic.AtomicLong();
    private volatile Instant backoffUntil = Instant.EPOCH;

    public ApiAuthService(HttpClient http, Duration requestTimeout, URI challengeUri, URI verifyUri,
                          Executor executor, Supplier<SessionSnapshot> sessionSupplier,
                          MojangSessionJoiner joiner, ResponseVerifier verifier, Clock clock,
                          Supplier<ConsentPermit> permits) {
        this.http = http;
        this.requestTimeout = requestTimeout;
        this.challengeUri = challengeUri;
        this.verifyUri = verifyUri;
        this.executor = executor;
        this.sessionSupplier = sessionSupplier;
        this.joiner = joiner;
        this.verifier = verifier;
        this.clock = clock;
        this.permits = permits;
    }

    public CompletableFuture<ApiToken> tokenAsync() {
        long generation = authGeneration.get();
        ConsentPermit permit = permits.get().restrict(() -> generation == authGeneration.get());
        if (!permit.valid()) {
            invalidate();
            return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException("Online services disabled"));
        }
        CachedToken current = cachedToken.get();
        if (current != null && current.permit().valid() && current.token().usable(clock.instant())) {
            return CompletableFuture.completedFuture(current.token());
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
            return tokenAsync();
        }
        permit.start(() -> CompletableFuture.supplyAsync(() -> handshakeWithRetry(permit), executor))
                .whenComplete((result, error) -> {
                    if (pending.get() != attempt || !permit.valid()) {
                        pending.compareAndSet(attempt, null);
                        attempt.completeExceptionally(new java.util.concurrent.CancellationException("Authentication revoked"));
                        return;
                    }
                    if (error != null) {
                        backoffUntil = clock.instant().plus(backoffFor(error));
                        DebugLog.debug("[api/auth] Handshake fehlgeschlagen ({}) — Backoff bis {}",
                                summarize(error), backoffUntil);
                        pending.compareAndSet(attempt, null);
                        attempt.completeExceptionally(error);
                    } else {
                        try {
                            permit.commit(() -> {
                                unregisterToken.run();
                                CachedToken stored = new CachedToken(result, permit);
                                cachedToken.set(stored);
                                unregisterToken = permit.onRevoke(() -> {
                                    if (cachedToken.compareAndSet(stored, null)) unregisterToken = () -> { };
                                });
                            });
                            pending.compareAndSet(attempt, null);
                            permit.require();
                            // Dependent callbacks may acquire service locks; never run them under the consent gate.
                            attempt.complete(result);
                        } catch (java.util.concurrent.CancellationException revoked) {
                            pending.compareAndSet(attempt, null);
                            attempt.completeExceptionally(revoked);
                        }
                    }
                });
        return attempt;
    }

    public void invalidate() {
        authGeneration.incrementAndGet();
        cachedToken.set(null);
        unregisterToken.run();
        unregisterToken = () -> { };
        CompletableFuture<ApiToken> old = pending.getAndSet(null);
        if (old != null) old.cancel(true);
        backoffUntil = Instant.EPOCH;
    }

    public boolean backedOff() {
        return clock.instant().isBefore(backoffUntil);
    }

    private ApiToken handshakeWithRetry(ConsentPermit permit) {
        try {
            return handshakeOnce(permit);
        } catch (ChallengeExpiredException first) {

            try {
                return handshakeOnce(permit);
            } catch (ChallengeExpiredException second) {
                throw ApiProblem.httpStatus(verifyUri, 410).toException();
            }
        }
    }

    private ApiToken handshakeOnce(ConsentPermit permit) {
        permit.require();
        SessionSnapshot session = sessionSupplier.get();
        if (session == null || !session.valid()) {

            DebugLog.debug("[api/auth] keine gültige Session — Offline-Modus");
            throw ApiProblem.badRequest("Keine gültige Minecraft-Session").toException();
        }

        JsonObject challengeRequest = new JsonObject();
        challengeRequest.addProperty("username", session.username());
        challengeRequest.addProperty("uuid", session.uuid().toString());
        JsonObject challenge = postJson(challengeUri, challengeRequest, permit);
        // Eine gültige, signierte Challenge beweist, dass das Backend wieder
        // erreichbar ist. Frühere temporäre Fehler dürfen nicht weiter sperren.
        backoffUntil = Instant.EPOCH;
        String serverId = challenge.has("serverId") ? challenge.get("serverId").getAsString() : null;
        if (serverId == null || serverId.isBlank()) {
            throw ApiProblem.parse(challengeUri, "serverId fehlt").toException();
        }
        if (serverId.length() > 40) {
            throw ApiProblem.parse(challengeUri,
                    "serverId ist nicht Minecraft-kompatibel (maximal 40 Zeichen)").toException();
        }

        try {

            permit.require();
            joiner.joinServer(session.uuid(), session.accessToken(), serverId);
        } catch (Exception e) {
            String detail = safeExceptionDetail(e);
            DebugLog.debug("[api/auth] Mojang joinServer fehlgeschlagen ({})", detail);
            throw ApiProblem.offline(challengeUri, "joinServer: " + detail).toException();
        }

        JsonObject verifyRequest = new JsonObject();
        verifyRequest.addProperty("username", session.username());
        verifyRequest.addProperty("serverId", serverId);
        JsonObject verify = postJson(verifyUri, verifyRequest, permit);
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

    private JsonObject postJson(URI uri, JsonObject body, ConsentPermit permit) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response;
        try {
            response = ConsentHttp.send(http, request, de.ottoextra.privacy.BoundedBody.bytes(64 * 1024), permit).join();
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

    private static String safeExceptionDetail(Exception error) {
        // Authlib/server messages can echo credentials or challenge values. Log only the error type.
        return error.getClass().getSimpleName();
    }

    private static final class ChallengeExpiredException extends RuntimeException {
    }
}
