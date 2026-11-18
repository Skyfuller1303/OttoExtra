package de.ottoextra.horses;

import com.google.gson.JsonObject;
import de.ottoextra.OttoExtra;
import de.ottoextra.api.model.AnimalBreedingFarm;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.horses.FarmShareWire.AnimalRec;
import de.ottoextra.horses.FarmShareWire.FarmRec;
import de.ottoextra.privacy.ConsentPolicy;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Code-based sharing of farm lists (protocol: OttoExtra-Discordbot/web/docs/farm-lists-protokoll.md).
 * Polls every shared list every 30 seconds and uploads queued changes in one PATCH per list. Nothing is
 * contacted without the consent for online services. All network calls run on the HTTP client's threads;
 * results are applied on the client thread.
 */
public final class FarmShareService {
    static final long INTERVAL_MS = 30_000L;
    /** since = server time of the previous answer minus this overlap. */
    static final long SINCE_OVERLAP_MS = 10_000L;
    /** Upper bound of entries per PATCH; the rest follows shortly after. */
    private static final int MAX_FARMS_PER_PATCH = 300;
    private static final int MAX_ANIMALS_PER_PATCH = 500;

    public enum JoinResult { JOINED, UPGRADED, ALREADY, INVALID, NO_CONSENT, FULL }

    private static final FarmShareApi API = new FarmShareApi();
    private static final Map<String, State> STATES = new HashMap<>();

    private static final class State {
        boolean inFlight;
        long nextAt;
        int polls;
        String error = "";
        long lastSuccess;
    }

    /** Entries of one list queued for a single PATCH. */
    private record Upload(JsonObject body, Map<String, Long> farms, Map<String, Long> animals, Long meta,
                          boolean more) {
        boolean empty() {
            return farms.isEmpty() && animals.isEmpty() && meta == null;
        }
    }

    private FarmShareService() {
    }

    // ---- state for the UI -------------------------------------------------------------

    /** True when the consent for online services is given. */
    public static boolean allowed() {
        return ConsentPolicy.allowed(OttoExtraConfig.active(), ConsentPolicy.Purpose.ONLINE_SERVICES);
    }

    public static String baseUrl() {
        String value = OttoExtraConfig.active().animalInfo.farmShareBaseUrl;
        return FarmShareApi.baseUri(value) == null ? FarmShareApi.DEFAULT_BASE_URL : value.trim();
    }

    public static String joinUrl(String code) {
        return FarmShareApi.joinUrl(baseUrl(), code);
    }

    /** Last error of a list, or an empty string. */
    public static synchronized String error(FarmShareList list) {
        State state = list == null ? null : STATES.get(list.localId);
        return state == null ? "" : state.error;
    }

    /** Time of the last successful poll in this session, 0 for none. */
    public static synchronized long lastSync(FarmShareList list) {
        State state = list == null ? null : STATES.get(list.localId);
        return state == null ? 0L : state.lastSuccess;
    }

    /** Makes the next tick contact the service for this list. */
    public static synchronized void syncSoon(FarmShareList list) {
        if (list != null) STATES.computeIfAbsent(list.localId, id -> new State()).nextAt = 0L;
    }

    /** Called on disconnect: sessions start with a poll without {@code since}. */
    public static synchronized void reset() {
        STATES.clear();
    }

    /** Register key of a farm name; same rule as the animal book. */
    public static String farmKey(String farmName) {
        if (farmName == null || farmName.isBlank()) return "";
        for (AnimalBreedingFarm farm : HorseRegistry.farms()) {
            if (farm.name().equalsIgnoreCase(farmName.trim())) return farm.nameKey();
        }
        return "name:" + farmName.trim().toLowerCase(Locale.ROOT);
    }

    // ---- actions ----------------------------------------------------------------------

