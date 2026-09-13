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
    void observingAnExistingUuidAgainUpdatesCurrentNameOwnerImmediately() {
        var store = new LocalRpIdentityStore(false);
        try {
            var first = store.ensureSeen("Anna", "uuid-a", RpNameSource.SEEN_ONLINE);
            store.ensureSeen("Anna", "uuid-b", RpNameSource.SEEN_ONLINE);
            store.ensureSeen("Anna", "uuid-a", RpNameSource.SEEN_ONLINE);
            assertSame(first, store.findByName("Anna").orElseThrow());
        } finally {
            store.shutdown();
        }
    }

    @Test
    void uuidEditPreservesOtherProfileAndCurrentOwnerAcrossReload() {
        Path file = directory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(file, directory.resolve("backups"));
        try {
            var selected = store.ensureSeen("Anna", "uuid-a", RpNameSource.SEEN_ONLINE);
            var current = store.ensureSeen("Anna", "uuid-b", RpNameSource.SEEN_ONLINE);
            current.notes = "Andere Person";
            long observedAt = selected.lastSeenAt;
            var edited = store.updateManual("Anna", "uuid-a", p -> {
                p.rpName = "Ada";
                p.title = "Gräfin";
                p.notes = "Meine Notiz";
                p.colors.chatNameColor = "#123456";
            }, true);
            assertSame(selected, edited);
            assertSame(current, store.findByName("Anna").orElseThrow());
            assertFalse(current.hasRpName());
            assertEquals("Andere Person", current.notes);
            assertEquals(observedAt, selected.lastSeenAt);
        } finally {
            store.shutdown();
        }
        var restored = new LocalRpIdentityStore(file, directory.resolve("backups"));
        try {
            restored.load();
            assertEquals("uuid-b", restored.findByName("Anna").orElseThrow().uuid);
            var edited = restored.find("uuid-a", "Anna").orElseThrow();
            assertEquals("Ada", edited.rpName);
            assertEquals("Gräfin", edited.title);
            assertEquals("Meine Notiz", edited.notes);
            assertEquals("#123456", edited.colors.chatNameColor);
            assertEquals(KnowledgeState.MANUAL_LOCKED, edited.knowledgeState);
            assertTrue(edited.locked);
        } finally {
            restored.shutdown();
        }
    }

    @Test
    void editingAnUnseenUuidDoesNotReuseAnotherPersonsProfile() {
        var store = new LocalRpIdentityStore(false);
        try {
            var original = store.ensureSeen("Anna", "uuid-a", RpNameSource.SEEN_ONLINE);
            var edited = store.updateManual("Anna", "uuid-b", p -> p.rpName = "Ada", false);
            assertNotSame(original, edited);
            assertEquals("uuid-b", edited.uuid);
            assertFalse(original.hasRpName());
            assertEquals(2, store.size());
        } finally {
            store.shutdown();
        }
    }

    @Test
    void changedNameOwnerIsPersistedEvenAfterPreviousSaveWasClean() {
        Path file = directory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(file, directory.resolve("backups"));
        try {
            store.ensureSeen("Anna", "uuid-a", RpNameSource.SEEN_ONLINE);
            store.ensureSeen("Anna", "uuid-b", RpNameSource.SEEN_ONLINE);
            store.saveNow();
            store.ensureSeen("Anna", "uuid-a", RpNameSource.SEEN_ONLINE);
            store.saveNow();
        } finally {
            store.shutdown();
        }
        var restored = new LocalRpIdentityStore(file, directory.resolve("backups"));
        try {
            restored.load();
            assertEquals("uuid-a", restored.findByName("Anna").orElseThrow().uuid);
        } finally {
            restored.shutdown();
        }
    }

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
            assertEquals(java.util.List.of(next), store.currentAccounts());
            store.saveNow();
        } finally {
            store.shutdown();
        }
        var restored = new LocalRpIdentityStore(file, directory.resolve("backups"));
        try {
            restored.load();
            assertEquals(2, restored.size());
            assertEquals(b, restored.findByName("Anna").orElseThrow().uuid);
            assertEquals(1, restored.currentAccounts().size());
            assertEquals(b, restored.currentAccounts().getFirst().uuid);
            assertEquals("Privat", restored.find(a, "Anna").orElseThrow().notes);
            restored.ensureSeen("Annabelle", a, RpNameSource.SEEN_ONLINE);
            assertEquals(b, restored.findByName("Anna").orElseThrow().uuid);
            assertEquals(a, restored.findByName("Annabelle").orElseThrow().uuid);
            assertEquals(2, restored.currentAccounts().size());
        } finally {
            restored.shutdown();
        }
    }

    @Test
    void accountRecipientsIgnoreCaseDuplicatesWithoutBorrowingHistoricalIdentities() {
        var store = new LocalRpIdentityStore(false);
        try {
            var old = store.ensureSeen("Cyhoxl", "00000000-0000-0000-0000-000000000001", RpNameSource.SEEN_ONLINE);
            old.rpName = "Theodor";
            old.notes = "Notiz zur alten Identität";
            store.ensureSeen("cyhoxl", "00000000-0000-0000-0000-000000000002", RpNameSource.SEEN_ONLINE);
            var current = store.ensureSeen("CYHOXL", "00000000-0000-0000-0000-000000000003", RpNameSource.SEEN_ONLINE);
            assertEquals(java.util.List.of(current), store.currentAccounts());
            assertFalse(current.hasRpName());
            assertEquals(3, store.all().size());
            assertEquals("Theodor", old.rpName);
            assertEquals("Notiz zur alten Identität", old.notes);
        } finally {
            store.shutdown();
        }
    }

    @Test
    void differentMinecraftAccountsWithTheSameRpNameRemainSeparateRecipients() {
        var store = new LocalRpIdentityStore(false);
        try {
            var first = store.ensureSeen("Anna", "00000000-0000-0000-0000-000000000001", RpNameSource.SEEN_ONLINE);
            var second = store.ensureSeen("Berta", "00000000-0000-0000-0000-000000000002", RpNameSource.SEEN_ONLINE);
            first.rpName = second.rpName = "Lilly";
            assertEquals(2, store.currentAccounts().size());
            assertTrue(store.currentAccounts().containsAll(java.util.List.of(first, second)));
        } finally {
            store.shutdown();
        }
    }
}
