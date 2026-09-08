package de.ottoextra.rpnames.upload;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RpNameConsentQueueTest {
    @AfterEach void reset() { RpNameUploadService.resetObservedSession(); }

    @Test
    void queuedUploadsAreInvalidatedByRevocationAndCanBeObservedAgain() throws Exception {
        for (boolean reenableBeforeDrain : new boolean[]{false, true}) {
            RpNameUploadService.resetObservedSession();
            var config = new OttoExtraConfig();
            config.consent.setCommunityRpNameUploads(true);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger requests = new AtomicInteger();
            RpNameUploadService.Transport transport = (cfg, data, allowed) -> {
                requests.incrementAndGet();
                if (data.targetName().equals("A")) {
                    started.countDown();
                    try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new AssertionError(e); }
                }
                return new RpNameUploadService.Result(true, true, 200, "");
            };
            try {
                var a = RpNameUploadService.enqueue(config, data("A"), executor, transport);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                var b = RpNameUploadService.enqueue(config, data("B"), executor, transport);
                var c = RpNameUploadService.enqueue(config, data("C"), executor, transport);
                config.consent.setCommunityRpNameUploads(false);
                if (reenableBeforeDrain) config.consent.setCommunityRpNameUploads(true);
                release.countDown();
                assertTrue(a.get(5, TimeUnit.SECONDS).attempted());
                assertFalse(b.get(5, TimeUnit.SECONDS).attempted());
                assertFalse(c.get(5, TimeUnit.SECONDS).attempted());
                assertEquals(1, requests.get());
                config.consent.setCommunityRpNameUploads(true);
                assertTrue(RpNameUploadService.enqueue(config, data("B"), executor, transport).get(5, TimeUnit.SECONDS).attempted());
                assertEquals(2, requests.get());
            } finally {
                release.countDown();
                executor.shutdownNow();
            }
        }
    }

    private static RpNameUploadService.UploadData data(String target) {
        return RpNameUploadService.createUploadDataForActor(null, "Actor", target, null, "Name " + target);
    }
}
