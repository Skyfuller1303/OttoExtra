package de.ottoextra.logging;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageLogTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-07T12:34:56.123Z"), ZoneOffset.UTC);
    private static final String PREFIX = "[2026-09-07 12:34:56.123] ";
    @TempDir Path directory;

    @Test
    void writesOnlyMessagesWithTimestampAndFlushesImmediately() throws Exception {
        Path chatDirectory = directory.resolve("logs/chat");
        Path file = chatDirectory.resolve("ottoextra-chat-2026-09-07-1.log");
        try (ChatMessageLog log = new ChatMessageLog(chatDirectory, () -> true, CLOCK)) {
            log.append(Text.literal("[System] Neustart in 5 Minuten"));
            log.append(Text.literal("<Alrik> Grüße 👋"));
            assertEquals(List.of(PREFIX + "[System] Neustart in 5 Minuten", PREFIX + "<Alrik> Grüße 👋"),
                    Files.readAllLines(file));
        }
    }

    @Test
    void newSessionGetsNextFileAndKeepsGenuineRepeatedMessages() throws Exception {
        Text message = Text.literal("noch einmal");
        try (ChatMessageLog first = new ChatMessageLog(directory, () -> true, CLOCK)) {
            first.append(message);
        }
        try (ChatMessageLog second = new ChatMessageLog(directory, () -> true, CLOCK)) {
            second.append(message);
            second.append(message);
        }
        assertEquals(List.of(PREFIX + "noch einmal"), Files.readAllLines(file(1)));
        assertEquals(List.of(PREFIX + "noch einmal", PREFIX + "noch einmal"), Files.readAllLines(file(2)));
    }

    @Test
    void disabledCreatesNoFileAndToggleAppliesWithoutRestart() throws Exception {
        AtomicBoolean enabled = new AtomicBoolean(false);
        try (ChatMessageLog log = new ChatMessageLog(directory, enabled::get, CLOCK)) {
            log.append(Text.literal("aus"));
            assertFalse(Files.exists(file(1)));
            enabled.set(true);
            log.append(Text.literal("an"));
            enabled.set(false);
            log.append(Text.literal("wieder aus"));
            enabled.set(true);
            log.append(Text.literal("wieder an"));
        }
        assertEquals(List.of(PREFIX + "an", PREFIX + "wieder an"), Files.readAllLines(file(1)));
        assertFalse(Files.exists(file(2)), "Toggling must not start another session file");
    }

    @Test
    void escapesLineBreaksAndBackslashesButKeepsUnicodeAndStripsFormatting() throws Exception {
        try (ChatMessageLog log = new ChatMessageLog(directory, () -> true, CLOCK)) {
            log.append(Text.literal("§aGrüße\r\nZwei\\nDrei").styled(style -> style.withBold(true)));
        }
        assertEquals(List.of(PREFIX + "Grüße\\r\\nZwei\\\\nDrei"), Files.readAllLines(file(1)));
    }

    @Test
    void debugOffDoesNotDisableChatLog() throws Exception {
        boolean previous = DebugLog.isEnabled();
        try (ChatMessageLog log = new ChatMessageLog(directory, () -> true, CLOCK)) {
            DebugLog.setEnabled(false);
            log.append(Text.literal("ohne Debug"));
        } finally {
            DebugLog.setEnabled(previous);
        }
        assertEquals(List.of(PREFIX + "ohne Debug"), Files.readAllLines(file(1)));
    }

    @Test
    void writeFailuresDoNotEscapeIntoChatAndClosedLogStaysClosed() throws Exception {
        Path parentFile = directory.resolve("not-a-directory");
        Files.writeString(parentFile, "behalten");
        try (ChatMessageLog log = new ChatMessageLog(parentFile.resolve("chat"), () -> true, CLOCK)) {
            assertDoesNotThrow(() -> log.append(Text.literal("Fehlerpfad")));
            assertDoesNotThrow(() -> log.append(Text.literal("weiter")));
        }
        assertEquals("behalten", Files.readString(parentFile));
        Path closedDirectory = directory.resolve("closed");
        ChatMessageLog closed = new ChatMessageLog(closedDirectory, () -> true, CLOCK);
        closed.close();
        closed.append(Text.literal("zu spät"));
        assertFalse(Files.exists(closedDirectory));
    }

    @Test
    void numberingContinuesAfterHighestExistingIndexWithoutChangingOldLogs() throws Exception {
        Files.writeString(file(1), "erste Sitzung");
        Files.writeString(file(3), "dritte Sitzung");
        Path otherDay = directory.resolve("ottoextra-chat-2026-09-06-99.log");
        Files.writeString(otherDay, "anderer Tag");
        Path unrelated = directory.resolve("ottoextra-chat-2026-09-07-not-a-number.log");
        Files.writeString(unrelated, "unbekannte Datei");
        try (ChatMessageLog log = new ChatMessageLog(directory, () -> true, CLOCK)) {
            log.append(Text.literal("neue Sitzung"));
        }
        assertEquals(List.of(PREFIX + "neue Sitzung"), Files.readAllLines(file(4)));
        assertFalse(Files.exists(file(2)));
        assertEquals("erste Sitzung", Files.readString(file(1)));
        assertEquals("dritte Sitzung", Files.readString(file(3)));
        assertEquals("anderer Tag", Files.readString(otherDay));
        assertEquals("unbekannte Datei", Files.readString(unrelated));
    }

    @Test
    void localMidnightStartsNewDayAndClockRollbackNeverReopensOldFile() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-07T21:59:59Z"));
        Clock clock = mutableClock(now, ZoneId.of("Europe/Berlin"));
        try (ChatMessageLog log = new ChatMessageLog(directory, () -> true, clock)) {
            log.append(Text.literal("vor Mitternacht"));
            now.set(Instant.parse("2026-09-07T22:00:00Z"));
            log.append(Text.literal("nach Mitternacht"));
            assertEquals(List.of("[2026-09-07 23:59:59.000] vor Mitternacht"), Files.readAllLines(file(1)));
            assertEquals(List.of("[2026-09-08 00:00:00.000] nach Mitternacht"),
                    Files.readAllLines(directory.resolve("ottoextra-chat-2026-09-08-1.log")));
            now.set(Instant.parse("2026-09-07T21:59:58Z"));
            log.append(Text.literal("Uhr zurückgestellt"));
        }
        assertEquals(List.of("[2026-09-07 23:59:58.000] Uhr zurückgestellt"), Files.readAllLines(file(2)));
    }

    @Test
    void concurrentClientsKeepSeparateFiles() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = java.util.stream.IntStream.range(0, 12)
                    .<java.util.concurrent.Callable<Void>>mapToObj(id -> () -> {
                        try (ChatMessageLog log = new ChatMessageLog(directory, () -> true, CLOCK)) {
                            log.append(Text.literal("Client " + id));
                        }
                        return null;
                    }).toList();
            for (var future : executor.invokeAll(tasks)) future.get();
        }
        var messages = new java.util.HashSet<String>();
        for (int index = 1; index <= 12; index++) {
            var lines = Files.readAllLines(file(index));
            assertEquals(1, lines.size());
            messages.add(lines.getFirst());
        }
        assertEquals(12, messages.size());
    }

    @Test
    void legacyLogIsPreservedAndNewMessagesUseChatDirectory() throws Exception {
        Path legacy = directory.resolve("logs/ottoextra-chat.log");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "alte Chatnachrichten");
        Path chatDirectory = legacy.getParent().resolve("chat");
        try (ChatMessageLog log = new ChatMessageLog(chatDirectory, () -> true, CLOCK)) {
            log.append(Text.literal("neu"));
        }
        assertEquals("alte Chatnachrichten", Files.readString(legacy));
        assertEquals(List.of(PREFIX + "neu"),
                Files.readAllLines(chatDirectory.resolve("ottoextra-chat-2026-09-07-1.log")));
    }

    private Path file(int index) {
        return directory.resolve("ottoextra-chat-2026-09-07-" + index + ".log");
    }

    @Test
    void rotatesAtEightMiBAndSkipsOversizedSingleMessages() throws Exception {
        try (ChatMessageLog log = new ChatMessageLog(directory, () -> true, CLOCK)) {
            log.append(Text.literal("x".repeat(65536)));
            assertFalse(Files.exists(file(1)));
            for (int i = 0; i < 132; i++) log.append(Text.literal("x".repeat(63 * 1024)));
        }
        assertTrue(Files.exists(file(2)));
        assertTrue(Files.size(file(1)) <= 8 * 1024 * 1024);
        assertTrue(Files.size(file(2)) <= 8 * 1024 * 1024);
    }

    @Test
    void fullQuotaPreservesOldFilesAndCreatesNoNewSessionFile() throws Exception {
        try (var old = new java.io.RandomAccessFile(file(1).toFile(), "rw")) {
            old.setLength(64L * 1024 * 1024);
        }
        try (ChatMessageLog log = new ChatMessageLog(directory, () -> true, CLOCK)) {
            assertDoesNotThrow(() -> log.append(Text.literal("quota reached")));
        }
        assertFalse(Files.exists(file(2)));
        assertEquals(64L * 1024 * 1024, Files.size(file(1)));
    }

    private static Clock mutableClock(AtomicReference<Instant> now, ZoneId zone) {
        return new Clock() {
            @Override public ZoneId getZone() { return zone; }
            @Override public Clock withZone(ZoneId nextZone) { return mutableClock(now, nextZone); }
            @Override public Instant instant() { return now.get(); }
        };
    }

    @Test
    void defaultsOffForExistingConfigsAndPreservesExplicitOffAcrossRestore() {
        OttoExtraConfig config = new OttoExtraConfig();
        assertFalse(config.tweaks.chatLog);
        config.restoreFrom("{\"tweaks\":{\"combatModeHud\":false}}");
        assertFalse(config.tweaks.chatLog);
        config.tweaks.chatLog = false;
        config.restoreFrom(config.snapshotJson());
        assertFalse(config.tweaks.chatLog);
    }
}
