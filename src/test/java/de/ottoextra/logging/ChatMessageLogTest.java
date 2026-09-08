package de.ottoextra.logging;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageLogTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-07T12:34:56.123Z"), ZoneOffset.UTC);
    private static final String PREFIX = "[2026-09-07 12:34:56.123] ";
    @TempDir Path directory;

    @Test
    void writesOnlyMessagesWithTimestampAndFlushesImmediately() throws Exception {
        Path file = directory.resolve("logs/ottoextra-chat.log");
        try (ChatMessageLog log = new ChatMessageLog(file, () -> true, CLOCK)) {
            log.append(Text.literal("[System] Neustart in 5 Minuten"));
            log.append(Text.literal("<Alrik> Grüße 👋"));
            assertEquals(List.of(PREFIX + "[System] Neustart in 5 Minuten", PREFIX + "<Alrik> Grüße 👋"),
                    Files.readAllLines(file));
        }
    }

    @Test
    void reopeningAppendsAndKeepsGenuineRepeatedMessages() throws Exception {
        Path file = directory.resolve("chat.log");
        Text message = Text.literal("noch einmal");
        try (ChatMessageLog first = new ChatMessageLog(file, () -> true, CLOCK)) {
            first.append(message);
        }
        try (ChatMessageLog second = new ChatMessageLog(file, () -> true, CLOCK)) {
            second.append(message);
            second.append(message);
        }
        assertEquals(List.of(PREFIX + "noch einmal", PREFIX + "noch einmal", PREFIX + "noch einmal"),
                Files.readAllLines(file));
    }

    @Test
    void disabledCreatesNoFileAndToggleAppliesWithoutRestart() throws Exception {
        Path file = directory.resolve("chat.log");
        AtomicBoolean enabled = new AtomicBoolean(false);
        try (ChatMessageLog log = new ChatMessageLog(file, enabled::get, CLOCK)) {
            log.append(Text.literal("aus"));
            assertFalse(Files.exists(file));
            enabled.set(true);
            log.append(Text.literal("an"));
            enabled.set(false);
            log.append(Text.literal("wieder aus"));
            enabled.set(true);
            log.append(Text.literal("wieder an"));
        }
        assertEquals(List.of(PREFIX + "an", PREFIX + "wieder an"), Files.readAllLines(file));
    }

    @Test
    void escapesLineBreaksAndBackslashesButKeepsUnicodeAndStripsFormatting() throws Exception {
        Path file = directory.resolve("chat.log");
        try (ChatMessageLog log = new ChatMessageLog(file, () -> true, CLOCK)) {
            log.append(Text.literal("§aGrüße\r\nZwei\\nDrei").styled(style -> style.withBold(true)));
        }
        assertEquals(List.of(PREFIX + "Grüße\\r\\nZwei\\\\nDrei"), Files.readAllLines(file));
    }

    @Test
    void debugOffDoesNotDisableChatLog() throws Exception {
        Path file = directory.resolve("chat.log");
        boolean previous = DebugLog.isEnabled();
        try (ChatMessageLog log = new ChatMessageLog(file, () -> true, CLOCK)) {
            DebugLog.setEnabled(false);
            log.append(Text.literal("ohne Debug"));
        } finally {
            DebugLog.setEnabled(previous);
        }
        assertEquals(List.of(PREFIX + "ohne Debug"), Files.readAllLines(file));
    }

    @Test
    void writeFailuresDoNotEscapeIntoChatAndClosedLogStaysClosed() throws Exception {
        Path parentFile = directory.resolve("not-a-directory");
        Files.writeString(parentFile, "behalten");
        try (ChatMessageLog log = new ChatMessageLog(parentFile.resolve("chat.log"), () -> true, CLOCK)) {
            assertDoesNotThrow(() -> log.append(Text.literal("Fehlerpfad")));
            assertDoesNotThrow(() -> log.append(Text.literal("weiter")));
        }
        assertEquals("behalten", Files.readString(parentFile));
        Path closedFile = directory.resolve("closed.log");
        ChatMessageLog closed = new ChatMessageLog(closedFile, () -> true, CLOCK);
        closed.close();
        closed.append(Text.literal("zu spät"));
        assertFalse(Files.exists(closedFile));
    }

    @Test
    void defaultsOnForExistingConfigsAndPreservesExplicitOffAcrossRestore() {
        OttoExtraConfig config = new OttoExtraConfig();
        assertTrue(config.tweaks.chatLog);
        config.restoreFrom("{\"tweaks\":{\"combatModeHud\":false}}");
        assertTrue(config.tweaks.chatLog);
        config.tweaks.chatLog = false;
        config.restoreFrom(config.snapshotJson());
        assertFalse(config.tweaks.chatLog);
    }
}
