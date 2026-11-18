package de.ottoextra.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

/** Local, per-player aggregates. Message classifications apply retroactively to all occurrences. */
final class ResourceStatistics {
    static final long RUN_IDLE_MILLIS = 5 * 60_000;
    static final String LEGACY_RUN_ID = "legacy";
    static final String PICKUP_MESSAGE = "__resource_item_pickup__";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Data data = new Data();
    private final Map<Key, Entry> index = new HashMap<>();
    private boolean dirty;
    private String storageError;
    private boolean readable = true;
    private long nextSave;
    private long revision;
    private Run activeRun;
    private Path legacyBackup;
    private boolean legacyBackupCreated;

    enum Outcome {
        UNKNOWN, SUCCESS, FAILURE, IGNORED;

        Outcome next() { return values()[(ordinal() + 1) % values().length]; }
        String translationKey() { return "ottoextra.resources.stats.outcome." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    record Row(String resourceId, String resourceName, String type, String dimension, String region,
               String message, Outcome outcome, long count, boolean pickupConfirmed) { }
    record RegionSummary(String region, Totals totals) { }
    record RunSummary(String id, String resourceName, String region, long startedAt, long endedAt,
                      boolean active, Totals totals) { }
    record Totals(long successes, long failures, long unknown, long ignored) {
        long attempts() { return successes + failures; }
        double successRate() { return attempts() == 0 ? 0 : successes * 100.0 / attempts(); }
    }
    private record Key(String runId, String resourceId, String dimension, String region, String message, boolean pickupConfirmed) { }
    static final class RecordedResult {
        private Entry entry;
        private RecordedResult(Entry entry) { this.entry = entry; }
    }

    ResourceStatistics(Path file) {
        this.file = file;
        if (Files.exists(file)) {
            try {
                Data loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
                if (loaded == null || (loaded.version != 1 && loaded.version != 2)
                        || loaded.entries == null || loaded.outcomes == null || loaded.runs == null) {
                    throw new IOException("Ungültiges Statistikformat");
                }
                var runIds = new HashSet<String>();
                for (Run run : loaded.runs) {
                    if (run == null || run.id == null || run.id.equals(LEGACY_RUN_ID) || !runIds.add(run.id)
                            || run.resourceId == null || run.resourceName == null || run.dimension == null
                            || run.region == null || run.startedAt < 0 || run.lastActivityAt < run.startedAt
                            || (run.endedAt != -1 && (run.endedAt < run.startedAt || run.endedAt > run.lastActivityAt))) {
                        throw new IOException("Ungültiger Minengang");
                    }
                    // A client restart always starts a fresh run, including after an unclean shutdown.
                    if (run.endedAt == -1) {
                        run.endedAt = run.lastActivityAt;
                        dirty = true;
                    }
                }
                var entryKeys = new HashSet<Key>();
                for (Entry entry : loaded.entries) {
                    if (entry == null || entry.resourceId == null || entry.resourceName == null
                            || entry.type == null || entry.dimension == null || entry.message == null
                            || entry.message.isBlank() || entry.count < 1) {
                        throw new IOException("Ungültiger Statistikeintrag");
                    }
                    // Earlier local statistics had no region field; keep these in the unknown group.
                    if (entry.region == null) entry.region = "";
                    if (loaded.version == 1) entry.runId = LEGACY_RUN_ID;
                    if (entry.runId == null || (!entry.runId.equals(LEGACY_RUN_ID) && !runIds.contains(entry.runId))) {
                        throw new IOException("Statistikeintrag ohne Minengang");
                    }
                    if (!entryKeys.add(entry.key())) throw new IOException("Doppelter Statistikeintrag");
                }
                if (loaded.version == 1) {
                    legacyBackup = file.resolveSibling(file.getFileName() + ".v1-" + UUID.randomUUID() + ".bak");
                    loaded.version = 2;
                    dirty = true;
                }
                data = loaded;
                data.entries.forEach(entry -> index.put(entry.key(), entry));
            } catch (Exception error) {
                readable = false; // Preserve an unreadable file instead of overwriting the history.
                storageError = error.toString();
            }
        }
    }

    RecordedResult record(ResourceActionbarTracker.Observation observation) {
        return record(observation, System.currentTimeMillis());
    }

    RecordedResult record(ResourceActionbarTracker.Observation observation, long now) {
        var target = observation.target();
        interact(target, now);
        String region = ResourceActionbarTracker.normalize(target.region());
        Key key = new Key(activeRun.id, target.id(), target.dimension(), region, observation.message(), false);
        Entry entry = index.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.runId = activeRun.id;
            entry.resourceId = target.id();
            entry.resourceName = target.name();
            entry.type = target.type();
            entry.dimension = target.dimension();
            entry.region = region;
            entry.message = observation.message();
            index.put(key, entry);
            data.entries.add(entry);
        }
        entry.resourceName = target.name();
        entry.count++;
        dirty = true;
        revision++;
        return new RecordedResult(entry);
    }

