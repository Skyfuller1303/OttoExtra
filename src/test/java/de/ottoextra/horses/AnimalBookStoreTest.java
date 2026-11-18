package de.ottoextra.horses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AnimalBookStoreTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong(1_800_000_000_000L);
    private final UUID uuid = UUID.fromString("06cb65df-d251-4a03-8270-31fd416942ac");

    private AnimalBookStore store(String server) {
        AnimalBookStore store = new AnimalBookStore(directory.resolve("book.json"), server, now::get);
        store.load();
        return store;
    }

    @Test
    void noteAndFarmSymbolSurviveAReload() {
        AnimalBookStore first = store("server-a");
        first.saveFarm("eichengrund", "Eichengrund", "Stall am Bach", "schild_gruen");
        first.saveAnimal(uuid, "eichengrund", "Eichengrund", "  Tochter von Donar  ");

        AnimalBookStore second = store("server-a");
        assertEquals("Tochter von Donar", second.note(uuid));
        assertEquals("schild_gruen", second.symbol("eichengrund"));
        assertEquals("Stall am Bach", second.farmNote("eichengrund"));
        var farm = second.farmForApi("eichengrund");
        assertNotNull(farm);
        assertTrue(farm.id().matches("[a-z0-9-]{8,40}"));
        assertEquals(farm.id(), second.animals().getFirst().farmId());
    }

    @Test
    void unknownFarmGetsTheDefaultSymbolAndInvalidSymbolsFallBack() {
        AnimalBookStore store = store("server-a");
        assertEquals(FarmSymbols.DEFAULT, store.symbol("nope"));
        assertEquals("haus_gruen", FarmSymbols.DEFAULT);
        store.saveFarm("k", "Mühlbach", "", "drache_lila");
        assertEquals(FarmSymbols.DEFAULT, store.symbol("k"));
        assertEquals("burg_rot", FarmSymbols.withColor(FarmSymbols.withShape("x", "burg"), "rot"));
        assertEquals(72, FarmSymbols.SHAPES.size() * FarmSymbols.COLORS.size());
    }

    @Test
    void unchangedSaveKeepsUpdatedAtAndChangeRaisesIt() {
        AnimalBookStore store = store("server-a");
        var created = store.saveFarm("k", "Hof", "", "haus_blau");
        now.addAndGet(1_000);
        assertSame(created, store.saveFarm("k", "Hof", "", "haus_blau"));
        var changed = store.saveFarm("k", "Hof", "", "krone_gold");
        assertEquals(created.id(), changed.id());
        assertTrue(changed.updatedAt() > created.updatedAt());
    }

    @Test
    void noteIsClippedAndControlCharactersRemoved() {
        AnimalBookStore store = store("server-a");
        store.saveAnimal(uuid, "", "", "a\u0007b" + "x".repeat(300));
        String note = store.note(uuid);
        assertEquals(AnimalBookStore.NOTE_MAX, note.length());
        assertTrue(note.startsWith("abx"));
    }

    @Test
    void deleteLeavesATombstoneThatExpires() {
        AnimalBookStore store = store("server-a");
        store.saveAnimal(uuid, "", "", "Notiz");
        store.deleteAnimal(uuid);
        assertEquals("", store.note(uuid));
        assertTrue(store.animals().getFirst().deleted());
        assertTrue(store(("server-a")).animals().getFirst().deleted());

        now.addAndGet(AnimalBookStore.TOMBSTONE_MS + 1);
        AnimalBookStore later = store("server-a");
        assertTrue(later.animals().isEmpty());
    }

    @Test
    void fileOfAnotherServerOrBrokenFileIsIgnored() throws Exception {
        store("server-a").saveAnimal(uuid, "", "", "Notiz");
        assertEquals("", store("server-b").note(uuid));
        Files.writeString(directory.resolve("book.json"), "{ kaputt");
        assertEquals("", store("server-a").note(uuid));
    }

    @Test
    void storeWithoutFileKeepsDataInMemoryOnly() {
        AnimalBookStore memory = new AnimalBookStore(null, "", now::get);
        memory.saveAnimal(uuid, "", "", "nur hier");
        assertEquals("nur hier", memory.note(uuid));
        assertFalse(Files.exists(directory.resolve("book.json")));
    }
}
