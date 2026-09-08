package de.ottoextra.resourcepack;

import de.ottoextra.OttoExtra;
import de.ottoextra.api.ApiProblem;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.config.OttoExtraConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PackDownloadService {

    private static final int BUFFER = 64 * 1024;

    private final HttpClient http;
    private final Duration requestTimeout;
    private final long maxBytes;
    private final Executor worker;

    public PackDownloadService(HttpClient http, Duration requestTimeout, long maxBytes, Executor worker) {
        this.http = http;
        this.requestTimeout = requestTimeout;
        this.maxBytes = maxBytes;
        this.worker = worker;
    }

    public CompletableFuture<Path> download(URI zipUri, String expectedSha) {
        if (!downloadsAllowed()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Resourcepack-Download nicht erlaubt"));
        }
        HttpRequest request = HttpRequest.newBuilder(zipUri)
                .timeout(requestTimeout)
                .header("Accept", "application/zip")
                .header("User-Agent", "OttoExtra-resourcepack")
                .GET()
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApplyAsync(resp -> handle(zipUri, resp, expectedSha), worker);
    }

    private Path handle(URI uri, HttpResponse<InputStream> resp, String expectedSha) {
        return handle(uri, resp, expectedSha, OttoExtraPaths::resourcepackTmp);
    }

    Path handle(URI uri, HttpResponse<InputStream> resp, String expectedSha, Supplier<Path> target) {
        Path tmp = null;
        try (InputStream in = resp.body()) {
            if (resp.statusCode() / 100 != 2) {
                throw ApiProblem.httpStatus(uri, resp.statusCode()).toException();
            }
            requireDownloadAllowed();
            tmp = target.get();
            Files.createDirectories(tmp.getParent());
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (OutputStream out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[BUFFER];
                int n;
                while ((n = in.read(buf)) != -1) {
                    requireDownloadAllowed();
                    total += n;
                    if (total > maxBytes) {
                        throw ApiProblem.parse(uri, "Download ueberschreitet Limit (" + maxBytes + " B)").toException();
                    }
                    sha.update(buf, 0, n);
                    out.write(buf, 0, n);
                }
            }

            requireDownloadAllowed();

            if (expectedSha != null && !expectedSha.isBlank()) {
                String got = HexFormat.of().formatHex(sha.digest());
                if (!got.equalsIgnoreCase(expectedSha.trim())) {
                    throw ApiProblem.parse(uri, "SHA-256 stimmt nicht (erwartet " + expectedSha + ")").toException();
                }
            }

            verifyZip(uri, tmp);
            OttoExtra.LOGGER.info("[resourcepack] Download ok ({} B) -> {}", total, tmp.getFileName());
            return tmp;
        } catch (ApiProblem.ApiException e) {
            safeDelete(tmp);
            throw e;
        } catch (Exception e) {
            safeDelete(tmp);
            throw ApiProblem.parse(uri, e.getClass().getSimpleName() + ": " + e.getMessage()).toException();
        }
    }

    static boolean downloadsAllowed() {
        var config = OttoExtraConfig.active();
        return config != null && config.resourcepack.enabled
                && config.consent.automaticResourcePackDownloads;
    }

    private static void requireDownloadAllowed() {
        if (!downloadsAllowed() || Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("Resourcepack-Download abgebrochen");
        }
    }

    private static void verifyZip(URI uri, Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry mcmeta = zip.getEntry("pack.mcmeta");
            if (mcmeta == null) {
                throw ApiProblem.parse(uri, "ZIP enthaelt kein pack.mcmeta").toException();
            }
        } catch (ApiProblem.ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiProblem.parse(uri, "kein gueltiges ZIP: " + e.getMessage()).toException();
        }
    }

    private static void safeDelete(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {

        }
    }
}