    void confirmPickup(RecordedResult result) {
        reassign(result, result.entry.message, true);
    }

    void attachPickupMessage(RecordedResult result, String message) {
        reassign(result, message, true);
    }

    private void reassign(RecordedResult result, String message, boolean confirmed) {
        Entry previous = result.entry;
        Key key = new Key(previous.runId, previous.resourceId, previous.dimension, previous.region, message, confirmed);
        if (key.equals(previous.key())) return;
        Entry next = index.get(key);
        if (next == null) {
            next = new Entry();
            next.runId = previous.runId;
            next.resourceId = previous.resourceId;
            next.resourceName = previous.resourceName;
            next.type = previous.type;
            next.dimension = previous.dimension;
            next.region = previous.region;
            next.message = message;
            next.pickupConfirmed = confirmed;
            index.put(key, next);
            data.entries.add(next);
        }
        next.count++;
        if (--previous.count == 0) {
            index.remove(previous.key());
            data.entries.remove(previous);
        }
        result.entry = next;
        dirty = true;
        revision++;
    }

    void interact(ResourceActionbarTracker.Target target, long now) {
        String region = ResourceActionbarTracker.normalize(target.region());
        if (activeRun != null && (!activeRun.resourceId.equals(target.id())
                || !activeRun.dimension.equals(target.dimension()) || !activeRun.region.equals(region)
                || now < activeRun.lastActivityAt || now - activeRun.lastActivityAt >= RUN_IDLE_MILLIS)) {
            finishRun();
        }
        if (activeRun == null) {
            activeRun = new Run();
            activeRun.id = UUID.randomUUID().toString();
            activeRun.resourceId = target.id();
            activeRun.resourceName = target.name();
            activeRun.dimension = target.dimension();
            activeRun.region = region;
            activeRun.startedAt = now;
            data.runs.add(activeRun);
            revision++;
        }
        activeRun.lastActivityAt = now;
        dirty = true;
    }

    boolean finishRun() {
        if (activeRun == null) return false;
        activeRun.endedAt = activeRun.lastActivityAt;
        activeRun = null;
        dirty = true;
        revision++;
        return true;
    }

    List<RunSummary> runs(String resourceId) {
        Map<String, long[]> counts = new HashMap<>();
        for (Entry entry : data.entries) {
            if (resourceId != null && !resourceId.equals(entry.resourceId)) continue;
            counts.computeIfAbsent(entry.runId, ignored -> new long[Outcome.values().length])
                    [outcome(entry).ordinal()] += entry.count;
        }
        List<RunSummary> result = new ArrayList<>();
        for (Run run : data.runs.reversed()) {
            if (resourceId != null && !resourceId.equals(run.resourceId)) continue;
            result.add(new RunSummary(run.id, run.resourceName, run.region, run.startedAt,
                    run.endedAt, run == activeRun, fromCounts(counts.get(run.id))));
        }
        result.sort(Comparator.comparingLong(RunSummary::startedAt).reversed());
        if (counts.containsKey(LEGACY_RUN_ID)) {
            result.add(new RunSummary(LEGACY_RUN_ID, "", "", 0, 0, false,
                    fromCounts(counts.get(LEGACY_RUN_ID))));
        }
        return List.copyOf(result);
    }

    private static Totals fromCounts(long[] counts) {
        return counts == null ? new Totals(0, 0, 0, 0) : new Totals(counts[Outcome.SUCCESS.ordinal()],
                counts[Outcome.FAILURE.ordinal()], counts[Outcome.UNKNOWN.ordinal()], counts[Outcome.IGNORED.ordinal()]);
    }

    List<Row> rowsForRun(String runId, String resourceId) {
        return data.entries.stream().filter(entry -> Objects.equals(runId, entry.runId)
                        && (resourceId == null || resourceId.equals(entry.resourceId)))
                .map(this::row).sorted(Comparator.comparing(Row::resourceName).thenComparing(Row::message)).toList();
    }

