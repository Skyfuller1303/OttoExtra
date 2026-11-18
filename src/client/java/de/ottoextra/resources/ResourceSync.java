package de.ottoextra.resources;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;

/**
 * Polls and uploads shared lists. All network calls run on the HTTP client's threads; results are
 * applied on the client thread. Nothing happens while sharing is off or consent is missing.
 */
final class ResourceSync {
    static final long INTERVAL_MILLIS = 15_000L;
    /** since = server time of the previous successful response minus this overlap (server arrival time). */
    private static final long SINCE_OVERLAP_MILLIS = 10_000L;

    private final ResourceRepository repository;
    private final ResourcesConfig config;
    private final BooleanSupplier allowed;
    private final Runnable changed;
    private final ResourceShareApi api = new ResourceShareApi();
    private final Map<String, State> states = new HashMap<>();

    private static final class State {
        boolean inFlight;
        long nextAt;
        int polls;
        String error = "";
    }

    /** Entries of one list queued for a single PATCH. */
    private record Upload(JsonObject body, Map<String, Long> zones, Map<String, Long> timers, Long meta) {
        boolean empty() { return zones.isEmpty() && timers.isEmpty() && meta == null; }
    }

    ResourceSync(ResourceRepository repository, ResourcesConfig config, BooleanSupplier allowed,
                 Runnable changed) {
        this.repository = repository;
        this.config = config;
        this.allowed = allowed;
        this.changed = changed;
    }

    /** True when sharing is switched on and the core consent for online services is given. */
    boolean active() {
        return allowed.getAsBoolean();
    }

    /** Last error of a list, or an empty string. */
    String error(String localId) {
        State state = states.get(localId);
        return state == null ? "" : state.error;
    }

    /** Makes the next tick contact the service for this list. */
    void syncSoon(String localId) {
        states.computeIfAbsent(localId, id -> new State()).nextAt = 0L;
    }

    void forget(String localId) {
        states.remove(localId);
    }

    void tick(MinecraftClient client, long now) {
        if (!active()) return;
        URI base = ResourceShareApi.baseUri(config.shareBaseUrl);
        if (base == null) return;
        for (ResourceList list : repository.lists()) {
            if (!list.shared()) continue;
            State state = states.computeIfAbsent(list.localId, id -> new State());
            if (state.inFlight || now < state.nextAt) continue;
            cycle(client, list, state, base, now);
        }
    }

    private void cycle(MinecraftClient client, ResourceList list, State state, URI base, long now) {
        state.inFlight = true;
        state.nextAt = now + INTERVAL_MILLIS;
        String shareId = list.shareId();
        // The first poll of a session and of a fresh share has no since; merging is idempotent.
        long since = state.polls++ == 0 ? 0L : list.since;
        String writeCode = list.writeCode;
        ResourceList.Retired retired = list.retired.isEmpty() ? null : list.retired.getFirst();
        Upload upload = writeCode != null ? collect(list) : new Upload(new JsonObject(),
                Map.of(), Map.of(), null);
        String[] error = {""};

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        if (retired != null) {
            chain = chain.thenCompose(v -> api.patch(base, retired.shareId(), retired.writeCode(),
                            tombstoneBody(retired))
                    .handle((result, failure) -> {
                        client.execute(() -> {
                            // A rejected old list is gone for good; only network trouble retries.
                            if (failure == null || permanent(failure)) {
                                list.retired.remove(retired);
                                repository.save(list);
                            }
                        });
                        if (failure != null) error[0] = message(failure);
                        return (Void) null;
                    }));
        }
        if (!upload.empty()) {
            chain = chain.thenCompose(v -> api.patch(base, shareId, writeCode, upload.body())
                    .handle((result, failure) -> {
                        if (failure != null) error[0] = message(failure);
                        else client.execute(() -> acknowledge(list, upload));
                        return (Void) null;
                    }));
        }
        chain.thenCompose(v -> api.fetch(base, shareId, since))
                .whenComplete((remote, failure) -> client.execute(() -> {
                    state.inFlight = false;
                    if (failure != null) {
                        state.error = error[0].isEmpty() ? message(failure) : error[0];
                        return;
                    }
                    state.error = error[0];
                    if (repository.list(list.localId) == list && shareId.equals(list.shareId())) {
                        apply(list, remote);
                    }
                }));
    }

