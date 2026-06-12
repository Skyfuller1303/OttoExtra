package de.ottoextra.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.ottoextra.OttoExtra;
import de.ottoextra.api.auth.ApiAuthService;
import de.ottoextra.api.auth.MinecraftSessionAuth;
import de.ottoextra.api.auth.ResponseVerifier;
import de.ottoextra.api.auth.SpkiPinning;
import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.api.model.CompactPlayer;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.config.OttoExtraConfig;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link OttoExtraApiClient}-Implementierung auf Basis von {@link java.net.http.HttpClient}.
 *
 * <p>Alle Anfragen laufen asynchron auf einem eigenen Daemon-Threadpool — niemals
 * auf dem Render-/Tick-Thread. Antworten werden tolerant
 * geparst (unbekannte Felder ignoriert).</p>
 *
 * <p>Sicherheit: mit {@code api.useV2Auth} laufen
 * Datenabrufe über die authentifizierten {@code /v2}-Routen (Mojang-Handshake →
 * Bearer-Token, nur im RAM). Antwortsignaturen werden per Ed25519 geprüft
 * ({@link ResponseVerifier}); optionales SPKI-Pinning über {@link SpkiPinning}.
 * Ist /v2 nicht erreichbar, fällt der Client automatisch auf die alten
 * {@code public-*}-Routen zurück, bis die Server-Migration abgeschlossen ist.
 * In der Mod gibt es weiterhin keine Secrets — nur Public Keys.</p>
 */
public final class HttpOttoExtraApiClient implements OttoExtraApiClient {

    /** Hartes Größenlimit für Binärdownloads (Banner/Heads) gegen Speicher-Missbrauch. */
    private static final long MAX_BINARY_BYTES = 5L * 1024 * 1024;

    private static final String USER_AGENT = "OttoExtra/" + "client";

    private final Gson gson = new Gson();
    private final OttoExtraConfig.Api apiConfig;
    private final OttoExtraApiRoutes routes;
    private final Duration requestTimeout;
    private final ExecutorService executor;
    private final HttpClient http;
    private final ResponseVerifier verifier;
    private final ApiAuthService auth;

