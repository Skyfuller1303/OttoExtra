package de.ottoextra.horses;

import de.ottoextra.OttoExtra;
import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.api.model.AnimalBreedingHorseName;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.map.LehenPolygonStore;
import de.ottoextra.regions.RegionsServices;

import java.time.Instant;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
import java.util.function.BooleanSupplier;

/**
 * Addon-independent livestock identity and protected overview service supplied by
 * OttoExtra. Advanced record editing remains the responsibility of AnimalBreeding.
 */
public final class HorseRegistry {

    static final int MAX_OBSERVATIONS = 1_024;
    private static final Map<UUID, AnimalBreedingHorseName> NAMES =
            new ConcurrentHashMap<>();
    private static final Map<UUID, AnimalInfoObservation> OBSERVATIONS =
            new ConcurrentHashMap<>();
    private static final Map<UUID, AnimalBreedingHorse> DATABASE_RECORDS =
            new ConcurrentHashMap<>();
    private static final ScheduledExecutorService IDENTITY_IO = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "OttoExtra-AnimalCache");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile AnimalIdentityCache identityCache;
    private static ScheduledFuture<?> identityFlush;

    private static volatile OttoExtraApiClient api;
    private static volatile BooleanSupplier onOttonien = () -> false;
    private static volatile boolean basicSurfaceActive;
    static final long DATABASE_RETRY_MS = 5_000;
    private static boolean databaseLoaded;
    private static boolean namesLoaded;
    private static boolean databaseLoading;
    private static long databaseRetryAt;
    private static long databaseRequest;
    private static long namesRequest;
    private static long namingPlayerRequest;
    private static volatile PlayerRecord namingPlayer;
    private static java.util.function.LongSupplier clock = System::currentTimeMillis;
    private static volatile long connectionGeneration;

    private HorseRegistry() {
    }

    static void initialize(OttoExtraApiClient client,
                           BooleanSupplier serverGate,
                           boolean enableBasicSurface) {
        initialize(client, serverGate, enableBasicSurface, System::currentTimeMillis);
    }

    static synchronized void initialize(OttoExtraApiClient client, BooleanSupplier serverGate,
                                         boolean enableBasicSurface, java.util.function.LongSupplier time) {
        clock = time;
        api = client;
        onOttonien = serverGate == null ? () -> false : serverGate;
        basicSurfaceActive = enableBasicSurface;
        clearConnection();
    }

    static synchronized void observe(AnimalInfoObservation observation) {
        if (observation != null && observation.snapshot().complete()) {
            AnimalInfoObservation previous = OBSERVATIONS.get(observation.entityUuid());
            if (previous != null && previous.observedAt().isAfter(observation.observedAt())) return;
            OBSERVATIONS.put(observation.entityUuid(), observation);
            if (identityCache != null) identityCache.remember(identity(observation));
            if (OBSERVATIONS.size() > MAX_OBSERVATIONS) {
                OBSERVATIONS.values().stream().min(Comparator.comparing(AnimalInfoObservation::observedAt))
                        .ifPresent(oldest -> OBSERVATIONS.remove(oldest.entityUuid()));
            }
        }
    }

    static synchronized CompletableFuture<Void> openIdentityCache(Path file, String server, boolean enabled) {
        closeIdentityCache();
        AnimalIdentityCache cache = new AnimalIdentityCache(file, server, clock);
        if (!enabled) return CompletableFuture.runAsync(cache::delete, IDENTITY_IO);
        identityCache = cache;
        CompletableFuture<Void> loading = CompletableFuture.runAsync(cache::load, IDENTITY_IO);
        OBSERVATIONS.values().forEach(observation -> cache.remember(identity(observation)));
        identityFlush = IDENTITY_IO.scheduleWithFixedDelay(cache::saveIfDirty, 1, 1, TimeUnit.SECONDS);
        return loading;
    }

    private static synchronized void closeIdentityCache() {
        if (identityFlush != null) identityFlush.cancel(false);
        identityFlush = null;
        AnimalIdentityCache cache = identityCache;
        identityCache = null;
        if (cache != null) IDENTITY_IO.execute(cache::saveIfDirty);
    }

    private static AnimalIdentityCache.Identity identity(AnimalInfoObservation observation) {
        return new AnimalIdentityCache.Identity(observation.entityUuid(), observation.snapshot().animalName(),
                observation.snapshot().gender(), observation.observedAt().toEpochMilli());
    }

    /** Identity fallback is display-only: observation() and saveName() never use disk data. */
    static AnimalIdentityCache.Identity knownIdentity(UUID uuid) {
        AnimalInfoObservation observation = uuid == null ? null : OBSERVATIONS.get(uuid);
        if (observation != null && AnimalGender.color(observation.snapshot().gender()) != null) return identity(observation);
        AnimalIdentityCache cache = identityCache;
        return cache == null ? null : cache.find(uuid);
    }

    /** Returns the latest server-approved name for a horse UUID. */
    public static Optional<String> name(UUID entityUuid) {
        AnimalBreedingHorseName entry = entityUuid == null ? null : NAMES.get(entityUuid);
        if (entry != null && !entry.registryName().isBlank()) return Optional.of(entry.registryName());
        AnimalBreedingHorse record = entityUuid == null ? null : DATABASE_RECORDS.get(entityUuid);
        return record == null || record.registryName().isBlank()
                ? Optional.empty() : Optional.of(record.registryName());
    }

    public static boolean canEditName(UUID entityUuid) {
        return canEditName(entityUuid, null);
    }

    /** Existing names require record permission; initial naming also requires the animal's own-Lehen location. */
    public static synchronized boolean canEditName(UUID entityUuid, AnimalBreedingHorseLocation location) {
        if (entityUuid == null || !onOttonien.getAsBoolean()
                || !databaseLoaded || databaseLoading) return false;
        AnimalBreedingHorse record = DATABASE_RECORDS.get(entityUuid);
        if (record != null) return record.canEdit() || record.canManage();
        return namesLoaded && name(entityUuid).isEmpty() && canInitiallyNameAt(location);
    }

    /** Refresh once when opening a record, never infer membership from a breeder role or a toast. */
    public static synchronized CompletableFuture<Void> refreshNamingPlayer(UUID playerUuid) {
        namingPlayer = null;
        long request = ++namingPlayerRequest;
        long generation = connectionGeneration;
        if (api == null || playerUuid == null || !onOttonien.getAsBoolean()) {
            return CompletableFuture.completedFuture(null);
        }
        LehenPolygonStore.ensureLoaded();
        return api.player(playerUuid).thenAccept(player -> {
            synchronized (HorseRegistry.class) {
                if (generation == connectionGeneration && request == namingPlayerRequest
                        && onOttonien.getAsBoolean() && player != null
                        && playerUuid.toString().equalsIgnoreCase(player.uuid())) {
                    namingPlayer = player;
                }
            }
        });
    }

    public static boolean canInitiallyNameAt(AnimalBreedingHorseLocation location) {
        var data = RegionsServices.data();
        if (!onOttonien.getAsBoolean() || namingPlayer == null || data == null
                || !data.bootstrapCompletion().isDone() || data.bootstrapCompletion().isCompletedExceptionally()) {
            return false;
        }
        return AnimalNamingTerritory.allows(namingPlayer,
                key -> data.factionByKey(key).orElse(null), location, LehenPolygonStore.territoryPolygons());
    }

    /** Returns a stable alphabetical snapshot of all public horse identities. */
    public static List<AnimalBreedingHorseName> names() {
        List<AnimalBreedingHorseName> result = new ArrayList<>(NAMES.values());
        result.sort(Comparator.comparing(AnimalBreedingHorseName::registryName,
                String.CASE_INSENSITIVE_ORDER).thenComparing(AnimalBreedingHorseName::entityUuid));
        return List.copyOf(result);
    }

    /** Latest AnimalInfo observation seen during this connection. */
    public static Optional<AnimalInfoObservation> observation(UUID entityUuid) {
        return Optional.ofNullable(entityUuid == null ? null : OBSERVATIONS.get(entityUuid));
    }

    /**
     * Starts one lazy protected-record refresh for the current connection.
     * The animal-info toast uses this instead of interacting with a horse while
     * the automatic interaction hand contains a lead.
     */
    public static synchronized void requestDatabaseRecords() {
        if (databaseLoaded || databaseLoading || clock.getAsLong() < databaseRetryAt) return;
        refreshDatabaseRecords().exceptionally(error -> {
            OttoExtra.LOGGER.debug("Pferdedaten konnten nicht geladen werden: {}", error.toString());
            return List.of();
        });
    }

    /** Refreshes protected records; a failed lazy load can retry after a cooldown. */
    public static synchronized CompletableFuture<List<AnimalBreedingHorse>> refreshDatabaseRecords() {
        OttoExtraApiClient current = api;
        if (current == null || !onOttonien.getAsBoolean()) {
            return CompletableFuture.completedFuture(List.of());
        }
        long generation = connectionGeneration;
        long request = ++databaseRequest;
        databaseLoading = true;
        databaseLoaded = false;
        // Fail closed: never keep records from an older permission snapshot visible.
        DATABASE_RECORDS.clear();
        CompletableFuture<List<AnimalBreedingHorse>> result;
        try {
            result = current.animalBreedingRole().thenCompose(role -> {
                synchronized (HorseRegistry.class) {
                    if (!isCurrentDatabaseRequest(generation, request) || role.isEmpty()) {
                        return CompletableFuture.completedFuture(List.of());
                    }
                    return current.animalBreedingHorses();
                }
            });
        } catch (Exception error) {
            result = CompletableFuture.failedFuture(error);
        }
        return result.whenComplete((remote, error) -> {
            synchronized (HorseRegistry.class) {
                if (!isCurrentDatabaseRequest(generation, request)) return;
                databaseLoading = false;
                databaseLoaded = error == null;
                databaseRetryAt = error == null ? 0 : clock.getAsLong() + DATABASE_RETRY_MS;
                if (error == null) replaceDatabaseRecords(remote, generation);
            }
        }).thenApply(ignored -> databaseRecords());
    }

    private static boolean isCurrentDatabaseRequest(long generation, long request) {
        return generation == connectionGeneration && request == databaseRequest && onOttonien.getAsBoolean();
    }

    /** Returns a stable snapshot of the protected records visible to the user. */
    public static List<AnimalBreedingHorse> databaseRecords() {
        List<AnimalBreedingHorse> result = new ArrayList<>(DATABASE_RECORDS.values());
        result.sort(Comparator.comparing(AnimalBreedingHorse::currentFarm,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AnimalBreedingHorse::registryName,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AnimalBreedingHorse::entityUuid));
        return List.copyOf(result);
    }

    /** Deletes a horse record after the server has verified management rights. */
    public static synchronized CompletableFuture<Void> deleteDatabaseRecord(UUID entityUuid) {
        OttoExtraApiClient current = api;
        if (current == null || entityUuid == null || !onOttonien.getAsBoolean()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Keine aktive Verbindung zur Pferdedatenbank"));
        }
        long generation = connectionGeneration;
        return current.deleteAnimalBreedingHorse(entityUuid).thenRun(() -> {
            synchronized (HorseRegistry.class) {
                requireCurrentConnection(generation);
                invalidateReadRequests();
                DATABASE_RECORDS.remove(entityUuid);
                NAMES.remove(entityUuid);
            }
        });
    }

    /** Reads existing data only, without building toast snapshots or starting requests. */
    static String knownGender(UUID entityUuid) {
        if (entityUuid == null) return "";
        AnimalInfoObservation observation = OBSERVATIONS.get(entityUuid);
        if (observation != null && AnimalGender.color(observation.snapshot().gender()) != null) {
            return observation.snapshot().gender();
        }
        AnimalBreedingHorse record = DATABASE_RECORDS.get(entityUuid);
        if (record != null && AnimalGender.color(record.gender()) != null) return record.gender();
        var known = knownIdentity(entityUuid);
        return known == null ? "" : known.gender();
    }

    /** Returns the protected database values formatted for the animal-info toast. */
    public static Optional<AnimalInfoSnapshot> databaseSnapshot(UUID entityUuid) {
        AnimalBreedingHorse horse = entityUuid == null
                ? null : DATABASE_RECORDS.get(entityUuid);
        return horse == null ? Optional.empty()
                : Optional.of(databaseSnapshot(horse));
    }

    static AnimalInfoSnapshot databaseSnapshot(AnimalBreedingHorse horse) {
        String title = horse.registryName();
        if (title.isBlank()) {
            title = horse.animalType();
        } else if (!horse.animalType().isBlank()) {
            title += " \u00b7 " + horse.animalType();
        }
        if (title.isBlank()) {
            title = "Pferd";
        }
        return new AnimalInfoSnapshot(
                title, horse.gender(), horse.mast(), horse.breedingReady(),
                "", "", horse.jumpHeight(), horse.speed(), horse.life());
    }

    private static synchronized void replaceDatabaseRecords(List<AnimalBreedingHorse> remote,
                                               long generation) {
        if (generation != connectionGeneration || !onOttonien.getAsBoolean()) {
            return;
        }
        DATABASE_RECORDS.clear();
        for (AnimalBreedingHorse horse : remote) {
            try {
                DATABASE_RECORDS.put(horse.uuid(), horse);
            } catch (IllegalArgumentException ignored) {
                // One malformed remote row must not hide all valid records.
            }
        }
    }

    /** Refreshes the public UUID/name directory from the authenticated API. */
    public static synchronized CompletableFuture<List<AnimalBreedingHorseName>> refresh() {
        OttoExtraApiClient current = api;
        if (current == null || !onOttonien.getAsBoolean()) {
            return CompletableFuture.completedFuture(names());
        }
        long generation = connectionGeneration;
        long request = ++namesRequest;
        namesLoaded = false;
        return current.animalBreedingHorseNames().thenApply(remote -> {
            synchronized (HorseRegistry.class) {
                if (generation != connectionGeneration || request != namesRequest || !onOttonien.getAsBoolean()) {
                    return names();
                }
                NAMES.clear();
                for (AnimalBreedingHorseName entry : remote) {
                    try {
                        if (!entry.registryName().isBlank()) {
                            NAMES.put(entry.uuid(), entry);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // One malformed remote row must not hide all valid names.
                    }
                }
                namesLoaded = true;
                return names();
            }
        });
    }

    /**
     * Saves only the basic identity. The server preserves an existing farm,
     * origin and breeding record and applies the Lehen/owner permission rules.
     */
    public static CompletableFuture<AnimalBreedingHorse> saveName(
            UUID entityUuid, String registryName,
            AnimalBreedingHorseLocation location) {
        return saveName(entityUuid, registryName, location, "", "");
    }

    public static synchronized CompletableFuture<AnimalBreedingHorse> saveName(
            UUID entityUuid, String registryName,
            AnimalBreedingHorseLocation location,
            String coatColor, String marking) {
        OttoExtraApiClient current = api;
        AnimalInfoObservation observation = entityUuid == null
                ? null : OBSERVATIONS.get(entityUuid);
        String cleanName = registryName == null ? "" : registryName.trim();
        if (current == null || !onOttonien.getAsBoolean() || observation == null || cleanName.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Tier, Name oder aktuelle Tierbeobachtung fehlt"));
        }
        AnimalBreedingHorse draft = buildHorse(
                entityUuid, cleanName, observation, coatColor, marking,
                System.currentTimeMillis());
        long generation = connectionGeneration;
        return current.putAnimalBreedingHorseName(draft, location).thenApply(stored -> {
            synchronized (HorseRegistry.class) {
                requireCurrentConnection(generation);
                if (stored == null || !entityUuid.equals(stored.uuid())) {
                    throw new IllegalStateException("Tier-UUID der Speicherantwort stimmt nicht überein");
                }
                AnimalBreedingHorseName knownName = NAMES.get(entityUuid);
                AnimalBreedingHorse knownRecord = DATABASE_RECORDS.get(entityUuid);
                if ((knownName != null && knownName.revision() > stored.revision())
                        || (knownRecord != null && knownRecord.revision() > stored.revision())) return stored;
                invalidateReadRequests();
                AnimalBreedingHorseName entry = new AnimalBreedingHorseName(
                    stored.entityUuid(), stored.registryName(), stored.revision());
                NAMES.put(entry.uuid(), entry);
                DATABASE_RECORDS.put(stored.uuid(), stored);
                return stored;
            }
        });
    }

    private static void requireCurrentConnection(long generation) {
        if (generation != connectionGeneration || !onOttonien.getAsBoolean()) {
            throw new java.util.concurrent.CancellationException("Tierantwort gehört zu einer beendeten Verbindung");
        }
    }

    private static void invalidateReadRequests() {
        namesRequest++;
        databaseRequest++;
        namesLoaded = false;
        databaseLoaded = false;
        databaseLoading = false;
        databaseRetryAt = 0;
    }

    static AnimalBreedingHorse buildHorse(UUID entityUuid, String registryName,
                                           AnimalInfoObservation observation,
                                           String coatColor, String marking,
                                           long updatedAtEpochMillis) {
        var snapshot = observation.snapshot();
        Instant observedAt = observation.observedAt();
        AnimalBreedingHorseName known = NAMES.get(entityUuid);
        return new AnimalBreedingHorse(
                entityUuid.toString(), registryName, "", "",
                snapshot.animalName(), snapshot.gender(), coatColor, marking,
                snapshot.life(), snapshot.jumpHeight(), snapshot.speed(),
                snapshot.mast(), snapshot.breedingReady(),
                Math.max(0L, observedAt.toEpochMilli()),
                Math.max(1L, updatedAtEpochMillis),
                known == null ? 0L : known.revision(),
                "", "", false, false, "", "");
    }

    public static boolean basicSurfaceActive() {
        return basicSurfaceActive;
    }

    static synchronized void clearConnection() {
        closeIdentityCache();
        connectionGeneration++;
        namingPlayer = null;
        namingPlayerRequest++;
        databaseLoaded = false;
        namesLoaded = false;
        databaseLoading = false;
        databaseRetryAt = 0;
        databaseRequest++;
        namesRequest++;
        NAMES.clear();
        OBSERVATIONS.clear();
        DATABASE_RECORDS.clear();
    }

    static void shutdown() {
        clearConnection();
        CompletableFuture.runAsync(() -> { }, IDENTITY_IO).join();
        api = null;
        onOttonien = () -> false;
        basicSurfaceActive = false;
    }
}
