package de.ottoextra.map;

import com.google.gson.Gson;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapSelectionInfoTest {
    private final Gson gson = new Gson();

    @Test void separatesSettlementNameFromFiefAndLeaderFromOverlord() {
        var region = region("""
                {"id":"lehen_8","region_id":"8","name":"Brokholt"}
                """);
        var faction = faction("Wyndham", "Holdstewik");
        var result = MapSelectionInfo.from("lehen_8", region, faction, key -> null, key -> null);
        assertEquals("Wyndham", result.title());
        assertEquals("Brokholt", result.regionName());
        assertEquals("8", result.regionId());
        assertEquals("Cadwgawn", result.leader());
        assertEquals(3, result.residents());
        assertEquals("Holdstewik", result.directLord());
    }

    @Test void resolvesDirectAndTopOverlordIndependently() {
        var factions = Map.of("Baronie", faction("Baronie", "Königreich"), "Königreich", faction("Königreich", ""));
        var result = MapSelectionInfo.from("Brokholt", null, faction("Wyndham", "Baronie"), factions::get, key -> null);
        assertEquals("Baronie", result.directLord());
        assertEquals("Königreich", result.realm());
    }

    @Test void missingFactionDoesNotInventPopulationOrLeader() {
        var result = MapSelectionInfo.from("lehen_8", region("{\"name\":\"Brokholt\"}"), null, key -> null, key -> null);
        assertEquals("Brokholt", result.title());
        assertNull(result.residents());
        assertEquals("", result.leader());
        assertEquals("", result.description());
    }

    @Test void descriptionPrefersRegionNoteOverFactionDescription() {
        var region = region("""
                {"name":"Brokholt","region_info":{"note":"Ein Dorf am Fluss."}}
                """);
        var faction = gson.fromJson("{\"description\":\"Alte Beschreibung\"}", FactionRecord.class);
        var result = MapSelectionInfo.from("Brokholt", region, faction, key -> null, key -> null);
        assertEquals("Ein Dorf am Fluss.", result.description());
    }

    @Test void supportsCapabilitiesNoteAndFactionDescriptionFallback() {
        var region = region("""
                {"name":"Brokholt","region_capabilities":{"region_info":{"note":"Historischer Ort."}}}
                """);
        assertEquals("Historischer Ort.", MapSelectionInfo.from("Brokholt", region, null, key -> null, key -> null).description());
        var faction = gson.fromJson("{\"description\":\"Am Waldrand.\"}", FactionRecord.class);
        assertEquals("Am Waldrand.", MapSelectionInfo.from("Brokholt", null, faction, key -> null, key -> null).description());
    }

    @Test void supportsNpcParentRegionsWithoutFaction() {
        var child = region("{\"name\":\"Dorf\",\"parent_region_ref\":\"parent\"}");
        var parent = region("{\"name\":\"Mährstein\"}");
        var result = MapSelectionInfo.from("Dorf", child, null, key -> null, key -> parent);
        assertEquals("Mährstein", result.directLord());
        assertEquals("Mährstein", result.realm());
    }

    @Test void cyclicHierarchyTerminatesWithoutClaimingATopOverlord() {
        var a = faction("A", "B");
        var b = faction("B", "A");
        var result = MapSelectionInfo.from("Dorf", null, a, Map.of("A", a, "B", b)::get, key -> null);
        assertEquals("B", result.directLord());
        assertEquals("", result.realm());
        var self = faction("A", "A");
        result = MapSelectionInfo.from("Dorf", null, self, key -> self, key -> null);
        assertEquals("", result.directLord());
    }

    private RegionRecord region(String json) { return gson.fromJson(json, RegionRecord.class); }

    private FactionRecord faction(String name, String lord) {
        var json = new com.google.gson.JsonObject();
        json.addProperty("name", name);
        json.addProperty("lord_name", lord);
        json.addProperty("leader_name", "Cadwgawn");
        json.addProperty("player_count", 3);
        json.addProperty("rank_name", "Dorf");
        return gson.fromJson(json, FactionRecord.class);
    }
}
