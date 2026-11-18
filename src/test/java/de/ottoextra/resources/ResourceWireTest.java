package de.ottoextra.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceWireTest {
    private static ZoneRecord point() {
        return new ZoneRecord("11111111-aaaa", "Nordmine", "IRON_MINE", "minecraft:overworld",
                "Holdstewik", ResourceZone.point(10, 64, -20, 3), null, "", 1_790_000_000_000L, false);
    }

    private static ZoneRecord cuboid() {
        return new ZoneRecord("22222222-bbbb", "Tiefe Salzmine", "SALT_MINE", "minecraft:overworld",
                "", ResourceZone.cuboid(new BlockPos(5, 10, 20), new BlockPos(-4, -30, 2)), 90,
                "Eingang im Norden", 1_790_000_000_001L, true);
    }

    @Test
    void zoneJsonHasExactlyTheProtocolFields() {
        JsonObject json = ResourceWire.zoneJson(point());
        assertEquals(java.util.Set.of("id", "name", "type", "dimension", "territory", "zone",
                "customMinutes", "note", "updatedAt", "deleted", "symbol"), json.keySet());
        assertTrue(json.get("customMinutes").isJsonNull());
        assertTrue(json.get("symbol").isJsonNull());
        assertEquals("POINT", json.getAsJsonObject("zone").get("type").getAsString());
        assertEquals(java.util.Set.of("type", "x", "y", "z", "radius"),
                json.getAsJsonObject("zone").keySet());
        JsonObject box = ResourceWire.zoneJson(cuboid()).getAsJsonObject("zone");
        assertEquals(java.util.Set.of("type", "min", "max"), box.keySet());
        assertEquals(-4, box.getAsJsonObject("min").get("x").getAsInt());
        assertEquals(20, box.getAsJsonObject("max").get("z").getAsInt());
    }

    @Test
    void timerJsonHasExactlyTheProtocolFieldsAndKeepsNulls() {
        JsonObject json = ResourceWire.timerJson(new TimerRecord(null, null, 5));
        assertEquals(java.util.Set.of("depletedAt", "by", "updatedAt"), json.keySet());
        assertTrue(json.get("depletedAt").isJsonNull());
        assertTrue(json.get("by").isJsonNull());
        TimerRecord back = ResourceWire.timer(ResourceWire.timerJson(new TimerRecord(99L, "Anna_1", 7)));
        assertEquals(new TimerRecord(99L, "Anna_1", 7), back);
    }

    @Test
    void zonesRoundTripThroughJson() {
        for (ZoneRecord record : List.of(point(), cuboid())) {
            assertEquals(record, ResourceWire.zone(ResourceWire.zoneJson(record)));
        }
    }

    @Test
    void validationRejectsEveryOutOfRangeField() {
        ZoneRecord ok = point();
        assertNull(ResourceWire.problem(ok));
        assertEquals("id", ResourceWire.problem(new ZoneRecord("SHORT", ok.name(), ok.type(),
                ok.dimension(), "", ok.zone(), null, "", 1, false)));
        assertEquals("name", ResourceWire.problem(new ZoneRecord(ok.id(), "", ok.type(),
                ok.dimension(), "", ok.zone(), null, "", 1, false)));
        assertEquals("name", ResourceWire.problem(new ZoneRecord(ok.id(), "x".repeat(61), ok.type(),
                ok.dimension(), "", ok.zone(), null, "", 1, false)));
        assertEquals("type", ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), "GOLD_MINE",
                ok.dimension(), "", ok.zone(), null, "", 1, false)));
        assertEquals("note", ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), ok.type(),
                ok.dimension(), "", ok.zone(), null, "x".repeat(201), 1, false)));
        assertEquals("territory", ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), ok.type(),
                ok.dimension(), "x".repeat(61), ok.zone(), null, "", 1, false)));
        assertEquals("customMinutes", ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), ok.type(),
                ok.dimension(), "", ok.zone(), 0, "", 1, false)));
        assertEquals("customMinutes", ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), ok.type(),
                ok.dimension(), "", ok.zone(), 10_081, "", 1, false)));
        assertNull(ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), ok.type(),
                ok.dimension(), "", ok.zone(), 10_080, "", 1, false)));
        assertEquals("zone", ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), ok.type(),
                ok.dimension(), "", ResourceZone.point(30_000_001, 0, 0, 3), null, "", 1, false)));
        assertEquals("zone", ResourceWire.problem(new ZoneRecord(ok.id(), ok.name(), ok.type(),
                ok.dimension(), "", ResourceZone.point(0, 2049, 0, 3), null, "", 1, false)));
        assertThrows(IllegalArgumentException.class, () -> ResourceZone.point(0, 0, 0, 65));
    }

    @Test
    void parsingRejectsInvalidZones() {
        JsonObject json = ResourceWire.zoneJson(point());
        json.addProperty("type", "NOPE");
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.zone(json));
        JsonObject missing = ResourceWire.zoneJson(point());
        missing.remove("updatedAt");
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.zone(missing));
        JsonObject timer = new JsonObject();
        timer.addProperty("updatedAt", 1);
        timer.addProperty("by", "bad name!");
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.timer(timer));
    }

    @Test
    void textExportDropsTimersTombstonesAndIdentity() {
        String text = ResourceWire.exportText("Holdstewik", List.of(point(), cuboid()));
        JsonObject root = JsonParser.parseString(text).getAsJsonObject();
        assertEquals(1, root.getAsJsonArray("zones").size());
        JsonObject zone = root.getAsJsonArray("zones").get(0).getAsJsonObject();
        assertFalse(zone.has("id"));
        assertFalse(zone.has("updatedAt"));
        assertFalse(zone.has("depletedAt"));
        assertFalse(text.contains("\n"));
    }

    @Test
    void textRoundTripGivesTheSameEntries() {
        ZoneRecord second = new ZoneRecord("33333333-cccc", "Kohlemine", "COAL_MINE", "minecraft:overworld",
                "Holdstewik", ResourceZone.cuboid(new BlockPos(1, 2, 3), new BlockPos(40, 50, 60)), 75,
                "Tor \"Süd\"", 5, false);
        String text = ResourceWire.exportText("Holdstewik", List.of(point(), second));
        AtomicInteger counter = new AtomicInteger();
        ResourceWire.ImportedList imported = ResourceWire.importText(text,
                () -> "imported-id-" + counter.incrementAndGet(), 123_456L);
        assertEquals("Holdstewik", imported.name());
        assertEquals(0, imported.skipped());
        assertEquals(2, imported.zones().size());
        List<ZoneRecord> expected = new ArrayList<>(List.of(point(), second));
        for (int index = 0; index < 2; index++) {
            ZoneRecord got = imported.zones().get(index);
            ZoneRecord want = expected.get(index);
            assertEquals(want.name(), got.name());
            assertEquals(want.type(), got.type());
            assertEquals(want.dimension(), got.dimension());
            assertEquals(want.territory(), got.territory());
            assertEquals(want.zone(), got.zone());
            assertEquals(want.customMinutes(), got.customMinutes());
            assertEquals(want.note(), got.note());
            assertEquals(123_456L, got.updatedAt());
            assertFalse(got.deleted());
            assertNotNull(got.id());
        }
        assertEquals("imported-id-1", imported.zones().get(0).id());
    }

    @Test
    void importSkipsInvalidEntriesAndRejectsGarbage() {
        String text = "{\"format\":\"ottoextra-resources-list\",\"version\":1,\"name\":\"X\",\"zones\":["
                + "{\"name\":\"\",\"type\":\"IRON_MINE\",\"dimension\":\"d\",\"zone\":{\"type\":\"POINT\",\"x\":1,\"y\":1,\"z\":1,\"radius\":3}},"
                + "{\"name\":\"Gut\",\"type\":\"APIARY\",\"dimension\":\"d\",\"zone\":{\"type\":\"POINT\",\"x\":1,\"y\":1,\"z\":1,\"radius\":3}}]}";
        ResourceWire.ImportedList imported = ResourceWire.importText("  " + text + "\n", ResourceWire::newId, 1);
        assertEquals(1, imported.zones().size());
        assertEquals(1, imported.skipped());
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.importText("hallo", ResourceWire::newId, 1));
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.importText("{\"zones\":[]}", ResourceWire::newId, 1));
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.importText("", ResourceWire::newId, 1));
        assertThrows(IllegalArgumentException.class, () -> ResourceWire.importText(null, ResourceWire::newId, 1));
    }

    @Test
    void generatedIdsMatchTheProtocolPattern() {
        for (int index = 0; index < 20; index++) {
            assertTrue(ResourceWire.newId().matches("[a-z0-9-]{8,40}"));
        }
    }
}
