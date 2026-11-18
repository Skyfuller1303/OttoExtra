package de.ottoextra.crafting.werkstatt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Remembered recipes and the job queue, per server and account. One file per server under
 * {@code config/ottoextra/werkstatt/<sha256(server address)>.json}, accounts inside it, so the
 * address itself is not part of the path.
 */
public final class WerkstattStore {
    public static final int FILE_VERSION = 1;
    public static final int MAX_SAVED = 30;
    public static final int MAX_QUEUE = 20;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Data of one account. */
    public static final class AccountData {
        public List<WerkstattJob> saved = new ArrayList<>();
        public List<WerkstattJob> queue = new ArrayList<>();

        void sanitize() {
            if (saved == null) saved = new ArrayList<>();
            if (queue == null) queue = new ArrayList<>();
            saved.removeIf(j -> j == null || j.name == null || j.name.isBlank());
            queue.removeIf(j -> j == null || j.name == null || j.name.isBlank());
            saved.forEach(WerkstattJob::sanitize);
            queue.forEach(WerkstattJob::sanitize);
            while (saved.size() > MAX_SAVED) saved.remove(saved.size() - 1);
            while (queue.size() > MAX_QUEUE) queue.remove(queue.size() - 1);
        }
    }

    private static final class Stored {
        int version = FILE_VERSION;
        String server = "";
        Map<String, AccountData> accounts = new LinkedHashMap<>();
    }

    private final Path file;
    private final String server;
    private final String account;
    private Stored stored = new Stored();

    private WerkstattStore(Path file, String server, String account) {
        this.file = file;
        this.server = server;
        this.account = account;
    }

    /** File name for a server address: SHA-256 of the lower-cased address. */
    public static String fileName(String server) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalizeServer(server).getBytes(StandardCharsets.UTF_8))) + ".json";
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String normalizeServer(String server) {
        return server == null ? "" : server.trim().toLowerCase(Locale.ROOT);
    }

    /** Opens the store of {@code account} on {@code server} in {@code dir}; a broken file starts empty. */
    public static WerkstattStore open(Path dir, String server, String account) {
        String s = normalizeServer(server);
        String a = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        WerkstattStore store = new WerkstattStore(dir == null ? null : dir.resolve(fileName(s)), s, a);
        store.load();
        return store;
    }

    /** An in-memory store without a file (not connected). */
    public static WerkstattStore memory() {
        return new WerkstattStore(null, "", "");
    }

    public AccountData data() {
        return stored.accounts.computeIfAbsent(account, k -> new AccountData());
    }

    private void load() {
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            Stored read = JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Stored.class);
            if (read != null && read.version >= 1 && read.version <= FILE_VERSION && server.equals(read.server)) {
                if (read.accounts == null) read.accounts = new LinkedHashMap<>();
                read.accounts.values().removeIf(d -> d == null);
                read.accounts.values().forEach(AccountData::sanitize);
                stored = read;
            }
        } catch (IOException | JsonParseException | IllegalStateException error) {
            de.ottoextra.OttoExtra.LOGGER.warn("Werkstatt-Datei nicht lesbar, beginne leer: {}", error.toString());
        }
    }

    /** Writes the file atomically; small data, runs on the calling thread. */
    public void save() {
        if (file == null) {
            return;
        }
        stored.server = server;
        stored.version = FILE_VERSION;
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                Files.writeString(temporary, JSON.toJson(stored), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException error) {
            de.ottoextra.OttoExtra.LOGGER.warn("Werkstatt-Datei konnte nicht gespeichert werden: {}", error.toString());
        }
    }
}
