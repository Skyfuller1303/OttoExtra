package de.ottoextra.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListMetaTest {
    @TempDir
    Path directory;

    private static ListMeta meta(String name, long at) {
        return new ListMeta(name, "", "schild", "blau", at);
    }

    @Test
    void validationFollowsTheProtocolLimits() {
        assertNull(ListMeta.problem(meta("Schwarzhain", 1)));
        assertNull(ListMeta.problem(new ListMeta("x".repeat(40), "y".repeat(200), "krone", "violett", 0)));
        assertEquals("name", ListMeta.problem(meta("", 1)));
        assertEquals("name", ListMeta.problem(meta("x".repeat(41), 1)));
        assertEquals("note", ListMeta.problem(new ListMeta("A", "y".repeat(201), "schild", "blau", 1)));
        assertEquals("icon", ListMeta.problem(new ListMeta("A", "", "drache", "blau", 1)));
        assertEquals("color", ListMeta.problem(new ListMeta("A", "", "schild", "pink", 1)));
        assertEquals("updatedAt", ListMeta.problem(meta("A", -1)));
        assertEquals("meta", ListMeta.problem(null));
    }

    @Test
    void everyShapeAndColourIsAccepted() {
        for (String icon : java.util.List.of("marker", "schild", "burg", "flagge", "schwert", "schluessel",
                "krone", "spitzhacke", "kristall", "zelt", "haus", "beutel")) {
            for (String color : java.util.List.of("blau", "rot", "gruen", "gold", "violett", "grau")) {
                assertNull(ListMeta.problem(new ListMeta("A", "", icon, color, 1)));
            }
        }
        assertEquals(12, ListMeta.ICONS.size());
        assertEquals(6, ListMeta.COLORS.size());
    }

    @Test
    void newerHeaderWinsTieKeepsLocalAndMissingRemoteKeepsLocal() {
        ListMeta local = meta("lokal", 100);
        assertEquals("fremd", ListMeta.merge(local, meta("fremd", 101)).name());
        assertSame(local, ListMeta.merge(local, meta("fremd", 100)));
        assertSame(local, ListMeta.merge(local, meta("alt", 99)));
        assertSame(local, ListMeta.merge(local, null));
        ListMeta remote = meta("neu", 5);
        assertSame(remote, ListMeta.merge(null, remote));
    }

    @Test
    void wireShapeHasExactlyTheProtocolFieldsAndRoundTrips() {
        ListMeta meta = new ListMeta("Nordeck", "Burg", "turm", "rot", 1_790_000_000_000L);
        JsonObject json = ResourceWire.metaJson(meta);
        assertEquals(java.util.Set.of("name", "note", "icon", "color", "updatedAt"), json.keySet());
        assertEquals(meta, ResourceWire.meta(json));
        JsonObject bad = json.deepCopy();
        bad.addProperty("icon", "drache");
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.meta(bad));
        JsonObject missing = json.deepCopy();
        missing.remove("updatedAt");
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.meta(missing));
    }

    @Test
    void listResponseCarriesTheHeaderOrNull() {
        var withMeta = ResourceShareApi.remote(JsonParser.parseString("{\"serverTime\":5,\"zones\":{},\"timers\":{},"
                + "\"meta\":{\"name\":\"A\",\"note\":\"\",\"icon\":\"krone\",\"color\":\"gold\",\"updatedAt\":9}}")
                .getAsJsonObject());
        assertEquals("krone", withMeta.meta().icon());
        var without = ResourceShareApi.remote(JsonParser.parseString(
                "{\"serverTime\":5,\"zones\":{},\"timers\":{},\"meta\":null}").getAsJsonObject());
        assertNull(without.meta());
        var invalid = ResourceShareApi.remote(JsonParser.parseString("{\"serverTime\":5,\"zones\":{},\"timers\":{},"
                + "\"meta\":{\"name\":\"\",\"note\":\"\",\"icon\":\"krone\",\"color\":\"gold\",\"updatedAt\":9}}")
                .getAsJsonObject());
        assertNull(invalid.meta());
        assertEquals(1, invalid.invalid());
    }

    @Test
    void editingAnOwnSharedListQueuesTheHeaderWithANewerTime() {
        ResourceRepository repository = new ResourceRepository(new ResourceStore(directory));
        ResourceList list = repository.createList("Alt");
        assertFalse(list.metaDirty);
        repository.share(list);
        assertTrue(list.metaDirty);
        list.metaDirty = false;
        long before = list.metaUpdatedAt;
        assertTrue(repository.updateMeta(list, "Neu", "Notiz", "krone", "gold", before - 1000));
        assertEquals("Neu", list.name);
        assertEquals("krone", list.icon);
        assertEquals("gold", list.color);
        assertEquals(before + 1, list.metaUpdatedAt);
        assertTrue(list.metaDirty);
    }

    @Test
    void localListHeaderIsNotQueuedAndInvalidInputIsRejected() {
        ResourceRepository repository = new ResourceRepository(new ResourceStore(directory));
        ResourceList list = repository.createList("Lokal");
        assertTrue(repository.updateMeta(list, "Privat", "", "spitzhacke", "grau", 10));
        assertFalse(list.metaDirty);
        assertFalse(repository.updateMeta(list, "", "", "schild", "blau", 11));
        assertFalse(repository.updateMeta(list, "A", "", "drache", "blau", 11));
        assertEquals("Privat", list.name);
    }

    @Test
    void readOnlyListCannotChangeItsHeader() {
        ResourceRepository repository = new ResourceRepository(new ResourceStore(directory));
        ResourceList list = repository.join("Fremd", null, ResourceCodes.readCode("ABCDEFGHJKMNPQRSTVWX"));
        assertEquals("flagge", list.icon);
        assertEquals("grau", list.color);
        assertFalse(repository.updateMeta(list, "Neu", "", "schild", "blau", 10));
        assertEquals("Fremd", list.name);
    }

    @Test
    void armsChoiceIsLocalAndStoredButNeverSent() throws Exception {
        ResourceStore store = new ResourceStore(directory);
        ResourceRepository repository = new ResourceRepository(store);
        ResourceList list = repository.createList("Nordeck");
        assertTrue(list.useArms);
        repository.setUseArms(list, false);
        assertFalse(store.loadAll().stream().filter(back -> back.localId.equals(list.localId))
                .findFirst().orElseThrow().useArms);
        assertFalse(ResourceWire.metaJson(list.meta()).has("useArms"));

        java.nio.file.Path file = directory.resolve(list.localId + ".json");
        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(
                java.nio.file.Files.readString(file)).getAsJsonObject();
        json.remove("useArms");
        assertTrue(ResourceStore.fromJson(json, list.localId).useArms);

        ResourceList readOnly = repository.join("Fremd", null, ResourceCodes.readCode("ABCDEFGHJKMNPQRSTVWX"));
        repository.setUseArms(readOnly, false);
        assertFalse(readOnly.useArms);
    }

    @Test
    void headerSurvivesStorage() {
        ResourceStore store = new ResourceStore(directory);
        ResourceList list = new ResourceList("local-meta", "Nordeck");
        list.note = "Burg";
        list.icon = "turm";
        list.color = "rot";
        list.metaUpdatedAt = 42;
        list.metaDirty = true;
        store.save(list);
        ResourceList back = store.loadAll().getFirst();
        assertEquals(new ListMeta("Nordeck", "Burg", "burg", "rot", 42), back.meta());
        assertEquals("burg", back.icon);
        assertTrue(back.metaDirty);
        assertTrue(back.hasPendingChanges());
    }

    @Test
    void renewalAndFirstShareQueueTheHeader() {
        ResourceRepository repository = new ResourceRepository(new ResourceStore(directory));
        ResourceList list = repository.createList("Test");
        repository.share(list);
        assertTrue(list.metaUpdatedAt > 0);
        list.metaDirty = false;
        repository.renewCode(list, System.currentTimeMillis() + 5);
        assertTrue(list.metaDirty);
    }

    @Test
    void factionNamesIgnorePlaceholdersAndPreferTheRegionsOwnName() {
        assertEquals("Haus Falk", ResourceTerritories.cleanFactionName("Haus Falk", "Anderer"));
        assertEquals("Haus Falk", ResourceTerritories.cleanFactionName("", "Haus Falk"));
        assertEquals("Haus Falk", ResourceTerritories.cleanFactionName("Unbekanntes Gefolge", "Haus Falk"));
        assertEquals("", ResourceTerritories.cleanFactionName("-", "Unknown"));
        assertEquals("", ResourceTerritories.cleanFactionName(null, null));
        assertEquals(60, ResourceTerritories.cleanFactionName("x".repeat(80), "").length());
    }
}