    /**
     * Shares one of the user's own farms: returns the owned list that contains it, creating a new list named
     * after the farm when there is none. Null when the farm has no name or the list limit is reached.
     */
    public static FarmShareList share(String farmName) {
        AnimalBookStore store = AnimalBookStore.current();
        String key = farmKey(farmName);
        if (key.isEmpty()) return null;
        AnimalBookStore.Farm header = store.farmForApi(key);
        if (header == null) header = store.saveFarm(key, farmName, "", FarmSymbols.DEFAULT);
        if (header == null) return null;
        FarmShareList existing = store.listOfFarm(header.id());
        if (existing != null) {
            syncSoon(existing);
            return existing;
        }
        long now = System.currentTimeMillis();
        FarmShareList list = new FarmShareList();
        list.owned = true;
        list.writeCode = FarmShareCodes.generateWriteCode();
        list.readCode = FarmShareCodes.readCode(list.writeCode);
        list.listId = FarmShareCodes.listId(list.readCode);
        list.name = AnimalBookStore.clip(header.name(), FarmShareWire.NAME_MAX);
        list.icon = FarmSymbols.shape(header.symbol());
        list.color = FarmSymbols.color(header.symbol());
        list.metaUpdatedAt = now;
        list.metaDirty = true;
        list.members.add(header.id());
        if (!store.addShareList(list)) return null;
        rebuild(store, list, now);
        syncSoon(list);
        return list;
    }

    /** Adds or removes one of the user's farms (by register name) to or from an owned list. */
    public static boolean toggleMember(FarmShareList list, String farmName) {
        if (list == null || !list.owned || list.readOnly()) return false;
        AnimalBookStore store = AnimalBookStore.current();
        String key = farmKey(farmName);
        AnimalBookStore.Farm header = store.farmForApi(key);
        if (header == null) header = store.saveFarm(key, farmName, "", FarmSymbols.DEFAULT);
        if (header == null) return false;
        FarmShareList other = store.listOfFarm(header.id());
        if (other != null && other != list) return false;
        if (!list.members.remove(header.id())) list.members.add(header.id());
        rebuild(store, list, System.currentTimeMillis());
        store.saveShareLists();
        syncSoon(list);
        return true;
    }

    /** Joins a list by write or read code. */
    public static JoinResult join(String rawCode) {
        if (!allowed()) return JoinResult.NO_CONSENT;
        FarmShareCodes.Kind kind = FarmShareCodes.kind(rawCode);
        if (kind == FarmShareCodes.Kind.INVALID) return JoinResult.INVALID;
        String normalized = FarmShareCodes.normalize(rawCode);
        String write = kind == FarmShareCodes.Kind.WRITE ? normalized : null;
        String read = write != null ? FarmShareCodes.readCode(write) : normalized;
        String listId = FarmShareCodes.listId(read);
        AnimalBookStore store = AnimalBookStore.current();
        FarmShareList existing = store.shareListById(listId);
        if (existing != null) {
            if (write != null && existing.readOnly()) {
                existing.writeCode = write;
                store.saveShareLists();
                syncSoon(existing);
                return JoinResult.UPGRADED;
            }
            syncSoon(existing);
            return JoinResult.ALREADY;
        }
        FarmShareList list = new FarmShareList();
        list.owned = false;
        list.writeCode = write;
        list.readCode = read;
        list.listId = listId;
        if (!store.addShareList(list)) return JoinResult.FULL;
        syncSoon(list);
        return JoinResult.JOINED;
    }

    /** The list a code belongs to, or null when it is not joined or invalid. */
    public static FarmShareList listForCode(String rawCode) {
        FarmShareCodes.Kind kind = FarmShareCodes.kind(rawCode);
        if (kind == FarmShareCodes.Kind.INVALID) return null;
        String normalized = FarmShareCodes.normalize(rawCode);
        String read = kind == FarmShareCodes.Kind.WRITE ? FarmShareCodes.readCode(normalized) : normalized;
        return AnimalBookStore.current().shareListById(FarmShareCodes.listId(read));
    }

    /** Header change of a farm of a joined, writable list. */
    public static boolean editSharedFarm(FarmShareList list, String farmId, String name, String note, String symbol) {
        if (list == null || list.owned) return false;
        if (!FarmShareMerge.editFarm(list, farmId, name, note, symbol, System.currentTimeMillis())) return false;
        AnimalBookStore.current().saveShareLists();
        syncSoon(list);
        return true;
    }

    /** "Codes erneuern": new codes; the old id receives tombstones. */
    public static boolean renew(FarmShareList list) {
        if (list == null || list.readOnly()) return false;
        if (!FarmShareMerge.renew(list, FarmShareCodes.generateWriteCode(), System.currentTimeMillis())) return false;
        AnimalBookStore.current().saveShareLists();
        synchronized (FarmShareService.class) {
            State state = STATES.computeIfAbsent(list.localId, id -> new State());
            state.polls = 0;
            state.nextAt = 0L;
        }
        return true;
    }