    public HttpOttoExtraApiClient(OttoExtraConfig config) {
        this.apiConfig = config.api;
        this.routes = new OttoExtraApiRoutes(apiConfig.baseUrl);
        this.requestTimeout = Duration.ofMillis(Math.max(1_000, apiConfig.requestTimeoutMs));
        this.executor = Executors.newFixedThreadPool(3, daemonThreads());
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1_000, apiConfig.connectTimeoutMs)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(executor);
        SSLContext pinned = SpkiPinning.pinnedContextOrNull(apiConfig.tlsPinning);
        if (pinned != null) {
            builder.sslContext(pinned);
        }
        this.http = builder.build();
        this.verifier = new ResponseVerifier(() -> apiConfig.requireSignatures);
        this.auth = new ApiAuthService(
                http,
                requestTimeout,
                routes.v2AuthChallenge(),
                routes.v2AuthVerify(),
                executor,
                MinecraftSessionAuth.sessionSupplier(),
                MinecraftSessionAuth.joiner(),
                verifier,
                Clock.systemUTC());
    }

    private static ThreadFactory daemonThreads() {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "ottoextra-api-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    @Override
    public OttoExtraApiRoutes routes() {
        return routes;
    }

    // ---- Öffentliche API ------------------------------------------------

    @Override
    public CompletableFuture<ApiEnvelope> bootstrap() {
        return getJson(routes.v2Bootstrap(), routes.bootstrap(), ApiEnvelope.class);
    }

    @Override
    public CompletableFuture<ApiEnvelope> sync(long cursor) {
        return getJson(routes.v2Sync(cursor), routes.sync(cursor), ApiEnvelope.class);
    }

    @Override
    public CompletableFuture<List<RegionRecord>> regionList() {
        return getJson(routes.v2RegionList(), routes.regionList(), ApiEnvelope.class)
                .thenApply(env -> env.regions() == null ? List.of() : env.regions());
    }

    @Override
    public CompletableFuture<RegionRecord> regionByName(String name) {
        if (name == null || name.isBlank()) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Regionsname fehlt").toException());
        }
        return getJson(routes.v2RegionByName(name), routes.regionByName(name), ApiEnvelope.class)
                .thenApply(ApiEnvelope::region);
    }

    @Override
    public CompletableFuture<FactionRecord> faction(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Fraktions-UUID fehlt").toException());
        }
        return getJson(routes.v2Faction(uuid.toString()), routes.faction(uuid.toString()), ApiEnvelope.class)
                .thenApply(ApiEnvelope::faction);
    }

    @Override
    public CompletableFuture<List<PlayerRecord>> factionPlayers(UUID factionUuid) {
        if (factionUuid == null) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Fraktions-UUID fehlt").toException());
        }
        return getJson(routes.v2FactionPlayers(factionUuid.toString()),
                routes.factionPlayers(factionUuid.toString()), ApiEnvelope.class)
                .thenApply(env -> {
                    if (env.players() != null) return env.players();
                    if (env.participants() != null) return env.participants();
                    return List.of();
                });
    }

    @Override
    public CompletableFuture<PlayerRecord> player(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Spieler-UUID fehlt").toException());
        }
        return getJson(routes.v2Player(uuid.toString()), routes.player(uuid.toString()), ApiEnvelope.class)
                .thenApply(ApiEnvelope::profile);
    }

    @Override
    public CompletableFuture<List<CompactPlayer>> compactPlayers() {
        URI legacy = routes.compactPlayers();
        return getString(routes.v2CompactPlayers(), legacy)
                .thenApply(body -> parseCompactPlayers(legacy, body));
    }

    @Override
    public CompletableFuture<byte[]> downloadBinary(URI uri) {
        if (uri == null) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Download-URI fehlt").toException());
        }
        HttpRequest request = baseRequest(uri).build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(resp -> {
                    if (resp.statusCode() / 100 != 2) {
                        throw ApiProblem.httpStatus(uri, resp.statusCode()).toException();
                    }
                    byte[] bytes = resp.body();
                    if (bytes != null && bytes.length > MAX_BINARY_BYTES) {
                        throw ApiProblem.parse(uri, "Download zu gross: " + bytes.length + " B").toException();
                    }
                    if (!verifier.accept(resp.headers(), bytes)) {
                        throw ApiProblem.parse(uri, "Signatur ungültig").toException();
                    }
                    return bytes;
                })
                .exceptionallyCompose(t -> CompletableFuture.failedFuture(mapError(uri, t)));
    }

    @Override
    public void close() {
        try {
            http.close();
        } catch (Throwable ignored) {
            // best effort
        }
        executor.shutdownNow();
    }

    // ---- Intern ----------------------------------------------------------

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET();
    }

    /**
     * Datenabruf: bevorzugt authentifiziert über /v2, bei 401 genau EIN
     * Re-Handshake; scheitert v2 (Backoff, nicht deployed, Auth kaputt),
     * Fallback auf die alte public-*-Route.
     */
    private CompletableFuture<String> getString(URI v2Uri, URI legacyUri) {
        if (!apiConfig.useV2Auth) {
            return fetch(legacyUri, null);
        }
        return auth.tokenAsync()
                .thenCompose(token -> fetch(v2Uri, token.token())
                        .exceptionallyCompose(t -> {
                            if (isHttpStatus(t, 401)) {
                                // Token serverseitig ungültig: verwerfen, EIN Re-Auth, wiederholen
                                auth.invalidate();
                                return auth.tokenAsync()
                                        .thenCompose(fresh -> fetch(v2Uri, fresh.token()));
                            }
                            return CompletableFuture.failedFuture(t);
                        }))
                .exceptionallyCompose(t -> {
                    OttoExtra.LOGGER.debug("[api] v2 nicht nutzbar ({}) — Fallback public-*", summarize(t));
                    return fetch(legacyUri, null);
                });
    }

    /** Roh-Abruf inkl. Signatur-Check über die exakt empfangenen Bytes. */
    private CompletableFuture<String> fetch(URI uri, String bearerToken) {
        HttpRequest.Builder builder = baseRequest(uri);
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(resp -> {
                    if (resp.statusCode() / 100 != 2) {
                        throw ApiProblem.httpStatus(uri, resp.statusCode()).toException();
                    }
                    byte[] raw = resp.body();
                    if (!verifier.accept(resp.headers(), raw)) {
                        OttoExtra.LOGGER.warn("[api] Signatur ungültig — Antwort verworfen ({})", uri.getPath());
                        throw ApiProblem.parse(uri, "Signatur ungültig").toException();
                    }
                    return new String(raw == null ? new byte[0] : raw, StandardCharsets.UTF_8);
                })
                .exceptionallyCompose(t -> CompletableFuture.failedFuture(mapError(uri, t)));
    }

    private <T> CompletableFuture<T> getJson(URI v2Uri, URI legacyUri, Class<T> type) {
        return getString(v2Uri, legacyUri).thenApply(body -> {
            try {
                T value = gson.fromJson(body, type);
                if (value == null) {
                    throw ApiProblem.parse(legacyUri, "Leere Antwort").toException();
                }
                return value;
            } catch (ApiProblem.ApiException e) {
                throw e;
            } catch (Exception e) {
                throw ApiProblem.parse(legacyUri, e.getMessage()).toException();
            }
        });
    }

    private List<CompactPlayer> parseCompactPlayers(URI uri, String body) {
        try {
            JsonElement root = com.google.gson.JsonParser.parseString(body);
            JsonArray array;
            if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                JsonElement players = obj.has("players") ? obj.get("players")
                        : obj.has("participants") ? obj.get("participants") : null;
                array = players != null && players.isJsonArray() ? players.getAsJsonArray() : new JsonArray();
            } else {
                array = new JsonArray();
            }
            return gson.fromJson(array, new TypeToken<List<CompactPlayer>>() {
            }.getType());
        } catch (Exception e) {
            throw ApiProblem.parse(uri, e.getMessage()).toException();
        }
    }

    /** Trägt der Fehler genau diesen HTTP-Status? */
    private static boolean isHttpStatus(Throwable t, int status) {
        Throwable cause = (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
        return cause instanceof ApiProblem.ApiException api && api.problem().isHttpStatus(status);
    }

    /** Kurzfassung für Debug-Logs — keine Bodies, keine Tokens. */
    private static String summarize(Throwable t) {
        Throwable cause = (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
        if (cause instanceof ApiProblem.ApiException api) {
            return api.problem().kind() + " " + api.problem().message();
        }
        return cause.getClass().getSimpleName();
    }

    /** Normalisiert beliebige Fehler zu einer {@link ApiProblem.ApiException}. */
    private static Throwable mapError(URI uri, Throwable t) {
        Throwable cause = (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
        if (cause instanceof ApiProblem.ApiException) {
            return cause;
        }
        OttoExtra.LOGGER.debug("API-Fehler {}: {}", uri, cause.toString());
        return ApiProblem.offline(uri, cause.getClass().getSimpleName()).toException();
    }
}
