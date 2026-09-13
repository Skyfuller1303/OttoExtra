package de.ottoextra.map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapSelectionTextTest {
    private static final Gson GSON = new Gson();
    private static final String FEHDE = "Dieses Lehen hat sich der Fehde der Grafschaft Mährstein gegen den Herzog angeschlossen.";
    private static final String HOLDERN = "Die Stadt Holdern ist der Familiensitz des Herzogs Heinrich von Memleben. Das Herzogtum Ottonien ist Teil des Heiligen Römischen Reiches auf dem Festland.";

    @Test void eberfeldShowsOnlyTheFeudTextDespiteItsParentRegion() {
        var eberfeld = region("""
                {"region_id":"2","name":"Eberfeld","parent_region_ref":"lehen_4",
                 "region_info":{"playable":false,"status_label":"Fehde-Lehen"}}
                """, FEHDE);
        var parent = region("{\"name\":\"Mährstein\"}", "");
        var info = MapSelectionInfo.from("lehen_2", eberfeld, null, name -> null, name -> parent);
        assertEquals(List.of(FEHDE), paragraphs(info, "de_de"));
    }

    @Test void maehrsteinKeepsHeldwegAsTheOriginalFiefName() {
        var region = region("""
                {"region_id":"4","name":"Mährstein","original_name":"Heldweg",
                 "region_info":{"playable":false,"status_label":"NPC-Siedlung"}}
                """, FEHDE);
        var info = MapSelectionInfo.from("lehen_4", region, null, name -> null, name -> null);
        assertEquals("Mährstein", info.title());
        assertEquals("Heldweg", info.regionName());
        assertEquals(List.of(FEHDE), paragraphs(info, "de_de"));
    }

    @Test void rabenstadUsesVogteiAndNamesTheCountyAndDirectOverlord() {
        var faction = faction("Rabenstad", "Vogtei", "Danny_Dajeno", 9, "Ährstein");
        var parent = faction("Ährstein", "Grafschaft", "", 0, "");
        var info = MapSelectionInfo.from("lehen_17", region("{\"name\":\"Niederpölt\"}", ""), faction,
                name -> parent, name -> null);
        assertEquals(List.of("Die Vogtei wird von Danny_Dajeno geführt und zählt 9 Bewohner. Das Lehen ist Teil der Grafschaft Ährstein und zudem ein direkter Vasall von Ährstein."),
                paragraphs(info, "de_de"));
    }

    @Test void independentSeeschlangeUsesTheDukeSentence() {
        var faction = faction("Seeschlange", "Dorf", "Zauselrak", 5, "");
        var info = MapSelectionInfo.from("Gladebek", null, faction, name -> null, name -> null);
        assertEquals(List.of("Das Dorf wird von Zauselrak geführt und zählt 5 Bewohner. Das Lehen untersteht dem Herzog und ist abgesehen davon unabhängig."),
                paragraphs(info, "de_de"));
    }

    @Test void occupiedNpcSettlementShowsItsCurrentFactionWithoutAnExtraDescription() {
        var region = region("""
                {"region_id":"44","name":"Wegscheide",
                 "region_info":{"playable":true,"status_label":"NPC-Siedlung"}}
                """, "");
        var faction = faction("Achental", "Kloster", "Artami", 4, "Warglau");
        var parent = faction("Warglau", "Vogtei", "Romegon", 9, "");
        var info = MapSelectionInfo.from("lehen_44", region, faction, name -> parent, name -> null);
        assertEquals(List.of("Das Kloster wird von Artami geführt und zählt 4 Bewohner. Das Lehen ist Teil der Vogtei Warglau und zudem ein direkter Vasall von Warglau."),
                paragraphs(info, "de_de"));
    }

    @Test void npcLabelFromCapabilitiesDoesNotHideCurrentFactionFacts() {
        var region = GSON.fromJson("""
                {"region_id":"65","name":"Karlshof",
                 "region_capabilities":{"region_info":{"playable":true,"status_label":"NPC-Siedlung",
                 "note":"Ein Lehen im Norden."}}}
                """, RegionRecord.class);
        var faction = faction("Nordeck", "Freiherrschaft", "Arrtakh", 13, "");
        var info = MapSelectionInfo.from("lehen_65", region, faction, name -> null, name -> null);
        assertEquals(List.of("Die Freiherrschaft wird von Arrtakh geführt und zählt 13 Bewohner. Das Lehen untersteht dem Herzog und ist abgesehen davon unabhängig.",
                        "Ein Lehen im Norden."), paragraphs(info, "de_de"));
    }

    @Test void explicitlyUnplayableFiefKeepsItsStoryWithoutFactionStatistics() {
        var region = region("""
                {"region_id":"2","name":"Eberfeld",
                 "region_info":{"playable":false,"status_label":"Fehde-Lehen"}}
                """, FEHDE);
        var faction = faction("Eberfeld", "Dorf", "", 0, "Mährstein");
        var info = MapSelectionInfo.from("lehen_2", region, faction, name -> null, name -> null);
        assertEquals(List.of(FEHDE), paragraphs(info, "de_de"));
    }

    @Test void npcVillageAddsAnActivityParagraphWithItsOwnName() {
        var region = region("""
                {"region_id":"44","name":"Wegscheide",
                 "region_info":{"playable":true,"status_label":"NPC-Siedlung",
                 "npc_village":{"name":"NPC Siedlung Quelltal"}}}
                """, "Ein Lehen in den Bergen.");
        var faction = faction("Achental", "Kloster", "Artami", 4, null);
        var info = MapSelectionInfo.from("lehen_44", region, faction, name -> null, name -> null);
        assertEquals(List.of("Das Kloster wird von Artami geführt und zählt 4 Bewohner.",
                        "Ein Lehen in den Bergen.",
                        "In diesem Gebiet befindet sich das Dorf Quelltal. Hier gibt es besonders viele Aufgaben und Aktivitäten."),
                paragraphs(info, "de_de"));
        assertEquals("The village of Quelltal is located in this area. There are plenty of quests and activities here.",
                paragraphs(info, "en_us").getLast());
    }

    @Test void npcVillageFromCapabilitiesReplacesTheEmptyDetailsPlaceholder() {
        var region = GSON.fromJson("""
                {"name":"Karlshof","region_info":{},
                 "region_capabilities":{"region_info":{"npc_village":"NPC-Siedlung Mühlsverd"}}}
                """, RegionRecord.class);
        var info = MapSelectionInfo.from("lehen_65", region, null, name -> null, name -> null);
        assertEquals(List.of("In diesem Gebiet befindet sich das Dorf Mühlsverd. Hier gibt es besonders viele Aufgaben und Aktivitäten."),
                paragraphs(info, "de_de"));
    }

    @Test void npcVillageWithoutItsDisplayPrefixKeepsTheName() {
        var region = region("""
                {"name":"Wiesdorf","region_info":{"npc_village":{"name":"Pfuhldorf"}}}
                """, "");
        var info = MapSelectionInfo.from("lehen_21", region, null, name -> null, name -> null);
        assertEquals(List.of("In diesem Gebiet befindet sich das Dorf Pfuhldorf. Hier gibt es besonders viele Aufgaben und Aktivitäten."),
                paragraphs(info, "de_de"));
    }

    @Test void holdernUsesTheSuppliedStoryWithoutTheSyntheticZeroResidents() {
        var holdern = region("""
                {"region_id":"27","name":"Holdern","region_info":{"playable":true,"status_label":"NPC-Siedlung",
                 "npc_village":{"name":"NPC Siedlung Holdern"}}}
                """, "Die Stadt Holdern ist Sitz des Herzogs von Ottonien.");
        var fallback = GSON.fromJson("{\"source\":\"local\",\"name\":\"Holdern\",\"rank_name\":\"Herzogtum\",\"player_count\":0}", FactionRecord.class);
        var info = MapSelectionInfo.from("lehen_27", holdern, fallback, name -> null, name -> null);
        assertEquals("Holdern (Ottonien)", info.title());
        assertEquals("Holdern", info.regionName());
        assertEquals(List.of(HOLDERN,
                        "In diesem Gebiet befindet sich das Dorf Holdern. Hier gibt es besonders viele Aufgaben und Aktivitäten."),
                paragraphs(info, "de_de"));
    }

    @Test void newerApiStoryForHoldernTakesPrecedence() {
        var holdern = region("{\"region_id\":\"27\",\"name\":\"Holdern\"}", "Eine aktualisierte Geschichte.");
        var info = MapSelectionInfo.from("lehen_27", holdern, null, name -> null, name -> null);
        assertEquals(List.of("Eine aktualisierte Geschichte."), paragraphs(info, "de_de"));
    }

    @Test void multiLevelHierarchyAndMasculineGenitiveRemainCorrect() {
        var child = faction("Dorf", "Dorf", "Spieler", 1, "Baronie");
        var barony = faction("Baronie", "Freiherrschaft", "", 0, "Nordland");
        var kingdom = faction("Nordland", "Herzogtum", "", 0, "");
        var info = MapSelectionInfo.from("Dorf", null, child,
                Map.of("Baronie", barony, "Nordland", kingdom)::get, name -> null);
        assertEquals(List.of("Das Dorf wird von Spieler geführt und zählt 1 Bewohner. Das Lehen ist Teil des Herzogtums Nordland und zudem ein direkter Vasall von Baronie."),
                paragraphs(info, "de_de"));
        assertTrue(paragraphs(info, "en_us").getFirst().contains("of the Herzogtum Nordland"));
    }

    @Test void missingOverlordDataDoesNotClaimIndependence() {
        var faction = faction("Siedlung", "Abtei", "Abt", 2, null);
        var info = MapSelectionInfo.from("Siedlung", null, faction, name -> null, name -> null);
        assertEquals(List.of("Die Abtei wird von Abt geführt und zählt 2 Bewohner."), paragraphs(info, "de_de"));
    }

    @Test void playerDescriptionStaysAnAdditionalParagraph() {
        var region = region("{\"name\":\"Gladebek\"}", "Ein Dorf am Fluss.");
        var faction = faction("Seeschlange", "Dorf", "Zauselrak", 7, "");
        var info = MapSelectionInfo.from("Gladebek", region, faction, name -> null, name -> null);
        assertEquals(2, paragraphs(info, "de_de").size());
        assertTrue(paragraphs(info, "de_de").getFirst().contains("7 Bewohner"));
        assertEquals("Ein Dorf am Fluss.", paragraphs(info, "de_de").getLast());
    }

    private static List<String> paragraphs(MapSelectionInfo info, String language) {
        try (var reader = new InputStreamReader(MapSelectionTextTest.class.getResourceAsStream(
                "/assets/ottoextra/lang/" + language + ".json"), StandardCharsets.UTF_8)) {
            JsonObject translations = GSON.fromJson(reader, JsonObject.class);
            return MapSelectionText.paragraphs(info, (key, args) -> {
                assertTrue(translations.has(key), "Missing translation: " + key);
                return String.format(Locale.ROOT, translations.get(key).getAsString(), args);
            });
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    private static RegionRecord region(String json, String note) {
        var object = GSON.fromJson(json, JsonObject.class);
        if (!object.has("region_info")) object.add("region_info", new JsonObject());
        object.getAsJsonObject("region_info").addProperty("note", note);
        return GSON.fromJson(object, RegionRecord.class);
    }

    private static FactionRecord faction(String name, String rank, String leader, int count, String lord) {
        var object = new JsonObject();
        object.addProperty("name", name); object.addProperty("rank_name", rank);
        object.addProperty("leader_name", leader); object.addProperty("player_count", count);
        object.addProperty("lord_name", lord);
        return GSON.fromJson(object, FactionRecord.class);
    }

}