    /** Removes a list from this device only; the service keeps its last state. */
    public static void leave(FarmShareList list) {
        if (list == null) return;
        AnimalBookStore.current().removeShareList(list);
        synchronized (FarmShareService.class) {
            STATES.remove(list.localId);
        }
    }

    // ---- sync -------------------------------------------------------------------------

    public static void tick(MinecraftClient client) {
        if (client == null) return;
        AnimalBookStore store = AnimalBookStore.current();
        List<FarmShareList> lists = store.shareLists();
        if (lists.isEmpty() || !allowed()) return;
        URI base = FarmShareApi.baseUri(baseUrl());
        if (base == null) return;
        long now = System.currentTimeMillis();
        for (FarmShareList list : lists) {
            State state;
            synchronized (FarmShareService.class) {
                state = STATES.computeIfAbsent(list.localId, id -> new State());
                if (state.inFlight || now < state.nextAt) continue;
                state.inFlight = true;
                state.nextAt = now + INTERVAL_MS;
            }
            rebuild(store, list, now);
            if (FarmShareMerge.pruneTombstones(list, now)) store.saveShareLists();
            cycle(client, store, list, state, base);
        }
    }

    /** Owned lists follow the register and the local notes. */
    private static void rebuild(AnimalBookStore store, FarmShareList list, long now) {
        if (!list.owned) return;
        List<FarmShareMerge.RegisterAnimal> register = null;
        if (HorseRegistry.databaseLoaded()) {
            register = new ArrayList<>();
            for (AnimalBreedingHorse horse : HorseRegistry.databaseRecords()) {
                register.add(new FarmShareMerge.RegisterAnimal(horse.entityUuid().toLowerCase(Locale.ROOT),
                        farmKey(horse.currentFarm()), horse.registryName()));
            }
        }
        if (FarmShareMerge.rebuildOwned(list, store.headersById(), register, store.notesByUuid(), now)) {
            store.saveShareLists();
        }
    }

    private static void cycle(MinecraftClient client, AnimalBookStore store, FarmShareList list, State state, URI base) {
        String listId = list.listId;
        long since;
        synchronized (FarmShareService.class) {
            // The first poll of a session and of a renewed list has no since; merging is idempotent.
            since = state.polls++ == 0 ? 0L : list.since;
        }
        String writeCode = list.writeCode;
        FarmShareList.Retired retired = writeCode == null || list.retired.isEmpty() ? null : list.retired.getFirst();
        Upload upload = writeCode == null ? null : collect(list);
        String[] error = {""};

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        if (retired != null) {
            JsonObject body = FarmShareWire.body(null, retired.farms(), retired.animals());
            chain = chain.thenCompose(v -> API.patch(base, retired.listId(), retired.writeCode(), body)
                    .handle((result, failure) -> {
                        client.execute(() -> {
                            // A rejected old list is gone for good; only network trouble retries.
                            if (failure == null || permanent(failure)) {
                                list.retired.remove(retired);
                                store.saveShareLists();
                            }
                        });
                        if (failure != null) error[0] = message(failure);
                        return (Void) null;
                    }));
        }
        if (upload != null && !upload.empty()) {
            chain = chain.thenCompose(v -> API.patch(base, listId, writeCode, upload.body())
                    .handle((result, failure) -> {
                        if (failure != null) error[0] = message(failure);
                        boolean drop = failure != null && permanent(failure);
                        if (drop) {
                            OttoExtra.LOGGER.warn("Hofliste: Änderungen abgelehnt ({}).", error[0]);
                        }
                        if (failure == null || drop) client.execute(() -> acknowledge(store, list, upload));
                        return (Void) null;
                    }));
        }
        chain.thenCompose(v -> API.fetch(base, listId, since))
                .whenComplete((remote, failure) -> client.execute(() -> {
                    synchronized (FarmShareService.class) {
                        state.inFlight = false;
                        if (failure != null) {
                            state.error = error[0].isEmpty() ? message(failure) : error[0];
                            return;
                        }
                        state.error = error[0];
                        state.lastSuccess = System.currentTimeMillis();
                        if (upload != null && upload.more() && error[0].isEmpty()) {
                            state.nextAt = System.currentTimeMillis() + 2_000L;
                        }
                    }
                    if (store == AnimalBookStore.current() && store.shareList(list.localId) == list
                            && listId.equals(list.listId)) {
                        apply(store, list, remote);
                    }
                }));
    }

