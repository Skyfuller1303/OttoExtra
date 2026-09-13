package de.ottoextra.privacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class CacheBudgetTest {
    @TempDir Path directory;
    @Test void quotaRejectsBeforeWritingAndDoesNotRemoveExistingData() throws Exception {
        Path previous = directory.resolve("old.png"), target = directory.resolve("new.png");
        Files.writeString(previous, "old");
        var written = new AtomicBoolean();
        assertThrows(java.io.IOException.class, () -> CacheBudget.write(directory, target, CacheBudget.MAX_BYTES,
                () -> written.set(true)));
        assertFalse(written.get());
        assertEquals("old", Files.readString(previous));
        assertFalse(Files.exists(target));
        CacheBudget.write(directory, target, 3, () -> Files.writeString(target, "new"));
        assertEquals("new", Files.readString(target));
    }
    @Test void outsideDestinationIsRejectedBeforeWrite() {
        assertThrows(java.io.IOException.class, () -> CacheBudget.write(directory,
                directory.resolve("../outside"), 1, () -> fail("Must not write")));
    }
}
