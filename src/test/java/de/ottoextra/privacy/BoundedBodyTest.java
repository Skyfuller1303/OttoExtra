package de.ottoextra.privacy;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class BoundedBodyTest {
    private static ConsentPermit permit() { return new ConsentPermit.Gate().capture(() -> true); }

    @Test void exactLimitSucceedsButAdvertisedAndChunkedExcessFail() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            boolean chunked = exchange.getRequestURI().getPath().equals("/chunked");
            int size = exchange.getRequestURI().getPath().equals("/exact") ? 1024 : 2048;
            try (exchange) {
                exchange.sendResponseHeaders(200, chunked ? 0 : size);
                exchange.getResponseBody().write(new byte[size]);
            }
        });
        server.start();
        try (var http = HttpClient.newHttpClient()) {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            assertEquals(1024, ConsentHttp.send(http, request(base + "/exact"), BoundedBody.bytes(1024), permit()).join().body().length);
            for (String path : new String[]{"/advertised", "/chunked"})
                assertThrows(java.util.concurrent.ExecutionException.class, () -> ConsentHttp.send(http, request(base + path),
                        BoundedBody.bytes(1024), permit()).get(2, TimeUnit.SECONDS));
        } finally { server.stop(0); }
    }

    @Test void slowBodyIsCancelledForAccumulatedAndStreamingResponses() throws Exception {
        for (boolean stream : new boolean[]{false, true}) {
            var release = new CountDownLatch(1);
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try (exchange) {
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().write(42);
                    exchange.getResponseBody().flush();
                    try { release.await(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                }
            });
            server.start();
            try (var http = HttpClient.newHttpClient()) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                        .timeout(Duration.ofMillis(600)).build();
                if (!stream) {
                    assertThrows(java.util.concurrent.ExecutionException.class, () -> ConsentHttp.send(http, request, BoundedBody.text(1024), permit())
                            .get(2, TimeUnit.SECONDS));
                } else {
                    try (var body = ConsentHttp.download(http, request, permit(), uri -> true).get(2, TimeUnit.SECONDS).body()) {
                        var read = CompletableFuture.supplyAsync(() -> {
                            try { return body.readNBytes(2); } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
                        });
                        assertThrows(java.util.concurrent.ExecutionException.class, () -> read.get(2, TimeUnit.SECONDS));
                    }
                }
            } finally { release.countDown(); server.stop(0); }
        }
    }

    @Test void pngDimensionsAreCheckedBeforePixelDecoding() throws Exception {
        byte[] png = new byte[33];
        var buffer = java.nio.ByteBuffer.wrap(png);
        buffer.put(new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10});
        buffer.putInt(13).putInt(0x49484452).putInt(64).putInt(64);
        assertDoesNotThrow(() -> PngLimits.check(png, 1024, 1024));
        buffer.putInt(16, Integer.MAX_VALUE);
        assertThrows(java.io.IOException.class, () -> PngLimits.check(png, 1024, 1024));
        buffer.putInt(16, -1);
        assertThrows(java.io.IOException.class, () -> PngLimits.check(png, 1024, 1024));
        assertThrows(java.io.IOException.class, () -> PngLimits.check(new byte[10], 1024, 1024));
        assertThrows(java.io.IOException.class, () -> PngLimits.check(png, 20, 1024));
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).build();
    }
}
