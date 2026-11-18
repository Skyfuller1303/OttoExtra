package de.ottoextra.regions;

import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.api.model.RegionRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RegionDetailRefreshTest {
    private static final Gson GSON = new Gson();
    @TempDir Path directory;

    @Test
    void openViewAwaitsExistingPrefetchAndRefreshesAllDetailsOnClientExecutor() throws Exception {
        List<CompletableFuture<RegionRecord>> remote = new ArrayList<>();
        RegionDataService service = service(remote);
        try {
            service.applyEnvelope(GSON.fromJson("""
                    {"ok":true,"regions":[{"id":"lehen_1","name":"Nordmark",
                     "region_info":{"note":"Alter Stand"}}],
                     "factions":[{"uuid":"vassal","name":"Vasall"}]}
                    """, ApiEnvelope.class), false);
            AtomicReference<RegionInfoScreen.DataSnapshot> shown = new AtomicReference<>(
                    RegionInfoScreen.DataSnapshot.read(service, "Nordmark"));
            assertNull(shown.get().faction());
            service.requestRegionDetail("Nordmark"); // Started by the region-enter message.
            var completion = service.requestRegionDetailAsync("Nordmark");
            assertEquals(1, remote.size());
            assertFalse(completion.isDone());
            ArrayDeque<Runnable> clientTasks = new ArrayDeque<>();
            var view = new RegionInfoScreen.ViewRequests();
            view.whenComplete(completion, clientTasks::add, () -> true, (ignored, error) -> {
                assertNull(error);
                shown.set(RegionInfoScreen.DataSnapshot.read(service, "Nordmark"));
            });

            remote.getFirst().complete(detail("Neue Notiz"));

            assertEquals("Alter Stand", shown.get().region().region_info().note());
            assertEquals(1, clientTasks.size());
            clientTasks.remove().run();
            assertEquals("Neue Notiz", shown.get().region().region_info().note());
            assertEquals("Getreide", shown.get().region().region_capabilities().agriculture());
            assertEquals("Neue Fraktion", shown.get().faction().name());
            assertEquals("Vasall", shown.get().vassals().getFirst().name());
            assertTrue(service.requestRegionDetailAsync("Nordmark").isDone());
            assertEquals(1, remote.size()); // Successful refresh still respects the cooldown.
        } finally {
            stop(service);
        }
    }

    @Test
    void queuedResponseCannotUpdateClosedOrReopenedViewLifetime() {
        var view = new RegionInfoScreen.ViewRequests();
        var request = new CompletableFuture<String>();
        ArrayDeque<Runnable> clientTasks = new ArrayDeque<>();
        AtomicInteger updates = new AtomicInteger();
        view.whenComplete(request, clientTasks::add, () -> true,
                (value, error) -> updates.incrementAndGet());
        request.complete("ready");
        view.invalidate(); // removed(), even if the same screen object is reopened.
        clientTasks.remove().run();
        assertEquals(0, updates.get());
        view.whenComplete(request, clientTasks::add, () -> true,
                (value, error) -> updates.incrementAndGet());
        clientTasks.remove().run();
        assertEquals(1, updates.get());
    }

    @Test
    void responseForAnotherRegionOrPreviousFactionIsIgnored() {
        var view = new RegionInfoScreen.ViewRequests();
        var request = new CompletableFuture<String>();
        AtomicReference<String> selected = new AtomicReference<>("Nordmark");
        AtomicInteger updates = new AtomicInteger();
        ArrayDeque<Runnable> clientTasks = new ArrayDeque<>();
        view.whenComplete(request, clientTasks::add, () -> selected.get().equals("Nordmark"),
                (value, error) -> updates.incrementAndGet());
        request.complete("old region or faction");
        selected.set("Südmark");
        clientTasks.remove().run();
        assertEquals(0, updates.get());
    }

    @Test
    void failedDetailsLeaveCacheIntactAndAllowANewAttempt() throws Exception {
        List<CompletableFuture<RegionRecord>> remote = new ArrayList<>();
        RegionDataService service = service(remote);
        try {
            var failed = service.requestRegionDetailAsync("Nordmark");
            remote.getFirst().completeExceptionally(new IllegalStateException("temporary"));
            assertTrue(failed.isCompletedExceptionally());
            assertTrue(service.regionByName("Nordmark").isEmpty());
            var retry = service.requestRegionDetailAsync("Nordmark");
            assertEquals(2, remote.size());
            remote.getLast().complete(detail("Nach Retry"));
            assertFalse(retry.isCompletedExceptionally());
            assertEquals("Nach Retry", service.regionByName("Nordmark").orElseThrow().region_info().note());
        } finally {
            stop(service);
        }
    }

    @Test
    void oldConnectionDetailCannotReplaceNewDetailsOrReleaseNewRequest() throws Exception {
        List<CompletableFuture<RegionRecord>> remote = new ArrayList<>();
        RegionDataService service = service(remote);
        try {
            var old = service.requestRegionDetailAsync("Nordmark");
            service.onDisconnect();
            var current = service.requestRegionDetailAsync("Nordmark");
            remote.getFirst().complete(detail("Alt"));
            assertTrue(old.isCancelled());
            assertSame(current, service.requestRegionDetailAsync("Nordmark"));
            assertEquals(2, remote.size());
            assertTrue(service.regionByName("Nordmark").isEmpty());
            remote.getLast().complete(detail("Neu"));
            assertEquals("Neu", service.regionByName("Nordmark").orElseThrow().region_info().note());
        } finally {
            stop(service);
        }
    }

    @Test
    void detailStartedBeforeAuthoritativeSnapshotCannotOverwriteIt() throws Exception {
        List<CompletableFuture<RegionRecord>> remote = new ArrayList<>();
        RegionDataService service = service(remote);
        try {
            var stale = service.requestRegionDetailAsync("Nordmark");
            service.applyEnvelope(GSON.fromJson("""
                    {"ok":true,"sync_cursor":10,"snapshot_complete":true,
                     "factions":[],"regions":[{"id":"lehen_1","name":"Nordmark",
                     "region_info":{"note":"Snapshot"}}]}
                    """, ApiEnvelope.class), false);
            assertTrue(stale.isCancelled());
            var fresh = service.requestRegionDetailAsync("Nordmark");
            assertEquals(2, remote.size());
            remote.getFirst().complete(detail("Alt"));
            assertTrue(stale.isCancelled());
            assertFalse(fresh.isDone());
            assertEquals("Snapshot", service.regionByName("Nordmark").orElseThrow()
                    .region_info().note());
            remote.getLast().complete(detail("Fresh"));
            assertEquals("Fresh", service.regionByName("Nordmark").orElseThrow()
                    .region_info().note());
        } finally {
            stop(service);
        }
    }

    @Test
    void detailRenameRemovesOldAliasesForSameStableRegion() throws Exception {
        List<CompletableFuture<RegionRecord>> remote = new ArrayList<>();
        RegionDataService service = service(remote);
        try {
            service.applyEnvelope(GSON.fromJson("""
                    {"ok":true,"regions":[{"id":"lehen_1","region_ref":"region-1",
                     "region_id":"1","name":"Nordmark"}]}
                    """, ApiEnvelope.class), false);
            var refresh = service.requestRegionDetailAsync("Nordmark");
            remote.getFirst().complete(GSON.fromJson("""
                    {"id":"lehen_1","region_ref":"region-1","region_id":"1",
                     "name":"Nordmark Neu"}
                    """, RegionRecord.class));

            assertTrue(refresh.isDone());
            assertTrue(service.regionByName("Nordmark").isEmpty());
            assertEquals("Nordmark Neu", service.regionByName("Nordmark Neu").orElseThrow().name());
            assertEquals(1, service.allRegions().size());
        } finally {
            stop(service);
        }
    }

    private RegionDataService service(List<CompletableFuture<RegionRecord>> requests) {
        OttoExtraApiClient api = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> {
                    assertEquals("regionByName", method.getName());
                    var request = new CompletableFuture<RegionRecord>();
                    requests.add(request);
                    return request;
                });
        return new RegionDataService(api, 30, directory.resolve("snapshot.json"));
    }

    private static RegionRecord detail(String note) {
        return GSON.fromJson("""
                {"id":"lehen_1","name":"Nordmark","region_info":{"note":"%s"},
                 "region_capabilities":{"agriculture":"Getreide"},
                 "current_faction":{"uuid":"00000000-0000-0000-0000-000000000001",
                  "name":"Neue Fraktion","vassal_uuids":["vassal"]}}
                """.formatted(note), RegionRecord.class);
    }

    private static void stop(RegionDataService service) throws Exception {
        var field = RegionDataService.class.getDeclaredField("scheduler");
        field.setAccessible(true);
        var scheduler = (ScheduledExecutorService) field.get(service);
        scheduler.submit(() -> { }).get(5, TimeUnit.SECONDS);
        service.shutdown();
        assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
    }
}
