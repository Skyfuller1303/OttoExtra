package de.ottoextra.regions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.privacy.ConsentPermit;
import de.ottoextra.privacy.ConsentPolicy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class RegionDataService {

    private static final long BOOTSTRAP_RETRY_SECONDS = 45;
    private static final long ACTIVITY_RETRY_SECONDS = 5;
    private static final long SYNC_INTERVAL_MINUTES = 20;
    private static final long REGION_DETAIL_COOLDOWN_MS = 10 * 60_000L;
    private static final int MAX_ACTIVITY_RESPONSE_CHARS = 2_000_000;
    private static final URI ACTIVITY_API = URI.create(
            "https://mcp-api.ottonien.com/api/public/regions");
    private static final String ACTIVITY_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient ACTIVITY_HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final OttoExtraApiClient api;
    private final java.util.function.Supplier<ConsentPermit> permits;
    private final ScheduledExecutorService scheduler;
    private final Path snapshotFile;

    private final Map<String, FactionRecord> factionsByUuid = new ConcurrentHashMap<>();
    private final Map<String, String> factionUuidByKey = new ConcurrentHashMap<>();
    private final Map<String, RegionRecord> regionsByKey = new ConcurrentHashMap<>();
    private final Map<String, Long> detailRequestedAt = new ConcurrentHashMap<>();
    // Accessed under the lifecycle lock; screens can await an existing prefetch.
    private final Map<String, CompletableFuture<Void>> detailRequests = new HashMap<>();
    private final Map<String, Integer> gatheringByKey = new ConcurrentHashMap<>();
    private volatile List<ActivityEntry> activityEntries = List.of();
    private volatile long activityUpdatedAtMs;
    private volatile int activityRefreshSeconds;
    private final AtomicLong bootstrapGeneration = new AtomicLong();
    private final AtomicLong activityConnectionGeneration = new AtomicLong();
    private long syncInFlightGeneration = -1;
    private final AtomicBoolean gatheringRunning = new AtomicBoolean(false);

    private volatile long bootstrapInFlightGeneration = -1;
    private volatile boolean bootstrapReady;
    private volatile long syncCursor = -1;
    private volatile CompletableFuture<Void> bootstrapCompletion =
            CompletableFuture.completedFuture(null);
    private volatile ScheduledFuture<?> syncTask;
    private volatile ScheduledFuture<?> bootstrapRetryTask;
    private volatile ScheduledFuture<?> gatheringTask;
    private volatile ScheduledFuture<?> activityRetryTask;

    public RegionDataService(OttoExtraApiClient api, int activityRefreshSeconds) {
        this(api, activityRefreshSeconds, OttoExtraPaths.apiCache().resolve("snapshot.json"),
                () -> ConsentPolicy.capture(ConsentPolicy.Purpose.ONLINE_SERVICES));
    }

    // Filesystem/transport seam for offline fixture tests; production uses the consent supplier above.
    RegionDataService(OttoExtraApiClient api, int activityRefreshSeconds, Path snapshotFile) {
        this(api, activityRefreshSeconds, snapshotFile, () -> new ConsentPermit.Gate().capture(() -> true));
    }

    RegionDataService(OttoExtraApiClient api, int activityRefreshSeconds, Path snapshotFile,
                      java.util.function.Supplier<ConsentPermit> permits) {
        this.api = api;
        this.permits = permits;
        this.snapshotFile = snapshotFile;
        this.activityRefreshSeconds = sanitizeActivityRefreshSeconds(
                activityRefreshSeconds);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ottoextra-regions-scheduler");
            t.setDaemon(true);
            return t;
        });
        loadSnapshotFromDisk();
        applyLocalOverrides();
    }

    public void onServerJoin() {
        onServerJoin(true);
    }

    synchronized void onServerJoin(boolean loadActivity) {
        if (!permits.get().valid()) return;
        syncInFlightGeneration = -1;
        long activityGeneration = activityConnectionGeneration.incrementAndGet();
        long generation = bootstrapGeneration.incrementAndGet();
        detailRequestedAt.clear();
        detailRequests.clear();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        bootstrapCompletion = completion;
        bootstrapReady = false;
        cancelBootstrapRetry();
        requestBootstrap(generation, completion);
        startSyncLoop();
        if (loadActivity) startGatheringLoop(activityGeneration);
    }

    public CompletableFuture<Void> bootstrapCompletion() {
        return bootstrapCompletion;
    }

    public synchronized void onDisconnect() {
        syncInFlightGeneration = -1;
        activityConnectionGeneration.incrementAndGet();
        bootstrapGeneration.incrementAndGet();
        detailRequestedAt.clear();
        detailRequests.clear();
        bootstrapReady = false;
        cancelBootstrapRetry();
        bootstrapCompletion.complete(null);
        stopSyncLoop();
        gatheringByKey.clear();
        activityEntries = List.of();
        activityUpdatedAtMs = 0L;
    }

    public void shutdown() {
        onDisconnect();
        scheduler.shutdownNow();
    }

    /** Fordert fehlende Grunddaten erneut an, sonst nur Aenderungen seit dem Cursor. */
    public void refreshNow() {
        long generation = bootstrapGeneration.get();
        if (!bootstrapReady) {
            requestBootstrap(generation, bootstrapCompletion);
            return;
        }
        syncNow();
    }

    private void requestBootstrap(long generation, CompletableFuture<Void> completion) {
        ConsentPermit permit = permits.get();
        if (!permit.valid()) { completion.cancel(false); return; }
        synchronized (this) {
            if (generation != bootstrapGeneration.get() || bootstrapReady
                    || bootstrapInFlightGeneration == generation) {
                return;
            }
            bootstrapInFlightGeneration = generation;
        }

        CompletableFuture<ApiEnvelope> request;
        try {
            request = permit.start(api::bootstrap);
        } catch (Throwable error) {
            finishBootstrap(generation, completion, null, error, permit);
            return;
        }
        if (request == null) {
            finishBootstrap(generation, completion, null,
                    new IllegalStateException("Bootstrap-Future fehlt"), permit);
            return;
        }
        request.whenComplete((env, error) ->
                finishBootstrap(generation, completion, env, error, permit));
    }

    private synchronized void finishBootstrap(long generation, CompletableFuture<Void> completion,
                                 ApiEnvelope env, Throwable error, ConsentPermit permit) {
        try {
            permit.commit(() -> finishBootstrapAllowed(generation, completion, env, error));
        } catch (java.util.concurrent.CancellationException revoked) {
            if (bootstrapInFlightGeneration == generation) bootstrapInFlightGeneration = -1;
            completion.cancel(false);
        }
    }

    private void finishBootstrapAllowed(long generation, CompletableFuture<Void> completion,
                                 ApiEnvelope env, Throwable error) {
        boolean complete = false;
        try {
            if (generation != bootstrapGeneration.get()) {
                return;
            }
            if (error != null) {
                DebugLog.debug("[regions] Bootstrap nicht moeglich ({}) — neuer Versuch in {} s.",
                        rootMessage(error), BOOTSTRAP_RETRY_SECONDS);
            } else if (env == null || !env.ok()) {
                DebugLog.debug("[regions] Bootstrap ungueltig — neuer Versuch in {} s.",
                        BOOTSTRAP_RETRY_SECONDS);
            } else {
                applyEnvelope(env, true);
                complete = isCompleteBootstrap(env);
                if (complete && generation == bootstrapGeneration.get()) {
                    bootstrapReady = true;
                    DebugLog.debug("[regions] Bootstrap: {} Fraktionen, {} Regionen.",
                            factionsByUuid.size(), regionsByKey.size());
                } else {
                    DebugLog.debug("[regions] Bootstrap unvollstaendig ({} Fraktionen, {} Regionen)"
                                    + " — neuer Versuch in {} s.",
                            sizeOf(env.factions()), sizeOf(env.regions()),
                            BOOTSTRAP_RETRY_SECONDS);
                }
            }
        } finally {
            synchronized (this) {
                if (bootstrapInFlightGeneration == generation) {
                    bootstrapInFlightGeneration = -1;
                }
            }
            if (generation == bootstrapGeneration.get()) {
                if (complete) {
                    completion.complete(null);
                    cancelBootstrapRetry();
                } else {
                    scheduleBootstrapRetry(generation, completion);
                }
            }
        }
    }

    private void scheduleBootstrapRetry(long generation, CompletableFuture<Void> completion) {
        synchronized (this) {
            if (generation != bootstrapGeneration.get() || bootstrapReady
                    || completion.isDone() || scheduler.isShutdown()) {
                return;
            }
            ScheduledFuture<?> old = bootstrapRetryTask;
            if (old != null) {
                old.cancel(false);
            }
            bootstrapRetryTask = scheduler.schedule(
                    () -> requestBootstrap(generation, completion),
                    BOOTSTRAP_RETRY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private synchronized void cancelBootstrapRetry() {
        ScheduledFuture<?> task = bootstrapRetryTask;
        if (task != null) {
            task.cancel(false);
            bootstrapRetryTask = null;
        }
    }

    static boolean isCompleteBootstrap(ApiEnvelope env) {
        return env != null && env.ok()
                && env.factions() != null && !env.factions().isEmpty()
                && env.regions() != null && !env.regions().isEmpty();
    }

    private static int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private void startSyncLoop() {
        stopSyncLoop();
        syncTask = scheduler.scheduleAtFixedRate(this::syncNow,
                SYNC_INTERVAL_MINUTES, SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    public synchronized void syncNow() {
        ConsentPermit permit = permits.get();
        if (!permit.valid()) return;
        long generation = bootstrapGeneration.get();
        if (!bootstrapReady || bootstrapInFlightGeneration == generation
                || syncInFlightGeneration == generation) return;
        syncInFlightGeneration = generation;
        try {
            permit.start(() -> api.sync(Math.max(0, syncCursor))).whenComplete((env, error) -> {
                synchronized (this) {
                    // Checking and applying share the lifecycle lock: disconnect
                    // cannot race between this check and snapshot persistence.
                    if (generation != bootstrapGeneration.get()) return;
                    try {
                        if (error != null) {
                            DebugLog.debug("[regions] Sync uebersprungen: {}", rootMessage(error));
                        } else {
                            permit.commit(() -> applyEnvelope(env, false));
                        }
                    } finally {
                        if (syncInFlightGeneration == generation) syncInFlightGeneration = -1;
                    }
                }
            });
        } catch (Exception error) {
            syncInFlightGeneration = -1;
            DebugLog.debug("[regions] Sync uebersprungen: {}", rootMessage(error));
        }
    }

    private void stopSyncLoop() {
        ScheduledFuture<?> task = syncTask;
        if (task != null) {
            task.cancel(false);
            syncTask = null;
        }
        ScheduledFuture<?> gt = gatheringTask;
        if (gt != null) {
            gt.cancel(false);
            gatheringTask = null;
        }
        ScheduledFuture<?> retry = activityRetryTask;
        if (retry != null) {
            retry.cancel(false);
            activityRetryTask = null;
        }
    }

    private synchronized void startGatheringLoop(long connectionGeneration) {
        ScheduledFuture<?> current = gatheringTask;
        if (current != null) {
            current.cancel(false);
        }
        gatheringTask = scheduler.scheduleAtFixedRate(
                () -> fetchActivityNow(connectionGeneration),
                0, activityRefreshSeconds, TimeUnit.SECONDS);
    }

    public synchronized void setActivityRefreshSeconds(int seconds) {
        activityRefreshSeconds = sanitizeActivityRefreshSeconds(seconds);
        if (gatheringTask != null && !scheduler.isShutdown()) {
            startGatheringLoop(activityConnectionGeneration.get());
        }
    }

    public int activityRefreshSeconds() {
        return activityRefreshSeconds;
    }

    static int sanitizeActivityRefreshSeconds(int seconds) {
        return Math.max(OttoExtraConfig.Regions.MIN_ACTIVITY_HUD_REFRESH_SECONDS,
                Math.min(OttoExtraConfig.Regions.MAX_ACTIVITY_HUD_REFRESH_SECONDS,
                        seconds));
    }

    private void fetchActivityNow(long connectionGeneration) {
        boolean acquired = false;
        try {
            ConsentPermit permit = permits.get();
            if (!permit.valid() || connectionGeneration != activityConnectionGeneration.get()
                    || !gatheringRunning.compareAndSet(false, true)) {
                return;
            }
            acquired = true;
            HttpRequest request = HttpRequest.newBuilder(ACTIVITY_API)
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Cache-Control", "no-cache")
                    .header("User-Agent", ACTIVITY_USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response = de.ottoextra.privacy.ConsentHttp.send(ACTIVITY_HTTP, request,
                    de.ottoextra.privacy.BoundedBody.text(de.ottoextra.privacy.BoundedBody.JSON_BYTES), permit).join();
            boolean[] applied = {false};
            permit.commit(() -> {
                if (connectionGeneration == activityConnectionGeneration.get()) {
                    applied[0] = applyActivityResponse(response);
                }
            });
            if (!applied[0]) {
                scheduleActivityRetry(connectionGeneration);
            } else {
                cancelActivityRetry();
            }
        } catch (Exception error) {
            DebugLog.debug("[regions] Aktivitaetsabfrage nicht moeglich: {}",
                    rootMessage(error));
            scheduleActivityRetry(connectionGeneration);
        } finally {
            if (acquired) {
                gatheringRunning.set(false);
            }
        }
    }

    private synchronized void scheduleActivityRetry(long connectionGeneration) {
        if (scheduler.isShutdown() || connectionGeneration != activityConnectionGeneration.get()
                || activityRetryTask != null && !activityRetryTask.isDone()) {
            return;
        }
        try {
            activityRetryTask = scheduler.schedule(() -> {
                synchronized (RegionDataService.this) {
                    activityRetryTask = null;
                }
                fetchActivityNow(connectionGeneration);
            }, ACTIVITY_RETRY_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            activityRetryTask = null;
        }
    }

    private synchronized void cancelActivityRetry() {
        ScheduledFuture<?> retry = activityRetryTask;
        if (retry != null) {
            retry.cancel(false);
            activityRetryTask = null;
        }
    }

    private boolean applyActivityResponse(HttpResponse<String> response) {
        if (response == null || response.statusCode() != 200) {
            DebugLog.debug("[regions] Aktivitaets-API antwortete mit HTTP {}.",
                    response != null ? response.statusCode() : -1);
            return false;
        }
        String body = response.body();
        if (body == null || body.isBlank() || body.length() > MAX_ACTIVITY_RESPONSE_CHARS) {
            DebugLog.debug("[regions] Aktivitaets-API lieferte keine gueltigen Daten.");
            return false;
        }
        try {
            applyActivityJson(body);
            return true;
        } catch (RuntimeException error) {
            DebugLog.debug("[regions] Aktivitaetsdaten konnten nicht gelesen werden: {}",
                    rootMessage(error));
            return false;
        }
    }

    void applyActivityJson(String body) {
        JsonArray regions = activityArray(JsonParser.parseString(body));
        Map<String, Integer> fresh = new HashMap<>();
        Map<String, ActivityEntry> activeByKey = new HashMap<>();
        for (JsonElement element : regions) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject raw = element.getAsJsonObject();
            String apiId = jsonString(raw, "id");
            String apiName = jsonString(raw, "name");
            int activityLevel = parseActivityLevel(raw.get("player_gathering"));
            String key = RegionNameKeys.normalize(firstNonBlank(apiId, apiName));
            if (activityLevel <= 0 || activityLevel > 2 || key.isEmpty()) {
                continue;
            }

            putActivityKey(fresh, apiId, activityLevel);
            putActivityKey(fresh, apiName, activityLevel);
            RegionRecord indexedRegion = findIndexedRegion(apiId, apiName).orElse(null);
            if (indexedRegion != null) {
                putActivityKey(fresh, indexedRegion.id(), activityLevel);
                putActivityKey(fresh, indexedRegion.region_ref(), activityLevel);
                putActivityKey(fresh, indexedRegion.region_id(), activityLevel);
                putActivityKey(fresh, indexedRegion.name(), activityLevel);
                putActivityKey(fresh, indexedRegion.original_name(), activityLevel);
            }

            FactionRecord faction = indexedRegion != null ? indexedRegion.current_faction() : null;
            if (faction == null) {
                faction = factionForRegion(firstNonBlank(apiId, apiName)).orElse(null);
            }
            String factionName = usableFactionName(faction);
            String regionName = firstNonBlank(apiName,
                    indexedRegion != null ? indexedRegion.name() : null,
                    indexedRegion != null ? indexedRegion.original_name() : null,
                    apiId);
            boolean hasFaction = !factionName.isBlank();
            String displayName = hasFaction ? factionName : regionName;
            String factionIdentity = hasFaction
                    ? firstNonBlank(faction.uuid(), faction.faction_ref(),
                    faction.name(), faction.banner_name()) : "";
            String activityKey = RegionNameKeys.normalize(hasFaction
                    ? firstNonBlank(factionIdentity, displayName) : key);
            String regionLookupKey = RegionNameKeys.normalize(firstNonBlank(
                    indexedRegion != null ? indexedRegion.id() : null,
                    indexedRegion != null ? indexedRegion.region_ref() : null,
                    apiId, apiName));
            ActivityEntry candidate = new ActivityEntry(
                    firstNonBlank(regionLookupKey, key), displayName, activityLevel);
            activeByKey.merge(activityKey, candidate, RegionDataService::preferActivityEntry);
        }

        List<ActivityEntry> ordered = new ArrayList<>(activeByKey.values());
        ordered.sort(Comparator.comparingInt(ActivityEntry::activityLevel).reversed()
                .thenComparing(ActivityEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        gatheringByKey.clear();
        gatheringByKey.putAll(fresh);
        activityEntries = List.copyOf(ordered);
        activityUpdatedAtMs = System.currentTimeMillis();
    }

    private static ActivityEntry preferActivityEntry(ActivityEntry left, ActivityEntry right) {
        if (right.activityLevel() != left.activityLevel()) {
            return right.activityLevel() > left.activityLevel() ? right : left;
        }
        return left.displayName().compareToIgnoreCase(right.displayName()) <= 0 ? left : right;
    }

    private static JsonArray activityArray(JsonElement root) {
        if (root == null || root.isJsonNull()) {
            return new JsonArray();
        }
        if (root.isJsonArray()) {
            return root.getAsJsonArray();
        }
        if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            for (String key : List.of("regions", "data", "result")) {
                JsonElement candidate = object.get(key);
                if (candidate != null && candidate.isJsonArray()) {
                    return candidate.getAsJsonArray();
                }
            }
        }
        throw new IllegalArgumentException("Unerwartetes Format der Aktivitaets-API");
    }

    private Optional<RegionRecord> findIndexedRegion(String apiId, String apiName) {
        for (String raw : List.of(apiId == null ? "" : apiId,
                apiName == null ? "" : apiName)) {
            String key = RegionNameKeys.normalize(raw);
            RegionRecord region = key.isEmpty() ? null : regionsByKey.get(key);
            if (region != null) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    private static void putActivityKey(Map<String, Integer> target,
                                       String rawKey, int activityLevel) {
        String key = RegionNameKeys.normalize(rawKey);
        if (!key.isEmpty()) {
            target.merge(key, activityLevel, Math::max);
        }
    }

    private static int parseActivityLevel(JsonElement value) {
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.getAsString().trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String jsonString(JsonObject object, String key) {
        JsonElement value = object != null ? object.get(key) : null;
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return "";
        }
        return value.getAsString().trim();
    }

    private static String usableFactionName(FactionRecord faction) {
        if (faction == null) {
            return "";
        }
        String value = firstNonBlank(faction.name(), faction.banner_name(), faction.faction_ref());
        if (value.equalsIgnoreCase("Unbekanntes Gefolge")
                || value.equalsIgnoreCase("Unknown faction")
                || value.equalsIgnoreCase("Unbekannt")
                || value.equalsIgnoreCase("Unknown")
                || value.equals("-")) {
            return "";
        }
        return value;
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    public int gatheringCount(String regionKey) {
        Integer g = gatheringByKey.get(RegionNameKeys.normalize(regionKey));
        return g != null ? g : 0;
    }

    public Optional<FactionRecord> factionForRegion(String regionName) {
        String key = RegionNameKeys.normalize(regionName);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        RegionRecord region = regionsByKey.get(key);
        if (region != null && region.current_faction() != null) {
            FactionRecord byUuid = region.current_faction().uuid() != null
                    ? factionsByUuid.get(region.current_faction().uuid()) : null;
            return Optional.of(byUuid != null ? byUuid : region.current_faction());
        }
        String uuid = factionUuidByKey.get(key);
        if (uuid != null) {
            FactionRecord f = factionsByUuid.get(uuid);
            if (f != null) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    public Optional<RegionRecord> regionByName(String regionName) {
        String key = RegionNameKeys.normalize(regionName);
        return key.isEmpty() ? Optional.empty() : Optional.ofNullable(regionsByKey.get(key));
    }

    public void requestRegionDetail(String regionName) {
        requestRegionDetailAsync(regionName);
    }

    /** Completes only after the requested detail (or an existing prefetch) is indexed. */
    public synchronized CompletableFuture<Void> requestRegionDetailAsync(String regionName) {
        ConsentPermit permit = permits.get();
        if (!permit.valid()) return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException("Online services disabled"));
        String key = RegionNameKeys.normalize(regionName);
        if (key.isEmpty()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> existing = detailRequests.get(key);
        if (existing != null) return existing;
        long now = System.currentTimeMillis();
        Long last = detailRequestedAt.get(key);
        if (last != null && now - last < REGION_DETAIL_COOLDOWN_MS) {
            return CompletableFuture.completedFuture(null);
        }
        long generation = bootstrapGeneration.get();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        detailRequests.put(key, completion);
        detailRequestedAt.put(key, now);
        try {
            permit.start(() -> api.regionByName(regionName)).whenComplete((region, error) -> {
                synchronized (this) {
                    detailRequests.remove(key, completion);
                    if (generation != bootstrapGeneration.get()) {
                        completion.cancel(false);
                        return;
                    }
                    if (error != null) {
                        detailRequestedAt.remove(key, now);
                        completion.completeExceptionally(error);
                        return;
                    }
                    try {
                        if (region != null) {
                            permit.commit(() -> {
                                indexRegion(region);
                                persistSnapshot();
                            });
                        }
                        completion.complete(null);
                    } catch (Exception failure) {
                        detailRequestedAt.remove(key, now);
                        completion.completeExceptionally(failure);
                    }
                }
            });
        } catch (Exception error) {
            detailRequests.remove(key, completion);
            detailRequestedAt.remove(key, now);
            completion.completeExceptionally(error);
        }
        return completion;
    }

    public CompletableFuture<List<PlayerRecord>> factionPlayers(FactionRecord faction) {
        try {
            return api.factionPlayers(UUID.fromString(faction.uuid()));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    public Optional<FactionRecord> factionByUuid(String uuid) {
        return uuid == null ? Optional.empty() : Optional.ofNullable(factionsByUuid.get(uuid));
    }

    public Optional<FactionRecord> factionByKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String key = rawKey.trim();
        FactionRecord direct = factionsByUuid.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }
        try {
            direct = factionsByUuid.get(UUID.fromString(key).toString());
            if (direct != null) {
                return Optional.of(direct);
            }
        } catch (IllegalArgumentException ignored) {
        }
        String uuid = factionUuidByKey.get(RegionNameKeys.normalize(key));
        return uuid == null ? Optional.empty() : Optional.ofNullable(factionsByUuid.get(uuid));
    }

    public List<FactionRecord> allFactions() {
        return new ArrayList<>(factionsByUuid.values());
    }

    public List<ActivityEntry> activityEntries() {
        return activityEntries;
    }

    public long activityUpdatedAtMs() {
        return activityUpdatedAtMs;
    }

    public List<RegionRecord> allRegions() {
        return regionsByKey.values().stream().distinct().toList();
    }

    public List<FactionRecord> vassalsOf(FactionRecord faction) {
        List<FactionRecord> result = new ArrayList<>();
        if (faction.vassal_uuids() != null) {
            for (String uuid : faction.vassal_uuids()) {
                FactionRecord f = factionsByUuid.get(uuid);
                if (f != null) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    void applyEnvelope(ApiEnvelope env, boolean fromBootstrap) {
        if (env == null) {
            return;
        }
        if (fromBootstrap && isCompleteBootstrap(env)) {
            // Ein vollstaendiger Bootstrap ist der autoritative Gesamtstand.
            // Ohne Leeren bleiben geloeschte Lehen aus dem Disk-Snapshot erhalten
            // und koennen spaeter die politische Zoom-Gruppierung verfälschen.
            factionsByUuid.clear();
            factionUuidByKey.clear();
            regionsByKey.clear();
        }
        if (env.sync_cursor() > 0) {
            syncCursor = env.sync_cursor();
        }
        if (env.factions() != null) {
            env.factions().forEach(this::indexFaction);
        }
        if (env.regions() != null) {
            env.regions().forEach(this::indexRegion);
        }
        applyLocalOverrides();
        persistSnapshot();
    }

    private void applyLocalOverrides() {
        applyFactionIfMissing("lehen_27", new FactionRecord(
                "local-holdern", null, null, "Holdern", "Herzogtum",
                null, null, "ottonien", null, null, "local",
                "lehen_27", "Holdern", 0, 0,
                null, null, null, 0, 0, null, null));
    }

    private void applyFactionIfMissing(String regionKey, FactionRecord override) {
        if (factionForRegion(regionKey).isPresent()) {
            return;
        }
        indexFaction(override);
    }

    private void indexFaction(FactionRecord f) {
        if (f == null || f.uuid() == null || f.uuid().isBlank()) {
            return;
        }
        factionsByUuid.put(f.uuid(), f);
        putFactionKey(f.region_name(), f.uuid());
        putFactionKey(f.region_id(), f.uuid());
        putFactionKey(f.name(), f.uuid());
        putFactionKey(f.banner_name(), f.uuid());
        putFactionKey(f.faction_ref(), f.uuid());
        if (f.entity_key() != null) {
            putFactionKey(f.entity_key(), f.uuid());
            for (String part : f.entity_key().split("\\|")) {
                putFactionKey(part, f.uuid());
            }
        }
    }

    private void indexRegion(RegionRecord r) {
        if (r == null) {
            return;
        }
        putRegionKey(r.id(), r);
        putRegionKey(r.region_ref(), r);
        putRegionKey(r.region_id(), r);
        putRegionKey(r.name(), r);
        putRegionKey(r.original_name(), r);
        if (r.region_id() != null) {
            putRegionKey("lehen_" + r.region_id(), r);
        }
        if (r.current_faction() != null && r.current_faction().uuid() != null) {
            indexFaction(r.current_faction());
            putFactionKey(r.name(), r.current_faction().uuid());
            putFactionKey(r.original_name(), r.current_faction().uuid());
        }
    }

    private void putRegionKey(String raw, RegionRecord r) {
        String key = RegionNameKeys.normalize(raw);
        if (!key.isEmpty()) {
            regionsByKey.put(key, r);
        }
    }

    private void putFactionKey(String raw, String uuid) {
        String key = RegionNameKeys.normalize(raw);
        if (!key.isEmpty() && uuid != null) {
            factionUuidByKey.put(key, uuid);
        }
    }

    private static final class Snapshot {
        long syncCursor = -1;
        List<FactionRecord> factions = List.of();
        List<RegionRecord> regions = List.of();
    }

    private Path snapshotFile() {
        return snapshotFile;
    }

    private void loadSnapshotFromDisk() {
        Path file = snapshotFile();
        if (!Files.exists(file)) {
            return;
        }
        try {
            if (Files.size(file) > 8 * 1024 * 1024) return;
            Snapshot snap = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Snapshot.class);
            if (snap == null) {
                return;
            }
            syncCursor = snap.syncCursor;
            if (snap.factions != null) {
                snap.factions.forEach(this::indexFaction);
            }
            if (snap.regions != null) {
                snap.regions.forEach(this::indexRegion);
            }
            DebugLog.debug("[regions] Snapshot geladen: {} Fraktionen, {} Regionen.",
                    factionsByUuid.size(), regionsByKey.size());
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[regions] Snapshot unlesbar — wird neu aufgebaut. ({})", e.getMessage());
        }
    }

    private void persistSnapshot() {
        ConsentPermit writePermit = permits.get();
        scheduler.execute(() -> {
            try {
                Snapshot snap = new Snapshot();
                snap.syncCursor = syncCursor;
                snap.factions = new ArrayList<>(factionsByUuid.values());

                List<RegionRecord> regions = new ArrayList<>();
                regionsByKey.values().stream().distinct().forEach(regions::add);
                snap.regions = regions;

                Path file = snapshotFile();
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling("snapshot.json.tmp");
                String json = GSON.toJson(snap);
                if (json.getBytes(StandardCharsets.UTF_8).length > 8 * 1024 * 1024) return;
                Path cacheRoot = file.getParent().getFileName().toString().equals("api")
                        ? file.getParent().getParent() : file.getParent();
                writePermit.commitIo(() -> de.ottoextra.privacy.CacheBudget.write(cacheRoot, file,
                        json.getBytes(StandardCharsets.UTF_8).length, () -> {
                    try {
                        Files.writeString(tmp, json, StandardCharsets.UTF_8);
                        try {
                            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        } catch (IOException atomicUnsupported) {
                            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } finally { Files.deleteIfExists(tmp); }
                }));
            } catch (Exception e) {
                DebugLog.debug("[regions] Snapshot speichern fehlgeschlagen: {}", e.getMessage());
            }
        });
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    public record ActivityEntry(String regionKey, String displayName, int activityLevel) {
    }
}
