package de.ottoextra.regions;

import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RegionBannerResolverTest {
    private static final Gson GSON = new Gson();
    private static final Identifier REGION_BANNER = Identifier.of("test", "maehrstein");
    private static final Identifier FACTION_BANNER = Identifier.of("test", "faction");
    @TempDir Path tempDir;
    private RegionDataService data;

    @BeforeEach
    void createDataService() {
        OttoExtraApiClient unusedApi = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> { throw new AssertionError("Unexpected API call: " + method.getName()); });
        data = new RegionDataService(unusedApi, 30, tempDir.resolve("snapshot.json"));
    }

    @AfterEach
    void shutdown() throws Exception {
        // Await the existing async snapshot writer before JUnit removes its temp directory.
        var field = RegionDataService.class.getDeclaredField("scheduler");
        field.setAccessible(true);
        var scheduler = (ScheduledExecutorService) field.get(data);
        scheduler.submit(() -> { }).get(5, TimeUnit.SECONDS);
        data.shutdown();
        assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @CsvSource({"1,Klepburg", "2,Eberfeld", "3,Miessteig", "4,Mährstein", "5,Riedorf", "7,Berhausen"})
    void displaysFixedMaehrsteinBannerWithoutAPlayerFaction(String id, String name) {
        load(id, name, "uploads/maehrstein.png", "", "", false);
        assertTrue(data.factionForRegion(name).isEmpty()); // Previous toast returned no banner here.

        var banner = RegionBannerResolver.resolve(data, name, (key, path) -> {
            assertEquals("region-lehen_" + id, key);
            assertEquals("uploads/maehrstein.png", path);
            return Optional.of(REGION_BANNER);
        }, faction -> { fail("NPC fiefs have no faction banner"); return Optional.empty(); });

        assertEquals(REGION_BANNER, banner.orElseThrow());
    }

    @Test
    void suggestedBannerAndCanonicalCacheKeyWorkForNameUmlautAliasAndPolygonId() {
        load("4", "Mährstein", "", "", "uploads/maehrstein.png", false);
        List<String> keys = new ArrayList<>();
        for (String name : List.of("Mährstein", "Maehrstein", "lehen_4")) {
            assertEquals(REGION_BANNER, RegionBannerResolver.resolve(data, name, (key, path) -> {
                keys.add(key);
                assertEquals("uploads/maehrstein.png", path);
                return Optional.of(REGION_BANNER);
            }, faction -> Optional.empty()).orElseThrow());
        }
        assertEquals(List.of("region-lehen_4", "region-lehen_4", "region-lehen_4"), keys);
    }

    @Test
    void explicitRegionOverrideWinsOverSuggestedAndFactionBanners() {
        load("4", "Mährstein", "uploads/base.png", "uploads/override.png", "uploads/suggested.png", true);
        assertEquals(REGION_BANNER, RegionBannerResolver.resolve(data, "Mährstein", (key, path) -> {
            assertEquals("uploads/override.png", path);
            return Optional.of(REGION_BANNER);
        }, faction -> { fail("Ready region override must take priority"); return Optional.empty(); }).orElseThrow());
    }

    @Test
    void retainsFactionBannerWhenThereIsNoRegionBanner() {
        load("27", "Holdern", "", "", "", true);
        assertEquals(FACTION_BANNER, RegionBannerResolver.resolve(data, "Holdern", (key, path) -> {
            fail("No region image to load"); return Optional.empty();
        }, faction -> Optional.of(FACTION_BANNER)).orElseThrow());
    }

    @Test
    void regionBannerAppearsWhenItsAsynchronousLoadFinishes() {
        load("4", "Mährstein", "uploads/maehrstein.png", "", "", false);
        AtomicBoolean loaded = new AtomicBoolean();
        java.util.function.BiFunction<String, String, Optional<Identifier>> loader =
                (key, path) -> loaded.get() ? Optional.of(REGION_BANNER) : Optional.empty();
        assertTrue(RegionBannerResolver.resolve(data, "Mährstein", loader, faction -> Optional.empty()).isEmpty());
        loaded.set(true);
        assertEquals(REGION_BANNER, RegionBannerResolver.resolve(data, "Mährstein", loader,
                faction -> Optional.empty()).orElseThrow());
    }

    @Test
    void unknownRegionHasNoBanner() {
        assertTrue(RegionBannerResolver.resolve(data, "Unbekannt", (key, path) -> {
            fail("Unknown region must not trigger a download"); return Optional.empty();
        }, faction -> { fail("Unknown region has no faction"); return Optional.empty(); }).isEmpty());
    }

    private void load(String id, String name, String path, String override, String suggested, boolean faction) {
        String currentFaction = faction ? "{\"uuid\":\"guild\",\"name\":\"Gefolge\"}" : "null";
        data.applyEnvelope(GSON.fromJson("""
                {"ok":true,"sync_cursor":1,"factions":[],"regions":[{
                  "id":"lehen_%s","region_ref":"lehen_%s","region_id":"%s","name":"%s",
                  "banner_path":"%s","banner_override_path":"%s",
                  "region_info":{"suggested_banner_path":"%s"},"current_faction":%s
                }]}
                """.formatted(id, id, id, name, path, override, suggested, currentFaction), ApiEnvelope.class), true);
    }
}
