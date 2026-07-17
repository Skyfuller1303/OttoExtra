package de.ottoextra.regions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.api.model.RegionRecord;
import de.ottoextra.config.OttoExtraPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
public final class RegionDataService {
    private static final long SYNC_INTERVAL_MINUTES = 30;
    private static final long GATHERING_INTERVAL_MINUTES = 7;
    private static final long REGION_DETAIL_COOLDOWN_MS = 10 * 60_000L;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final OttoExtraApiClient api;
    private final ScheduledExecutorService scheduler;
    private final Map<String, FactionRecord> factionsByUuid = new ConcurrentHashMap<>();
    private final Map<String, String> factionUuidByKey = new ConcurrentHashMap<>();
    private final Map<String, RegionRecord> regionsByKey = new ConcurrentHashMap<>();
    private final Map<String, Long> detailRequestedAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> gatheringByKey = new ConcurrentHashMap<>();
    private final AtomicBoolean bootstrapRunning = new AtomicBoolean(false);
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);
    private volatile long syncCursor = -1;
    private volatile ScheduledFuture<?> syncTask;
    private volatile ScheduledFuture<?> gatheringTask;
    public RegionDataService(OttoExtraApiClient api) {
        this.api = api;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ottoextra-regions-scheduler");
            t.setDaemon(true);
            return t;
        });
        loadSnapshotFromDisk();
        applyLocalOverrides();
    }
    public void onServerJoin() {
        if (!bootstrapRunning.compareAndSet(false, true)) {
            return;
        }
        api.bootstrap()
                .whenComplete((env, t) -> {
                    bootstrapRunning.set(false);
                    if (t != null) {
                        OttoExtra.LOGGER.info("[regions] Bootstrap nicht moeglich ({}) — nutze Snapshot.",
                                rootMessage(t));
                        return;
                    }
                    applyEnvelope(env, true);
                    OttoExtra.LOGGER.info("[regions] Bootstrap: {} Fraktionen, {} Regionen.",
                            factionsByUuid.size(), regionsByKey.size());
                });
        startSyncLoop();
        startGatheringLoop();
    }
    public void onDisconnect() {
        stopSyncLoop();
    }
    public void shutdown() {
        stopSyncLoop();
        scheduler.shutdownNow();
    }
    private void startSyncLoop() {
        stopSyncLoop();
        syncTask = scheduler.scheduleAtFixedRate(this::syncNow,
                SYNC_INTERVAL_MINUTES, SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    public void syncNow() {
        if (bootstrapRunning.get() || !syncRunning.compareAndSet(false, true)) {
            return;
        }
        long cursor = syncCursor;
        api.sync(Math.max(0, cursor)).whenComplete((env, t) -> {
            try {
                if (t != null) {
                    OttoExtra.LOGGER.debug("[regions] Sync uebersprungen: {}", rootMessage(t));
                    return;
                }
                applyEnvelope(env, false);
            } finally {
                syncRunning.set(false);
            }
        });
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
    }
    private void startGatheringLoop() {
        gatheringTask = scheduler.scheduleAtFixedRate(() ->
                api.regionList().whenComplete((regions, t) -> {
                    if (t != null || regions == null) {
                        return;
                    }
                    Map<String, Integer> fresh = new ConcurrentHashMap<>();
                    for (RegionRecord r : regions) {
                        Integer g = r.region_capabilities() != null
                                ? r.region_capabilities().player_gathering() : null;
                        String key = RegionNameKeys.normalize(r.id());
                        if (g != null && g > 0 && !key.isEmpty()) {
                            fresh.put(key, g);
                        }
                    }
                    gatheringByKey.clear();
                    gatheringByKey.putAll(fresh);
                }), 0, GATHERING_INTERVAL_MINUTES, TimeUnit.MINUTES);
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
        String key = RegionNameKeys.normalize(regionName);
        if (key.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = detailRequestedAt.get(key);
        if (last != null && now - last < REGION_DETAIL_COOLDOWN_MS) {
            return;
        }
        detailRequestedAt.put(key, now);
        api.regionByName(regionName).whenComplete((region, t) -> {
            if (t != null || region == null) {
                return;
            }
            indexRegion(region);
            if (region.current_faction() != null) {
                indexFaction(region.current_faction());
            }
            persistSnapshot();
        });
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
    public List<FactionRecord> allFactions() {
        return new ArrayList<>(factionsByUuid.values());
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
    private void applyEnvelope(ApiEnvelope env, boolean fromBootstrap) {
        if (env == null) {
            return;
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
            factionUuidByKey.putIfAbsent(key, uuid);
        }
    }
    private static final class Snapshot {
        long syncCursor = -1;
        List<FactionRecord> factions = List.of();
        List<RegionRecord> regions = List.of();
    }
    private Path snapshotFile() {
        return OttoExtraPaths.apiCache().resolve("snapshot.json");
    }
    private void loadSnapshotFromDisk() {
        Path file = snapshotFile();
        if (!Files.exists(file)) {
            return;
        }
        try {
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
            OttoExtra.LOGGER.info("[regions] Snapshot geladen: {} Fraktionen, {} Regionen.",
                    factionsByUuid.size(), regionsByKey.size());
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[regions] Snapshot unlesbar — wird neu aufgebaut. ({})", e.getMessage());
        }
    }
    private void persistSnapshot() {
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
                Files.writeString(tmp, GSON.toJson(snap), StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicUnsupported) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                OttoExtra.LOGGER.debug("[regions] Snapshot speichern fehlgeschlagen: {}", e.getMessage());
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
}
