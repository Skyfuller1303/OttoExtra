package de.ottoextra.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceShareApiTest {
    @Test
    void acceptsHttpsAndHttpOnlyForLocalhost() {
        assertEquals("https://ottoextra.dev", ResourceShareApi.baseUri("https://ottoextra.dev").toString());
        assertEquals("https://ottoextra.dev", ResourceShareApi.baseUri(" https://ottoextra.dev/ ").toString());
        assertEquals("http://localhost:8080", ResourceShareApi.baseUri("http://localhost:8080").toString());
        assertNotNull(ResourceShareApi.baseUri("http://127.0.0.1:9000"));
        assertNull(ResourceShareApi.baseUri("http://ottoextra.dev"));
        assertNull(ResourceShareApi.baseUri("ftp://ottoextra.dev"));
        assertNull(ResourceShareApi.baseUri("https://user:pw@ottoextra.dev"));
        assertNull(ResourceShareApi.baseUri("https://ottoextra.dev/api"));
        assertNull(ResourceShareApi.baseUri("https://ottoextra.dev?x=1"));
        assertNull(ResourceShareApi.baseUri("ottoextra.dev"));
        assertNull(ResourceShareApi.baseUri(""));
        assertNull(ResourceShareApi.baseUri(null));
    }

    @Test
    void linkUsesTheConfiguredBase() {
        assertEquals("https://ottoextra.dev/r/ABC", ResourceShareApi.joinUrl("https://ottoextra.dev", "ABC"));
        assertEquals("https://ottoextra.dev/r/ABC", ResourceShareApi.joinUrl("kaputt", "ABC"));
    }

    @Test
    void parsesAListResponseAndSkipsInvalidEntries() {
        JsonObject good = ResourceWire.zoneJson(new ZoneRecord("11111111-aaaa", "Mine", "APIARY",
                "minecraft:overworld", "Nord", ResourceZone.point(1, 2, 3, 3), null, "", 1_000, false));
        JsonObject bad = good.deepCopy();
        bad.addProperty("type", "NOPE");
        JsonObject root = new JsonObject();
        root.addProperty("listId", "x");
        root.addProperty("serverTime", 1_790_000_000_000L);
        JsonObject zones = new JsonObject();
        zones.add("11111111-aaaa", good);
        zones.add("22222222-bbbb", bad);
        root.add("zones", zones);
        JsonObject timers = new JsonObject();
        timers.add("11111111-aaaa", ResourceWire.timerJson(new TimerRecord(5L, "Anna", 6)));
        root.add("timers", timers);
        var remote = ResourceShareApi.remote(root);
        assertEquals(1, remote.zones().size());
        assertEquals(1, remote.timers().size());
        assertEquals(1, remote.invalid());
        assertEquals(1_790_000_000_000L, remote.serverTime());
    }

    @Test
    void unknownListIsAnEmptyListNotAnError() {
        var remote = ResourceShareApi.remote(JsonParser.parseString(
                "{\"listId\":\"x\",\"serverTime\":5,\"zones\":{},\"timers\":{}}").getAsJsonObject());
        assertEquals(0, remote.zones().size());
        assertEquals(0, remote.invalid());
    }

    @Test
    void entriesFromTheFutureAreClampedToServerTime() {
        JsonObject zone = ResourceWire.zoneJson(new ZoneRecord("11111111-aaaa", "Mine", "APIARY",
                "minecraft:overworld", "", ResourceZone.point(1, 2, 3, 3), null, "", 9_000_000, false));
        JsonObject root = new JsonObject();
        root.addProperty("serverTime", 1_000L);
        JsonObject zones = new JsonObject();
        zones.add("11111111-aaaa", zone);
        root.add("zones", zones);
        assertEquals(1_000L, ResourceShareApi.remote(root).zones().get("11111111-aaaa").updatedAt());
    }

    @Test
    void errorResponsesCarryTheServerMessageAndStatus() {
        ResourceShareApi.ShareException error = assertThrows(ResourceShareApi.ShareException.class,
                () -> ResourceShareApi.parse(422, "{\"error\":\"zones.0.name\"}"));
        assertEquals(422, error.status);
        assertEquals("zones.0.name", error.getMessage());
        assertEquals("HTTP 403", assertThrows(ResourceShareApi.ShareException.class,
                () -> ResourceShareApi.parse(403, "")).getMessage());
        assertEquals("HTTP 429", assertThrows(ResourceShareApi.ShareException.class,
                () -> ResourceShareApi.parse(429, "not json")).getMessage());
        assertEquals(5, ResourceShareApi.parse(200, "{\"accepted\":5}").get("accepted").getAsInt());
    }
}
