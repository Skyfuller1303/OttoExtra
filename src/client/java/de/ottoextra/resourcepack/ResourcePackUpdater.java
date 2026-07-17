package de.ottoextra.resourcepack;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
public final class ResourcePackUpdater {
    private final OttoExtraConfig.ResourcePack cfg;
    private final ExecutorService httpExecutor;
    private final ExecutorService ioExecutor;
    private final HttpClient http;
    private final PackManifestClient manifestClient;
    private final PackDownloadService downloadService;
    public ResourcePackUpdater(OttoExtraConfig config) {
        this.cfg = config.resourcepack;
        this.httpExecutor = Executors.newFixedThreadPool(2, daemonThreads("http"));
        this.ioExecutor = Executors.newSingleThreadExecutor(daemonThreads("io"));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1_000, cfg.connectTimeoutMs)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(httpExecutor)
                .build();
        Duration reqTimeout = Duration.ofMillis(Math.max(1_000, cfg.requestTimeoutMs));
        this.manifestClient = new PackManifestClient(http, reqTimeout);
        this.downloadService = new PackDownloadService(http, reqTimeout, cfg.maxSizeBytes, ioExecutor);
    }
    private static ThreadFactory daemonThreads(String role) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "ottoextra-resourcepack-" + role + "-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
    public void runAsync() {
        String source = cfg.effectiveSource();
        if (source.isBlank()) {
            OttoExtra.LOGGER.info("[resourcepack] Keine Quelle konfiguriert — uebersprungen.");
            return;
        }
        if (!source.startsWith("https://")) {
            OttoExtra.LOGGER.warn("[resourcepack] Quelle ist kein https:// — aus Sicherheitsgründen übersprungen.");
            return;
        }
        if (!cfg.checkOnStartup) {
            OttoExtra.LOGGER.info("[resourcepack] checkOnStartup=false — kein Update-Check.");
            return;
        }
        PackState state = PackState.load();
        try {
            CompletableFuture<Void> flow = cfg.usesManifest()
                    ? manifestFlow(URI.create(source), state)
                    : directZipFlow(URI.create(source), state);
            flow.exceptionally(t -> {
                handleFailure(t, state);
                return null;
            });
        } catch (Exception e) {
            handleFailure(e, state);
        }
    }
    private CompletableFuture<Void> manifestFlow(URI manifestUri, PackState state) {
        return manifestClient.fetch(manifestUri, cfg.assetName).thenCompose(manifest -> {
            boolean upToDate = manifest.hasSha()
                    ? state.matchesSha(manifest.sha256())
                    : (manifest.version() != null && manifest.version().equals(state.version));
            if (upToDate) {
                OttoExtra.LOGGER.info("[resourcepack] Aktuell (Version {}) — kein Download.", state.version);
                markChecked(state, manifest.version());
                ensureActivation(state);
                return CompletableFuture.completedFuture(null);
            }
            if (manifest.sizeBytes() != null && manifest.sizeBytes() > cfg.maxSizeBytes) {
                OttoExtra.LOGGER.warn("[resourcepack] Manifest meldet {} B > Limit {} B — uebersprungen.",
                        manifest.sizeBytes(), cfg.maxSizeBytes);
                markChecked(state, manifest.version());
                return CompletableFuture.completedFuture(null);
            }
            URI zip = URI.create(manifest.url());
            if (!zip.getScheme().equalsIgnoreCase("https")) {
                OttoExtra.LOGGER.warn("[resourcepack] Download-URL ist kein https — uebersprungen.");
                markChecked(state, manifest.version());
                return CompletableFuture.completedFuture(null);
            }
            OttoExtra.LOGGER.info("[resourcepack] Neue Version {} verfuegbar (lokal: {}) — lade herunter.",
                    manifest.version(), state.version != null ? state.version : "keine");
            return downloadService.download(zip, manifest.sha256()).thenAccept(tmp ->
                    installAndMaybeActivate(tmp, manifest.version(), manifest.sha256(), state, manifest.notes()));
        });
    }
    private CompletableFuture<Void> directZipFlow(URI zipUri, PackState state) {
        if (Files.exists(OttoExtraPaths.serverPackFile())) {
            OttoExtra.LOGGER.info("[resourcepack] Direkt-Modus: Pack vorhanden, kein erneuter Download (ETag-Check folgt).");
            ensureActivation(state);
            return CompletableFuture.completedFuture(null);
        }
        return downloadService.download(zipUri, null).thenAccept(tmp ->
                installAndMaybeActivate(tmp, "direct", null, state, null));
    }
    private void installAndMaybeActivate(java.nio.file.Path tmp, String version, String sha,
                                         PackState previous, String notes) {
        try {
            PackInstaller.install(tmp);
        } catch (Exception e) {
            OttoExtra.LOGGER.error("[resourcepack] Installation fehlgeschlagen: {}", e.getMessage());
            return;
        }
        String now = Instant.now().toString();
        PackState ns = new PackState();
        ns.version = version;
        ns.sha256 = sha;
        ns.installedAt = now;
        ns.lastCheckedAt = now;
        ns.remoteVersion = version;
        ns.enabled = previous.enabled;
        ns.save();
        OttoExtra.LOGGER.info("[resourcepack] Installiert: Version {}{}", version,
                notes != null && !notes.isBlank() ? " (" + notes + ")" : "");
        boolean userDisabled = cfg.respectUserDisable && !previous.enabled;
        if (cfg.autoEnable && !userDisabled) {
            PackInstaller.requestActivation(cfg.priorityTop);
            OttoExtra.LOGGER.info("[resourcepack] Aktivierung vorgemerkt (greift am Titelscreen).");
        } else {
            OttoExtra.LOGGER.info("[resourcepack] Geladen, aber nicht automatisch aktiviert (Config/Spielerwunsch).");
        }
    }
    private void handleFailure(Throwable t, PackState state) {
        Throwable cause = (t.getCause() != null) ? t.getCause() : t;
        OttoExtra.LOGGER.info("[resourcepack] Update nicht moeglich ({}). Gecachter Pack bleibt aktiv.",
                cause.getMessage());
        markChecked(state, null);
    }
    private void ensureActivation(PackState state) {
        boolean userDisabled = cfg.respectUserDisable && !state.enabled;
        if (!cfg.autoEnable || userDisabled) {
            return;
        }
        PackInstaller.requestActivation(cfg.priorityTop);
    }
    private void markChecked(PackState state, String remoteVersion) {
        state.lastCheckedAt = Instant.now().toString();
        if (remoteVersion != null) {
            state.remoteVersion = remoteVersion;
        }
        state.save();
    }
    public void close() {
        httpExecutor.shutdownNow();
        ioExecutor.shutdownNow();
        try {
            http.close();
        } catch (Throwable ignored) {
        }
    }
}