    private void apply(ResourceList list, ResourceShareApi.Remote remote) {
        boolean zones = ResourceMerge.zones(list.zones, remote.zones(), list.dirtyZones);
        boolean timers = ResourceMerge.timers(list.timers, remote.timers(), list.dirtyTimers);
        boolean meta = false;
        if (remote.meta() != null && remote.meta().updatedAt() > list.metaUpdatedAt) {
            list.applyMeta(remote.meta());
            list.metaDirty = false;
            meta = true;
        }
        list.since = Math.max(1L, remote.serverTime() - SINCE_OVERLAP_MILLIS);
        if (zones || timers || meta) {
            repository.save(list);
            changed.run();
        }
    }

    /** Collects the dirty entries; invalid ones are dropped because one bad entry fails the batch. */
    private Upload collect(ResourceList list) {
        JsonObject zones = new JsonObject();
        JsonObject timers = new JsonObject();
        Map<String, Long> sentZones = new LinkedHashMap<>();
        Map<String, Long> sentTimers = new LinkedHashMap<>();
        for (String id : java.util.List.copyOf(list.dirtyZones)) {
            ZoneRecord record = list.zones.get(id);
            if (record == null || ResourceWire.problem(record) != null) {
                list.dirtyZones.remove(id);
                continue;
            }
            zones.add(id, ResourceWire.zoneJson(record));
            sentZones.put(id, record.updatedAt());
        }
        for (String id : java.util.List.copyOf(list.dirtyTimers)) {
            TimerRecord timer = list.timers.get(id);
            if (timer == null) {
                list.dirtyTimers.remove(id);
                continue;
            }
            timers.add(id, ResourceWire.timerJson(timer));
            sentTimers.put(id, timer.updatedAt());
        }
        JsonObject body = new JsonObject();
        if (!sentZones.isEmpty()) body.add("zones", zones);
        if (!sentTimers.isEmpty()) body.add("timers", timers);
        Long sentMeta = null;
        if (list.metaDirty) {
            ListMeta meta = list.meta();
            if (ListMeta.problem(meta) == null) {
                body.add("meta", ResourceWire.metaJson(meta));
                sentMeta = meta.updatedAt();
            } else {
                list.metaDirty = false;
            }
        }
        return new Upload(body, sentZones, sentTimers, sentMeta);
    }

    private void acknowledge(ResourceList list, Upload upload) {
        // Entries changed again while the request was running stay queued.
        upload.zones().forEach((id, sentAt) -> {
            ZoneRecord current = list.zones.get(id);
            if (current == null || current.updatedAt() == sentAt) list.dirtyZones.remove(id);
        });
        upload.timers().forEach((id, sentAt) -> {
            TimerRecord current = list.timers.get(id);
            if (current == null || current.updatedAt() == sentAt) list.dirtyTimers.remove(id);
        });
        if (upload.meta() != null && list.metaUpdatedAt == upload.meta()) list.metaDirty = false;
        repository.save(list);
    }

    private static JsonObject tombstoneBody(ResourceList.Retired retired) {
        JsonObject zones = new JsonObject();
        retired.tombstones().forEach((id, record) -> zones.add(id, ResourceWire.zoneJson(record)));
        JsonObject body = new JsonObject();
        body.add("zones", zones);
        return body;
    }

    private static boolean permanent(Throwable failure) {
        Throwable cause = unwrap(failure);
        return cause instanceof ResourceShareApi.ShareException share
                && share.status >= 400 && share.status < 500 && share.status != 429;
    }

    private static String message(Throwable failure) {
        Throwable cause = unwrap(failure);
        String text = cause.getMessage();
        if (text == null || text.isBlank()) text = cause.getClass().getSimpleName();
        text = text.replaceAll("[\\p{Cntrl}]", "").trim();
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
