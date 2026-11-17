package de.ottoextra.regions;

import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import de.ottoextra.privacy.ConsentPermit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionDataServiceBootstrapTest {

    private static final Gson GSON = new Gson();

    @Test
    void acceptsOnlySuccessfulBootstrapWithFactionsAndRegions() {
        assertTrue(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"factions\":[{}],\"regions\":[{}]}")));
        assertTrue(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"snapshot_complete\":true,\"factions\":[],\"regions\":[]}")));

        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":false,\"factions\":[{}],\"regions\":[{}]}")));
        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"factions\":[],\"regions\":[{}]}")));
        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"factions\":[{}],\"regions\":[]}")));
        assertFalse(RegionDataService.isCompleteBootstrap(envelope(
                "{\"ok\":true,\"snapshot_complete\":false,\"factions\":[{}],\"regions\":[{}]}")));
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

    @Test
    void syncReplacesIndexesOnlyForMarkedCompleteSnapshot(@TempDir Path tempDir) throws Exception {
        OttoExtraApiClient unusedApi = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        RegionDataService service = new RegionDataService(unusedApi, 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.applyEnvelope(envelope("""
                    {"ok":true,"sync_cursor":1,"factions":[{"uuid":"old","name":"Old"}],
                     "regions":[{"id":"lehen_1","name":"Old region"}]}
                    """), true);
            service.applyEnvelope(envelope("""
                    {"ok":true,"sync_cursor":2,"snapshot_complete":true,
                     "factions":[],"regions":[]}
                    """), false);
            assertTrue(service.factionByUuid("old").isEmpty());
            assertTrue(service.regionByName("Old region").isEmpty());

            service.applyEnvelope(envelope("""
                    {"ok":true,"sync_cursor":3,"factions":[],"regions":[]}
                    """), false);
            assertTrue(service.regionByName("Old region").isEmpty());
            assertTrue(service.allFactions().stream().noneMatch(f -> "old".equals(f.uuid())));

            service.applyEnvelope(envelope("""
                    {"ok":true,"sync_cursor":4,"snapshot_complete":false,
                     "factions":[],"regions":[]}
                    """), false);
            assertTrue(service.regionByName("Old region").isEmpty());
            assertTrue(service.allFactions().stream().noneMatch(f -> "old".equals(f.uuid())));
            var cursor = RegionDataService.class.getDeclaredField("syncCursor");
            cursor.setAccessible(true);
            assertEquals(2L, cursor.getLong(service));
        } finally {
            service.shutdown();
        }
    }

    @Test
    void canonicalUnlandedFactionOverridesStaleRegionSummary(@TempDir Path tempDir) {
        OttoExtraApiClient unusedApi = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        RegionDataService service = new RegionDataService(unusedApi, 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.applyEnvelope(envelope("""
                    {"ok":true,"factions":[{"uuid":"faction","name":"Heerzug",
                     "region_name":"-","rank_name":"Ungelandet"}],
                     "regions":[{"id":"lehen_1","region_id":"1","name":"Nordmark",
                     "current_faction":{"uuid":"faction","name":"Alter Besitz",
                     "region_id":"1","region_name":"Nordmark"}}]}
                    """), true);
            assertTrue(service.factionForRegion("Nordmark").isEmpty());
            assertFalse(service.factionByKey("Nordmark").isPresent());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void freeRegionNameDoesNotResolveToUnlandedFactionAlias(@TempDir Path tempDir) {
        OttoExtraApiClient unusedApi = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        RegionDataService service = new RegionDataService(unusedApi, 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.applyEnvelope(envelope("""
                    {"ok":true,"factions":[{"uuid":"faction","name":"Nordmark",
                     "region_name":"-","rank_name":"Ungelandet"}],
                     "regions":[{"id":"lehen_1","region_id":"1","name":"Nordmark"}]}
                    """), true);
            assertTrue(service.factionForRegion("Nordmark").isEmpty());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void prefixedFactionRegionIdMatchesNumericRegionId(@TempDir Path tempDir) {
        OttoExtraApiClient unusedApi = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        RegionDataService service = new RegionDataService(unusedApi, 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.applyEnvelope(envelope("""
                    {"ok":true,"factions":[{"uuid":"faction","name":"Nordmark",
                     "region_id":"lehen_20","region_name":"Nordmark"}],
                     "regions":[{"id":"lehen_20","region_ref":"lehen_20",
                     "region_id":"20","name":"Nordmark",
                     "current_faction":{"uuid":"faction","name":"Nordmark"}}]}
                    """), true);

            assertEquals("faction", service.factionForRegion("Nordmark").orElseThrow().uuid());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void explicitEmptyRegionIdOverridesLegacyRegionNameFallback() {
        var emptyId = GSON.fromJson(
                "{\"region_id\":\"\",\"region_name\":\"Nordmark\",\"rank_name\":\"Dorf\"}",
                de.ottoextra.api.model.FactionRecord.class);
        var missingId = GSON.fromJson(
                "{\"region_name\":\"Nordmark\",\"rank_name\":\"Dorf\"}",
                de.ottoextra.api.model.FactionRecord.class);

        assertFalse(emptyId.isLanded());
        assertTrue(missingId.isLanded());
    }

    private static final String COMPLETE_BOOTSTRAP =
            "{\"ok\":true,\"sync_cursor\":1,\"factions\":[{\"uuid\":\"f\",\"name\":\"F\"}],"
                    + "\"regions\":[{\"id\":\"lehen_1\",\"name\":\"R\"}]}";

    private static OttoExtraApiClient bootstrapApi(List<CompletableFuture<ApiEnvelope>> calls) {
        return (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("bootstrap")) {
                        return new CompletableFuture<ApiEnvelope>();
                    }
                    CompletableFuture<ApiEnvelope> future = new CompletableFuture<>();
                    synchronized (calls) { calls.add(future); }
                    return future;
                });
    }

    @Test
    void watchdogRetriesBootstrapAfterPermitInvalidatedMidFlight(@TempDir Path tempDir) throws Exception {
        List<CompletableFuture<ApiEnvelope>> calls = new ArrayList<>();
        ConsentPermit.Gate gate = new ConsentPermit.Gate();
        RegionDataService service = new RegionDataService(bootstrapApi(calls), 30,
                tempDir.resolve("snapshot.json"), () -> gate.capture(() -> true));
        try {
            service.onServerJoin(false);
            assertEquals(1, calls.size());
            var completion = service.bootstrapCompletion();
            gate.invalidate();
            assertTrue(calls.get(0).isCancelled());
            assertFalse(completion.isDone());

            service.runWatchdog();
            // A retry may already be pending; either way the watchdog must not duplicate nor lose it.
            if (calls.size() == 1) {
                // retry task pending (15 s) -> watchdog leaves it alone, so request directly via refreshNow
                service.refreshNow();
            }
            assertEquals(2, calls.size());
            calls.get(1).complete(envelope(COMPLETE_BOOTSTRAP));
            assertTrue(service.bootstrapCompletion().isDone());
            assertFalse(service.bootstrapCompletion().isCancelled());
            assertTrue(service.factionByUuid("f").isPresent());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void watchdogBootstrapsOnceInitiallyInvalidPermitBecomesValid(@TempDir Path tempDir) throws Exception {
        List<CompletableFuture<ApiEnvelope>> calls = new ArrayList<>();
        ConsentPermit.Gate gate = new ConsentPermit.Gate();
        AtomicBoolean allowed = new AtomicBoolean(false);
        RegionDataService service = new RegionDataService(bootstrapApi(calls), 30,
                tempDir.resolve("snapshot.json"), () -> gate.capture(allowed::get));
        try {
            service.onServerJoin(false);
            assertEquals(0, calls.size());
            service.runWatchdog();
            assertEquals(0, calls.size());

            allowed.set(true);
            service.runWatchdog();
            assertEquals(1, calls.size());
            calls.get(0).complete(envelope(COMPLETE_BOOTSTRAP));
            assertTrue(service.bootstrapCompletion().isDone());
            assertFalse(service.bootstrapCompletion().isCancelled());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void incompleteSnapshotWithNamesIsMergedWithoutWipingExistingEntries(@TempDir Path tempDir) throws Exception {
        RegionDataService service = new RegionDataService(bootstrapApi(new ArrayList<>()), 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.applyEnvelope(envelope("""
                    {"ok":true,"snapshot_complete":true,"sync_cursor":5,
                     "factions":[{"uuid":"old","name":"Old"}],
                     "regions":[{"id":"lehen_1","name":"Old region"}]}
                    """), true);

            var result = service.applyEnvelope(envelope("""
                    {"ok":true,"snapshot_complete":false,"sync_cursor":99,
                     "factions":[{"uuid":"new","name":"Neu"}],
                     "regions":[{"id":"lehen_29","region_id":"29","name":"Waldbrück"}]}
                    """), true);

            assertEquals(RegionDataService.ApplyResult.MERGED, result);
            assertEquals("Waldbrück", service.regionByName("lehen_29").orElseThrow().name());
            assertTrue(service.regionByName("Old region").isPresent());
            assertTrue(service.factionByUuid("old").isPresent());
            assertTrue(service.factionByUuid("new").isPresent());
            var cursor = RegionDataService.class.getDeclaredField("syncCursor");
            cursor.setAccessible(true);
            assertEquals(5L, cursor.getLong(service));

            assertEquals(RegionDataService.ApplyResult.AUTHORITATIVE, service.applyEnvelope(envelope("""
                    {"ok":true,"snapshot_complete":true,"sync_cursor":6,
                     "factions":[{"uuid":"only","name":"Only"}],
                     "regions":[{"id":"lehen_2","name":"Only region"}]}
                    """), true));
            assertTrue(service.regionByName("lehen_29").isEmpty());
            assertTrue(service.factionByUuid("new").isEmpty());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void incompleteBootstrapMarksReadyAndSchedulesNoFastRetry(@TempDir Path tempDir) throws Exception {
        List<CompletableFuture<ApiEnvelope>> calls = new ArrayList<>();
        RegionDataService service = new RegionDataService(bootstrapApi(calls), 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.onServerJoin(false);
            assertEquals(1, calls.size());
            var completion = service.bootstrapCompletion();
            calls.get(0).complete(envelope("""
                    {"ok":true,"snapshot_complete":false,"sync_cursor":7,
                     "factions":[{"uuid":"f","name":"F"}],
                     "regions":[{"id":"lehen_29","name":"Waldbrück"}]}
                    """));

            assertTrue(completion.isDone());
            assertFalse(completion.isCancelled());
            assertEquals("Waldbrück", service.regionByName("lehen_29").orElseThrow().name());
            var ready = RegionDataService.class.getDeclaredField("bootstrapReady");
            ready.setAccessible(true);
            assertTrue(ready.getBoolean(service));
            var retry = RegionDataService.class.getDeclaredField("bootstrapRetryTask");
            retry.setAccessible(true);
            assertEquals(null, retry.get(service));
            service.runWatchdog();
            assertEquals(1, calls.size());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void forceRefreshFetchesWhileNotConnectedAndMerges(@TempDir Path tempDir) throws Exception {
        List<CompletableFuture<ApiEnvelope>> calls = new ArrayList<>();
        RegionDataService service = new RegionDataService(bootstrapApi(calls), 30,
                tempDir.resolve("snapshot.json"));
        try {
            var done = service.forceRefresh();
            assertEquals(1, calls.size());
            calls.get(0).complete(envelope("""
                    {"ok":true,"snapshot_complete":false,
                     "factions":[],"regions":[{"id":"lehen_29","name":"Waldbrück"}]}
                    """));
            assertTrue(done.isDone());
            assertFalse(done.isCompletedExceptionally());
            assertEquals("Waldbrück", service.regionByName("lehen_29").orElseThrow().name());
            assertTrue(service.diagnosticStatus().contains("Regionen="));
        } finally {
            service.shutdown();
        }
    }

    @Test
    void supersededBootstrapDoesNotClearInFlightMarkerOfNewerRequest(@TempDir Path tempDir) throws Exception {
        List<CompletableFuture<ApiEnvelope>> calls = new ArrayList<>();
        RegionDataService service = new RegionDataService(bootstrapApi(calls), 30,
                tempDir.resolve("snapshot.json"));
        try {
            service.onServerJoin(false);
            assertEquals(1, calls.size());
            service.forceRefresh();
            assertEquals(2, calls.size());

            // The older request fails after the manual refresh took over the marker.
            calls.get(0).completeExceptionally(new IllegalStateException("old request failed"));
            service.refreshNow();
            assertEquals(2, calls.size());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void changeListenerRunsForAppliedEnvelopesAndIsolatesFailures(@TempDir Path tempDir) {
        RegionDataService service = new RegionDataService(bootstrapApi(new ArrayList<>()), 30,
                tempDir.resolve("snapshot.json"));
        try {
            AtomicInteger changes = new AtomicInteger();
            service.setChangeListener(() -> {
                changes.incrementAndGet();
                throw new IllegalStateException("listener failure");
            });
            service.applyEnvelope(envelope("{\"ok\":false,\"factions\":[],\"regions\":[]}"), true);
            assertEquals(0, changes.get());
            assertEquals(RegionDataService.ApplyResult.AUTHORITATIVE,
                    service.applyEnvelope(envelope(COMPLETE_BOOTSTRAP), true));
            assertEquals(1, changes.get());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void watchdogDoesNothingAfterDisconnect(@TempDir Path tempDir) throws Exception {
        List<CompletableFuture<ApiEnvelope>> calls = new ArrayList<>();
        ConsentPermit.Gate gate = new ConsentPermit.Gate();
        AtomicBoolean allowed = new AtomicBoolean(false);
        RegionDataService service = new RegionDataService(bootstrapApi(calls), 30,
                tempDir.resolve("snapshot.json"), () -> gate.capture(allowed::get));
        try {
            service.onServerJoin(false);
            service.onDisconnect();
            allowed.set(true);
            service.runWatchdog();
            assertEquals(0, calls.size());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void failedBootstrapFallsBackToRegionListOncePerThrottleWindow(@TempDir Path tempDir) throws Exception {
        AtomicInteger regionListCalls = new AtomicInteger();
        List<de.ottoextra.api.model.RegionRecord> fresh = GSON.fromJson("""
                [{"id":"lehen_17","region_ref":"lehen_17","region_id":"17","name":"Niederpölt",
                  "current_faction":{"uuid":"f-17","name":"Kelterfels","region_id":"17",
                  "region_name":"Niederpölt"}}]
                """, new com.google.gson.reflect.TypeToken<List<de.ottoextra.api.model.RegionRecord>>() { }.getType());
        OttoExtraApiClient api = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("bootstrap")) {
                        return CompletableFuture.failedFuture(new IllegalStateException("HTTP 500"));
                    }
                    if (method.getName().equals("regionList")) {
                        regionListCalls.incrementAndGet();
                        return CompletableFuture.completedFuture(fresh);
                    }
                    return new CompletableFuture<>();
                });
        RegionDataService service = new RegionDataService(api, 30, tempDir.resolve("snapshot.json"));
        try {
            service.applyEnvelope(envelope("""
                    {"ok":true,"snapshot_complete":true,"sync_cursor":5,
                     "factions":[{"uuid":"f-17","name":"Südwall","region_id":"17","region_name":"Niederpölt"}],
                     "regions":[{"id":"lehen_17","region_ref":"lehen_17","region_id":"17","name":"Niederpölt",
                      "current_faction":{"uuid":"f-17","name":"Südwall","region_id":"17"}}]}
                    """), true);
            assertEquals("Südwall", service.factionForRegion("lehen_17").orElseThrow().name());

            service.onServerJoin(false);
            assertEquals(1, regionListCalls.get());
            assertEquals("Kelterfels", service.factionForRegion("lehen_17").orElseThrow().name());
            var cursor = RegionDataService.class.getDeclaredField("syncCursor");
            cursor.setAccessible(true);
            assertEquals(5L, cursor.getLong(service));
            var ready = RegionDataService.class.getDeclaredField("bootstrapReady");
            ready.setAccessible(true);
            assertFalse(ready.getBoolean(service));

            // A second failure inside the throttle window must not hit the region list again.
            service.forceRefresh();
            assertEquals(1, regionListCalls.get());
        } finally {
            service.shutdown();
        }
    }

    private static ApiEnvelope envelope(String json) {
        return GSON.fromJson(json, ApiEnvelope.class);
    }
}
