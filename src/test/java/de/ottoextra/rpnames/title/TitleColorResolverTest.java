package de.ottoextra.rpnames.title;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TitleColorResolverTest {
    @TempDir Path directory;

    @Test
    void mapsAllApiStateVariantsWithWhitespaceAndCaseNormalization() {
        Map<String, String> expected = Map.of(
                "  TEAMLER ", "system", "teamlerin", "system",
                " ADLIGER", "adel", "adlige ", "adel",
                "KlErIkEr", "klerus", " KLERIKERIN ", "klerus",
                "gemeiner", "allgemein", "  GeMeInE  ", "allgemein");
        expected.forEach((state, category) ->
                assertEquals(category, ApiStateCategory.category(state).orElseThrow()));
        assertTrue(ApiStateCategory.category(null).isEmpty());
        assertTrue(ApiStateCategory.category("Bürgerlich").isEmpty());
    }

    @Test
    void onlyActualNobleTitlesUseNobilityColorByDefault() throws Exception {
        var catalog = bundled();
        for (String state : new String[]{"Adliger", "Adlige", "Gemeiner", "Teamler", "Kleriker"}) {
            for (String title : new String[]{"Ritter", "Dame", "Freiherr", "Freifrau", "Graf", "Gräfin",
                    "Vogt", "Vogtin", "Ridder", "Freiin", "Kastellan", "Kirchvogtin", "Schutzvogt"}) {
                assertEquals("#b0a254", color(catalog, state, title, false), state + " / " + title);
            }
        }
        assertEquals("#a17f5f", color(catalog, "Adliger", "Kreuzfahrer", false));
        assertEquals("#a17f5f", color(catalog, "Adliger", "Legat", false));
        assertEquals("#a17f5f", color(catalog, "Adliger", "Gelehrter", false));
        assertEquals("#a17f5f", color(catalog, "Adliger", "Unlisted title", false));
    }

    @Test
    void optionRestoresNobleEstateColorForOtherTitles() throws Exception {
        var catalog = bundled();
        assertEquals("#b0a254", color(catalog, "Adlige", "Kreuzfahrerin", true));
        assertEquals("#b0a254", color(catalog, "Adliger", "Unlisted title", true));
        catalog.categories().get("adel").color = "#123456";
        assertEquals("#123456", color(catalog, "Adliger", "Legat", true));
        assertEquals("#a17f5f", color(catalog, "Gemeiner", "Legat", true));
    }

    @Test
    void fixedRankColorsWinForBothVariantsAndEveryEstateWithEitherOption() throws Exception {
        var catalog = bundled();
        Map<String, String> ranks = Map.of("Vorkoster", "#ea7749", "Vorkosterin", "#ea7749",
                "Legende", "#6a9fae", "Legendin", "#6a9fae",
                "Chronist", "#86a0b3", "Chronistin", "#86a0b3");
        ranks.forEach((title, expected) -> {
            for (String state : new String[]{"Adliger", "Adlige", "Gemeiner", "Kleriker", "Teamler", ""}) {
                for (boolean legacy : new boolean[]{false, true}) {
                    assertEquals(expected, color(catalog, state, "  " + title.toUpperCase(java.util.Locale.ROOT) + " ", legacy),
                            state + " / " + title);
                }
            }
        });
    }

    @Test
    void knownTeamClergyAndCustomTitlesUseTheirOwnCategories() throws Exception {
        var catalog = bundled();
        assertEquals("#bc4296", color(catalog, "Adliger", "Gamemaster", false));
        assertEquals("#bc4296", color(catalog, "Gemeiner", "Developer", false));
        assertEquals("#c7505e", color(catalog, "Adlige", "Verwaltungsnonne", false));
        assertEquals("#c7505e", color(catalog, "Teamler", "Priesterin", false));
        assertEquals("#a17f5f", color(catalog, "Kleriker", "Gelehrter", false));
        assertEquals("#a17f5f", color(catalog, "Teamler", "Federzunge", false));
    }

    @Test
    void developmentUsesTheSameTeamColorAsGamemasterForEveryEstate() throws Exception {
        var catalog = bundled();
        assertEquals("#bc4296", color(catalog, "", "Development", false));
        assertEquals("Development", catalog.displayForm("Development"));
        for (String state : new String[]{"Adliger", "Gemeiner", "Kleriker", "Teamler", ""}) {
            for (boolean legacy : new boolean[]{false, true}) {
                assertEquals(color(catalog, state, "Gamemaster", legacy),
                        color(catalog, state, "  DEVELOPMENT  ", legacy), state + " / " + legacy);
            }
        }
        catalog.categories().get("system").color = "#112233";
        assertEquals("#112233", color(catalog, "Gemeiner", "Development", false));
    }

    @Test
    void unknownApiTitlesMayStillUseNonNobleEstateFallbackWithoutChangingCatalog() {
        var catalog = new TitleCatalogStore();
        putCategory(catalog, "system", "#101010");
        putCategory(catalog, "adel", "#202020");
        putCategory(catalog, "klerus", "#303030");
        assertEquals("#101010", color(catalog, "Teamlerin", "Development", false));
        assertEquals("#a17f5f", color(catalog, "Adliger", "Truppführer", false));
        assertEquals("#202020", color(catalog, "Adliger", "Truppführer", true));
        assertEquals("#303030", color(catalog, "Klerikerin", "Ordenspriester", false));
        assertTrue(catalog.all().isEmpty(), "API titles must not be added to the catalog");
    }

    @Test
    void personalAndExplicitCatalogColorsRemainAuthoritative() throws Exception {
        var catalog = bundled();
        LocalRpProfile profile = profile("Adliger");
        assertEquals("#010203", TitleColorResolver.resolve(profile, "#010203", "Chronist", catalog, null, true));
        catalog.find("Kreuzfahrer").orElseThrow().colorOverride = "#445566";
        assertEquals("#445566", color(catalog, "Adliger", "Kreuzfahrer", true));
        assertEquals("#445566", color(catalog, "Gemeiner", "Kreuzfahrer", false));
    }

    @Test
    void registryAndDefaultFallbacksWorkWithoutCatalog() {
        var registry = new TitleRegistry();
        var group = new TitleRegistry.Group();
        group.titleColor = "#778899";
        group.titles.add("Testtitel");
        registry.apply(Map.of("CUSTOM", group));
        assertEquals("#778899", TitleColorResolver.resolve(profile("Adliger"),
                null, "Testtitel", null, registry, false));
        assertEquals("#a17f5f", TitleColorResolver.resolve(null, null, null, null, null, false));
    }

    @Test
    void publicResolverReadsTheLiveOptionAndConfigMigrationDefaultsOff() throws Exception {
        var config = new OttoExtraConfig();
        config.restoreFrom("{\"rpnames\":{\"enabled\":true}}");
        assertFalse(config.rpnames.nobilityColorForAllTitles);
        var catalog = bundled();
        var field = RpNamesServices.class.getDeclaredField("config");
        field.setAccessible(true);
        Object previous = field.get(null);
        try {
            field.set(null, config.rpnames);
            assertEquals("#a17f5f", TitleColorResolver.resolve(profile("Adliger"), null, "Legat", catalog, null));
            config.rpnames.nobilityColorForAllTitles = true;
            assertEquals("#b0a254", TitleColorResolver.resolve(profile("Adliger"), null, "Legat", catalog, null));
            config.restoreFrom(config.snapshotJson());
            assertTrue(config.rpnames.nobilityColorForAllTitles);
            config.rpnames.nobilityColorForAllTitles = false;
            assertEquals("#a17f5f", TitleColorResolver.resolve(profile("Adliger"), null, "Legat", catalog, null));
        } finally {
            field.set(null, previous);
        }
    }

    private TitleCatalogStore bundled() throws Exception {
        Path file = directory.resolve("catalog.json");
        try (var input = TitleCatalogStore.class.getResourceAsStream("/assets/ottoextra/rpnames/title-catalog-default.json")) {
            assertNotNull(input);
            Files.write(file, input.readAllBytes());
        }
        var catalog = new TitleCatalogStore();
        catalog.reloadAfterRestore(file);
        return catalog;
    }

    private static LocalRpProfile profile(String state) {
        var profile = new LocalRpProfile();
        profile.apiState = state;
        return profile;
    }

    private static String color(TitleCatalogStore catalog, String state, String title, boolean legacy) {
        return TitleColorResolver.resolve(profile(state), null, title, catalog, null, legacy);
    }

    private static void putCategory(TitleCatalogStore catalog, String key, String color) {
        var category = new TitleCatalogStore.Category();
        category.color = color;
        catalog.categories().put(key, category);
    }
}
