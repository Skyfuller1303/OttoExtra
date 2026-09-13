package de.ottoextra.regions;

import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.privacy.ConsentPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RegionDataConsentTest {
    @TempDir Path directory;

    @Test void deniedJoinRefreshAndDetailNeverCallApi() {
        OttoExtraConfig config = new OttoExtraConfig();
        AtomicInteger calls = new AtomicInteger();
        var api = api(calls, new CompletableFuture<>());
        var service = service(api, config);
        try {
            service.onServerJoin(false);
            service.refreshNow();
            service.syncNow();
            assertThrows(RuntimeException.class, () -> service.requestRegionDetailAsync("example").join());
            assertEquals(0, calls.get());
            assertFalse(Files.exists(directory.resolve("snapshot.json")));
        } finally { service.shutdown(); }
    }

    @Test void noncancellableOldResponseCannotWriteSnapshotAfterRegrant() {
        OttoExtraConfig config = new OttoExtraConfig();
        decide(config, true);
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<ApiEnvelope> response = new CompletableFuture<>() {
            @Override public boolean cancel(boolean mayInterrupt) { return false; }
        };
        var service = service(api(calls, response), config);
        try {
            service.onServerJoin(false);
            decide(config, false);
            decide(config, true);
            response.complete(new Gson().fromJson("""
                    {"ok":true,"sync_cursor":1,"factions":[{"uuid":"old","name":"old"}],
                    "regions":[{"id":"old","name":"old"}]}
                    """, ApiEnvelope.class));
            assertFalse(Files.exists(directory.resolve("snapshot.json")));
            assertTrue(service.regionByName("old").isEmpty());
            assertEquals(1, calls.get());
        } finally { service.shutdown(); }
    }

    private RegionDataService service(OttoExtraApiClient api, OttoExtraConfig config) {
        return new RegionDataService(api, 30, directory.resolve("snapshot.json"),
                () -> ConsentPolicy.capture(config, ConsentPolicy.Purpose.ONLINE_SERVICES));
    }

    private static void decide(OttoExtraConfig config, boolean allowed) {
        ConsentPolicy.decide(config, ConsentPolicy.Purpose.ONLINE_SERVICES, allowed,
                ConsentPolicy.scope(config, ConsentPolicy.Purpose.ONLINE_SERVICES));
    }

    private static OttoExtraApiClient api(AtomicInteger calls, CompletableFuture<ApiEnvelope> response) {
        return (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> {
                    calls.incrementAndGet();
                    return response;
                });
    }
}
