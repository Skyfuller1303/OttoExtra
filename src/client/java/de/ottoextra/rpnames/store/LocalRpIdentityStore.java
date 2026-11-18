package de.ottoextra.rpnames.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.logging.FailureLogGate;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;

import java.io.IOException;
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
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class LocalRpIdentityStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long SAVE_DEBOUNCE_MS = 2000;

    public enum RepairStatus {
        HEALTHY,
        RESTORED,
        NO_DATA,
        NO_VALID_BACKUP,
        FAILED
    }

    public record RepairResult(RepairStatus status, Path source, Path preservedFile,
                               int playerCount) {
    }

    private static final class FileModel {
        int schemaVersion = 1;
        long updatedAt = 0;
        List<LocalRpProfile> players = new ArrayList<>();
    }

    private final Map<String, LocalRpProfile> byUuid = new ConcurrentHashMap<>();
    private final Map<String, LocalRpProfile> byNameLower = new ConcurrentHashMap<>();
    private final FailureLogGate saveFailureLog = new FailureLogGate();
    private final ScheduledExecutorService scheduler;
    private final boolean persistenceEnabled;
    private final Path dataFile;
    private final Path recoveryBackupsDir;
    private volatile ScheduledFuture<?> pendingSave;
    private volatile boolean dirty = false;
    private volatile boolean persistenceBlocked = false;
    private volatile RepairResult lastRepairResult =
            new RepairResult(RepairStatus.NO_DATA, null, null, 0);

    public LocalRpIdentityStore() {
        this(true, OttoExtraPaths.rpnamesKnownPlayers(), OttoExtraPaths.rpnamesBackups());
    }

    LocalRpIdentityStore(boolean persistenceEnabled) {
        this(persistenceEnabled, null, null);
    }

    LocalRpIdentityStore(Path dataFile, Path recoveryBackupsDir) {
        this(true, dataFile, recoveryBackupsDir);
    }

    private LocalRpIdentityStore(boolean persistenceEnabled, Path dataFile,
                                 Path recoveryBackupsDir) {
        this.persistenceEnabled = persistenceEnabled;
        this.dataFile = dataFile;
        this.recoveryBackupsDir = recoveryBackupsDir;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ottoextra-rpnames-store");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void load() {
        Path file = dataFile;
        if (file == null) {
            return;
        }
        if (!Files.exists(file)) {
            lastRepairResult = new RepairResult(RepairStatus.NO_DATA, null, null, 0);
            return;
        }
        try {
            boolean migratedApiState = applyModel(readModel(file));
            persistenceBlocked = false;
            lastRepairResult = new RepairResult(
                    RepairStatus.HEALTHY, file, null, byNameLower.size());
            if (migratedApiState) {
                dirty = true;
                saveSoon();
            }
            DebugLog.debug("[rpnames] {} bekannte Personen geladen.", byNameLower.size());
        } catch (Exception e) {
            lastRepairResult = recoverCorruptFile(e);
        }
    }

    public synchronized RepairResult repairData() {
        if (!persistenceEnabled || dataFile == null) {
            return new RepairResult(RepairStatus.FAILED, null, null, byNameLower.size());
        }
        if (Files.exists(dataFile)) {
            try {
                FileModel model = readModel(dataFile);
                // A healthy disk file must not replace changes waiting for the debounce.
                // A previously blocked store, however, must reload the repaired file.
                if (!dirty || persistenceBlocked) {
                    dirty = applyModel(model);
                }
                persistenceBlocked = false;
                if (dirty) {
                    saveSoon();
                }
                RepairResult previousResult = lastRepairResult;
                if (previousResult.status() == RepairStatus.RESTORED) {
                    return previousResult;
                }
                lastRepairResult = new RepairResult(
                        RepairStatus.HEALTHY, dataFile, null, byNameLower.size());
                return lastRepairResult;
            } catch (Exception e) {
                lastRepairResult = recoverCorruptFile(e);
                return lastRepairResult;
            }
        }
        if (!persistenceBlocked && lastRepairResult.status() == RepairStatus.NO_DATA
                && recoveryCandidates().isEmpty()) {
            lastRepairResult = new RepairResult(RepairStatus.NO_DATA, null, null, size());
            return lastRepairResult;
        }
        lastRepairResult = restoreRecoveryCandidate(null);
        return lastRepairResult;
    }

    public synchronized boolean restoreBackup(BooleanSupplier restoreFiles) {
        saveNow();
        if (dirty && persistenceEnabled && !persistenceBlocked) {
            return false;
        }
        ScheduledFuture<?> pending = pendingSave;
        if (pending != null) {
            pending.cancel(false);
            pendingSave = null;
        }
        boolean restored;
        try {
            restored = restoreFiles.getAsBoolean();
        } finally {
            // Auch nach einem teilweise fehlgeschlagenen Restore nie alte Daten nachschreiben.
            dirty = false;
            load();
        }
        return restored && !persistenceBlocked;
    }

    public RepairResult lastRepairResult() {
        return lastRepairResult;
    }

    private RepairResult recoverCorruptFile(Exception cause) {
        Path broken = null;
        try {
            broken = dataFile.resolveSibling("known-players.broken-" + stamp() + ".json");
            Files.move(dataFile, broken, StandardCopyOption.REPLACE_EXISTING);
            OttoExtra.LOGGER.warn(
                    "[rpnames] known-players.json defekt ({}) — Original als {} erhalten.",
                    cause.toString(), broken.getFileName());
        } catch (Exception moveFail) {
            persistenceBlocked = true;
            dirty = false;
            OttoExtra.LOGGER.warn(
                    "[rpnames] Defekte known-players.json konnte nicht gesichert werden; "
                            + "Speichern bleibt zum Schutz der Daten gesperrt: {}",
                    moveFail.toString());
            return new RepairResult(
                    RepairStatus.FAILED, null, broken, byNameLower.size());
        }
        return restoreRecoveryCandidate(broken);
    }

    private RepairResult restoreRecoveryCandidate(Path broken) {
        for (Path candidate : recoveryCandidates()) {
            try {
                FileModel model = readModel(candidate);
                restoreFile(candidate);
                applyModel(model);
                persistenceBlocked = false;
                dirty = false;
                OttoExtra.LOGGER.warn(
                        "[rpnames] Personendaten automatisch aus {} wiederhergestellt ({} Personen).",
                        candidate, byNameLower.size());
                return new RepairResult(
                        RepairStatus.RESTORED, candidate, broken, byNameLower.size());
            } catch (Exception invalidCandidate) {
                DebugLog.debug("[rpnames] Reparaturkandidat {} unbrauchbar: {}",
                        candidate, invalidCandidate.toString());
            }
        }
        persistenceBlocked = true;
        dirty = false;
        OttoExtra.LOGGER.error(
                "[rpnames] Keine gültige Sicherung gefunden; automatisches Speichern bleibt gesperrt.");
        return new RepairResult(
                RepairStatus.NO_VALID_BACKUP, null, broken, byNameLower.size());
    }

    private List<Path> recoveryCandidates() {
        List<Path> candidates = new ArrayList<>();
        Path tmp = dataFile.resolveSibling("known-players.json.tmp");
        Path previous = dataFile.resolveSibling("known-players.previous.json");
        if (Files.exists(tmp)) {
            candidates.add(tmp);
        }
        if (Files.exists(previous)) {
            candidates.add(previous);
        }
        if (recoveryBackupsDir != null && Files.isDirectory(recoveryBackupsDir)) {
            try (Stream<Path> files = Files.walk(recoveryBackupsDir)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().equals("known-players.json"))
                        .sorted(Comparator.comparing(LocalRpIdentityStore::backupSortKey).reversed())
                        .forEach(candidates::add);
            } catch (Exception e) {
                DebugLog.debug("[rpnames] Backup-Suche fehlgeschlagen: {}", e.toString());
            }
        }
        return candidates;
    }

    private static String backupSortKey(Path file) {
        Path parent = file.getParent();
        return parent == null ? "" : parent.getFileName().toString();
    }

    private FileModel readModel(Path file) throws IOException {
        FileModel model = GSON.fromJson(
                Files.readString(file, StandardCharsets.UTF_8), FileModel.class);
        if (model == null || model.players == null) {
            throw new IOException("leere oder unvollständige Personendatei");
        }
        return model;
    }

    private boolean applyModel(FileModel model) {
        Map<String, LocalRpProfile> loadedByUuid = new ConcurrentHashMap<>();
        Map<String, LocalRpProfile> loadedByName = new ConcurrentHashMap<>();
        boolean migratedApiState = false;
        for (LocalRpProfile p : model.players) {
            if (p == null) {
                continue;
            }
            boolean hadApiState = p.apiState != null && !p.apiState.isBlank();
            p.repair();
            migratedApiState |= !hadApiState && !p.apiState.isBlank();
            if (p.accountName != null && !p.accountName.isBlank()) {
                loadedByName.merge(p.accountName.toLowerCase(Locale.ROOT), p,
                        (old, next) -> next.lastSeenAt >= old.lastSeenAt ? next : old);
            }
            if (p.uuid != null && !p.uuid.isBlank()) {
                loadedByUuid.put(p.uuid.toLowerCase(Locale.ROOT), p);
            }
        }
        byUuid.clear();
        byUuid.putAll(loadedByUuid);
        byNameLower.clear();
        byNameLower.putAll(loadedByName);
        return migratedApiState;
    }

    private void restoreFile(Path candidate) throws IOException {
        Files.createDirectories(dataFile.getParent());
        Path recoveryTmp = dataFile.resolveSibling("known-players.recovery.tmp");
        Files.copy(candidate, recoveryTmp, StandardCopyOption.REPLACE_EXISTING);
        readModel(recoveryTmp);
        replaceFile(recoveryTmp, dataFile);
    }

    public synchronized void saveSoon() {
        if (persistenceBlocked) {
            return;
        }
        dirty = true;
        if (!persistenceEnabled) {
            return;
        }
        ScheduledFuture<?> pending = pendingSave;
        if (pending != null) {
            pending.cancel(false);
        }
        pendingSave = scheduler.schedule(this::saveNow, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void saveNow() {
        if (!persistenceEnabled || persistenceBlocked || dataFile == null) {
            return;
        }
        if (!dirty && Files.exists(dataFile)) {
            return;
        }
        try {
            FileModel model = new FileModel();
            model.updatedAt = System.currentTimeMillis();
            model.players = all();
            model.players.sort(Comparator.comparing(
                    p -> p.accountName == null ? "" : p.accountName.toLowerCase(Locale.ROOT)));
            Path file = dataFile;
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("known-players.json.tmp");
            Files.writeString(tmp, GSON.toJson(model), StandardCharsets.UTF_8);
            readModel(tmp);
            preservePreviousVersion(file);
            replaceFile(tmp, file);
            dirty = false;
            if (saveFailureLog.onSuccess()) {
                DebugLog.debug("[rpnames] Speichern nach Fehler wieder möglich.");
            }
        } catch (Exception e) {
            if (saveFailureLog.onFailure()) {
                OttoExtra.LOGGER.warn("[rpnames] Speichern fehlgeschlagen: {}", e.toString());
            }
        }
    }

    private void preservePreviousVersion(Path file) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        readModel(file);
        Path previous = file.resolveSibling("known-players.previous.json");
        Path previousTmp = file.resolveSibling("known-players.previous.json.tmp");
        Files.copy(file, previousTmp, StandardCopyOption.REPLACE_EXISTING);
        readModel(previousTmp);
        replaceFile(previousTmp, previous);
    }

    private static void replaceFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicUnsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized Optional<Path> backup() {
        try {
            saveNow();
            Path[] sources = {
                    OttoExtraPaths.rpnamesKnownPlayers(),
                    OttoExtraPaths.rpnamesDir().resolve("title-catalog.json"),
                    OttoExtraPaths.rpnamesTitleGroups()
            };
            boolean any = false;
            for (Path source : sources) {
                any |= Files.exists(source);
            }
            if (!any) {
                return Optional.empty();
            }
            Path targetDir = OttoExtraPaths.rpnamesBackups().resolve(stamp());
            Files.createDirectories(targetDir);
            for (Path source : sources) {
                if (Files.exists(source)) {
                    Files.copy(source, targetDir.resolve(source.getFileName()),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return Optional.of(targetDir);
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
        saveNow();
        scheduler.shutdownNow();
    }

    private static String stamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS"));
    }

    public Optional<LocalRpProfile> find(String uuid, String accountName) {
        if (uuid != null && !uuid.isBlank()) {
            LocalRpProfile p = byUuid.get(uuid.toLowerCase(Locale.ROOT));
            if (p != null) {
                return Optional.of(p);
            }
        }
        return findByName(accountName).filter(p -> uuid == null || uuid.isBlank()
                || p.uuid == null || p.uuid.isBlank() || uuid.equalsIgnoreCase(p.uuid));
    }

    public Optional<LocalRpProfile> findByName(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byNameLower.get(accountName.toLowerCase(Locale.ROOT)));
    }

    public synchronized List<LocalRpProfile> all() {
        // Reused names must not remove the previous owner's UUID profile.
        java.util.Set<LocalRpProfile> profiles = new java.util.LinkedHashSet<>(byUuid.values());
        profiles.addAll(byNameLower.values());
        return new ArrayList<>(profiles);
    }

    /** One current name owner per Minecraft account, for account-addressed actions. */
    public synchronized List<LocalRpProfile> currentAccounts() {
        return new ArrayList<>(byNameLower.values());
    }

    public int size() {
        return all().size();
    }

    public synchronized LocalRpProfile ensureSeen(String accountName, String uuid, RpNameSource source) {
        LocalRpProfile previousNameOwner = findByName(accountName).orElse(null);
        // Keep name ownership deterministic across same-millisecond observations
        // and clock corrections when both UUID profiles are later loaded.
        long now = Math.max(System.currentTimeMillis(),
                previousNameOwner == null ? 0 : previousNameOwner.lastSeenAt + 1);
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

            if ((profile.uuid == null || profile.uuid.isBlank()) && uuid != null && !uuid.isBlank()) {
                profile.uuid = uuid;
                mergeDuplicateByUuid(profile);
                changed = true;
            }

            if (accountName != null && !accountName.isBlank()
                    && profile.accountName != null
                    && !accountName.equalsIgnoreCase(profile.accountName)) {
                byNameLower.remove(profile.accountName.toLowerCase(Locale.ROOT), profile);
                profile.accountName = accountName;
                changed = true;
            }
            if (changed) {
                index(profile);
                saveSoon();
            }
        }
        profile.lastSeenAt = now;
        // Re-observing an existing UUID must agree with the owner selected on reload.
        index(profile);
        if (previousNameOwner != profile) {
            saveSoon();
        }
        return profile;
    }

    private void mergeDuplicateByUuid(LocalRpProfile keep) {
        LocalRpProfile dupe = byUuid.get(keep.uuid.toLowerCase(Locale.ROOT));
        if (dupe == null || dupe == keep) {
            return;
        }

        if (!keep.hasRpName() && dupe.hasRpName()) {
            keep.rpName = dupe.rpName;
            keep.knowledgeState = dupe.knowledgeState;
            keep.source = dupe.source;
        }
        if (!keep.hasTitle() && dupe.hasTitle()) {
            keep.title = dupe.title;
            keep.titleGroup = dupe.titleGroup;
        }
        if ((keep.apiState == null || keep.apiState.isBlank())
                && dupe.apiState != null && !dupe.apiState.isBlank()) {
            keep.apiState = dupe.apiState;
        }
        keep.locked = keep.locked || dupe.locked;
        if (keep.relation == null || keep.relation == de.ottoextra.rpnames.model.Relation.NONE) {
            keep.relation = dupe.relation;
        }
        keep.firstSeenAt = Math.min(nonZero(keep.firstSeenAt), nonZero(dupe.firstSeenAt));
        keep.lastSeenAt = Math.max(keep.lastSeenAt, dupe.lastSeenAt);
        if (dupe.accountName != null) {
            byNameLower.remove(dupe.accountName.toLowerCase(Locale.ROOT), dupe);
        }
    }

    private static long nonZero(long v) {
        return v == 0 ? Long.MAX_VALUE : v;
    }

    public synchronized boolean learnIdentity(String accountName, String rpName, String title,
                                              String titleGroup, RpNameSource source) {
        if (accountName == null || accountName.isBlank()) {
            return false;
        }
        LocalRpProfile profile = ensureSeen(accountName, null, RpNameSource.SEEN_ONLINE);
        if (profile.locked || profile.knowledgeState == KnowledgeState.MANUAL_LOCKED) {
            profile.lastSeenAt = System.currentTimeMillis();
            return false;
        }
        long now = System.currentTimeMillis();
        boolean changed = false;

        if (profile.knowledgeState.allowsAutomaticUpdates()
                && rpName != null && !rpName.isBlank() && !rpName.equalsIgnoreCase(accountName)) {
            if (!profile.hasRpName() || profile.knowledgeState == KnowledgeState.API_IMPORTED) {
                if (profile.hasRpName() && !profile.rpName.equals(rpName)) {
                    profile.apiConflict = profile.rpName;
                }
                profile.rpName = rpName;
                profile.knowledgeState = KnowledgeState.HEARD_NAME;
                profile.source = source;
                profile.firstHeardAt = profile.firstHeardAt == 0 ? now : profile.firstHeardAt;
                changed = true;
            } else if (profile.rpName.equals(rpName)
                    && profile.knowledgeState == KnowledgeState.HEARD_NAME) {
                profile.knowledgeState = KnowledgeState.KNOWN;
                changed = true;
            }
        }
        if (canUpdateTitle(profile) && title != null && !title.isBlank() && !title.equals(profile.title)) {
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

    public synchronized boolean updateTitleIfChanged(String account, String uuid, String title) {
        if (account == null || account.isBlank() || title == null) {
            return false;
        }
        String t = title.trim();
        if (t.isEmpty()) {
            return false;
        }
        LocalRpProfile profile = find(uuid, account).orElse(null);
        if (!canUpdateTitle(profile)) {
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

    /** Manual names stay local; only explicit profile/title locks freeze the title. */
    private static boolean canUpdateTitle(LocalRpProfile profile) {
        return profile != null && !profile.locked && !profile.titleLocked
                && profile.knowledgeState != KnowledgeState.MANUAL_LOCKED;
    }

    public synchronized LocalRpProfile updateManual(String accountName,
                                                    java.util.function.Consumer<LocalRpProfile> edit,
                                                    boolean lock) {
        return updateManual(accountName, null, edit, lock);
    }

    public synchronized LocalRpProfile updateManual(String accountName, String uuid,
                                                    java.util.function.Consumer<LocalRpProfile> edit,
                                                    boolean lock) {
        // Editing an existing record is not a new observation of the name owner.
        LocalRpProfile profile = find(uuid, accountName)
                .orElseGet(() -> ensureSeen(accountName, uuid, RpNameSource.SEEN_ONLINE));
        edit.accept(profile);
        profile.repair();
        profile.knowledgeState = lock ? KnowledgeState.MANUAL_LOCKED : KnowledgeState.MANUAL;
        profile.locked = lock;
        profile.source = RpNameSource.MANUAL_EDIT;
        profile.lastUpdatedAt = System.currentTimeMillis();
        dirty = true;
        saveNow();
        de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
        return profile;
    }

    /**
     * Applies identity data shown by the server's own player GUI. The server is authoritative
     * except for values the user set in the editor: those are only filled when empty, and locked
     * profiles or titles stay untouched. A profile with a known name counts as met afterwards.
     * Null or blank arguments are skipped.
     */
    public synchronized boolean importServerGui(String accountName, String uuid, String rpName,
                                                String title, String state) {
        if (accountName == null || accountName.isBlank()) {
            return false;
        }
        LocalRpProfile profile = ensureSeen(accountName, uuid, RpNameSource.SEEN_ONLINE);
        boolean userSet = profile.locked
                || profile.knowledgeState == KnowledgeState.MANUAL
                || profile.knowledgeState == KnowledgeState.MANUAL_LOCKED
                || profile.source == RpNameSource.MANUAL_EDIT;
        boolean changed = false;

        if (rpName != null && !rpName.isBlank()
                && (!userSet || (!profile.hasRpName() && !profile.locked))
                && !rpName.equals(profile.rpName)) {
            profile.rpName = rpName;
            profile.apiConflict = null;
            changed = true;
        }
        if (title != null && !title.isBlank() && canUpdateTitle(profile)
                && (!userSet || !profile.hasTitle()) && !title.equals(profile.title)) {
            profile.title = title;
            changed = true;
        }
        String newState = state == null ? "" : state.trim();
        boolean stateEmpty = profile.apiState == null || profile.apiState.isBlank();
        if (!newState.isEmpty() && (!userSet || stateEmpty) && !newState.equals(profile.apiState)) {
            profile.apiState = newState;
            changed = true;
        }
        if (!userSet && profile.hasRpName()
                && (profile.knowledgeState != KnowledgeState.KNOWN
                || profile.source != RpNameSource.SERVER_GUI)) {
            profile.knowledgeState = KnowledgeState.KNOWN;
            profile.source = RpNameSource.SERVER_GUI;
            changed = true;
        }
        if (changed) {
            profile.lastUpdatedAt = System.currentTimeMillis();
            saveSoon();
            de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
        }
        return changed;
    }

    public synchronized boolean importApi(String accountName, String uuid, String rpName,
                                          String title, String state) {
        LocalRpProfile profile = find(uuid, accountName).orElse(null);
        if (profile == null) {
            return false;
        }

        boolean apiManaged = profile.knowledgeState == KnowledgeState.API_IMPORTED
                && profile.source == RpNameSource.API_IMPORTED;

        boolean recorded = false;
        if (rpName != null && !rpName.isBlank() && !rpName.equals(profile.apiRpName)) {
            profile.apiRpName = rpName;
            recorded = true;
        }
        String apiState = state == null ? "" : state.trim();
        if (!apiState.equals(profile.apiState == null ? "" : profile.apiState)) {
            profile.apiState = apiState;
            recorded = true;
        }
        if (canUpdateTitle(profile) && title != null && !title.isBlank() && !title.equals(profile.title)) {
            profile.title = title;
            recorded = true;
        }
        if (profile.locked || !profile.knowledgeState.allowsAutomaticUpdates()) {
            if (recorded) {
                profile.lastUpdatedAt = System.currentTimeMillis();
                saveSoon();
            }
            return recorded;
        }
        boolean changed = recorded;
        if ((profile.uuid == null || profile.uuid.isBlank()) && uuid != null && !uuid.isBlank()) {
            profile.uuid = uuid;
            index(profile);
            changed = true;
        }
        if (rpName != null && !rpName.isBlank()
                && (!profile.hasRpName() || apiManaged)) {
            if (!rpName.equals(profile.rpName)) {
                profile.rpName = rpName;
                changed = true;
            }
            if (profile.apiConflict != null) {
                profile.apiConflict = null;
                changed = true;
            }
            profile.knowledgeState = KnowledgeState.API_IMPORTED;
            profile.source = RpNameSource.API_IMPORTED;
        } else if (rpName != null && !rpName.isBlank() && profile.hasRpName()
                && !profile.rpName.equals(rpName)) {
            profile.apiConflict = rpName;
            changed = true;
        }
        if (changed) {
            profile.lastUpdatedAt = System.currentTimeMillis();
            saveSoon();
        }
        return changed;
    }

    /**
     * Übernimmt eine aktuell auf dem Server beobachtete Identität nur dann
     * automatisch, wenn das Profil weiterhin vollständig von der API verwaltet
     * wird. Manuelle, gesperrte und im Spiel kennengelernte Profile bleiben
     * unverändert.
     */
    public synchronized boolean updateImportedIdentityIfChanged(
            String accountName, String uuid, String rpName, String title) {
        LocalRpProfile profile = find(uuid, accountName).orElse(null);
        if (profile == null
                || profile.knowledgeState != KnowledgeState.API_IMPORTED
                || profile.source != RpNameSource.API_IMPORTED) {
            return false;
        }
        // Chatbeobachtungen enthalten keinen API-Stand und dürfen den zuletzt
        // synchronisierten Wert deshalb nicht löschen.
        boolean changed = importApi(accountName, uuid, rpName, title, profile.apiState);
        if (changed) {
            de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
        }
        return changed;
    }

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

    /** Entfernt nur automatisch erzeugte, unbekannte Tablisten-Proxies. */
    public synchronized boolean removeSyntheticTablistAlias(
            String accountName, String aliasUuid, LocalRpProfile canonical) {
        if (accountName == null || accountName.isBlank()
                || aliasUuid == null || aliasUuid.isBlank()
                || canonical == null || !canonical.hasRpName()
                || canonical.accountName == null
                || !accountName.equalsIgnoreCase(canonical.accountName)
                || canonical.uuid == null || canonical.uuid.isBlank()
                || aliasUuid.equalsIgnoreCase(canonical.uuid)) {
            return false;
        }
        LocalRpProfile alias = byUuid.get(aliasUuid.toLowerCase(Locale.ROOT));
        if (alias == null || alias == canonical || alias.hasRpName()
                || alias.source != RpNameSource.SEEN_TABLIST
                || alias.accountName == null
                || !accountName.equalsIgnoreCase(alias.accountName)) {
            return false;
        }
        byUuid.remove(aliasUuid.toLowerCase(Locale.ROOT), alias);
        byNameLower.remove(accountName.toLowerCase(Locale.ROOT), alias);
        index(canonical);
        saveSoon();
        return true;
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
