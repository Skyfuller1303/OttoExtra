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
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseName;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.api.model.CompactPlayer;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.api.model.ProtectedChatInboxEntry;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class HttpOttoExtraApiClient implements OttoExtraApiClient {

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
    private final AtomicBoolean warnedInvalidSignature = new AtomicBoolean();

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
        // Backend-JSON wird ausschließlich mit dem fest eingebetteten,
        // gemeinsamen Trust-Key akzeptiert. Der Server darf keinen Key liefern.
        this.verifier = new ResponseVerifier(() -> true);
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

    HttpOttoExtraApiClient(OttoExtraConfig config, ExecutorService executor,
                           HttpClient http, ResponseVerifier verifier,
                           ApiAuthService auth) {
        this.apiConfig = config.api;
        this.routes = new OttoExtraApiRoutes(apiConfig.baseUrl);
        this.requestTimeout = Duration.ofMillis(Math.max(1_000, apiConfig.requestTimeoutMs));
        this.executor = executor;
        this.http = http;
        this.verifier = verifier;
        this.auth = auth;
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

    @Override
    public CompletableFuture<ApiEnvelope> bootstrap() {
        return getJson(routes.v2Bootstrap(), ApiEnvelope.class);
    }

    @Override
    public CompletableFuture<ApiEnvelope> sync(long cursor) {
        return getJson(routes.v2Sync(cursor), ApiEnvelope.class);
    }

    @Override
    public CompletableFuture<List<RegionRecord>> regionList() {
        return getJson(routes.v2RegionList(), ApiEnvelope.class)
                .thenApply(env -> env.regions() == null ? List.of() : env.regions());
    }

    @Override
    public CompletableFuture<RegionRecord> regionByName(String name) {
        if (name == null || name.isBlank()) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Regionsname fehlt").toException());
        }
        return getJson(routes.v2RegionByName(name), ApiEnvelope.class)
                .thenApply(ApiEnvelope::region);
    }

    @Override
    public CompletableFuture<FactionRecord> faction(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Fraktions-UUID fehlt").toException());
        }
        return getJson(routes.v2Faction(uuid.toString()), ApiEnvelope.class)
                .thenApply(ApiEnvelope::faction);
    }

    @Override
    public CompletableFuture<List<PlayerRecord>> factionPlayers(UUID factionUuid) {
        if (factionUuid == null) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Fraktions-UUID fehlt").toException());
        }
        return getJson(routes.v2FactionPlayers(factionUuid.toString()), ApiEnvelope.class)
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
        return getJson(routes.v2Player(uuid.toString()), ApiEnvelope.class)
                .thenApply(ApiEnvelope::profile);
    }

    @Override
    public CompletableFuture<List<CompactPlayer>> compactPlayers() {
        URI uri = routes.v2CompactPlayers();
        return getString(uri)
                .thenApply(body -> parseCompactPlayers(uri, body));
    }

    @Override
    public CompletableFuture<String> createProtectedChatMessage(
            String original, List<String> allowedUsernames) {
        return createProtectedChatMessage(original, allowedUsernames, List.of());
    }

    @Override
    public CompletableFuture<String> createProtectedChatMessage(
            String original, List<String> allowedUsernames, List<String> translations) {
        if (original == null || original.isBlank()) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Originaltext fehlt").toException());
        }
        JsonObject body = new JsonObject();
        body.addProperty("original", original);
        JsonArray allowed = new JsonArray();
        boolean allowAll = allowedUsernames == null;
        if (allowedUsernames != null) {
            allowedUsernames.stream()
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim).distinct().limit(64).forEach(allowed::add);
        }
        // null bedeutet explizit ALL; eine leere Liste bleibt eine leere
        // Allowlist und darf niemals versehentlich zu ALL hochgestuft werden.
        body.addProperty("access", allowAll ? "all" : "allowlist");
        body.add("allowedUsernames", allowed);
        JsonArray translated = new JsonArray();
        if (translations != null) {
            translations.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .limit(16).forEach(translated::add);
        }
        body.add("translations", translated);
        URI uri = routes.v2ProtectedChatMessages();
        return authenticatedJson("POST", uri, body)
                .thenApply(json -> requiredString(json, "id", uri));
    }

    @Override
    public CompletableFuture<String> protectedChatMessage(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{8,64}")) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Ungültige Chat-Referenz").toException());
        }
        URI uri = routes.v2ProtectedChatMessage(id);
        return authenticatedJson("GET", uri, null)
                .thenApply(json -> requiredString(json, "original", uri));
    }

    @Override
    public CompletableFuture<List<ProtectedChatInboxEntry>> protectedChatInbox() {
        URI uri = routes.v2ProtectedChatInbox();
        return authenticatedJson("GET", uri, null).thenApply(json -> {
            JsonElement entries = json.get("entries");
            if (entries == null || !entries.isJsonArray()) return List.of();
            List<ProtectedChatInboxEntry> parsed = gson.fromJson(entries,
                    new TypeToken<List<ProtectedChatInboxEntry>>() { }.getType());
            return parsed == null ? List.of() : List.copyOf(parsed);
        });
    }

    @Override
    public CompletableFuture<List<AnimalBreedingHorse>> animalBreedingHorses() {
        URI uri = routes.v2AnimalBreedingHorses();
        return authenticatedJson("GET", uri, null).thenApply(json -> {
            JsonElement horses = json.get("horses");
            if (horses == null || !horses.isJsonArray()) {
                return List.of();
            }
            List<AnimalBreedingHorse> parsed = gson.fromJson(horses,
                    new TypeToken<List<AnimalBreedingHorse>>() { }.getType());
            return parsed == null ? List.of() : List.copyOf(parsed);
        });
    }

    @Override
    public CompletableFuture<Optional<String>> animalBreedingRole() {
        URI uri = routes.v2AnimalBreedingBreederAccess();
        return authenticatedJson("GET", uri, null).thenApply(json -> {
            JsonElement role = json.get("role");
            if (role == null || role.isJsonNull()) return Optional.empty();
            if (!role.isJsonPrimitive() || !role.getAsJsonPrimitive().isString()) {
                throw ApiProblem.parse(uri, "role ist ungültig").toException();
            }
            String value = role.getAsString().trim().toLowerCase(java.util.Locale.ROOT);
            if (!List.of("viewer", "editor", "admin").contains(value)) {
                throw ApiProblem.parse(uri, "role ist unbekannt").toException();
            }
            return Optional.of(value);
        });
    }

    @Override
    public CompletableFuture<Void> setAnimalBreedingBreederAccess(
            String playerName, boolean granted) {
        if (playerName == null
                || !playerName.trim().matches("[A-Za-z0-9_]{3,16}")) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Spielername ist ungültig").toException());
        }
        JsonObject body = new JsonObject();
        body.addProperty("playerName", playerName.trim());
        URI uri = routes.v2AnimalBreedingBreederAccess();
        return authenticatedJson(granted ? "PUT" : "DELETE", uri, body)
                .thenApply(json -> null);
    }

    @Override
    public CompletableFuture<List<AnimalBreedingHorseName>> animalBreedingHorseNames() {
        URI uri = routes.v2AnimalBreedingHorseNames();
        return authenticatedJson("GET", uri, null).thenApply(json -> {
            JsonElement names = json.get("horseNames");
            if (names == null || !names.isJsonArray()) {
                return List.of();
            }
            List<AnimalBreedingHorseName> parsed = gson.fromJson(names,
                    new TypeToken<List<AnimalBreedingHorseName>>() { }.getType());
            return parsed == null ? List.of() : List.copyOf(parsed);
        });
    }

    @Override
    public CompletableFuture<Optional<UUID>> animalBreedingFinderTarget() {
        return animalBreedingFinderTargets().thenApply(targets -> targets.stream().findFirst());
    }

    @Override
    public CompletableFuture<List<UUID>> animalBreedingFinderTargets() {
        URI uri = routes.v2AnimalBreedingFinder();
        return authenticatedJson("GET", uri, null).thenApply(json -> {
            JsonElement finders = json.get("finders");
            if (finders != null && finders.isJsonArray()) {
                java.util.LinkedHashSet<UUID> targets = new java.util.LinkedHashSet<>();
                for (JsonElement element : finders.getAsJsonArray()) {
                    if (!element.isJsonObject()) {
                        throw ApiProblem.parse(uri, "finders ist ungültig").toException();
                    }
                    try {
                        targets.add(UUID.fromString(requiredString(
                                element.getAsJsonObject(), "entityUuid", uri)));
                    } catch (ApiProblem.ApiException error) {
                        throw error;
                    } catch (Exception error) {
                        throw ApiProblem.parse(uri, "finders.entityUuid ist ungültig")
                                .toException();
                    }
                }
                return List.copyOf(targets);
            }
            JsonElement finder = json.get("finder");
            if (finder == null || finder.isJsonNull()) return List.of();
            if (!finder.isJsonObject()) {
                throw ApiProblem.parse(uri, "finder ist ungültig").toException();
            }
            try {
                return List.of(UUID.fromString(requiredString(
                        finder.getAsJsonObject(), "entityUuid", uri)));
            } catch (ApiProblem.ApiException error) {
                throw error;
            } catch (Exception error) {
                throw ApiProblem.parse(uri, "finder.entityUuid ist ungültig")
                        .toException();
            }
        });
    }

    @Override
    public CompletableFuture<Void> setAnimalBreedingFinderTarget(UUID entityUuid) {
        if (entityUuid == null) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Finder-Pferd fehlt").toException());
        }
        URI uri = routes.v2AnimalBreedingFinder();
        JsonObject body = new JsonObject();
        body.addProperty("entityUuid", entityUuid.toString());
        return authenticatedJson("PUT", uri, body).thenApply(json -> null);
    }

    @Override
    public CompletableFuture<Void> addAnimalBreedingFinderTarget(UUID entityUuid) {
        if (entityUuid == null) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Finder-Pferd fehlt").toException());
        }
        URI uri = routes.v2AnimalBreedingFinder(entityUuid.toString());
        return authenticatedJson("PUT", uri, null).thenApply(json -> null);
    }

    @Override
    public CompletableFuture<Void> removeAnimalBreedingFinderTarget(UUID entityUuid) {
        if (entityUuid == null) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Finder-Pferd fehlt").toException());
        }
        URI uri = routes.v2AnimalBreedingFinder(entityUuid.toString());
        return authenticatedJson("DELETE", uri, null).thenApply(json -> null);
    }

    @Override
    public CompletableFuture<Void> clearAnimalBreedingFinderTarget() {
        URI uri = routes.v2AnimalBreedingFinder();
        return authenticatedJson("DELETE", uri, null).thenApply(json -> null);
    }

    @Override
    public CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorse(
            AnimalBreedingHorse horse, AnimalBreedingHorseLocation location) {
        if (horse == null) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Pferdeakte fehlt").toException());
        }
        URI uri = routes.v2AnimalBreedingHorse(horse.entityUuid());
        return putAnimalBreedingHorseAt(uri, horse, location);
    }

    @Override
    public CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorseObservation(
            AnimalBreedingHorse horse) {
        if (horse == null) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Pferdebeobachtung fehlt").toException());
        }
        URI uri = routes.v2AnimalBreedingHorseObservation(horse.entityUuid());
        return putAnimalBreedingHorseAt(uri, horse, null);
    }

    @Override
    public CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorseName(
            AnimalBreedingHorse horse, AnimalBreedingHorseLocation location) {
        if (horse == null || horse.registryName().isBlank()) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Pferd oder Pferdename fehlt").toException());
        }
        URI uri = routes.v2AnimalBreedingHorseName(horse.entityUuid());
        return putAnimalBreedingHorseAt(uri, horse, location);
    }

    private CompletableFuture<AnimalBreedingHorse> putAnimalBreedingHorseAt(
            URI uri, AnimalBreedingHorse horse,
            AnimalBreedingHorseLocation location) {
        JsonObject body = new JsonObject();
        body.addProperty("entityUuid", horse.entityUuid());
        body.addProperty("registryName", horse.registryName());
        body.addProperty("origin", horse.origin());
        body.addProperty("currentFarm", horse.currentFarm());
        body.addProperty("animalType", horse.animalType());
        body.addProperty("gender", horse.gender());
        body.addProperty("coatColor", horse.coatColor());
        body.addProperty("marking", horse.marking());
        body.addProperty("life", horse.life());
        body.addProperty("jumpHeight", horse.jumpHeight());
        body.addProperty("speed", horse.speed());
        body.addProperty("mast", horse.mast());
        body.addProperty("breedingReady", horse.breedingReady());
        body.addProperty("observedAtEpochMillis", horse.observedAtEpochMillis());
        body.addProperty("updatedAtEpochMillis", horse.updatedAtEpochMillis());
        if (location != null) {
            JsonObject position = new JsonObject();
            position.addProperty("dimension", location.dimension());
            position.addProperty("x", location.x());
            position.addProperty("y", location.y());
            position.addProperty("z", location.z());
            body.add("location", position);
        }
        return authenticatedJson("PUT", uri, body).thenApply(json -> {
            JsonElement stored = json.get("horse");
            if (stored == null || !stored.isJsonObject()) {
                throw ApiProblem.parse(uri, "horse fehlt").toException();
            }
            try {
                AnimalBreedingHorse parsed = gson.fromJson(
                        stored, AnimalBreedingHorse.class);
                if (parsed == null) {
                    throw ApiProblem.parse(uri, "horse ist leer").toException();
                }
                return parsed;
            } catch (ApiProblem.ApiException error) {
                throw error;
            } catch (Exception error) {
                throw ApiProblem.parse(uri, error.getMessage()).toException();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteAnimalBreedingHorse(UUID entityUuid) {
        if (entityUuid == null) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Pferde-UUID fehlt").toException());
        }
        URI uri = routes.v2AnimalBreedingHorse(entityUuid.toString());
        return authenticatedJson("DELETE", uri, null).thenApply(json -> null);
    }

    @Override
    public CompletableFuture<Void> setAnimalBreedingHorseEditor(
            UUID horseUuid, String playerName, boolean granted) {
        if (horseUuid == null || playerName == null
                || !playerName.trim().matches("[A-Za-z0-9_]{3,16}")) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Pferd oder Spielername ist ungültig").toException());
        }
        JsonObject body = new JsonObject();
        body.addProperty("playerName", playerName.trim());
        URI uri = routes.v2AnimalBreedingHorseEditors(horseUuid.toString());
        return authenticatedJson(granted ? "PUT" : "DELETE", uri, body)
                .thenApply(json -> null);
    }

    @Override
    public CompletableFuture<UUID> createAnimalBreedingEvent(
            UUID mareUuid, UUID stallionUuid, String farmName,
            long pairedAtEpochMillis) {
        if (mareUuid == null || stallionUuid == null
                || mareUuid.equals(stallionUuid)
                || pairedAtEpochMillis <= 0L) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Ungültiges Zuchtereignis").toException());
        }
        JsonObject body = new JsonObject();
        body.addProperty("mareUuid", mareUuid.toString());
        body.addProperty("stallionUuid", stallionUuid.toString());
        body.addProperty("farmName", farmName == null ? "" : farmName.trim());
        body.addProperty("pairedAtEpochMillis", pairedAtEpochMillis);
        URI uri = routes.v2AnimalBreedingEvents();
        return authenticatedJson("POST", uri, body).thenApply(json -> {
            JsonElement event = json.get("event");
            if (event == null || !event.isJsonObject()) {
                throw ApiProblem.parse(uri, "event fehlt").toException();
            }
            try {
                return UUID.fromString(requiredString(
                        event.getAsJsonObject(), "id", uri));
            } catch (ApiProblem.ApiException error) {
                throw error;
            } catch (Exception error) {
                throw ApiProblem.parse(uri, "event.id ungültig").toException();
            }
        });
    }

    @Override
    public CompletableFuture<Void> linkAnimalBreedingFoal(
            UUID eventUuid, UUID foalUuid) {
        if (eventUuid == null || foalUuid == null) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Zuchtereignis oder Fohlen fehlt").toException());
        }
        JsonObject body = new JsonObject();
        body.addProperty("foalUuid", foalUuid.toString());
        URI uri = routes.v2AnimalBreedingEventFoal(eventUuid.toString());
        return authenticatedJson("PUT", uri, body).thenApply(json -> null);
    }

    @Override
    public CompletableFuture<byte[]> downloadBinary(URI uri) {
        if (uri == null) {
            return CompletableFuture.failedFuture(ApiProblem.badRequest("Download-URI fehlt").toException());
        }
        if (!isTrustedAssetUri(uri)) {
            return CompletableFuture.failedFuture(
                    ApiProblem.badRequest("Nicht vertrauenswürdige Download-URI").toException());
        }
        HttpRequest request = baseRequest(uri)
                .setHeader("Accept", "image/png")
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(resp -> {
                    if (!isTrustedAssetUri(resp.uri())) {
                        throw ApiProblem.badRequest("Nicht vertrauenswürdige Download-Weiterleitung")
                                .toException();
                    }
                    if (resp.statusCode() / 100 != 2) {
                        throw ApiProblem.httpStatus(uri, resp.statusCode()).toException();
                    }
                    byte[] bytes = resp.body();
                    if (bytes != null && bytes.length > MAX_BINARY_BYTES) {
                        throw ApiProblem.parse(uri, "Download zu gross: " + bytes.length + " B").toException();
                    }
                    if (verifier.check(resp.headers(), bytes) == ResponseVerifier.Result.INVALID) {
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

        }
        executor.shutdownNow();
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET();
    }

    private boolean isTrustedAssetUri(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }
        URI baseUri = URI.create(routes.baseUrl());
        return uri.getScheme() != null
                && uri.getScheme().equalsIgnoreCase(baseUri.getScheme())
                && uri.getHost().equalsIgnoreCase(baseUri.getHost())
                && effectivePort(uri) == effectivePort(baseUri);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443
                : "http".equalsIgnoreCase(uri.getScheme()) ? 80 : -1;
    }

    private CompletableFuture<String> getString(URI v2Uri) {
        return auth.tokenAsync()
                .thenCompose(token -> fetch(v2Uri, token.token())
                        .exceptionallyCompose(t -> {
                            if (isHttpStatus(t, 401)) {

                                auth.invalidate();
                                return auth.tokenAsync()
                                        .thenCompose(fresh -> fetch(v2Uri, fresh.token()));
                            }
                            return CompletableFuture.failedFuture(t);
                        }));
    }

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
                        warnInvalidSignature(uri);
                        throw ApiProblem.parse(uri, "Signatur ungültig").toException();
                    }
                    return new String(raw == null ? new byte[0] : raw, StandardCharsets.UTF_8);
                })
                .exceptionallyCompose(t -> CompletableFuture.failedFuture(mapError(uri, t)));
    }

    private CompletableFuture<JsonObject> authenticatedJson(
            String method, URI uri, JsonObject body) {
        return auth.tokenAsync().thenCompose(token ->
                sendAuthenticatedJson(method, uri, body, token.token())
                        .exceptionallyCompose(t -> {
                            if (!isHttpStatus(t, 401)) {
                                return CompletableFuture.failedFuture(t);
                            }
                            auth.invalidate();
                            return auth.tokenAsync().thenCompose(fresh ->
                                    sendAuthenticatedJson(method, uri, body, fresh.token()));
                        }));
    }

    private CompletableFuture<JsonObject> sendAuthenticatedJson(
            String method, URI uri, JsonObject body, String bearerToken) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .header("User-Agent", USER_AGENT)
                .method(method, publisher)
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw ApiProblem.httpStatus(uri, response.statusCode()).toException();
                    }
                    byte[] raw = response.body();
                    if (!verifier.accept(response.headers(), raw)) {
                        warnInvalidSignature(uri);
                        throw ApiProblem.parse(uri, "Signatur ungültig").toException();
                    }
                    try {
                        return com.google.gson.JsonParser.parseString(new String(
                                raw == null ? new byte[0] : raw, StandardCharsets.UTF_8))
                                .getAsJsonObject();
                    } catch (Exception e) {
                        throw ApiProblem.parse(uri, "Antwort kein JSON-Objekt").toException();
                    }
                })
                .exceptionallyCompose(t -> CompletableFuture.failedFuture(mapError(uri, t)));
    }

    private static String requiredString(JsonObject json, String key, URI uri) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()
                || json.get(key).getAsString().isBlank()) {
            throw ApiProblem.parse(uri, key + " fehlt").toException();
        }
        return json.get(key).getAsString();
    }

    private <T> CompletableFuture<T> getJson(URI uri, Class<T> type) {
        return getString(uri).thenApply(body -> {
            try {
                T value = gson.fromJson(body, type);
                if (value == null) {
                    throw ApiProblem.parse(uri, "Leere Antwort").toException();
                }
                return value;
            } catch (ApiProblem.ApiException e) {
                throw e;
            } catch (Exception e) {
                throw ApiProblem.parse(uri, e.getMessage()).toException();
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

    private static boolean isHttpStatus(Throwable t, int status) {
        Throwable cause = (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
        return cause instanceof ApiProblem.ApiException api && api.problem().isHttpStatus(status);
    }

    private void warnInvalidSignature(URI uri) {
        if (warnedInvalidSignature.compareAndSet(false, true)) {
            OttoExtra.LOGGER.warn(
                    "[api] Signatur ungültig — Antwort verworfen ({})", uri.getPath());
        }
    }

    private static Throwable mapError(URI uri, Throwable t) {
        Throwable cause = (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
        if (cause instanceof ApiProblem.ApiException) {
            return cause;
        }
        DebugLog.debug("API-Fehler {}: {}", uri, cause.toString());
        return ApiProblem.offline(uri, cause.getClass().getSimpleName()).toException();
    }
}
