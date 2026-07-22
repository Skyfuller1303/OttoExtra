package de.ottoextra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import de.ottoextra.logging.DebugLog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public final class OttoExtraBackupService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String REWORK_REASON = "settings-gui-rework";

    public static final class ManifestFile {
        public String source;
        public String target;
        public String sha256;
    }

    public static final class Manifest {
        public String createdAt;
        public String reason;
        public List<ManifestFile> files = new ArrayList<>();
    }

    private static final class State {
        String lastBackupReason;
        String lastBackupPath;
        boolean completed;
    }

    public record BackupEntry(Path dir, String name, long files) {
    }

    private static volatile boolean backupOk = false;
    private static volatile String lastBackupName = "";

    private OttoExtraBackupService() {
    }

    private static Path backupsRoot() {
        return OttoExtraPaths.root().resolve("backups").resolve(REWORK_REASON);
    }

    private static Path stateFile() {
        return OttoExtraPaths.root().resolve("backups").resolve(".state")
                .resolve(REWORK_REASON + ".json");
    }

    private static List<Path> sources() {
        Path cfg = OttoExtraPaths.root();
        Path mcConfig = cfg.getParent();
        List<Path> out = new ArrayList<>(List.of(
                cfg.resolve("ottoextra.json"),
                cfg.resolve("rpnames").resolve("known-players.json"),
                cfg.resolve("rpnames").resolve("title-catalog.json"),
                cfg.resolve("rpnames").resolve("title-groups.json"),
                cfg.resolve(".cache").resolve("letters"),
                cfg.resolve("letters")));
        if (mcConfig != null) {
            for (String legacy : new String[]{"ottonames.json", "ottoregions.json",
                    "ottoregions-menu.json", "ottochat-rpnames.json",
                    "ottochat-rpnames-cache.json", "ottoletter-draft.json",
                    "ottoletter-player-cache.json", "ottotalk.json"}) {
                out.add(mcConfig.resolve(legacy));
            }
        }
        return out;
    }

    public static synchronized boolean ensurePreMigrationBackup() {
        try {
            State state = loadState();
            if (state != null && state.completed
                    && REWORK_REASON.equals(state.lastBackupReason)
                    && state.lastBackupPath != null
                    && Files.isDirectory(Path.of(state.lastBackupPath))) {
                backupOk = true;
                lastBackupName = Path.of(state.lastBackupPath).getFileName().toString();
                return true;
            }
            Path dir = createBackup();
            State next = new State();
            next.lastBackupReason = REWORK_REASON;
            next.lastBackupPath = dir.toString();
            next.completed = true;
            saveState(next);
            backupOk = true;
            lastBackupName = dir.getFileName().toString();
            OttoExtra.LOGGER.info("[Backup] Backup complete: {}", dir);
            return true;
        } catch (Exception e) {
            backupOk = false;
            OttoExtra.LOGGER.error(
                    "[Backup] Backup failed. Migration aborted to protect user data.", e);
            return false;
        }
    }

    public static synchronized Path createBackup() throws Exception {
        String stamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path dir = backupsRoot().resolve(stamp);
        Files.createDirectories(dir);
        Manifest manifest = new Manifest();
        manifest.createdAt = LocalDateTime.now().toString();
        manifest.reason = REWORK_REASON;
        Path root = OttoExtraPaths.root().getParent();
        for (Path source : sources()) {
            if (!Files.exists(source)) {
                continue;
            }
            String rel = root != null && source.startsWith(root)
                    ? root.relativize(source).toString().replace('\\', '/')
                    : source.getFileName().toString();
            Path target = dir.resolve(rel);
            if (Files.isDirectory(source)) {
                copyRecursive(source, target, manifest, rel);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                addManifest(manifest, source, rel);
                DebugLog.debug("[Backup] Copied {}", rel);
            }
        }
        Files.writeString(dir.resolve("backup-manifest.json"), GSON.toJson(manifest),
                StandardCharsets.UTF_8);
        lastBackupName = stamp;
        backupOk = true;
        return dir;
    }

    private static void copyRecursive(Path sourceDir, Path targetDir, Manifest manifest,
                                      String relBase) throws Exception {
        try (Stream<Path> walk = Files.walk(sourceDir)) {
            for (Path p : walk.toList()) {
                if (Files.isDirectory(p)) {
                    continue;
                }
                String rel = relBase + "/" + sourceDir.relativize(p).toString().replace('\\', '/');
                Path target = targetDir.resolve(sourceDir.relativize(p).toString());
                Files.createDirectories(target.getParent());
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                addManifest(manifest, p, rel);
            }
        }
    }

    private static void addManifest(Manifest manifest, Path source, String rel) throws Exception {
        ManifestFile f = new ManifestFile();
        f.source = rel;
        f.target = rel;
        f.sha256 = sha256(source);
        manifest.files.add(f);
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
    }

    public static List<BackupEntry> listBackups() {
        List<BackupEntry> out = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(backupsRoot())) {
            for (Path d : dirs.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::getFileName).reversed()).toList()) {
                long count;
                try (Stream<Path> walk = Files.walk(d)) {
                    count = walk.filter(Files::isRegularFile).count();
                }
                out.add(new BackupEntry(d, d.getFileName().toString(), count));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static synchronized boolean restoreBackup(Path backupDir) {
        try {
            createBackup();
            Path root = OttoExtraPaths.root().getParent();
            if (root == null) {
                return false;
            }
            try (Stream<Path> walk = Files.walk(backupDir)) {
                for (Path p : walk.toList()) {
                    if (Files.isDirectory(p)
                            || p.getFileName().toString().equals("backup-manifest.json")) {
                        continue;
                    }
                    Path target = root.resolve(backupDir.relativize(p).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            OttoExtra.LOGGER.info("[Backup] Restored {}", backupDir.getFileName());
            return true;
        } catch (Exception e) {
            OttoExtra.LOGGER.error("[Backup] Restore failed: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isBackupOk() {
        return backupOk;
    }

    public static String lastBackupName() {
        return lastBackupName;
    }

    private static State loadState() {
        try {
            Path f = stateFile();
            if (!Files.exists(f)) {
                return null;
            }
            return GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), State.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static void saveState(State state) throws Exception {
        Path f = stateFile();
        Files.createDirectories(f.getParent());
        Files.writeString(f, GSON.toJson(state), StandardCharsets.UTF_8);
    }
}
