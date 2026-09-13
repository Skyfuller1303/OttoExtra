package de.ottoextra.logging;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Independent UTF-8 chat files, one per client session and local calendar day. */
public final class ChatMessageLog implements AutoCloseable {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS");
    private static volatile ChatMessageLog active;

    private final Path directory;
    private final BooleanSupplier enabled;
    private final Clock clock;
    private final FailureLogGate failures = new FailureLogGate();
    private static final long FILE_LIMIT = 8L * 1024 * 1024;
    private static final long TOTAL_LIMIT = 64L * 1024 * 1024;
    private BufferedWriter writer;
    private long fileBytes;
    private long totalBytes = -1;
    private LocalDate fileDate;
    private long retryAfterNanos;
    private boolean closed;

    ChatMessageLog(Path directory, BooleanSupplier enabled, Clock clock) {
        this.directory = directory;
        this.enabled = enabled;
        this.clock = clock;
    }

    public static synchronized void initialize(OttoExtraConfig config) {
        if (active != null) return;
        ChatMessageLog log = new ChatMessageLog(
                FabricLoader.getInstance().getGameDir().resolve("logs/chat"),
                () -> de.ottoextra.privacy.LocalPrivacy.chatAllowed(config,
                        de.ottoextra.OttoExtraGate.isOnOttonien(net.minecraft.client.MinecraftClient.getInstance())),
                Clock.systemDefaultZone());
        active = log;
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> log.close());
    }

    public static void recordIncoming(Text message) {
        ChatMessageLog log = active;
        if (log != null) log.append(message);
    }

    synchronized void append(Text message) {
        if (closed || message == null || !enabled.getAsBoolean()) return;
        if (retryAfterNanos != 0 && System.nanoTime() - retryAfterNanos < 0) return;
        try {
            var timestamp = clock.instant().atZone(clock.getZone());
            // Escape line breaks so one received message stays one unambiguous log entry.
            String plain = TextVisitFactory.removeFormattingCodes(message)
                    .replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
            String entry = "[" + TIMESTAMP.format(timestamp) + "] " + plain + System.lineSeparator();
            long bytes = entry.getBytes(StandardCharsets.UTF_8).length;
            if (bytes > 64 * 1024) return;
            if (totalBytes < 0) totalBytes = existingBytes();
            if (totalBytes + bytes > TOTAL_LIMIT) throw new IOException("Chatlog-Limit 64 MiB erreicht; lokale Datenverwaltung verwenden");
            if (writer == null || !timestamp.toLocalDate().equals(fileDate) || fileBytes + bytes > FILE_LIMIT) {
                closeWriter();
                // Include other client sessions before starting another file.
                totalBytes = existingBytes();
                if (totalBytes + bytes > TOTAL_LIMIT) throw new IOException("Chatlog-Limit erreicht");
                openWriter(timestamp.toLocalDate());
                fileBytes = 0;
            }
            writer.write(entry);
            fileBytes += bytes;
            totalBytes += bytes;
            writer.flush();
            failures.onSuccess();
            retryAfterNanos = 0;
        } catch (IOException | RuntimeException error) {
            closeWriter();
            retryAfterNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            if (failures.onFailure()) {
                OttoExtra.LOGGER.warn("Chatprotokoll in {} konnte nicht geschrieben werden: {}",
                        directory, error.getMessage());
            }
        }
    }

    private long existingBytes() throws IOException {
        if (!Files.isDirectory(directory)) return 0;
        long total = 0;
        int count = 0;
        try (var files = Files.newDirectoryStream(directory, "ottoextra-chat-*.log")) {
            for (Path path : files) {
                if (++count >= 4096) throw new IOException("Chatlog-Dateilimit erreicht");
                if (Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) total += Files.size(path);
            }
        }
        return total;
    }

    private void openWriter(LocalDate date) throws IOException {
        Files.createDirectories(directory);
        String prefix = "ottoextra-chat-" + date + "-";
        long index = 1;
        try (var files = Files.newDirectoryStream(directory, prefix + "*.log")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                try {
                    long existing = Long.parseLong(name.substring(prefix.length(), name.length() - 4));
                    if (existing >= index) {
                        if (existing == Long.MAX_VALUE) throw new IOException("Keine freie Chatlog-Nummer");
                        index = existing + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // Unrelated files do not participate in the session numbering.
                }
            }
        }
        while (true) {
            try {
                // CREATE_NEW is atomic, so parallel clients cannot overwrite each other.
                writer = Files.newBufferedWriter(directory.resolve(prefix + index + ".log"),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                fileDate = date;
                return;
            } catch (FileAlreadyExistsException collision) {
                if (index == Long.MAX_VALUE) throw new IOException("Keine freie Chatlog-Nummer", collision);
                index++;
            }
        }
    }

    private void closeWriter() {
        BufferedWriter previous = writer;
        writer = null;
        if (previous != null) {
            try {
                previous.close();
            } catch (IOException | RuntimeException ignored) {
                // Logging failure must not interrupt chat delivery or client shutdown.
            }
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        closeWriter();
    }
}
