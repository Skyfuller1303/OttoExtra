package de.ottoextra.resourcepack;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PackDownloadLifecycleTest {
    private static final URI URI_VALUE = URI.create("https://example.invalid/pack.zip");
    @TempDir Path directory;
    private Field active;
    private Object previous;
    private OttoExtraConfig config;

    @BeforeEach void configure() throws Exception {
        active = OttoExtraConfig.class.getDeclaredField("active");
        active.setAccessible(true);
        previous = active.get(null);
        config = new OttoExtraConfig();
        config.consent.automaticResourcePackDownloads = true;
        active.set(null, config);
    }

    @AfterEach void restore() throws Exception { active.set(null, previous); }

    @Test void httpErrorClosesBodyWithoutCreatingFile() {
        TrackingStream body = new TrackingStream(new byte[8]);
        assertThrows(Exception.class, () -> service(100).handle(URI_VALUE, response(503, body), null,
                () -> { fail("HTTP error must not create a file"); return null; }));
        assertTrue(body.closed);
    }

    @Test void revokedConsentBeforeResponseClosesBodyWithoutWriting() {
        TrackingStream body = new TrackingStream(new byte[8]);
        config.consent.automaticResourcePackDownloads = false;
        assertThrows(Exception.class, () -> service(100).handle(URI_VALUE, response(200, body), null,
                () -> { fail("Revoked consent must prevent file creation"); return null; }));
        assertTrue(body.closed);
    }

    @Test void revokedConsentDuringReadDeletesPartialFile() throws Exception {
        TrackingStream body = new TrackingStream(zip());
        body.onRead = () -> config.consent.automaticResourcePackDownloads = false;
        Path target = directory.resolve("pack.tmp");
        assertThrows(Exception.class, () -> service(10000).handle(URI_VALUE, response(200, body), null, () -> target));
        assertTrue(body.closed);
        assertFalse(Files.exists(target));
    }

    @Test void disablingModuleDuringReadAlsoStopsDownload() throws Exception {
        TrackingStream body = new TrackingStream(zip());
        body.onRead = () -> config.resourcepack.enabled = false;
        Path target = directory.resolve("pack.tmp");
        assertThrows(Exception.class, () -> service(10000).handle(URI_VALUE, response(200, body), null, () -> target));
        assertTrue(body.closed);
        assertFalse(Files.exists(target));
    }

    @Test void acceptedZipIsPreservedAndResponseClosed() throws Exception {
        byte[] content = zip();
        TrackingStream body = new TrackingStream(content);
        Path target = directory.resolve("pack.tmp");
        assertEquals(target, service(10000).handle(URI_VALUE, response(200, body), null, () -> target));
        assertArrayEquals(content, Files.readAllBytes(target));
        assertTrue(body.closed);
    }

    @Test void oversizedResponseDeletesPartialFile() {
        TrackingStream body = new TrackingStream(new byte[20]);
        Path target = directory.resolve("pack.tmp");
        assertThrows(Exception.class, () -> service(8).handle(URI_VALUE, response(200, body), null, () -> target));
        assertTrue(body.closed);
        assertFalse(Files.exists(target));
    }

    private static PackDownloadService service(long maxBytes) {
        return new PackDownloadService(null, Duration.ofSeconds(2), maxBytes, Runnable::run);
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> response(int status, InputStream body) {
        return (HttpResponse<InputStream>) Proxy.newProxyInstance(HttpResponse.class.getClassLoader(),
                new Class<?>[]{HttpResponse.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "statusCode" -> status;
                    case "body" -> body;
                    default -> throw new AssertionError(method.getName());
                });
    }

    private static byte[] zip() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private static final class TrackingStream extends ByteArrayInputStream {
        boolean closed;
        Runnable onRead;
        TrackingStream(byte[] bytes) { super(bytes); }
        @Override public synchronized int read(byte[] bytes, int offset, int length) {
            int count = super.read(bytes, offset, length);
            if (onRead != null) onRead.run();
            return count;
        }
        @Override public void close() { closed = true; }
    }
}
