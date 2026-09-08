package de.ottoextra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLoadRecoveryTest {
    @TempDir Path directory;

    @Test
    void invalidFieldPreservesAllOriginalBytesBeforeWritingDefaults() throws Exception {
        Path file = directory.resolve("ottoextra.json");
        byte[] original = "{\"api\":{\"requestTimeoutMs\":\"abc\"},\"chat\":{\"enabled\":false}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, original);
        OttoExtraConfig config = OttoExtraConfig.load(file);
        assertEquals(new OttoExtraConfig().api.requestTimeoutMs, config.api.requestTimeoutMs);
        try (var files = Files.list(directory)) {
            Path backup = files.filter(p -> p.getFileName().toString().contains(".broken-")).findFirst().orElseThrow();
            assertArrayEquals(original, Files.readAllBytes(backup));
        }
        assertFalse(Files.readString(file).contains("abc"));
    }

    @Test
    void failedBackupBlocksBothAutomaticAndLaterSaves() throws Exception {
        Path file = directory.resolve("ottoextra.json");
        Files.writeString(file, "{broken");
        OttoExtraConfig config = OttoExtraConfig.load(file, ignored -> { throw new IOException("no space"); });
        config.save(file);
        assertEquals("{broken", Files.readString(file));
        assertNotNull(config.consent);
    }
}
