package de.ottoextra.rpnames.store;

import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.RpNameSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class IdentityReuseTest {
    @TempDir Path directory;

    @Test
    void reusedNameKeepsBothUuidProfilesAcrossSaveAndRename() {
        Path file = directory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(file, directory.resolve("backups"));
        String a = "00000000-0000-0000-0000-000000000001";
        String b = "00000000-0000-0000-0000-000000000002";
        try {
            var old = store.ensureSeen("Anna", a, RpNameSource.SEEN_ONLINE);
            old.rpName = "Alte Identität";
            old.notes = "Privat";
            old.knowledgeState = KnowledgeState.KNOWN;
            old.lastSeenAt = 1;
            assertTrue(store.find(b, "Anna").isEmpty());
            var next = store.ensureSeen("Anna", b, RpNameSource.SEEN_ONLINE);
            assertNotSame(old, next);
            assertFalse(next.hasRpName());
            assertEquals("", next.notes);
            assertEquals(KnowledgeState.SEEN, next.knowledgeState);
            assertEquals(2, store.size());
            store.saveNow();
        } finally {
            store.shutdown();
        }
        var restored = new LocalRpIdentityStore(file, directory.resolve("backups"));
        try {
            restored.load();
            assertEquals(2, restored.size());
            assertEquals(b, restored.findByName("Anna").orElseThrow().uuid);
            assertEquals("Privat", restored.find(a, "Anna").orElseThrow().notes);
            restored.ensureSeen("Annabelle", a, RpNameSource.SEEN_ONLINE);
            assertEquals(b, restored.findByName("Anna").orElseThrow().uuid);
            assertEquals(a, restored.findByName("Annabelle").orElseThrow().uuid);
        } finally {
            restored.shutdown();
        }
    }
}