    private static void apply(AnimalBookStore store, FarmShareList list, FarmShareWire.Remote remote) {
        if (remote.invalid() > 0) {
            OttoExtra.LOGGER.debug("Hofliste: {} ungültige Einträge übersprungen.", remote.invalid());
        }
        FarmShareMerge.Merged merged = FarmShareMerge.apply(list, remote, SINCE_OVERLAP_MS);
        if (list.owned) {
            // Changes of a co-writer to the user's own farms and animals go into the local book, so the next
            // rebuild does not undo them.
            for (String id : merged.farms()) {
                FarmRec farm = list.farms.get(id);
                if (farm != null && !farm.deleted()) store.applySharedFarm(id, farm.note(), farm.symbol());
            }
            for (String uuid : merged.animals()) {
                AnimalRec animal = list.animals.get(uuid);
                if (animal != null && !animal.deleted()) store.applySharedNote(uuid, animal.note());
            }
        }
        // Only a poll that changed data is written; the new since alone is not worth a write (the first poll
        // of a session has no since anyway).
        if (merged.any()) store.saveShareLists();
    }

    /** Collects the queued entries; invalid ones are dropped because one bad entry fails the whole batch. */
    private static Upload collect(FarmShareList list) {
        Map<String, FarmRec> farms = new LinkedHashMap<>();
        Map<String, AnimalRec> animals = new LinkedHashMap<>();
        Map<String, Long> sentFarms = new LinkedHashMap<>();
        Map<String, Long> sentAnimals = new LinkedHashMap<>();
        boolean more = false;
        for (String id : List.copyOf(list.dirtyFarms)) {
            FarmRec farm = list.farms.get(id);
            if (farm == null || FarmShareWire.problem(farm) != null) {
                list.dirtyFarms.remove(id);
                continue;
            }
            if (farms.size() >= MAX_FARMS_PER_PATCH) {
                more = true;
                break;
            }
            farms.put(id, farm);
            sentFarms.put(id, farm.updatedAt());
        }
        for (String uuid : List.copyOf(list.dirtyAnimals)) {
            AnimalRec animal = list.animals.get(uuid);
            if (animal == null || FarmShareWire.problem(animal) != null) {
                list.dirtyAnimals.remove(uuid);
                continue;
            }
            if (animals.size() >= MAX_ANIMALS_PER_PATCH) {
                more = true;
                break;
            }
            animals.put(uuid, animal);
            sentAnimals.put(uuid, animal.updatedAt());
        }
        FarmShareWire.Meta meta = null;
        if (list.metaDirty) {
            FarmShareWire.Meta current = list.meta();
            if (FarmShareWire.problem(current) == null) meta = current;
            else list.metaDirty = false;
        }
        return new Upload(FarmShareWire.body(meta, farms, animals), sentFarms, sentAnimals,
                meta == null ? null : meta.updatedAt(), more);
    }

    private static void acknowledge(AnimalBookStore store, FarmShareList list, Upload upload) {
        // Entries changed again while the request was running stay queued.
        upload.farms().forEach((id, sentAt) -> {
            FarmRec current = list.farms.get(id);
            if (current == null || current.updatedAt() == sentAt) list.dirtyFarms.remove(id);
        });
        upload.animals().forEach((uuid, sentAt) -> {
            AnimalRec current = list.animals.get(uuid);
            if (current == null || current.updatedAt() == sentAt) list.dirtyAnimals.remove(uuid);
        });
        if (upload.meta() != null && list.metaUpdatedAt == upload.meta()) list.metaDirty = false;
        store.saveShareLists();
    }

    /** 4xx except 429: retrying the same request cannot succeed. */
    static boolean permanent(Throwable failure) {
        Throwable cause = unwrap(failure);
        return cause instanceof FarmShareApi.ShareException share
                && share.status >= 400 && share.status < 500 && share.status != 429;
    }

    private static String message(Throwable failure) {
        Throwable cause = unwrap(failure);
        if (cause instanceof FarmShareApi.ShareException share && share.status == 403) return "403";
        String text = cause.getMessage();
        if (text == null || text.isBlank()) text = cause.getClass().getSimpleName();
        text = text.replaceAll("[\\p{Cntrl}]", "").trim();
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null) current = current.getCause();
        return current;
    }
}
