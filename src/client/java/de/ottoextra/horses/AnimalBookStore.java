package de.ottoextra.horses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Local data of the animal book that the register API does not hold: the header of each farm (symbol,
 * note) and a note per animal. Stored per server under {@code config/ottoextra/animalbook/}.
 *
 * <p>Shared farm lists ({@link FarmShareList}) live in the same file; only those are ever sent, and only the
 * fields of the farm list protocol. The model follows the same scheme as the resource lists: every
 * record has a stable id, its own {@code updatedAt} and a {@code deleted} flag, so two copies can be merged
 * record by record ("the larger updatedAt wins") and deletions travel as tombstones. Farms that exist in the
 * register API are linked through {@code apiKey} (the API's {@code nameKey}); the API stays the source of
 * truth for names, membership and animal values.
 *
 * <p>Persistence: compact JSON, written atomically (temporary file, then move). The store of a connection
 * writes on a single background thread, coalesced to at most one write per second (last write wins) and
 * flushed on disconnect. A file that cannot be read is renamed to {@code <name>.broken-<millis>.json} (the
 * newest three are kept) before the store continues empty; if that rename fails, the store never writes, so
 * an unreadable file is never overwritten.
 */
public final class AnimalBookStore {
    public static final int NOTE_MAX = 200;
    public static final int FARM_NAME_MAX = 64;
    public static final int MAX_FARMS = 300;
    public static final int MAX_ANIMALS = 4_096;
    /** Tombstones are kept this long so a later merge can still see the deletion. */
    public static final long TOMBSTONE_MS = Duration.ofDays(30).toMillis();
    static final int FILE_VERSION = 2;
    /** At most this many shared farm lists per server. */
    public static final int MAX_SHARE_LISTS = 20;
    /** 20 lists with up to 1000 animals and 300 farms each plus 4096 own animals stay well below this. */
    static final long MAX_FILE_BYTES = 16L * 1024 * 1024;
    static final int MAX_BROKEN_BACKUPS = 3;
    private static final long WRITE_INTERVAL_MS = 1_000L;
    private static final Pattern ID = Pattern.compile("[a-z0-9-]{8,40}");
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "OttoExtra-Tierbuch");
        thread.setDaemon(true);
        return thread;
    });
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Header of a farm.
     *
     * @param id        stable local id, 8 to 40 characters of {@code [a-z0-9-]}
     * @param apiKey    {@code nameKey} of the register farm this header belongs to, empty if none
     * @param name      display name, 1 to 64 characters
     * @param note      free text, 0 to 200 characters
     * @param symbol    {@code <shape>_<colour>} from {@link FarmSymbols}
     * @param updatedAt last local change, epoch milliseconds
     * @param deleted   tombstone
     */
    public record Farm(String id, String apiKey, String name, String note, String symbol, long updatedAt,
                       boolean deleted) {
    }

    /**
     * Local part of an animal entry.
     *
     * @param uuid      entity UUID
     * @param farmId    {@link Farm#id()} of the farm the animal was last saved with, empty if none
     * @param note      free text, 0 to 200 characters
     * @param updatedAt last local change, epoch milliseconds
     * @param deleted   tombstone, set when the register entry is deleted
     */
    public record Animal(String uuid, String farmId, String note, long updatedAt, boolean deleted) {
    }

    private record Stored(int version, String server, List<Farm> farms, List<Animal> animals,
                          List<FarmShareList> lists) {
    }

    private static volatile AnimalBookStore current = new AnimalBookStore(null, "", System::currentTimeMillis);

    private final Path file;
    private final String server;
    private final LongSupplier clock;
    private final Map<String, Farm> farms = new LinkedHashMap<>();
    private final Map<String, Animal> animals = new LinkedHashMap<>();
    private final List<FarmShareList> lists = new ArrayList<>();
    /** Background writes (the store of a connection); tests write synchronously. */
    private final boolean background;
    private final AtomicReference<String> pendingJson = new AtomicReference<>();
    /** Set when an unreadable file could not be moved aside: nothing is written then. */
    private boolean blocked;
    private boolean dirty;
    private long lastWrite;

    AnimalBookStore(Path file, String server, LongSupplier clock) {
        this(file, server, clock, false);
    }

    private AnimalBookStore(Path file, String server, LongSupplier clock, boolean background) {
        this.file = file;
        this.server = server == null ? "" : server;
        this.clock = clock;
        this.background = background;
    }

    /** Store of the current server; an in-memory store without a file while not connected. */
    public static AnimalBookStore current() {
        return current;
    }

    /** Opens and loads the store of a server; called when joining. */
    static synchronized void open(Path file, String server) {
        current.flushNow();
        AnimalBookStore store = new AnimalBookStore(file, server, System::currentTimeMillis, true);
        store.load();
        current = store;
    }

    /** Called on disconnect: writes pending changes, then the next connection starts empty in memory. */
    static synchronized void close() {
        current.flushNow();
        current = new AnimalBookStore(null, "", System::currentTimeMillis);
    }

    /** Called every client tick: hands pending changes to the writer, at most once per second. */
    public void tick() {
        String json = null;
        synchronized (this) {
            long now = clock.getAsLong();
            if (!dirty || file == null || blocked || now - lastWrite < WRITE_INTERVAL_MS) return;
            dirty = false;
            lastWrite = now;
            json = serialize();
        }
        queue(json);
    }

    /** True when an unreadable file blocks every write. */
    public synchronized boolean blocked() {
        return blocked;
    }

    // ---- reading -------------------------------------------------------------------------

    /** Header of the register farm with this key, or null. */
    public synchronized Farm farmForApi(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return null;
        for (Farm farm : farms.values()) {
            if (!farm.deleted() && farm.apiKey().equals(apiKey)) return farm;
        }
        return null;
    }

    /** Symbol of a register farm; the default symbol when it has none yet. */
    public String symbol(String apiKey) {
        Farm farm = farmForApi(apiKey);
        return farm == null ? FarmSymbols.DEFAULT : FarmSymbols.canonical(farm.symbol());
    }

    /** Note of a farm header, empty when none. */
    public String farmNote(String apiKey) {
        Farm farm = farmForApi(apiKey);
        return farm == null ? "" : farm.note();
    }

    /** Note of an animal, empty when none. */
    public synchronized String note(UUID uuid) {
        Animal animal = uuid == null ? null : animals.get(uuid.toString());
        return animal == null || animal.deleted() ? "" : animal.note();
    }

    /** All records including tombstones, for a later sync and for tests. */
    public synchronized List<Farm> farms() {
        return List.copyOf(farms.values());
    }

    public synchronized List<Animal> animals() {
        return List.copyOf(animals.values());
    }

    // ---- writing -------------------------------------------------------------------------

    /**
     * Creates or updates the header of a register farm. Returns the stored header; an unchanged header keeps
     * its {@code updatedAt}.
     */
    public synchronized Farm saveFarm(String apiKey, String name, String note, String symbol) {
        String key = apiKey == null ? "" : apiKey.trim();
        String cleanName = clip(name, FARM_NAME_MAX);
        if (cleanName.isEmpty()) cleanName = key;
        if (cleanName.isEmpty()) return null;
        Farm existing = farmForApi(key);
        String id = existing == null ? newId() : existing.id();
        Farm updated = new Farm(id, key, cleanName, clip(note, NOTE_MAX), FarmSymbols.canonical(symbol),
                clock.getAsLong(), false);
        if (existing != null && existing.name().equals(updated.name()) && existing.note().equals(updated.note())
                && existing.symbol().equals(updated.symbol())) {
            return existing;
        }
        farms.put(id, updated);
        prune();
        save();
        return updated;
    }

    /**
     * Saves the local part of an animal entry: the note and the farm it was saved with. Nothing is written
     * when neither changed.
     */
    public synchronized void saveAnimal(UUID uuid, String farmApiKey, String farmName, String note) {
        if (uuid == null) return;
        String farmId = "";
        if (farmApiKey != null && !farmApiKey.isBlank()) {
            Farm farm = farmForApi(farmApiKey);
            if (farm == null) farm = saveFarm(farmApiKey, farmName, "", FarmSymbols.DEFAULT);
            if (farm != null) farmId = farm.id();
        }
        String cleanNote = clip(note, NOTE_MAX);
        Animal existing = animals.get(uuid.toString());
        if (existing != null && !existing.deleted() && existing.note().equals(cleanNote)
                && existing.farmId().equals(farmId)) {
            return;
        }
        if (existing == null && cleanNote.isEmpty() && farmId.isEmpty()) return;
        animals.put(uuid.toString(), new Animal(uuid.toString(), farmId, cleanNote, clock.getAsLong(), false));
        prune();
        save();
    }

    /** Marks an animal as deleted (its register entry is gone); the note is dropped. */
    public synchronized void deleteAnimal(UUID uuid) {
        if (uuid == null) return;
        Animal existing = animals.get(uuid.toString());
        if (existing == null || existing.deleted()) return;
        animals.put(uuid.toString(), new Animal(uuid.toString(), "", "", clock.getAsLong(), true));
        save();
    }

    // ---- shared lists --------------------------------------------------------------------

    /** Header with this local id, or null. */
    public synchronized Farm farmById(String id) {
        Farm farm = id == null ? null : farms.get(id);
        return farm == null || farm.deleted() ? null : farm;
    }

    /** Local headers by id, without tombstones. */
    synchronized Map<String, Farm> headersById() {
        Map<String, Farm> result = new LinkedHashMap<>();
        farms.forEach((id, farm) -> { if (!farm.deleted()) result.put(id, farm); });
        return result;
    }

    /** Local animal notes by lower-case UUID. */
    synchronized Map<String, String> notesByUuid() {
        Map<String, String> result = new LinkedHashMap<>();
        animals.forEach((uuid, animal) -> { if (!animal.deleted() && !animal.note().isEmpty()) result.put(uuid, animal.note()); });
        return result;
    }

    /**
     * A shared change of one of the user's own farms: takes note and symbol into the local header (the name
     * stays the register name). Returns false when nothing changed.
     */
    synchronized boolean applySharedFarm(String id, String note, String symbol) {
        Farm existing = farmById(id);
        if (existing == null) return false;
        String cleanNote = clip(note, NOTE_MAX);
        String cleanSymbol = FarmSymbols.canonical(symbol);
        if (existing.note().equals(cleanNote) && existing.symbol().equals(cleanSymbol)) return false;
        farms.put(id, new Farm(id, existing.apiKey(), existing.name(), cleanNote, cleanSymbol, clock.getAsLong(), false));
        save();
        return true;
    }

    /** A shared note of one of the user's own animals; the farm link is kept. */
    synchronized boolean applySharedNote(String uuid, String note) {
        Animal existing = uuid == null ? null : animals.get(uuid);
        String cleanNote = clip(note, NOTE_MAX);
        if (existing != null && !existing.deleted() && existing.note().equals(cleanNote)) return false;
        if (existing == null && cleanNote.isEmpty()) return false;
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
        String farmId = existing == null || existing.deleted() ? "" : existing.farmId();
        animals.put(uuid, new Animal(uuid, farmId, cleanNote, clock.getAsLong(), false));
        prune();
        save();
        return true;
    }

    public synchronized List<FarmShareList> shareLists() {
        return List.copyOf(lists);
    }

    public synchronized FarmShareList shareList(String localId) {
        for (FarmShareList list : lists) if (list.localId.equals(localId)) return list;
        return null;
    }

    synchronized FarmShareList shareListById(String listId) {
        for (FarmShareList list : lists) if (list.listId.equals(listId)) return list;
        return null;
    }

    /** Owned list that contains one of the user's farms (by local farm id), or null. */
    public synchronized FarmShareList listOfFarm(String farmId) {
        for (FarmShareList list : lists) if (list.owned && list.members.contains(farmId)) return list;
        return null;
    }

    synchronized boolean addShareList(FarmShareList list) {
        if (list == null || lists.size() >= MAX_SHARE_LISTS || !list.valid()) return false;
        if (list.localId.isEmpty()) list.localId = newId().replace("hof-", "liste-");
        lists.add(list);
        save();
        return true;
    }

    synchronized void removeShareList(FarmShareList list) {
        if (lists.remove(list)) save();
    }

    /** Persists changes made to the list objects. */
    synchronized void saveShareLists() {
        save();
    }

    // ---- validation ----------------------------------------------------------------------

    /** Trims, removes control characters and shortens to {@code max} code points. */
    static String clip(String value, int max) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        value.trim().codePoints().filter(c -> c >= 32 && c != 127).limit(max).forEach(out::appendCodePoint);
        return out.toString().trim();
    }

    static boolean valid(Farm farm) {
        return farm != null && farm.id() != null && ID.matcher(farm.id()).matches() && farm.apiKey() != null
                && farm.apiKey().length() <= 128 && farm.name() != null && !farm.name().isBlank()
                && farm.name().codePointCount(0, farm.name().length()) <= FARM_NAME_MAX && farm.note() != null
                && farm.note().codePointCount(0, farm.note().length()) <= NOTE_MAX
                && FarmSymbols.valid(farm.symbol()) && farm.updatedAt() > 0;
    }

    static boolean valid(Animal animal) {
        if (animal == null || animal.uuid() == null || animal.farmId() == null || animal.note() == null
                || animal.updatedAt() <= 0
                || animal.note().codePointCount(0, animal.note().length()) > NOTE_MAX) {
            return false;
        }
        if (!animal.farmId().isEmpty() && !ID.matcher(animal.farmId()).matches()) return false;
        try {
            return UUID.fromString(animal.uuid()).toString().equals(animal.uuid().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static String newId() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder id = new StringBuilder("hof-");
        for (int i = 0; i < 12; i++) id.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        return id.toString();
    }

    /** Drops old tombstones and, beyond the limits, the oldest records. */
    private void prune() {
        long now = clock.getAsLong();
        farms.values().removeIf(farm -> farm.deleted() && farm.updatedAt() < now - TOMBSTONE_MS);
        animals.values().removeIf(animal -> animal.deleted() && animal.updatedAt() < now - TOMBSTONE_MS);
        trim(farms, MAX_FARMS, Farm::updatedAt);
        trim(animals, MAX_ANIMALS, Animal::updatedAt);
    }

    private static <T> void trim(Map<String, T> map, int max, java.util.function.ToLongFunction<T> time) {
        if (map.size() <= max) return;
        List<Map.Entry<String, T>> oldest = new ArrayList<>(map.entrySet());
        oldest.sort(Comparator.comparingLong(entry -> time.applyAsLong(entry.getValue())));
        for (int i = 0; i < map.size() - max && i < oldest.size(); i++) map.remove(oldest.get(i).getKey());
    }

    // ---- persistence ---------------------------------------------------------------------

    synchronized void load() {
        if (file == null) return;
        try {
            if (!Files.exists(file)) return;
            if (Files.size(file) > MAX_FILE_BYTES) throw new IllegalArgumentException("file too large");
            Stored data = JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Stored.class);
            if (data == null || data.version() < 1 || data.version() > FILE_VERSION || !server.equals(data.server())) {
                throw new IllegalArgumentException("invalid envelope");
            }
            long now = clock.getAsLong();
            // Times from the future (clock changed) are clamped, not dropped.
            if (data.farms() != null) {
                for (Farm farm : data.farms()) {
                    if (!valid(farm)) continue;
                    farms.put(farm.id(), farm.updatedAt() <= now ? farm : new Farm(farm.id(), farm.apiKey(),
                            farm.name(), farm.note(), farm.symbol(), now, farm.deleted()));
                }
            }
            if (data.animals() != null) {
                for (Animal animal : data.animals()) {
                    if (valid(animal)) {
                        Animal normalized = new Animal(animal.uuid().toLowerCase(Locale.ROOT), animal.farmId(),
                                animal.note(), Math.min(animal.updatedAt(), now), animal.deleted());
                        animals.put(normalized.uuid(), normalized);
                    }
                }
            }
            if (data.lists() != null) {
                for (FarmShareList list : data.lists()) {
                    if (list == null) continue;
                    list.sanitize();
                    list.clampFuture(now);
                    if (list.valid() && lists.size() < MAX_SHARE_LISTS && shareListById(list.listId) == null) {
                        lists.add(list);
                    }
                }
            }
            prune();
        } catch (Exception error) {
            OttoExtra.LOGGER.warn("Lokales Tierbuch konnte nicht geladen werden: {}", error.toString());
            farms.clear();
            animals.clear();
            lists.clear();
            moveAside();
        }
    }

    /** Renames an unreadable file to {@code <name>.broken-<millis>.json}; blocks writing when that fails. */
    private void moveAside() {
        String name = file.getFileName().toString();
        String stem = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        String prefix = stem + ".broken-";
        try {
            Path target = file.resolveSibling(prefix + clock.getAsLong() + ".json");
            for (int attempt = 1; Files.exists(target); attempt++) {
                target = file.resolveSibling(prefix + clock.getAsLong() + "-" + attempt + ".json");
            }
            Files.move(file, target);
            OttoExtra.LOGGER.warn("Unlesbares Tierbuch gesichert als {}.", target.getFileName());
            List<Path> backups = new ArrayList<>();
            try (var siblings = Files.list(file.getParent())) {
                siblings.filter(path -> {
                    String each = path.getFileName().toString();
                    return each.startsWith(prefix) && each.endsWith(".json");
                }).forEach(backups::add);
            }
            backups.sort(Comparator.comparingLong(AnimalBookStore::modified).thenComparing(Path::toString));
            for (int i = 0; i < backups.size() - MAX_BROKEN_BACKUPS; i++) Files.deleteIfExists(backups.get(i));
        } catch (Exception error) {
            blocked = !Files.notExists(file);
            OttoExtra.LOGGER.warn("Unlesbares Tierbuch konnte nicht gesichert werden ({}); es wird nicht "
                    + "überschrieben.", error.toString());
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception error) {
            return 0L;
        }
    }

    /**
     * Marks the data for writing. The store of a connection writes on the background thread (see
     * {@link #tick()}); other stores write at once on the calling thread.
     */
    synchronized void save() {
        if (file == null || blocked) return;
        if (background) {
            dirty = true;
            return;
        }
        write(file, serialize());
    }

    /** Writes pending changes and waits for the writer (disconnect, shutdown). */
    void flushNow() {
        String json = null;
        synchronized (this) {
            if (dirty && file != null && !blocked) {
                dirty = false;
                json = serialize();
            }
        }
        if (json != null) pendingJson.set(json);
        if (!background) return;
        try {
            WRITER.submit(this::writePending).get(5, TimeUnit.SECONDS);
        } catch (Exception error) {
            OttoExtra.LOGGER.warn("Tierbuch: Schreiben beim Trennen nicht abgeschlossen: {}", error.toString());
        }
    }

    private void queue(String json) {
        if (pendingJson.getAndSet(json) == null) WRITER.execute(this::writePending);
    }

    private void writePending() {
        String json = pendingJson.getAndSet(null);
        if (json != null) write(file, json);
    }

    private String serialize() {
        return JSON.toJson(new Stored(FILE_VERSION, server, List.copyOf(farms.values()),
                List.copyOf(animals.values()), List.copyOf(lists)));
    }

    /** Writes the file atomically: temporary file, then move. */
    private static void write(Path file, String json) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                Files.writeString(temporary, json, StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception error) {
            OttoExtra.LOGGER.warn("Lokales Tierbuch konnte nicht gespeichert werden: {}", error.toString());
        }
    }
}
