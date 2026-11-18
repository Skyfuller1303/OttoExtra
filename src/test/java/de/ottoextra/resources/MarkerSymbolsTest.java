package de.ottoextra.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerSymbolsTest {
    @TempDir
    Path directory;

    private static ZoneRecord zone(String id, String type, String symbol, boolean deleted) {
        return new ZoneRecord(id, "Punkt " + id, type, "minecraft:overworld", "Nord",
                ResourceZone.point(1, 2, 3, 4), null, "", 1000, deleted, symbol);
    }

    @Test
    void keysCombineAKnownShapeAndColour() {
        assertTrue(MarkerSymbols.valid("marker_rot"));
        assertTrue(MarkerSymbols.valid("schluessel_violett"));
        assertFalse(MarkerSymbols.valid("marker_pink"));
        assertFalse(MarkerSymbols.valid("stecknadel_rot"));
        assertFalse(MarkerSymbols.valid("kreis_rot"));
        assertFalse(MarkerSymbols.valid(""));
        assertFalse(MarkerSymbols.valid(null));
        assertEquals("schluessel", MarkerSymbols.shape("schluessel_gold"));
        assertEquals("gold", MarkerSymbols.color("schluessel_gold"));
        assertEquals(MarkerSymbols.PIN, MarkerSymbols.shape("unsinn"));
        assertEquals(MarkerSymbols.DEFAULT_COLOR, MarkerSymbols.color("unsinn"));
        assertEquals(12, MarkerSymbols.SHAPES.size());
        assertEquals("marker_violett", MarkerSymbols.DEFAULT);
        assertEquals(6, MarkerSymbols.COLORS.size());
    }

    @Test
    void allSymbolsShareOneFolderAndOldNamesFindTheirNewTexture() {
        assertEquals("textures/gui/symbols/marker_blau.png", MarkerSymbols.texturePath("marker_blau"));
        assertEquals("textures/gui/symbols/krone_rot.png", MarkerSymbols.texturePath("krone_rot"));
        assertEquals("textures/gui/symbols/marker_violett.png", MarkerSymbols.texturePath("x"));
        assertEquals("textures/gui/symbols/burg_gold.png", MarkerSymbols.texturePath("turm_gold"));
        assertEquals("textures/gui/symbols/marker_rot.png", MarkerSymbols.texturePath("stecknadel_rot"));
    }

    @Test
    void oldShapeNamesAreReadAsTheirNewNames() {
        assertEquals("burg", ListMeta.canonicalIcon("turm"));
        assertEquals("flagge", ListMeta.canonicalIcon("fahne"));
        assertEquals("marker", ListMeta.canonicalIcon("stecknadel"));
        assertEquals("krone", ListMeta.canonicalIcon("krone"));
        assertEquals("drache", ListMeta.canonicalIcon("drache"));
        assertNull(ListMeta.canonicalIcon(null));
        assertEquals("burg_rot", MarkerSymbols.canonical("turm_rot"));
        assertEquals("flagge_grau", MarkerSymbols.canonical("fahne_grau"));
        assertEquals("marker_gold", MarkerSymbols.canonical("stecknadel_gold"));
        assertEquals("krone", MarkerSymbols.canonical("krone"));
        assertEquals("burg", new ListMeta("A", "", "turm", "blau", 1).icon());
        assertNull(ListMeta.problem(new ListMeta("A", "", "fahne", "blau", 1)));
        assertEquals("flagge_rot", zone("zone-0001", ResourcePolicy.MARKER, "fahne_rot", false).symbol());
        assertNull(zone("zone-0001", ResourcePolicy.MARKER, "stecknadel_violett", false).symbol());
        assertEquals("marker_rot", zone("zone-0001", ResourcePolicy.MARKER, "stecknadel_rot", false).symbol());
    }

    @Test
    void all72SymbolsAreValidAndShipAsTextures() {
        int count = 0;
        for (String shape : MarkerSymbols.SHAPES) {
            for (String color : MarkerSymbols.COLORS) {
                String key = MarkerSymbols.key(shape, color);
                assertTrue(MarkerSymbols.valid(key), key);
                assertNull(ListMeta.problem(new ListMeta("A", "", shape, color, 1)), key);
                assertTrue(getClass().getClassLoader().getResource(
                        "assets/ottoextra_resources/" + MarkerSymbols.texturePath(key)) != null, key);
                count++;
            }
        }
        assertEquals(72, count);
    }

    @Test
    void wireSendsOnlyNewNamesForOldSymbolsFromTheService() {
        JsonObject json = ResourceWire.zoneJson(zone("zone-0001", ResourcePolicy.MARKER, "krone_gold", false));
        json.addProperty("symbol", "turm_blau");
        ZoneRecord back = ResourceWire.zone(json);
        assertEquals("burg_blau", back.symbol());
        assertEquals("burg_blau", ResourceWire.zoneJson(back).get("symbol").getAsString());
    }

    @Test
    void storedOldNamesAreMigratedOnLoadAndTheFileIsRewritten() throws Exception {
        ResourceStore store = new ResourceStore(directory.resolve("lists"));
        ResourceList list = new ResourceList("local-0001", "Nordeck");
        list.zones.put("zone-0001", zone("zone-0001", ResourcePolicy.MARKER, null, false));
        list.zones.put("zone-0002", zone("zone-0002", ResourcePolicy.MARKER, null, false));
        assertTrue(store.save(list));
        Path file = directory.resolve("lists").resolve("local-0001.json");
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        json.addProperty("icon", "turm");
        json.getAsJsonObject("zones").getAsJsonObject("zone-0001").addProperty("symbol", "fahne_rot");
        json.getAsJsonObject("zones").getAsJsonObject("zone-0002").addProperty("symbol", "stecknadel_violett");
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        assertTrue(ResourceStore.usesOldSymbolNames(json));

        ResourceList back = store.loadAll().getFirst();
        assertEquals("burg", back.icon);
        assertEquals("flagge_rot", back.zones.get("zone-0001").symbol());
        assertNull(back.zones.get("zone-0002").symbol());
        JsonObject saved = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(ResourceStore.usesOldSymbolNames(saved));
        assertEquals("burg", saved.get("icon").getAsString());
    }

    @Test
    void recordKeepsTheSymbolOnlyOnMarkers() {
        assertEquals("burg_rot", zone("zone-0001", ResourcePolicy.MARKER, "burg_rot", false).symbol());
        assertNull(zone("zone-0001", "IRON_MINE", "burg_rot", false).symbol());
        assertNull(zone("zone-0001", ResourcePolicy.MARKER, "burg_pink", false).symbol());
        assertNull(zone("zone-0001", ResourcePolicy.MARKER, MarkerSymbols.DEFAULT, false).symbol());
        assertNull(new ZoneRecord("zone-0001", "A", ResourcePolicy.MARKER, "minecraft:overworld", "",
                ResourceZone.point(1, 2, 3, 4), null, "", 1, false).symbol());
        ZoneRecord marker = zone("zone-0001", ResourcePolicy.MARKER, "burg_rot", false);
        assertEquals("burg_rot", marker.tombstone(5).symbol());
        assertEquals("burg_rot", marker.withUpdatedAt(5).symbol());
    }

    @Test
    void wireSendsTheSymbolOrNullAndReadsItBack() {
        ZoneRecord marker = zone("zone-0001", ResourcePolicy.MARKER, "krone_gold", false);
        JsonObject json = ResourceWire.zoneJson(marker);
        assertEquals("krone_gold", json.get("symbol").getAsString());
        assertEquals(marker, ResourceWire.zone(json));

        JsonObject mine = ResourceWire.zoneJson(zone("zone-0002", "IRON_MINE", "krone_gold", false));
        assertTrue(mine.has("symbol"));
        assertTrue(mine.get("symbol").isJsonNull());
        assertNull(ResourceWire.zone(mine).symbol());
    }

    @Test
    void missingNullOrUnknownSymbolFromTheServiceKeepsTheZoneWithoutSymbol() {
        JsonObject json = ResourceWire.zoneJson(zone("zone-0001", ResourcePolicy.MARKER, "krone_gold", false));
        json.remove("symbol");
        assertNull(ResourceWire.zone(json).symbol());
        json.add("symbol", com.google.gson.JsonNull.INSTANCE);
        assertNull(ResourceWire.zone(json).symbol());
        for (String bad : new String[] {"krone_pink", "kreis_rot", "", "krone"}) {
            json.addProperty("symbol", bad);
            ZoneRecord back = ResourceWire.zone(json);
            assertNull(back.symbol(), bad);
            assertEquals("Punkt zone-0001", back.name());
        }
        json.addProperty("symbol", 5);
        assertNull(ResourceWire.zone(json).symbol());
    }

    @Test
    void changingTheSymbolBumpsTheUpdateTimeAndIsStoredWithTheZone() {
        ResourceRepository repository = new ResourceRepository(new ResourceStore(directory.resolve("lists")));
        ResourceList list = repository.createList("Nordeck");
        ZoneRecord marker = repository.addZone(list, "Turm", ResourcePolicy.MARKER, "minecraft:overworld", "",
                ResourceZone.point(1, 64, 1, 3), null, "", "burg_rot", 1000);
        assertEquals("burg_rot", marker.symbol());
        list.dirtyZones.clear();
        ZoneRecord changed = marker.withSymbol("flagge_gold", marker.updatedAt());
        assertTrue(repository.updateZone(list, changed, 1000));
        ZoneRecord stored = list.zones.get(marker.id());
        assertEquals("flagge_gold", stored.symbol());
        assertTrue(stored.updatedAt() > marker.updatedAt());
        ResourceList back = new ResourceStore(directory.resolve("lists")).loadAll().getFirst();
        assertEquals("flagge_gold", back.zones.get(marker.id()).symbol());
    }

    @Test
    void oldLocalSymbolsMoveIntoTheZonesOnceAndAreThenDropped() throws Exception {
        ResourceStore store = new ResourceStore(directory.resolve("lists"));
        ResourceList list = new ResourceList("local-0001", "Nordeck");
        list.writeCode = "ABCDEFGHJKMNPQRSTVWX";
        list.readCode = ResourceCodes.readCode(list.writeCode);
        list.zones.put("zone-0001", zone("zone-0001", ResourcePolicy.MARKER, null, false));
        list.zones.put("zone-0002", zone("zone-0002", ResourcePolicy.MARKER, null, true));
        list.zones.put("zone-0003", zone("zone-0003", "IRON_MINE", null, false));
        list.zones.put("zone-0004", zone("zone-0004", ResourcePolicy.MARKER, "burg_blau", false));
        assertTrue(store.save(list));
        Path file = directory.resolve("lists").resolve("local-0001.json");
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject legacy = new JsonObject();
        legacy.addProperty("zone-0001", "krone_gold");
        legacy.addProperty("zone-0002", "krone_gold");
        legacy.addProperty("zone-0003", "krone_gold");
        legacy.addProperty("zone-0004", "krone_gold");
        legacy.addProperty("zone-0009", "krone_gold");
        json.add(ResourceStore.LEGACY_MARKER_SYMBOLS, legacy);
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);

        ResourceList back = store.loadAll().getFirst();
        assertEquals("krone_gold", back.zones.get("zone-0001").symbol());
        assertTrue(back.zones.get("zone-0001").updatedAt() > 1000);
        assertTrue(back.dirtyZones.contains("zone-0001"));
        assertNull(back.zones.get("zone-0002").symbol());
        assertNull(back.zones.get("zone-0003").symbol());
        assertEquals("burg_blau", back.zones.get("zone-0004").symbol());
        assertFalse(back.dirtyZones.contains("zone-0004"));
        assertFalse(Files.readString(file, StandardCharsets.UTF_8).contains(ResourceStore.LEGACY_MARKER_SYMBOLS));
        long migratedAt = back.zones.get("zone-0001").updatedAt();
        assertEquals(migratedAt, store.loadAll().getFirst().zones.get("zone-0001").updatedAt());
    }

    @Test
    void readOnlyListsTakeOldSymbolsWithoutANewUpdateTime() {
        ResourceList list = new ResourceList("local-0001", "Fremd");
        list.readCode = "R" + "A".repeat(24);
        list.zones.put("zone-0001", zone("zone-0001", ResourcePolicy.MARKER, null, false));
        JsonObject json = new JsonObject();
        JsonObject legacy = new JsonObject();
        legacy.addProperty("zone-0001", "schild_rot");
        json.add(ResourceStore.LEGACY_MARKER_SYMBOLS, legacy);
        ResourceStore.migrateMarkerSymbols(json, list, 99_000);
        assertEquals("schild_rot", list.zones.get("zone-0001").symbol());
        assertEquals(1000, list.zones.get("zone-0001").updatedAt());
        assertTrue(list.dirtyZones.isEmpty());
    }
}
