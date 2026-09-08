package de.ottoextra.regions;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RegionSyncLifecycleTest {
    @Test
    void lateSyncCannotReplaceNewBootstrapOrReleaseItsPendingSync(@TempDir Path dir) throws Exception {
        List<CompletableFuture<ApiEnvelope>> bootstraps = new ArrayList<>();
        List<CompletableFuture<ApiEnvelope>> syncs = new ArrayList<>();
        List<Long> cursors = new ArrayList<>();
        OttoExtraApiClient api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> {
                    var future = new CompletableFuture<ApiEnvelope>();
                    if (method.getName().equals("bootstrap")) bootstraps.add(future);
                    else if (method.getName().equals("sync")) {
                        syncs.add(future);
                        cursors.add((Long) args[0]);
                    } else throw new AssertionError(method.getName());
                    return future;
                });
        Path file = dir.resolve("snapshot.json");
        var service = new RegionDataService(api, 30, file);
        try {
            service.onServerJoin(false);
            bootstraps.get(0).complete(envelope(10, "Alt"));
            service.syncNow();
            service.onDisconnect();
            service.onServerJoin(false);
            bootstraps.get(1).complete(envelope(30, "Neu"));
            service.syncNow();
            syncs.get(0).complete(envelope(11, "Veraltet"));
            service.syncNow();
            assertEquals(List.of(10L, 30L), cursors);
            assertEquals("Neu", service.factionByUuid("faction").orElseThrow().name());
            var field = RegionDataService.class.getDeclaredField("scheduler");
            field.setAccessible(true);
            ((ScheduledExecutorService) field.get(service)).submit(() -> { }).get(5, TimeUnit.SECONDS);
            var saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            assertEquals(30, saved.get("syncCursor").getAsLong());
            assertFalse(Files.readString(file).contains("Veraltet"));
            syncs.get(1).complete(envelope(31, "Aktuell"));
            service.syncNow();
            assertEquals(31L, cursors.getLast());
        } finally {
            var field = RegionDataService.class.getDeclaredField("scheduler");
            field.setAccessible(true);
            var scheduler = (ScheduledExecutorService) field.get(service);
            scheduler.submit(() -> { }).get(5, TimeUnit.SECONDS);
            service.shutdown();
            assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static ApiEnvelope envelope(int cursor, String name) {
        return new Gson().fromJson("{\"ok\":true,\"sync_cursor\":" + cursor
                + ",\"factions\":[{\"uuid\":\"faction\",\"name\":\"" + name
                + "\"}],\"regions\":[{\"id\":\"lehen_1\",\"name\":\"Region\"}]}", ApiEnvelope.class);
    }
}
