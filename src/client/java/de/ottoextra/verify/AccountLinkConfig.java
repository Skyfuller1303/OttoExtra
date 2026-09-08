package de.ottoextra.verify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.OttoExtra;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class AccountLinkConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object FILE_LOCK = new Object();

    String endpointUrl = "https://ottoextra.dev";
    int pollIntervalSeconds = 3;
    int requestTimeoutSeconds = 10;
    String deviceToken = "";
    boolean linkPending;
    String minecraftUuid = "";
    String minecraftName = "";
    String authorizationUrl = "";

    static AccountLinkConfig load() {
        Path file = file();
        AccountLinkConfig config = null;
        synchronized (FILE_LOCK) {
            if (Files.isRegularFile(file)) {
                try {
                    config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                            AccountLinkConfig.class);
                } catch (Exception error) {
                    OttoExtra.LOGGER.warn(
                            "Konto-Verknüpfung konnte nicht geladen werden: {}", file, error);
                }
            }
        }
        if (config == null) {
            config = new AccountLinkConfig();
        }
        config.endpointUrl = normalizeLegacyEndpoint(config.endpointUrl);
        config.pollIntervalSeconds = Math.clamp(config.pollIntervalSeconds, 1, 60);
        config.requestTimeoutSeconds = Math.clamp(config.requestTimeoutSeconds, 2, 30);
        return config;
    }

    synchronized void save() {
        Path file = file();
        synchronized (FILE_LOCK) {
            Path temporary = null;
            try {
                Files.createDirectories(file.getParent());
                temporary = Files.createTempFile(
                        file.getParent(), file.getFileName() + ".", ".tmp");
                Files.writeString(temporary, GSON.toJson(this), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception error) {
                OttoExtra.LOGGER.error(
                        "Konto-Verknüpfung konnte nicht gespeichert werden: {}", file, error);
            } finally {
                if (temporary != null) {
                    try {
                        Files.deleteIfExists(temporary);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    boolean hasDeviceToken() {
        return deviceToken != null && !deviceToken.isBlank();
    }

    void clear() {
        deviceToken = "";
        linkPending = false;
        minecraftUuid = "";
        minecraftName = "";
        authorizationUrl = "";
        save();
    }

    private static String normalizeLegacyEndpoint(String value) {
        if (value == null || value.isBlank()
                || "https://ottoextra.dev/discord/index.php".equalsIgnoreCase(value)
                || "https://ottoextra.dev/discord".equalsIgnoreCase(value)) {
            return "https://ottoextra.dev";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static Path file() {
        // Gleiche Datei wie das frühere Zusatzmodul: bestehende Verknüpfungen bleiben erhalten.
        return FabricLoader.getInstance().getConfigDir().resolve("ottoextra-discord.json");
    }
}
