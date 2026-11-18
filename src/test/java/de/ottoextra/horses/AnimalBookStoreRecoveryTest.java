package de.ottoextra.horses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AnimalBookStoreRecoveryTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong(1_800_000_000_000L);
    private final UUID uuid = UUID.fromString("06cb65df-d251-4a03-8270-31fd416942ac");

    private Path file() {
        return directory.resolve("book.json");
    }

    private AnimalBookStore load() {
        AnimalBookStore store = new AnimalBookStore(file(), "server-a", now::get);
        store.load();
        return store;
    }

    private List<Path> backups() throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().startsWith("book.broken-")).sorted().toList();
        }
    }

    @Test
    void unreadableFileIsMovedAsideAndNeverOverwritten() throws Exception {
        String broken = "{\"version\":2,\"server\":\"server-a\",\"lists\":[{\"writeCode\":";
        Files.writeString(file(), broken, StandardCharsets.UTF_8);
        AnimalBookStore store = load();
        assertTrue(store.farms().isEmpty());
        assertFalse(store.blocked());
        assertFalse(Files.exists(file()));
        List<Path> backups = backups();
        assertEquals(1, backups.size());
        assertTrue(backups.getFirst().getFileName().toString().matches("book\\.broken-\\d+\\.json"));

        store.saveFarm("eichengrund", "Eichengrund", "", "haus_gruen");
        assertEquals(broken, Files.readString(backups.getFirst(), StandardCharsets.UTF_8));
        assertNotNull(load().farmForApi("eichengrund"));
    }

    @Test
    void fileOfANewerVersionIsKept() throws Exception {
        String newer = "{\"version\":99,\"server\":\"server-a\",\"farms\":[]}";
        Files.writeString(file(), newer, StandardCharsets.UTF_8);
        load().saveAnimal(uuid, "", "", "Notiz");
        assertEquals(newer, Files.readString(backups().getFirst(), StandardCharsets.UTF_8));
    }

    @Test
    void atMostThreeBackupsAreKept() throws Exception {
        for (int i = 0; i < 5; i++) {
            Files.writeString(file(), "kaputt " + i, StandardCharsets.UTF_8);
            now.addAndGet(1_000L);
            load();
        }
        List<Path> backups = backups();
        assertEquals(AnimalBookStore.MAX_BROKEN_BACKUPS, backups.size());
        List<String> contents = new java.util.ArrayList<>();
        for (Path backup : backups) contents.add(Files.readString(backup, StandardCharsets.UTF_8));
        assertTrue(contents.contains("kaputt 4"));
        assertFalse(contents.contains("kaputt 0"));
    }

    @Test
    void timesFromTheFutureAreClampedAndTheFileIsCompact() throws Exception {
        AnimalBookStore first = load();
        first.saveFarm("eichengrund", "Eichengrund", "", "haus_gruen");
        first.saveAnimal(uuid, "eichengrund", "Eichengrund", "Notiz");
        String json = Files.readString(file(), StandardCharsets.UTF_8);
        assertFalse(json.contains("\n"), "compact JSON");

        now.addAndGet(-60_000L);
        AnimalBookStore second = load();
        assertEquals("Notiz", second.note(uuid));
        assertEquals(now.get(), second.animals().getFirst().updatedAt());
        assertEquals(now.get(), second.farmForApi("eichengrund").updatedAt());
        assertTrue(backups().isEmpty());
    }
}
