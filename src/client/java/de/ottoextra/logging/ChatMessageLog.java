package de.ottoextra.logging;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** An independent, append-only chat file. No root logger, debug or server gate. */
public final class ChatMessageLog implements AutoCloseable {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS");
    private static volatile ChatMessageLog active;

    private final Path file;
    private final BooleanSupplier enabled;
    private final Clock clock;
    private final FailureLogGate failures = new FailureLogGate();
    private FileAppender appender;
    private long retryAfterNanos;
    private boolean closed;

    ChatMessageLog(Path file, BooleanSupplier enabled, Clock clock) {
        this.file = file;
        this.enabled = enabled;
        this.clock = clock;
    }

    public static synchronized void initialize(OttoExtraConfig config) {
        if (active != null) return;
        ChatMessageLog log = new ChatMessageLog(
                FabricLoader.getInstance().getGameDir().resolve("logs/ottoextra-chat.log"),
                () -> config.tweaks.chatLog, Clock.systemDefaultZone());
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
            if (appender == null) {
                appender = FileAppender.newBuilder()
                        .setName("OttoExtraChatFile")
                        .withFileName(file.toString())
                        .withAppend(true)
                        .setImmediateFlush(true)
                        .setIgnoreExceptions(false)
                        .setLayout(PatternLayout.newBuilder().withPattern("%m%n")
                                .withCharset(StandardCharsets.UTF_8).build())
                        .build();
                if (appender == null) throw new IllegalStateException("Datei konnte nicht geöffnet werden");
                appender.start();
            }
            // Escape line breaks so one received message stays one unambiguous log entry.
            String plain = TextVisitFactory.removeFormattingCodes(message)
                    .replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
            String line = "[" + TIMESTAMP.format(clock.instant().atZone(clock.getZone())) + "] " + plain;
            appender.append(Log4jLogEvent.newBuilder().setLoggerName("OttoExtraChat")
                    .setLevel(Level.INFO).setMessage(new SimpleMessage(line)).build());
            failures.onSuccess();
            retryAfterNanos = 0;
        } catch (RuntimeException error) {
            stopAppender();
            retryAfterNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            if (failures.onFailure()) {
                OttoExtra.LOGGER.warn("Chatprotokoll {} konnte nicht geschrieben werden: {}",
                        file, error.getMessage());
            }
        }
    }

    private void stopAppender() {
        FileAppender previous = appender;
        appender = null;
        if (previous != null) {
            try {
                previous.stop();
            } catch (RuntimeException ignored) {
                // Logging failure must not interrupt chat delivery or client shutdown.
            }
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        stopAppender();
    }
}
