package de.ottoextra.animalinfo;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

/**
 * Live timers of all animals seen on the current connection. Readers (render thread, glow) only touch a
 * concurrent map. Persistence lives in the animal cache, which holds a reference to the store that was
 * current when the server was joined, so a later {@link #endSession()} cannot empty a pending save.
 */
public final class AnimalLiveStore {

    public static final long MAX_AGE_MS = Duration.ofDays(14).toMillis();
    public static final int MAX_ENTRIES = 1_024;

    private static volatile AnimalLiveStore current = new AnimalLiveStore();

    private final Map<UUID, AnimalLiveState> entries = new ConcurrentHashMap<>();
    private final AtomicLong revision = new AtomicLong();

    public static AnimalLiveStore current() {
        return current;
    }

    /** Called on disconnect: the next connection starts with an empty store. */
    public static void endSession() {
        current = new AnimalLiveStore();
    }

    public AnimalLiveState get(UUID uuid) {
        return uuid == null ? null : entries.get(uuid);
    }

    /** Applies a change to the entry of this animal, creating it if needed. */
    public synchronized void update(UUID uuid, long now, UnaryOperator<AnimalLiveState> change) {
        if (uuid == null) return;
        AnimalLiveState updated = change.apply(entries.getOrDefault(uuid, AnimalLiveState.empty(uuid)));
        if (updated == null) return;
        entries.put(uuid, updated);
        prune(now);
        revision.incrementAndGet();
    }

    /** Stores the answer of a server query. */
    public void observe(UUID uuid, AnimalInfoSnapshot snapshot, long now) {
        if (snapshot == null) return;
        update(uuid, now, state -> state.withInfo(snapshot, now));
    }

    /** Merges entries loaded from disk; entries that changed in memory since then win. */
    public synchronized void mergeLoaded(Collection<AnimalLiveState> loaded, long now) {
        if (loaded == null) return;
        for (AnimalLiveState entry : loaded) {
            if (entry == null || !entry.valid(now, MAX_AGE_MS)) continue;
            AnimalLiveState present = entries.get(entry.entityUuid());
            if (present == null || present.updatedAt() < entry.updatedAt()) {
                entries.put(entry.entityUuid(), entry);
            }
        }
        prune(now);
        revision.incrementAndGet();
    }

    /** Valid entries in a stable order, for saving. */
    public synchronized List<AnimalLiveState> snapshot(long now) {
        prune(now);
        return entries.values().stream()
                .sorted(Comparator.comparing(state -> state.entityUuid().toString()))
                .toList();
    }

    public synchronized void clear() {
        if (entries.isEmpty()) return;
        entries.clear();
        revision.incrementAndGet();
    }

    public int size() {
        return entries.size();
    }

    /** Increases with every change; used to detect unsaved data. */
    public long revision() {
        return revision.get();
    }

    private void prune(long now) {
        if (entries.values().removeIf(state -> !state.valid(now, MAX_AGE_MS))) revision.incrementAndGet();
        if (entries.size() > MAX_ENTRIES) {
            entries.values().stream()
                    .sorted(Comparator.comparingLong(AnimalLiveState::updatedAt))
                    .limit(entries.size() - MAX_ENTRIES)
                    .toList()
                    .forEach(state -> entries.remove(state.entityUuid()));
            revision.incrementAndGet();
        }
    }
}