    private Row row(Entry entry) {
        return new Row(entry.resourceId, entry.resourceName, entry.type, entry.dimension, entry.region,
                entry.message, outcome(entry), entry.count, entry.pickupConfirmed);
    }

    private Outcome outcome(Entry entry) {
        return entry.pickupConfirmed ? Outcome.SUCCESS : outcome(entry.message);
    }

    Outcome outcome(String message) {
        Outcome configured = data.outcomes.get(message);
        // Without confirmed server wording, unknown text must never silently affect the success rate.
        return configured == null ? Outcome.UNKNOWN : configured;
    }

    void classify(String message, Outcome outcome) {
        data.outcomes.put(message, outcome);
        dirty = true;
        revision++;
        save();
    }

    List<Row> rows(String resourceId) {
        return rows(resourceId, null);
    }

    List<Row> rows(String resourceId, String region) {
        Map<Key, Row> grouped = new HashMap<>();
        for (Entry entry : data.entries) {
            if (!matches(entry, resourceId, region)) continue;
            Key key = new Key(null, entry.resourceId, entry.dimension, entry.region, entry.message, entry.pickupConfirmed);
            Row previous = grouped.get(key);
            grouped.put(key, new Row(entry.resourceId, entry.resourceName, entry.type, entry.dimension,
                    entry.region, entry.message, outcome(entry), entry.count + (previous == null ? 0 : previous.count()), entry.pickupConfirmed));
        }
        return grouped.values().stream()
                .sorted(Comparator.comparing(Row::region).thenComparing(Row::resourceName).thenComparing(Row::message)).toList();
    }

    Totals totals(String resourceId) {
        return totals(resourceId, null);
    }

    Totals totals(String resourceId, String region) {
        long success = 0, failure = 0, unknown = 0, ignored = 0;
        for (Entry entry : data.entries) {
            if (!matches(entry, resourceId, region)) continue;
            switch (outcome(entry)) {
                case SUCCESS -> success += entry.count;
                case FAILURE -> failure += entry.count;
                case UNKNOWN -> unknown += entry.count;
                case IGNORED -> ignored += entry.count;
            }
        }
        return new Totals(success, failure, unknown, ignored);
    }

    List<RegionSummary> regions(String resourceId) {
        return data.entries.stream().filter(entry -> matches(entry, resourceId, null))
                .map(entry -> entry.region).distinct().sorted()
                .map(region -> new RegionSummary(region, totals(resourceId, region))).toList();
    }

    private static boolean matches(Entry entry, String resourceId, String region) {
        return (resourceId == null || resourceId.equals(entry.resourceId))
                && (region == null || region.equals(entry.region));
    }

    long revision() { return revision; }
    String storageError() { return storageError; }

    void tick(long now) {
        if (activeRun != null && (now < activeRun.lastActivityAt
                || now - activeRun.lastActivityAt >= RUN_IDLE_MILLIS)) finishRun();
        if (dirty && now >= nextSave) {
            nextSave = now + 5_000;
            save();
        }
    }

    boolean save() {
        if (!readable) return false;
        if (!dirty) return true;
        try {
            Files.createDirectories(file.getParent());
            if (legacyBackup != null && !legacyBackupCreated) {
                // This UUID path belongs to this migration; retry a failed partial copy safely.
                Files.copy(file, legacyBackup, StandardCopyOption.REPLACE_EXISTING);
                legacyBackupCreated = true;
            }
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            storageError = null;
            return true;
        } catch (IOException error) {
            storageError = error.toString();
            return false;
        }
    }

    private static final class Data {
        int version = 2;
        List<Entry> entries = new ArrayList<>();
        Map<String, Outcome> outcomes = new HashMap<>();
        List<Run> runs = new ArrayList<>();
    }

    private static final class Run {
        String id;
        String resourceId;
        String resourceName;
        String dimension;
        String region;
        long startedAt;
        long lastActivityAt;
        long endedAt = -1;
    }

    private static final class Entry {
        String runId;
        String resourceId;
        String resourceName;
        String type;
        String dimension;
        String region = "";
        String message;
        long count;
        boolean pickupConfirmed;

        Key key() { return new Key(runId, resourceId, dimension, region, message, pickupConfirmed); }
    }
}
