package de.ottoextra.horses;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ottoextra.horses.FarmShareWire.AnimalRec;
import de.ottoextra.horses.FarmShareWire.FarmRec;
import de.ottoextra.horses.FarmShareWire.Meta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FarmShareTest {
    private static final String WRITE = "ABCDEFGHJKMNPQRSTVWX";
    /** Same values as OttoExtra-Discordbot/web/tests/farm-lists.php. */
    private static final String READ = "H5259A79BDE7501FA4E27023B";
    private static final String LIST_ID = "c2f89d4f79c832eef8803c37e01600ffefc06a4455cfe797c7338d9af277b194";
    private static final long NOW = 1_790_000_000_000L;
    private static final String UUID_A = "123e4567-e89b-42d3-a456-426614174000";
    private static final String UUID_B = "06cb65df-d251-4a03-8270-31fd416942ac";

    @TempDir Path directory;

    // ---- codes --------------------------------------------------------------------------

    @Test
    void testVectorMatchesTheService() {
        assertEquals(READ, FarmShareCodes.readCode(WRITE));
        assertEquals(LIST_ID, FarmShareCodes.listId(READ));
        assertEquals(READ, FarmShareCodes.readCode("abcde-fghjk mnpqr-stvwx"));
        assertEquals(LIST_ID, FarmShareCodes.listId(READ.toLowerCase()));
    }

    @Test
    void codeKindsAndDisplay() {
        assertEquals(FarmShareCodes.Kind.WRITE, FarmShareCodes.kind("abcde-FGHJK-mnpqr-STVWX"));
        assertEquals(FarmShareCodes.Kind.READ, FarmShareCodes.kind(READ));
        assertEquals(FarmShareCodes.Kind.READ, FarmShareCodes.kind("h5259-a79bd-e7501-fa4e2-7023b"));
        // R is the prefix of the resource lists, not of the farm lists.
        assertEquals(FarmShareCodes.Kind.INVALID, FarmShareCodes.kind("R" + READ.substring(1)));
        assertEquals(FarmShareCodes.Kind.INVALID, FarmShareCodes.kind("ABCDEFGHJKMNPQRSTVWI"));
        assertEquals(FarmShareCodes.Kind.INVALID, FarmShareCodes.kind(""));
        assertEquals(FarmShareCodes.Kind.INVALID, FarmShareCodes.kind(null));
        assertEquals("ABCDE-FGHJK-MNPQR-STVWX", FarmShareCodes.display(WRITE));
        String generated = FarmShareCodes.generateWriteCode(new Random(7));
        assertEquals(FarmShareCodes.Kind.WRITE, FarmShareCodes.kind(generated));
    }

    @Test
    void joinUrlUsesTheLinkPageAndOnlySafeBases() {
        assertEquals("https://ottoextra.dev/h/" + READ, FarmShareApi.joinUrl("https://ottoextra.dev/", READ));
        assertEquals("http://localhost:8080/h/" + READ, FarmShareApi.joinUrl("http://localhost:8080", READ));
        assertNull(FarmShareApi.baseUri("http://ottoextra.dev"));
        assertNull(FarmShareApi.baseUri("https://ottoextra.dev/api"));
        assertNull(FarmShareApi.baseUri("https://user@ottoextra.dev"));
        assertEquals("https://ottoextra.dev/h/" + READ, FarmShareApi.joinUrl("ftp://x", READ));
    }

    @Test
    void writeCodesOnlyGoToOttoextraDevOrLoopback() {
        assertNotNull(FarmShareApi.baseUri("https://ottoextra.dev"));
        assertNotNull(FarmShareApi.baseUri("https://OttoExtra.dev:443/"));
        assertNotNull(FarmShareApi.baseUri("http://127.0.0.1:8000"));
        assertNotNull(FarmShareApi.baseUri("https://localhost"));
        assertNull(FarmShareApi.baseUri("https://example.org"));
        assertNull(FarmShareApi.baseUri("https://api.ottoextra.dev"));
        assertNull(FarmShareApi.baseUri("https://ottoextra.dev.example.org"));
        assertNull(FarmShareApi.baseUri("https://ottoextra.dev:8443"));
        assertNull(FarmShareApi.baseUri("http://ottoextra.dev"));
        assertNull(FarmShareApi.baseUri("https://ottoextra.dev?x=1"));
        assertNull(FarmShareApi.baseUri(null));
        assertTrue(FarmShareApi.allowed(java.net.URI.create("https://ottoextra.dev")));
        assertFalse(FarmShareApi.allowed(java.net.URI.create("https://example.org")));
        assertFalse(FarmShareApi.allowed(null));
        // A refused base never receives the write code.
        var refused = new FarmShareApi().patch(java.net.URI.create("https://example.org"), LIST_ID, WRITE,
                new JsonObject());
        assertTrue(refused.isCompletedExceptionally());
        assertEquals("https://ottoextra.dev/h/" + READ, FarmShareApi.joinUrl("https://example.org", READ));
    }

    // ---- wire ---------------------------------------------------------------------------

    @Test
    void wireRoundTripWritesOnlyProtocolFields() {
        Meta meta = new Meta("ä".repeat(64), "ö".repeat(200), "beutel", "violett", NOW);
        FarmRec farm = new FarmRec("12345678", "A", "", "marker_gold", NOW, false);
        AnimalRec animal = new AnimalRec(UUID_A, "12345678", "ö".repeat(200), "ä".repeat(64), NOW, true);
        AnimalRec loose = new AnimalRec(UUID_B, null, "", "", NOW, false);
        JsonObject body = FarmShareWire.body(meta, Map.of(farm.id(), farm), Map.of(UUID_A, animal, UUID_B, loose));
        assertEquals(Set.of("meta", "farms", "animals"), body.keySet());
        assertEquals(Set.of("name", "note", "icon", "color", "updatedAt"), body.getAsJsonObject("meta").keySet());
        assertEquals(Set.of("id", "name", "note", "symbol", "updatedAt", "deleted"),
                body.getAsJsonObject("farms").getAsJsonObject("12345678").keySet());
        assertEquals(Set.of("farmId", "note", "label", "updatedAt", "deleted"),
                body.getAsJsonObject("animals").getAsJsonObject(UUID_A).keySet());
        assertTrue(body.getAsJsonObject("animals").getAsJsonObject(UUID_B).get("farmId").isJsonNull());

        JsonObject response = JsonParser.parseString(body.toString()).getAsJsonObject();
        response.addProperty("listId", LIST_ID);
        response.addProperty("serverTime", NOW);
        FarmShareWire.Remote remote = FarmShareWire.remote(response);
        assertEquals(meta, remote.meta());
        assertEquals(farm, remote.farms().get("12345678"));
        assertEquals(animal, remote.animals().get(UUID_A));
        assertEquals(loose, remote.animals().get(UUID_B));
        assertEquals(0, remote.invalid());
    }

    @Test
    void emptyAnswerOfAnUnknownList() {
        FarmShareWire.Remote remote = FarmShareWire.remote(JsonParser.parseString(
                "{\"listId\":\"" + LIST_ID + "\",\"serverTime\":" + NOW + ",\"meta\":null,\"farms\":{},\"animals\":{}}")
                .getAsJsonObject());
        assertNull(remote.meta());
        assertTrue(remote.farms().isEmpty());
        assertTrue(remote.animals().isEmpty());
    }

    @Test
    void invalidEntriesAreSkippedAndFutureTimesCapped() {
        String json = "{\"serverTime\":" + NOW + ",\"farms\":{"
                + "\"12345678\":{\"id\":\"12345678\",\"name\":\"A\",\"note\":\"\",\"symbol\":\"marker_gold\",\"updatedAt\":"
                + (NOW + 3_600_000L) + ",\"deleted\":false},"
                + "\"x\":{\"id\":\"x\",\"name\":\"B\",\"note\":\"\",\"symbol\":\"marker_gold\",\"updatedAt\":1,\"deleted\":false},"
                + "\"abcdefgh\":{\"id\":\"zzzzzzzz\",\"name\":\"C\",\"note\":\"\",\"symbol\":\"marker_gold\",\"updatedAt\":1,\"deleted\":false}},"
                + "\"animals\":{\"" + UUID_A.toUpperCase() + "\":{\"farmId\":null,\"note\":\"\",\"updatedAt\":1,\"deleted\":false},"
                + "\"" + UUID_B + "\":{\"farmId\":null,\"note\":\"\",\"updatedAt\":1,\"deleted\":false}}}";
        FarmShareWire.Remote remote = FarmShareWire.remote(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(Set.of("12345678"), remote.farms().keySet());
        assertEquals(NOW, remote.farms().get("12345678").updatedAt());
        assertEquals(Set.of(UUID_B), remote.animals().keySet());
        assertEquals("", remote.animals().get(UUID_B).label());
        assertEquals(3, remote.invalid());
    }

    @Test
    void validationFollowsTheProtocol() {
        assertNull(FarmShareWire.problem(new FarmRec("hof-abc12345", "x".repeat(64), "", "haus_gruen", 0, false)));
        assertEquals("name", FarmShareWire.problem(new FarmRec("hof-abc12345", "x".repeat(65), "", "haus_gruen", 0, false)));
        assertEquals("name", FarmShareWire.problem(new FarmRec("hof-abc12345", "", "", "haus_gruen", 0, false)));
        assertEquals("id", FarmShareWire.problem(new FarmRec("Hof-ABC12345", "A", "", "haus_gruen", 0, false)));
        assertEquals("id", FarmShareWire.problem(new FarmRec("short", "A", "", "haus_gruen", 0, false)));
        assertEquals("symbol", FarmShareWire.problem(new FarmRec("hof-abc12345", "A", "", "haus", 0, false)));
        assertEquals("note", FarmShareWire.problem(new FarmRec("hof-abc12345", "A", "n".repeat(201), "haus_gruen", 0, false)));
        assertEquals("uuid", FarmShareWire.problem(new AnimalRec(UUID_A.toUpperCase(), null, "", "", 0, false)));
        assertEquals("farmId", FarmShareWire.problem(new AnimalRec(UUID_A, "x", "", "", 0, false)));
        assertEquals("label", FarmShareWire.problem(new AnimalRec(UUID_A, null, "", "l".repeat(65), 0, false)));
        assertEquals("icon", FarmShareWire.problem(new Meta("A", "", "pferd", "gruen", 0)));
        assertEquals("color", FarmShareWire.problem(new Meta("A", "", "haus", "pink", 0)));
    }

    // ---- merge --------------------------------------------------------------------------

    @Test
    void largerUpdatedAtWinsAndTieKeepsTheExistingEntry() {
        FarmShareList list = joined(WRITE);
        list.farms.put("12345678", new FarmRec("12345678", "Lokal", "", "haus_gruen", 100, false));
        list.farms.put("abcdefgh", new FarmRec("abcdefgh", "Lokal", "", "haus_gruen", 100, false));
        list.dirtyFarms.add("12345678");
        list.dirtyFarms.add("abcdefgh");
        FarmShareWire.Remote remote = new FarmShareWire.Remote(5_000_000L,
                new Meta("Remote", "", "burg", "rot", 7),
                Map.of("12345678", new FarmRec("12345678", "Neu", "", "burg_rot", 101, false),
                        "abcdefgh", new FarmRec("abcdefgh", "Gleich", "", "burg_rot", 100, false),
                        "neuerhof", new FarmRec("neuerhof", "Dazu", "", "burg_rot", 1, false)),
                Map.of(UUID_A, new AnimalRec(UUID_A, "neuerhof", "Notiz", "Donar", 1, false)), 0);
        FarmShareMerge.Merged merged = FarmShareMerge.apply(list, remote, FarmShareService.SINCE_OVERLAP_MS);
        assertEquals("Neu", list.farms.get("12345678").name());
        assertEquals("Lokal", list.farms.get("abcdefgh").name());
        assertEquals(Set.of("abcdefgh"), list.dirtyFarms);
        assertEquals(Set.of("12345678", "neuerhof"), merged.farms());
        assertEquals("Remote", list.name());
        assertEquals(5_000_000L - 10_000L, list.since);
        assertEquals(List.of(UUID_A), list.animalsOf("neuerhof").stream().map(AnimalRec::uuid).toList());
        assertTrue(merged.meta());
    }

    @Test
    void oldTombstonesArePrunedUnlessQueued() {
        FarmShareList list = joined(WRITE);
        long old = NOW - AnimalBookStore.TOMBSTONE_MS - 1;
        list.farms.put("12345678", new FarmRec("12345678", "A", "", "haus_gruen", old, true));
        list.farms.put("abcdefgh", new FarmRec("abcdefgh", "A", "", "haus_gruen", old, true));
        list.dirtyFarms.add("abcdefgh");
        assertTrue(FarmShareMerge.pruneTombstones(list, NOW));
        assertEquals(Set.of("abcdefgh"), list.farms.keySet());
    }

    @Test
    void ownedListFollowsRegisterAndLocalNotes() {
        FarmShareList list = owned();
        AnimalBookStore.Farm header = new AnimalBookStore.Farm("hof-eichengrund", "key-eichen", "Eichengrund",
                "Stall", "schild_gruen", 1, false);
        list.members.add(header.id());
        Map<String, AnimalBookStore.Farm> headers = Map.of(header.id(), header);
        // Register not loaded yet: the farm is shared, animals stay untouched.
        assertTrue(FarmShareMerge.rebuildOwned(list, headers, null, Map.of(), NOW));
        assertEquals("Stall", list.farm("hof-eichengrund").note());
        assertTrue(list.animals.isEmpty());

        List<FarmShareMerge.RegisterAnimal> register = List.of(
                new FarmShareMerge.RegisterAnimal(UUID_A, "key-eichen", "Donar"),
                new FarmShareMerge.RegisterAnimal(UUID_B, "key-anderer", "Fremd"));
        assertTrue(FarmShareMerge.rebuildOwned(list, headers, register, Map.of(UUID_A, "Tochter von Wotan"), NOW + 1));
        AnimalRec donar = list.animals.get(UUID_A);
        assertEquals("hof-eichengrund", donar.farmId());
        assertEquals("Tochter von Wotan", donar.note());
        assertEquals("Donar", donar.label());
        assertFalse(list.animals.containsKey(UUID_B));
        // Unchanged state: nothing new to send.
        list.dirtyAnimals.clear();
        list.dirtyFarms.clear();
        assertFalse(FarmShareMerge.rebuildOwned(list, headers, register, Map.of(UUID_A, "Tochter von Wotan"), NOW + 2));
        assertTrue(list.dirtyAnimals.isEmpty());

        // The animal leaves the farm and the farm leaves the list: tombstones.
        assertTrue(FarmShareMerge.rebuildOwned(list, headers, List.of(), Map.of(), NOW + 3));
        assertTrue(list.animals.get(UUID_A).deleted());
        assertNull(list.animals.get(UUID_A).farmId());
        list.members.clear();
        assertTrue(FarmShareMerge.rebuildOwned(list, headers, List.of(), Map.of(), NOW + 4));
        assertTrue(list.farms.get("hof-eichengrund").deleted());
        assertTrue(list.dirtyFarms.contains("hof-eichengrund"));
    }

    @Test
    void renewWritesEverythingUnderTheNewIdAndTombstonesUnderTheOld() {
        FarmShareList list = owned();
        list.farms.put("hof-eichengrund", new FarmRec("hof-eichengrund", "Eichengrund", "", "haus_gruen", NOW, false));
        list.animals.put(UUID_A, new AnimalRec(UUID_A, "hof-eichengrund", "", "Donar", NOW, false));
        list.animals.put(UUID_B, new AnimalRec(UUID_B, null, "", "", NOW, true));
        String oldId = list.listId;
        String next = FarmShareCodes.generateWriteCode(new Random(1));
        assertTrue(FarmShareMerge.renew(list, next, NOW + 5));
        assertEquals(next, list.writeCode());
        assertEquals(FarmShareCodes.listId(FarmShareCodes.readCode(next)), list.listId());
        assertEquals(Set.of("hof-eichengrund"), list.dirtyFarms);
        assertEquals(Set.of(UUID_A), list.dirtyAnimals);
        assertTrue(list.metaDirty);
        FarmShareList.Retired retired = list.retired.getFirst();
        assertEquals(oldId, retired.listId());
        assertEquals(WRITE, retired.writeCode());
        assertTrue(retired.farms().get("hof-eichengrund").deleted());
        assertTrue(retired.animals().values().stream().allMatch(AnimalRec::deleted));
        assertTrue(retired.animals().get(UUID_A).updatedAt() > NOW);
    }

    // ---- read-only ----------------------------------------------------------------------

    @Test
    void readOnlyListsRefuseEveryChange() {
        FarmShareList list = joined(null);
        list.farms.put("12345678", new FarmRec("12345678", "A", "", "haus_gruen", NOW, false));
        assertTrue(list.readOnly());
        assertFalse(FarmShareMerge.editFarm(list, "12345678", "B", "Notiz", "burg_rot", NOW + 1));
        assertFalse(FarmShareMerge.renew(list, FarmShareCodes.generateWriteCode(), NOW + 1));
        assertEquals("A", list.farm("12345678").name());
        assertFalse(list.pending());

        FarmShareList writable = joined(WRITE);
        writable.farms.put("12345678", new FarmRec("12345678", "A", "", "haus_gruen", NOW, false));
        assertTrue(FarmShareMerge.editFarm(writable, "12345678", "B", "Notiz", "burg_rot", NOW + 1));
        assertEquals("B", writable.farm("12345678").name());
        assertTrue(writable.dirtyFarms.contains("12345678"));
    }

    @Test
    void sharedListsSurviveAReload() {
        AnimalBookStore first = new AnimalBookStore(directory.resolve("book.json"), "server-a", () -> NOW);
        FarmShareList list = joined(null);
        list.farms.put("12345678", new FarmRec("12345678", "A", "", "haus_gruen", NOW, false));
        list.animals.put(UUID_A, new AnimalRec(UUID_A, "12345678", "Notiz", "Donar", NOW, false));
        assertTrue(first.addShareList(list));
        FarmShareList broken = joined(null);
        broken.listId = "0".repeat(64);
        assertFalse(first.addShareList(broken));

        AnimalBookStore second = new AnimalBookStore(directory.resolve("book.json"), "server-a", () -> NOW);
        second.load();
        assertEquals(1, second.shareLists().size());
        FarmShareList loaded = second.shareLists().getFirst();
        assertTrue(loaded.readOnly());
        assertEquals(READ, loaded.readCode());
        assertEquals("Notiz", loaded.animalsOf("12345678").getFirst().note());
    }

    private static FarmShareList joined(String writeCode) {
        FarmShareList list = new FarmShareList();
        list.writeCode = writeCode;
        list.readCode = READ;
        list.listId = LIST_ID;
        return list;
    }

    private static FarmShareList owned() {
        FarmShareList list = joined(WRITE);
        list.owned = true;
        list.name = "Eichengrund";
        return list;
    }
}
