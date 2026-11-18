package de.ottoextra.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceStoreTest {
    @TempDir
    Path directory;

    private static ZoneRecord zone(String id, boolean deleted) {
        return new ZoneRecord(id, "Mine " + id, "COAL_MINE", "minecraft:overworld", "Nord",
                ResourceZone.point(1, 2, 3, 4), 30, "Notiz", 1000, deleted);
    }

    @Test
    void listSurvivesSaveAndLoadWithSharingState() {
        ResourceStore store = new ResourceStore(directory.resolve("lists"));
        ResourceList list = new ResourceList("local-0001", "Holdstewik");
        list.writeCode = "ABCDEFGHJKMNPQRSTVWX";
        list.readCode = ResourceCodes.readCode(list.writeCode);
        list.since = 77;
        list.zones.put("zone-0001", zone("zone-0001", false));
        list.zones.put("zone-0002", zone("zone-0002", true));
        list.timers.put("zone-0001", new TimerRecord(5L, "Anna", 6));
        list.dirtyZones.add("zone-0001");
        list.dirtyTimers.add("zone-0001");
        list.retired.add(new ResourceList.Retired("old-share-id", "ZZZZZZZZZZZZZZZZZZZZ",
                Map.of("zone-0001", zone("zone-0001", true))));
        assertTrue(store.save(list));

        List<ResourceList> loaded = store.loadAll();
        assertEquals(1, loaded.size());
        ResourceList back = loaded.getFirst();
        assertEquals("local-0001", back.localId);
        assertEquals("Holdstewik", back.name);
        assertEquals(list.writeCode, back.writeCode);
        assertEquals(list.readCode, back.readCode);
        assertEquals(77, back.since);
        assertEquals(list.zones, back.zones);
        assertEquals(list.timers, back.timers);
        assertEquals(list.dirtyZones, back.dirtyZones);
        assertEquals(list.dirtyTimers, back.dirtyTimers);
        assertEquals(1, back.retired.size());
        assertEquals("old-share-id", back.retired.getFirst().shareId());
        assertTrue(back.writable());
        assertEquals(list.shareId(), back.shareId());
    }

    @Test
    void localListHasNoCodesAndSavingLeavesNoTemporaryFile() throws Exception {
        ResourceStore store = new ResourceStore(directory);
        ResourceList list = new ResourceList("local-0002", "Lokal");
        store.save(list);
        ResourceList back = store.loadAll().getFirst();
        assertNull(back.writeCode);
        assertNull(back.readCode);
        assertFalse(back.shared());
        assertTrue(back.writable());
        try (var files = Files.list(directory)) {
            assertEquals(List.of("local-0002.json"),
                    files.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void readOnlyListIsNotWritable() {
        ResourceStore store = new ResourceStore(directory);
        ResourceList list = new ResourceList("local-0003", "Fremd");
        list.readCode = ResourceCodes.readCode("ABCDEFGHJKMNPQRSTVWX");
        store.save(list);
        ResourceList back = store.loadAll().getFirst();
        assertTrue(back.shared());
        assertFalse(back.writable());
    }

    @Test
    void unreadableFilesAreSkippedAndKept() throws Exception {
        ResourceStore store = new ResourceStore(directory);
        store.save(new ResourceList("local-0004", "Gut"));
        Files.writeString(directory.resolve("kaputt.json"), "{ not json", StandardCharsets.UTF_8);
        List<ResourceList> loaded = store.loadAll();
        assertEquals(1, loaded.size());
        assertTrue(Files.exists(directory.resolve("kaputt.json")));
    }

    @Test
    void missingDirectoryLoadsNothingAndDeleteRemovesTheFile() {
        ResourceStore store = new ResourceStore(directory.resolve("fehlt"));
        assertTrue(store.loadAll().isEmpty());
        ResourceList list = new ResourceList("local-0005", "Weg");
        store.save(list);
        assertEquals(1, store.loadAll().size());
        store.delete(list);
        assertTrue(store.loadAll().isEmpty());
    }
}
