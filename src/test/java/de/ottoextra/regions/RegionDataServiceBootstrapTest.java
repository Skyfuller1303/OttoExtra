package de.ottoextra.regions;

import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionDataServiceBootstrapTest {

    private static final Gson GSON = new Gson();

    @Test
    void acceptsOnlySuccessfulBootstrapWithFactionsAndRegions() {
        assertTrue(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"factions\":[{}],\"regions\":[{}]}")));

        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":false,\"factions\":[{}],\"regions\":[{}]}")));
        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"factions\":[],\"regions\":[{}]}")));
        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"factions\":[{}],\"regions\":[]}")));
        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true}")));
        assertFalse(RegionDataService.isCompleteBootstrap(null));
    }

    @Test
    void completeBootstrapRemovesOldLandedFactionState(@TempDir Path tempDir) throws Exception {
        OttoExtraApiClient unusedApi = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> {
                    throw new AssertionError("API-Aufruf im Bootstrap-Index-Test: " + method.getName());
                });
        RegionDataService service = new RegionDataService(unusedApi, 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.applyEnvelope(envelope("""
                    {"ok":true,"sync_cursor":1,
                     "factions":[
                       {"uuid":"whitewater","faction_ref":"lehen_29",
                        "name":"Weißwasserhaven","lord_name":"Ährstein",
                        "region_id":"29","region_name":"Waldbrück"},
                       {"uuid":"old-rusteveld","faction_ref":"lehen_28",
                        "name":"Rusteveld","lord_name":"Weißwasserhaven",
                        "region_id":"28","region_name":"Eugensbrück"}],
                     "regions":[
                       {"id":"lehen_29","region_ref":"lehen_29","region_id":"29",
                        "name":"Waldbrück","current_faction":{"uuid":"whitewater",
                        "name":"Weißwasserhaven","lord_name":"Ährstein"}},
                       {"id":"lehen_28","region_ref":"lehen_28","region_id":"28",
                        "name":"Eugensbrück","current_faction":{"uuid":"old-rusteveld",
                        "name":"Rusteveld","lord_name":"Weißwasserhaven"}}]}
                    """), true);
            assertTrue(service.factionByUuid("old-rusteveld").isPresent());

            service.applyEnvelope(envelope("""
                    {"ok":true,"sync_cursor":2,
                     "factions":[
                       {"uuid":"whitewater","name":"Weißwasserhaven",
                        "rank_name":"Ungelandet","region_name":"-"},
                       {"uuid":"new-rusteveld","faction_ref":"lehen_28",
                        "name":"Rusteveld","lord_name":"Sturmhöhe",
                        "region_id":"28","region_name":"Eugensbrück"},
                       {"uuid":"ramberg","faction_ref":"lehen_29","name":"Ramberg",
                        "region_id":"29","region_name":"Waldbrück"}],
                     "regions":[
                       {"id":"lehen_28","region_ref":"lehen_28","region_id":"28",
                        "name":"Eugensbrück","current_faction":{"uuid":"new-rusteveld",
                        "name":"Rusteveld","lord_name":"Sturmhöhe"}},
                       {"id":"lehen_29","region_ref":"lehen_29","region_id":"29",
                        "name":"Waldbrück","current_faction":{"uuid":"ramberg",
                        "name":"Ramberg"}}]}
                    """), true);

            assertFalse(service.factionByUuid("old-rusteveld").isPresent());
            assertFalse(service.factionByKey("Weißwasserhaven").orElseThrow().isLanded());
            assertTrue(service.factionByKey("Rusteveld")
                    .filter(faction -> "Sturmhöhe".equals(faction.lord_name()))
                    .isPresent());
            assertTrue(service.factionForRegion("lehen_29")
                    .filter(faction -> "Ramberg".equals(faction.name()))
                    .isPresent());
        } finally {
            service.shutdown();
            // shutdownNow interrupts writes but does not wait for them to release the temporary directory.
            var field = RegionDataService.class.getDeclaredField("scheduler");
            field.setAccessible(true);
            var scheduler = (ScheduledExecutorService) field.get(service);
            assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static ApiEnvelope envelope(String json) {
        return GSON.fromJson(json, ApiEnvelope.class);
    }
}
