package de.ottoextra.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceRepositoryTest {
    private static final long NOW = 1_790_000_000_000L;
    private static final long MINUTE = 60_000L;
    @TempDir
    Path directory;

    private ResourceRepository repository() {
        return new ResourceRepository(new ResourceStore(directory));
    }

    private static ZoneRecord add(ResourceRepository repository, ResourceList list, String type) {
        return repository.addZone(list, "Mine", type, "minecraft:overworld", "Nord",
                ResourceZone.point(1, 64, 1, 3), null, "", NOW);
    }

    @Test
    void localChangesAreNotQueuedForUpload() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Lokal");
        ZoneRecord zone = add(repository, list, "IRON_MINE");
        repository.startTimer(list, zone.id(), "Anna", NOW);
        assertTrue(list.dirtyZones.isEmpty());
        assertTrue(list.dirtyTimers.isEmpty());
    }

    @Test
    void sharedListQueuesEveryChange() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Geteilt");
        repository.share(list);
        assertTrue(list.shared());
        assertEquals(20, list.writeCode.length());
        ZoneRecord zone = add(repository, list, "IRON_MINE");
        assertEquals(java.util.Set.of(zone.id()), list.dirtyZones);
        repository.startTimer(list, zone.id(), null, NOW);
        assertEquals(java.util.Set.of(zone.id()), list.dirtyTimers);
    }

    @Test
    void readOnlyListRejectsEveryChange() {
        ResourceRepository repository = repository();
        ResourceList own = repository.createList("Eigen");
        ZoneRecord zone = add(repository, own, "IRON_MINE");
        ResourceList readOnly = repository.join("Fremd", null, ResourceCodes.readCode("ABCDEFGHJKMNPQRSTVWX"));
        readOnly.zones.put(zone.id(), zone);
        assertFalse(readOnly.writable());
        assertNull(add(repository, readOnly, "APIARY"));
        assertFalse(repository.startTimer(readOnly, zone.id(), null, NOW));
        assertFalse(repository.resetTimer(readOnly, zone.id(), NOW));
        assertFalse(repository.deleteZone(readOnly, zone.id(), NOW));
        assertFalse(repository.updateZone(readOnly, zone, NOW));
        assertTrue(readOnly.dirtyZones.isEmpty());
    }

    @Test
    void timerStartResetAndShift() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Test");
        ZoneRecord zone = add(repository, list, "APIARY");
        assertTrue(repository.startTimer(list, zone.id(), "Anna", NOW));
        assertEquals(NOW, list.timers.get(zone.id()).depletedAt());
        assertEquals("Anna", list.timers.get(zone.id()).by());
        assertEquals("REGENERATING", repository.snapshots(NOW + MINUTE).getFirst().status());
        // Remaining time grows by five minutes.
        assertTrue(repository.shiftTimer(list, zone.id(), 5 * MINUTE, NOW + 10 * MINUTE));
        assertEquals(NOW + 5 * MINUTE, list.timers.get(zone.id()).depletedAt());
        // Shrinking past the full duration leaves a timer that has run out.
        assertTrue(repository.shiftTimer(list, zone.id(), -60 * MINUTE, NOW + 10 * MINUTE));
        assertEquals("READY", repository.snapshots(NOW + 10 * MINUTE).getFirst().status());
        assertTrue(repository.resetTimer(list, zone.id(), NOW + 11 * MINUTE));
        assertNull(list.timers.get(zone.id()).depletedAt());
        assertNull(list.timers.get(zone.id()).by());
        assertFalse(repository.shiftTimer(list, zone.id(), MINUTE, NOW));
    }

    @Test
    void shiftCannotMoveTheStartIntoTheFuture() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Test");
        ZoneRecord zone = add(repository, list, "CLAY_PIT");
        repository.startTimer(list, zone.id(), null, NOW);
        repository.shiftTimer(list, zone.id(), 500 * MINUTE, NOW + MINUTE);
        assertEquals(NOW + MINUTE, list.timers.get(zone.id()).depletedAt());
    }

    @Test
    void invalidByNameIsNotStored() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Test");
        ZoneRecord zone = add(repository, list, "CLAY_PIT");
        repository.startTimer(list, zone.id(), "not valid!", NOW);
        assertNull(list.timers.get(zone.id()).by());
    }

    @Test
    void updateTimesAlwaysAdvanceEvenWithSkewedClock() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Test");
        ZoneRecord zone = add(repository, list, "CLAY_PIT");
        repository.startTimer(list, zone.id(), null, NOW);
        repository.resetTimer(list, zone.id(), NOW - 5_000);
        assertEquals(NOW + 1, list.timers.get(zone.id()).updatedAt());
        assertEquals(10, ResourceRepository.stamp(9, 3));
        assertEquals(50, ResourceRepository.stamp(9, 50));
    }

    @Test
    void deleteLeavesATombstoneAndDropsTheTimer() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Test");
        repository.share(list);
        ZoneRecord zone = add(repository, list, "CLAY_PIT");
        repository.startTimer(list, zone.id(), null, NOW);
        assertTrue(repository.deleteZone(list, zone.id(), NOW + 1_000));
        assertTrue(list.zones.get(zone.id()).deleted());
        assertTrue(list.zones.get(zone.id()).updatedAt() > zone.updatedAt());
        assertFalse(list.timers.containsKey(zone.id()));
        assertTrue(list.dirtyZones.contains(zone.id()));
        assertTrue(repository.snapshots(NOW).isEmpty());
        assertEquals(0, list.liveZoneCount());
    }

    @Test
    void sharingAnExistingListQueuesAllLiveEntries() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Alt");
        ZoneRecord first = add(repository, list, "IRON_MINE");
        ZoneRecord second = add(repository, list, "APIARY");
        repository.deleteZone(list, second.id(), NOW + 5);
        repository.startTimer(list, first.id(), null, NOW);
        repository.share(list);
        assertEquals(java.util.Set.of(first.id()), list.dirtyZones);
        assertEquals(java.util.Set.of(first.id()), list.dirtyTimers);
        assertEquals(0L, list.since);
    }

    @Test
    void renewCodeMovesTheListAndRetiresTheOldIdentity() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Test");
        repository.share(list);
        ZoneRecord zone = add(repository, list, "IRON_MINE");
        repository.startTimer(list, zone.id(), null, NOW);
        list.dirtyZones.clear();
        list.dirtyTimers.clear();
        String oldWrite = list.writeCode;
        String oldShareId = list.shareId();
        assertTrue(repository.renewCode(list, NOW + 1_000));
        assertFalse(oldWrite.equals(list.writeCode));
        assertFalse(oldShareId.equals(list.shareId()));
        assertEquals(java.util.Set.of(zone.id()), list.dirtyZones);
        assertEquals(java.util.Set.of(zone.id()), list.dirtyTimers);
        assertEquals(1, list.retired.size());
        ResourceList.Retired retired = list.retired.getFirst();
        assertEquals(oldShareId, retired.shareId());
        assertEquals(oldWrite, retired.writeCode());
        ZoneRecord tombstone = retired.tombstones().get(zone.id());
        assertTrue(tombstone.deleted());
        assertTrue(tombstone.updatedAt() > zone.updatedAt());
        // The live copy under the new id is untouched.
        assertFalse(list.zones.get(zone.id()).deleted());
    }

    @Test
    void renewCodeNeedsTheWriteCode() {
        ResourceRepository repository = repository();
        ResourceList readOnly = repository.join("Fremd", null, ResourceCodes.readCode("ABCDEFGHJKMNPQRSTVWX"));
        assertFalse(repository.renewCode(readOnly, NOW));
        assertFalse(repository.renewCode(repository.createList("Lokal"), NOW));
    }

    @Test
    void joiningWithWriteCodeUpgradesAReadOnlyList() {
        ResourceRepository repository = repository();
        String write = "ABCDEFGHJKMNPQRSTVWX";
        ResourceList list = repository.join("Fremd", null, ResourceCodes.readCode(write));
        assertNotNull(repository.listByShareId(ResourceCodes.listId(ResourceCodes.readCode(write))));
        repository.upgrade(list, write);
        assertTrue(list.writable());
    }

    @Test
    void entriesAndListsSurviveARestart() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Bleibt");
        ZoneRecord zone = add(repository, list, "PEAT_BOG");
        repository.startTimer(list, zone.id(), null, NOW);
        ResourceRepository reloaded = repository();
        assertEquals(1, reloaded.lists().size());
        ResourceList back = reloaded.lists().getFirst();
        assertEquals("Bleibt", back.name);
        assertEquals(NOW, back.timers.get(zone.id()).depletedAt());
        assertEquals(back, reloaded.listOfZone(zone.id()));
        repository.remove(list);
        assertTrue(repository().lists().isEmpty());
    }

    @Test
    void listIsLimitedToThreeHundredEntries() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Voll");
        for (int index = 0; index < ResourceWire.MAX_ZONES; index++) {
            assertNotNull(add(repository, list, "APIARY"));
        }
        assertNull(add(repository, list, "APIARY"));
    }

    @Test
    void snapshotsCarryListAndTimerInformation() {
        ResourceRepository repository = repository();
        ResourceList list = repository.createList("Holdstewik");
        ZoneRecord zone = add(repository, list, "IRON_MINE");
        repository.startTimer(list, zone.id(), null, NOW);
        ResourceSnapshot snapshot = repository.snapshots(NOW + 130 * MINUTE).getFirst();
        assertEquals(zone.id(), snapshot.id());
        assertEquals(list.localId, snapshot.listId());
        assertEquals("Holdstewik", snapshot.listName());
        assertEquals("Nord", snapshot.territoryName());
        assertEquals(70, snapshot.regenerationPercentage());
        assertTrue(snapshot.writable());
        List<ResourceSnapshot> markers = List.of(snapshot);
        assertFalse(markers.getFirst().markerOnly());
    }

    @Test
    void importedListIsIndependentOfTheSource() {
        ResourceRepository repository = repository();
        ZoneRecord record = new ZoneRecord(ResourceWire.newId(), "Eingefügt", "APIARY",
                "minecraft:overworld", "Nord", ResourceZone.point(1, 1, 1, 3), null, "", NOW, false);
        ResourceList list = repository.importZones("Import", List.of(record));
        assertEquals(1, list.liveZoneCount());
        assertFalse(list.shared());
        assertTrue(list.timers.isEmpty());
    }
}
