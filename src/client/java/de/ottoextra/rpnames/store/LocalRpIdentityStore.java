package de.ottoextra.rpnames.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Lokales RP-Bekanntschaftssystem: known-players.json laden/speichern,
 * first-seen anlegen, Identitäten lernen, manuell bearbeiten
 *.
 *
 * <p>Merge-Prioritäten: {@code MANUAL_LOCKED/MANUAL > gelernt (Hover/Chat) >
 * API-Import > gesehen}. Leere eingehende Werte ändern nie etwas; Hover darf
 * API-Werte ersetzen, API füllt nur "Unbekannt". Speichern debounced (2 s),
 * defekte Dateien werden beiseitegelegt statt überschrieben.</p>
 */
public final class LocalRpIdentityStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long SAVE_DEBOUNCE_MS = 2000;

    /** Persistenz-Form. */
    private static final class FileModel {
        int schemaVersion = 1;
        long updatedAt = 0;
        List<LocalRpProfile> players = new ArrayList<>();
    }

    private final Map<String, LocalRpProfile> byUuid = new ConcurrentHashMap<>();
    private final Map<String, LocalRpProfile> byNameLower = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> pendingSave;
    private volatile boolean dirty = false;

    public LocalRpIdentityStore() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ottoextra-rpnames-store");
            t.setDaemon(true);
            return t;
        });
    }

    // ---- Laden / Speichern --------------------------------------------------

    public synchronized void load() {
        Path file = OttoExtraPaths.rpnamesKnownPlayers();
        if (!Files.exists(file)) {
            return;
        }
        try {
            FileModel model = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8), FileModel.class);
            if (model == null || model.players == null) {
                throw new IllegalStateException("leere Datei");
            }
            byUuid.clear();
            byNameLower.clear();
            for (LocalRpProfile p : model.players) {
                if (p == null) {
                    continue;
                }
                p.repair();
                index(p);
            }
            OttoExtra.LOGGER.info("[rpnames] {} bekannte Personen geladen.", byNameLower.size());
        } catch (Exception e) {
            // Defekt: NICHT überschreiben — beiseitelegen, mit leerem Store starten
            try {
                Path broken = file.resolveSibling("known-players.broken-" + stamp() + ".json");
                Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
                OttoExtra.LOGGER.warn("[rpnames] known-players.json defekt ({}) — nach {} verschoben.",
                        e.toString(), broken.getFileName());
            } catch (Exception moveFail) {
                OttoExtra.LOGGER.warn("[rpnames] known-players.json defekt und nicht verschiebbar: {}",
                        moveFail.toString());
            }
        }
    }

    /** Debounced speichern (2 s) — für automatisches Lernen. */
    public void saveSoon() {
        dirty = true;
        ScheduledFuture<?> pending = pendingSave;
        if (pending != null) {
            pending.cancel(false);
        }
        pendingSave = scheduler.schedule(this::saveNow, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /** Sofort speichern — für manuelle Änderungen und Shutdown. */
    public synchronized void saveNow() {
        if (!dirty && Files.exists(OttoExtraPaths.rpnamesKnownPlayers())) {
            return;
        }
        try {
            FileModel model = new FileModel();
            model.updatedAt = System.currentTimeMillis();
            model.players = new ArrayList<>(byNameLower.values());
            model.players.sort(Comparator.comparing(
                    p -> p.accountName == null ? "" : p.accountName.toLowerCase(Locale.ROOT)));
            Path file = OttoExtraPaths.rpnamesKnownPlayers();
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("known-players.json.tmp");
            Files.writeString(tmp, GSON.toJson(model), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] Speichern fehlgeschlagen: {}", e.toString());
        }
    }

    /** Backup nach backups/known-players-<stamp>.json (vor Migration/Import). */
    public synchronized Optional<Path> backup() {
        try {
            Path src = OttoExtraPaths.rpnamesKnownPlayers();
            if (!Files.exists(src)) {
                return Optional.empty();
            }
            Path dir = OttoExtraPaths.rpnamesBackups();
            Files.createDirectories(dir);
            Path target = dir.resolve("known-players-" + stamp() + ".json");
            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(target);
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[rpnames] Backup fehlgeschlagen: {}", e.toString());
            return Optional.empty();
        }
    }

    public void shutdown() {
        ScheduledFuture<?> pending = pendingSave;
        if (pending != null) {
            pending.cancel(false);
        }
        saveNow(); // synchron, aber ohne Netzwerk — kein join()-Freeze wie im Bestand
        scheduler.shutdownNow();
    }

    private static String stamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
    }

    // ---- Lookup ---------------------------------------------------------------

    public Optional<LocalRpProfile> find(String uuid, String accountName) {
        if (uuid != null && !uuid.isBlank()) {
            LocalRpProfile p = byUuid.get(uuid.toLowerCase(Locale.ROOT));
            if (p != null) {
                return Optional.of(p);
            }
        }
        return findByName(accountName);
    }

    public Optional<LocalRpProfile> findByName(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byNameLower.get(accountName.toLowerCase(Locale.ROOT)));
    }

    public List<LocalRpProfile> all() {
        return new ArrayList<>(byNameLower.values());
    }

    public int size() {
        return byNameLower.size();
    }

    // ---- First seen -----------------------------------------------------------

    /**
     * Spieler gesehen: Eintrag anlegen ("Unbekannt") oder UUID/Name upgraden
     * und {@code lastSeenAt} pflegen. Nie Namen/Titel verändern.
     */
    public synchronized LocalRpProfile ensureSeen(String accountName, String uuid, RpNameSource source) {
        long now = System.currentTimeMillis();
        LocalRpProfile profile = find(uuid, accountName).orElse(null);
        if (profile == null) {
            profile = new LocalRpProfile();
            profile.accountName = accountName;
            profile.uuid = uuid;
            profile.source = source;
            profile.firstSeenAt = now;
            profile.knowledgeState = KnowledgeState.SEEN;
            index(profile);
            saveSoon();
        } else {
            boolean changed = false;
            // UUID-Upgrade (Eintrag war nur per Name bekannt)
            if ((profile.uuid == null || profile.uuid.isBlank()) && uuid != null && !uuid.isBlank()) {
                profile.uuid = uuid;
                mergeDuplicateByUuid(profile);
                changed = true;
            }
            // Namenswechsel (gleiche UUID, neuer Accountname)
            if (accountName != null && !accountName.isBlank()
                    && profile.accountName != null
                    && !accountName.equalsIgnoreCase(profile.accountName)) {
                byNameLower.remove(profile.accountName.toLowerCase(Locale.ROOT));
                profile.accountName = accountName;
                changed = true;
            }
            if (changed) {
                index(profile);
                saveSoon();
            }
        }
        profile.lastSeenAt = now;
        return profile;
    }

    /** Beim UUID-Upgrade evtl. vorhandenes UUID-Duplikat in dieses Profil mergen. */
    private void mergeDuplicateByUuid(LocalRpProfile keep) {
        LocalRpProfile dupe = byUuid.get(keep.uuid.toLowerCase(Locale.ROOT));
        if (dupe == null || dupe == keep) {
            return;
        }
        // lokal stärkerer Datensatz gewinnt feldweise
        if (!keep.hasRpName() && dupe.hasRpName()) {
            keep.rpName = dupe.rpName;
            keep.knowledgeState = dupe.knowledgeState;
            keep.source = dupe.source;
        }
        if (!keep.hasTitle() && dupe.hasTitle()) {
            keep.title = dupe.title;
            keep.titleGroup = dupe.titleGroup;
        }
        keep.locked = keep.locked || dupe.locked;
        keep.firstSeenAt = Math.min(nonZero(keep.firstSeenAt), nonZero(dupe.firstSeenAt));
        keep.lastSeenAt = Math.max(keep.lastSeenAt, dupe.lastSeenAt);
        if (dupe.accountName != null) {
            byNameLower.remove(dupe.accountName.toLowerCase(Locale.ROOT), dupe);
        }
    }

    private static long nonZero(long v) {
        return v == 0 ? Long.MAX_VALUE : v;
    }

    // ---- Lernen ----------------------------------------------------------------

    /**
     * RP-Name/Titel aus Chat/Hover gelernt. Regeln: blank ändert
     * nichts; MANUAL/MANUAL_LOCKED unantastbar; Hover darf API-Werte ersetzen;
     * Titel separat nachtragbar; gleicher Name erneut gehört -> KNOWN.
     */
    public synchronized boolean learnIdentity(String accountName, String rpName, String title,
                                              String titleGroup, RpNameSource source) {
        if (accountName == null || accountName.isBlank()) {
            return false;
        }
        LocalRpProfile profile = ensureSeen(accountName, null, RpNameSource.SEEN_ONLINE);
        if (!profile.knowledgeState.allowsAutomaticUpdates() || profile.locked) {
            profile.lastSeenAt = System.currentTimeMillis();
            return false;
        }
        long now = System.currentTimeMillis();
        boolean changed = false;

        if (rpName != null && !rpName.isBlank() && !rpName.equalsIgnoreCase(accountName)) {
            if (!profile.hasRpName() || profile.knowledgeState == KnowledgeState.API_IMPORTED) {
                if (profile.hasRpName() && !profile.rpName.equals(rpName)) {
                    profile.apiConflict = profile.rpName; // API-Wert als Konflikt merken
                }
                profile.rpName = rpName;
                profile.knowledgeState = KnowledgeState.HEARD_NAME;
                profile.source = source;
                profile.firstHeardAt = profile.firstHeardAt == 0 ? now : profile.firstHeardAt;
                changed = true;
            } else if (profile.rpName.equals(rpName)
                    && profile.knowledgeState == KnowledgeState.HEARD_NAME) {
                profile.knowledgeState = KnowledgeState.KNOWN; // zweite Bestätigung
                changed = true;
            }
        }
        if (title != null && !title.isBlank() && !profile.hasTitle()) {
            profile.title = title;
            if (titleGroup != null && !titleGroup.isBlank()) {
                profile.titleGroup = titleGroup;
            }
            changed = true;
        }
        if (changed) {
            profile.lastUpdatedAt = now;
            profile.lastSeenAt = now;
            saveSoon();
        }
        return changed;
    }

    /**
     * Titel eines vorhandenen Profils aktualisieren, wenn er sich geändert hat
     * (Server-Quelle: Tabliste/Chat). Anders als {@link #learnIdentity} greift
     * dies auch bei MANUAL-Profilen — nur {@code locked} (RP-Buch gesperrt)
     * schützt. Leerer Titel wird ignoriert (kein versehentliches Löschen).
     *
     * @return true, wenn der Titel geändert wurde
     */
    public synchronized boolean updateTitleIfChanged(String account, String uuid, String title) {
        if (account == null || account.isBlank() || title == null) {
            return false;
        }
        String t = title.trim();
        if (t.isEmpty()) {
            return false;
        }
        LocalRpProfile profile = find(uuid, account).orElse(null);
        if (profile == null || profile.locked) {
            return false;
        }
        if (t.equals(profile.title == null ? "" : profile.title)) {
            return false;
        }
        profile.title = t;
        profile.lastUpdatedAt = System.currentTimeMillis();
        saveSoon();
        return true;
    }

    // ---- Manuell ----------------------------------------------------------------

    /** Manuelle Bearbeitung; {@code lock} schützt zusätzlich gegen Automatik. */
    public synchronized LocalRpProfile updateManual(String accountName,
                                                    java.util.function.Consumer<LocalRpProfile> edit,
                                                    boolean lock) {
        LocalRpProfile profile = ensureSeen(accountName, null, RpNameSource.SEEN_ONLINE);
        edit.accept(profile);
        profile.repair();
        profile.knowledgeState = lock ? KnowledgeState.MANUAL_LOCKED : KnowledgeState.MANUAL;
        profile.locked = lock || profile.locked;
        profile.source = RpNameSource.MANUAL_EDIT;
        profile.lastUpdatedAt = System.currentTimeMillis();
        index(profile);
        saveNow();
        return profile;
    }

    /** API-Import eines Profils: füllt nur "Unbekannt" und leere Felder. */
    public synchronized boolean importApi(String accountName, String uuid, String rpName, String title) {
        LocalRpProfile profile = find(uuid, accountName).orElse(null);
        if (profile == null) {
            return false; // Anlage übernimmt der Importer je nach Modus explizit
        }
        if (profile.locked || !profile.knowledgeState.allowsAutomaticUpdates()) {
            return false;
        }
        boolean changed = false;
        if ((profile.uuid == null || profile.uuid.isBlank()) && uuid != null && !uuid.isBlank()) {
            profile.uuid = uuid;
            index(profile);
            changed = true;
        }
        if (rpName != null && !rpName.isBlank() && !profile.hasRpName()) {
            profile.rpName = rpName;
            profile.knowledgeState = KnowledgeState.API_IMPORTED;
            profile.source = RpNameSource.API_IMPORTED;
            changed = true;
        } else if (rpName != null && !rpName.isBlank() && profile.hasRpName()
                && !profile.rpName.equals(rpName)) {
            profile.apiConflict = rpName; // nur vermerken, nie übernehmen
            changed = true;
        }
        if (title != null && !title.isBlank() && !profile.hasTitle()) {
            profile.title = title;
            changed = true;
        }
        if (changed) {
            profile.lastUpdatedAt = System.currentTimeMillis();
            saveSoon();
        }
        return changed;
    }

    /**
     * OttoPlus-Import: überschreibt rpName/title/Titelfarbe eines vorhandenen
     * Profils autoritativ. Gesperrte Profile ({@code locked}) bleiben unberührt.
     * Leere/"Unbekannt"-Werte löschen nie vorhandene Daten (additiv). UUID wird
     * nur nachgetragen, nie ersetzt. Speichern debounced; Aufrufer ruft am Ende
     * {@link #saveNow()}.
     *
     * @return true, wenn ein Feld geändert wurde
     */
    public synchronized boolean importOttoPlus(String account, String uuid, String rpName,
                                               String title, String titleColorHex) {
        LocalRpProfile profile = find(uuid, account).orElse(null);
        if (profile == null || profile.locked) {
            return false;
        }
        boolean changed = false;
        if (uuid != null && !uuid.isBlank() && (profile.uuid == null || profile.uuid.isBlank())) {
            profile.uuid = uuid;
            index(profile);
            changed = true;
        }
        if (rpName != null && !rpName.isBlank() && !rpName.equals(profile.rpName)) {
            profile.rpName = rpName;
            changed = true;
        }
        if (title != null && !title.isBlank() && !title.equals(profile.title)) {
            profile.title = title;
            changed = true;
        }
        if (titleColorHex != null && !titleColorHex.isBlank()) {
            if (!titleColorHex.equals(profile.colors.chatTitleColor)) {
                changed = true;
            }
            profile.colors.chatTitleColor = titleColorHex;
            profile.colors.tabTitleColor = titleColorHex;
            profile.colors.nametagTitleColor = titleColorHex;
        }
        if (changed) {
            profile.knowledgeState = KnowledgeState.KNOWN;
            profile.source = RpNameSource.IMPORTED_FROM_OTTOPLUS;
            profile.lastUpdatedAt = System.currentTimeMillis();
            saveSoon();
        }
        return changed;
    }

    /** Neues Profil direkt einfügen (Importer/Migration). Respektiert vorhandene Einträge nicht — Aufrufer prüft. */
    public synchronized void insert(LocalRpProfile profile) {
        profile.repair();
        index(profile);
        saveSoon();
    }

    public synchronized void remove(LocalRpProfile profile) {
        if (profile.accountName != null) {
            byNameLower.remove(profile.accountName.toLowerCase(Locale.ROOT), profile);
        }
        if (profile.uuid != null) {
            byUuid.remove(profile.uuid.toLowerCase(Locale.ROOT), profile);
        }
        saveSoon();
    }

    private void index(LocalRpProfile profile) {
        if (profile.accountName != null && !profile.accountName.isBlank()) {
            byNameLower.put(profile.accountName.toLowerCase(Locale.ROOT), profile);
        }
        if (profile.uuid != null && !profile.uuid.isBlank()) {
            byUuid.put(profile.uuid.toLowerCase(Locale.ROOT), profile);
        }
    }
}
