package de.ottoextra.rpnames.title;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TitleCatalogUpgradeTest {
    @TempDir Path directory;

    @Test
    void existingBundledCatalogGetsRankColorsAndMissingTitlesExactlyOnce() throws Exception {
        Path file = legacy(false, false);
        var catalog = new TitleCatalogStore();
        catalog.reloadAfterRestore(file);
        assertEquals("#ea7749", catalog.titleColor("Vorkosterin").orElseThrow());
        assertEquals("#86a0b3", catalog.titleColor("Chronistin").orElseThrow());
        assertEquals("#6a9fae", catalog.titleColor("Legendin").orElseThrow());
        assertEquals("fertigkeit", catalog.find("Gelehrter").orElseThrow().category);
        assertEquals("klerus", catalog.find("Verwaltungsnonne").orElseThrow().category);
        assertEquals("custom", catalog.find("Kreuzfahrerin").orElseThrow().category);
        for (String title : new String[]{"Federzunge", "Komplementär", "Gildenmitglied"}) {
            assertEquals("custom", catalog.find(title).orElseThrow().category);
        }
        String upgraded = Files.readString(file);
        catalog.reloadAfterRestore(file);
        assertEquals(upgraded, Files.readString(file));
        long distinct = catalog.all().stream().map(e -> e.id).distinct().count();
        assertEquals(catalog.all().size(), distinct);
    }

    @Test
    void existingPersonalCatalogEditsAndDisabledTitlesSurviveUpgrade() throws Exception {
        Path file = legacy(true, false);
        var catalog = new TitleCatalogStore();
        catalog.reloadAfterRestore(file);
        assertEquals("#123456", catalog.titleColor("Chronistin").orElseThrow());
        assertTrue(catalog.find("Vorkoster").isEmpty());
        assertEquals("#6a9fae", catalog.titleColor("Legendin").orElseThrow());
    }

    @Test
    void editedCategoryColorRemainsInUse() throws Exception {
        Path file = legacy(false, true);
        var catalog = new TitleCatalogStore();
        catalog.reloadAfterRestore(file);
        assertEquals("#112233", catalog.titleColor("Vorkoster").orElseThrow());
        assertEquals("#445566", catalog.titleColor("Chronist").orElseThrow());
    }

    @Test
    void existingAliasPreventsDuplicateNewTitleAndRemovedNewEntriesStayRemoved() throws Exception {
        Path file = legacy(false, false);
        JsonObject model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        model.getAsJsonArray("titles").add(JsonParser.parseString("""
                {"id":"my_legend","title":"Heldin","aliases":["Legende","Legendin"],
                 "variants":["Heldin"],"category":"custom","colorOverride":"#123abc"}
                """));
        Files.writeString(file, model.toString());
        var catalog = new TitleCatalogStore();
        catalog.reloadAfterRestore(file);
        assertEquals("#123abc", catalog.titleColor("Legendin").orElseThrow());
        assertTrue(catalog.all().stream().noneMatch(e -> "legende".equals(e.id)));
        JsonObject upgraded = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        upgraded.getAsJsonArray("titles").asList().removeIf(e -> "kreuzfahrer".equals(e.getAsJsonObject().get("id").getAsString()));
        Files.writeString(file, upgraded.toString());
        catalog.reloadAfterRestore(file);
        assertTrue(catalog.find("Kreuzfahrer").isEmpty());
    }

    @Test
    void independentCustomCatalogIsNotFilledWithBundledEntries() throws Exception {
        Path file = directory.resolve("custom.json");
        String original = "{\"titles\":[{\"id\":\"only\",\"title\":\"Mein Titel\"}]}";
        Files.writeString(file, original);
        var catalog = new TitleCatalogStore();
        catalog.reloadAfterRestore(file);
        assertEquals(1, catalog.all().size());
        assertEquals(original, Files.readString(file));
    }

    private Path legacy(boolean edited, boolean editedCategory) throws Exception {
        JsonObject model;
        try (var input = TitleCatalogStore.class.getResourceAsStream("/assets/ottoextra/rpnames/title-catalog-default.json")) {
            assertNotNull(input);
            model = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        model.remove("bundledRevision");
        var titles = model.getAsJsonArray("titles");
        Set<String> additions = Set.of("kreuzfahrer", "federzunge", "komplementaer",
                "gildenmitglied", "verwaltungsnonne", "legende");
        titles.asList().removeIf(e -> additions.contains(e.getAsJsonObject().get("id").getAsString()));
        for (var value : titles) {
            var entry = value.getAsJsonObject();
            String id = entry.get("id").getAsString();
            if ("chronist".equals(id) || "vorkoster".equals(id)) entry.add("colorOverride", null);
            if ("gelehrter".equals(id)) entry.addProperty("category", "klerus");
            if (edited && "chronist".equals(id)) entry.addProperty("colorOverride", "#123456");
            if (edited && "vorkoster".equals(id)) entry.addProperty("enabled", false);
        }
        model.getAsJsonObject("categories").getAsJsonObject("vorkoster")
                .addProperty("color", editedCategory ? "#112233" : "#e97749");
        if (editedCategory) model.getAsJsonObject("categories").getAsJsonObject("allgemein")
                .addProperty("color", "#445566");
        Path file = directory.resolve("legacy.json");
        Files.writeString(file, model.toString());
        return file;
    }
}
