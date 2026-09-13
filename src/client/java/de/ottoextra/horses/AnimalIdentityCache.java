package de.ottoextra.horses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Display-only identities. Never contains breeding state or grants register access. */
final class AnimalIdentityCache {
    static final long MAX_AGE_MS = Duration.ofDays(14).toMillis();
    static final int MAX_ENTRIES = 1_024;
    private static final long MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    record Identity(UUID entityUuid, String animalType, String gender, long observedAt) { }
    private record Stored(int version, String server, List<Identity> animals) { }

    private final Path file;
    private final String server;
    private final LongSupplier clock;
    private final Map<UUID, Identity> entries = new ConcurrentHashMap<>();
    private long revision;
    private long savedRevision;
    private boolean loaded;
    private final Set<UUID> changedDuringLoad = new HashSet<>();

    AnimalIdentityCache(Path file, String server, LongSupplier clock) {
        this.file = file;
        this.server = server;
        this.clock = clock;
    }

    Identity find(UUID uuid) {
        Identity entry = uuid == null ? null : entries.get(uuid);
        return valid(entry, clock.getAsLong()) ? entry : null;
    }

    synchronized void remember(Identity entry) {
        long now = clock.getAsLong();
        if (entry == null || entry.entityUuid() == null || entry.observedAt() > now
                || entry.observedAt() <= now - MAX_AGE_MS) return;
        Identity previous = entries.get(entry.entityUuid());
        if (previous != null && previous.observedAt() > entry.observedAt()) return;
        if (!loaded) changedDuringLoad.add(entry.entityUuid());
        if (valid(entry, now)) entries.put(entry.entityUuid(), entry);
        else entries.remove(entry.entityUuid());
        prune(now);
        revision++;
    }

    /** Called only on the shared cache I/O worker; merge preserves observations received during loading. */
    void load() {
        try {
            if (!Files.exists(file)) return;
            if (Files.size(file) > MAX_FILE_BYTES) throw new IllegalArgumentException("cache too large");
            Stored data = JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Stored.class);
            if (data == null || data.version() != 1 || !server.equals(data.server())
                    || data.animals() == null || data.animals().size() > 4096) {
                throw new IllegalArgumentException("invalid cache envelope");
            }
            synchronized (this) {
                long now = clock.getAsLong();
                for (Identity entry : data.animals()) {
                    if (!valid(entry, now) || changedDuringLoad.contains(entry.entityUuid())) continue;
                    Identity newer = entries.get(entry.entityUuid());
                    if (newer == null || newer.observedAt() < entry.observedAt()) entries.put(entry.entityUuid(), entry);
                }
                prune(now);
                revision++;
            }
        } catch (Exception error) {
            OttoExtra.LOGGER.warn("Lokaler Tiercache konnte nicht geladen werden: {}", error.toString());
        } finally {
            synchronized (this) { loaded = true; changedDuringLoad.clear(); }
        }
    }

    /** Snapshot under the lock, disk writes outside it. Only the I/O worker calls this method. */
    void saveIfDirty() {
        List<Identity> snapshot;
        long writingRevision;
        synchronized (this) {
            prune(clock.getAsLong());
            if (savedRevision == revision) return;
            writingRevision = revision;
            snapshot = entries.values().stream().sorted(Comparator.comparing(e -> e.entityUuid().toString())).toList();
        }
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            String json = JSON.toJson(new Stored(1, server, snapshot));
            Path cacheRoot = file.getParent().getFileName().toString().equals("animals")
                    ? file.getParent().getParent() : file.getParent();
            de.ottoextra.privacy.CacheBudget.write(cacheRoot, file, json.getBytes(StandardCharsets.UTF_8).length, () -> {
                try {
                    Files.writeString(temporary, json, StandardCharsets.UTF_8);
                    try {
                        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException unsupported) {
                        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally { Files.deleteIfExists(temporary); }
            });
            synchronized (this) { savedRevision = writingRevision; }
        } catch (Exception error) {
            OttoExtra.LOGGER.warn("Lokaler Tiercache konnte nicht gespeichert werden: {}", error.toString());
        }
    }

    void delete() {
        try { Files.deleteIfExists(file); }
        catch (Exception error) { OttoExtra.LOGGER.warn("Lokaler Tiercache konnte nicht gelöscht werden: {}", error.toString()); }
    }

    private void prune(long now) {
        if (entries.values().removeIf(entry -> !valid(entry, now))) revision++;
        if (entries.size() > MAX_ENTRIES) {
            entries.values().stream().sorted(Comparator.comparingLong(Identity::observedAt))
                    .limit(entries.size() - MAX_ENTRIES).toList().forEach(e -> entries.remove(e.entityUuid()));
            revision++;
        }
    }

    private static boolean valid(Identity entry, long now) {
        return entry != null && entry.entityUuid() != null && entry.animalType() != null
                && !entry.animalType().isBlank() && entry.animalType().length() <= 128
                && entry.gender() != null && entry.gender().length() <= 64 && AnimalGender.color(entry.gender()) != null
                && entry.observedAt() <= now && entry.observedAt() > now - MAX_AGE_MS;
    }
}
