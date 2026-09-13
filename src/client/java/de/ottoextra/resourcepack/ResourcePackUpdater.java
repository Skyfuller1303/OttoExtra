package de.ottoextra.resourcepack;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.privacy.ConsentPolicy;
import de.ottoextra.privacy.ConsentPermit;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ResourcePackUpdater {

    private final OttoExtraConfig config;
    private final OttoExtraConfig.ResourcePack cfg;
    private final ExecutorService httpExecutor;
    private final ExecutorService ioExecutor;
    private final HttpClient http;
    private final PackManifestClient manifestClient;
    private ConsentPermit attempt;

    public ResourcePackUpdater(OttoExtraConfig config) {
        this.config = config;
        this.cfg = config.resourcepack;

        this.httpExecutor = Executors.newFixedThreadPool(2, daemonThreads("http"));
        this.ioExecutor = Executors.newSingleThreadExecutor(daemonThreads("io"));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1_000, cfg.connectTimeoutMs)))
                .followRedirects(HttpClient.Redirect.NEVER)
                .executor(httpExecutor)
                .build();
        Duration reqTimeout = Duration.ofMillis(Math.max(1_000, cfg.requestTimeoutMs));
        this.manifestClient = new PackManifestClient(http, reqTimeout);
    }

    private static ThreadFactory daemonThreads(String role) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "ottoextra-resourcepack-" + role + "-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    public synchronized void runAsync() {
        if (!ConsentPolicy.allowed(config, ConsentPolicy.Purpose.RESOURCE_PACKS)) {
            return;
        }
        if (attempt != null && attempt.valid()) return;
        ConsentPermit permit = ConsentPolicy.capture(config, ConsentPolicy.Purpose.RESOURCE_PACKS);
        attempt = permit;
        String source = cfg.effectiveSource();
        if (source.isBlank()) {
            DebugLog.debug("[resourcepack] Keine Quelle konfiguriert — uebersprungen.");
            return;
        }
        if (!source.startsWith("https://")) {
            OttoExtra.LOGGER.warn("[resourcepack] Quelle ist kein https:// — aus Sicherheitsgründen übersprungen.");
            return;
        }
        if (!cfg.checkOnStartup) {
            DebugLog.debug("[resourcepack] checkOnStartup=false — kein Update-Check.");
            return;
        }

        PackState state = PackState.load();
        try {
            CompletableFuture<Void> flow = cfg.usesManifest()
                    ? manifestFlow(URI.create(source), state, permit)
                    : directZipFlow(URI.create(source), state, permit);
            flow.exceptionally(t -> {
                handleFailure(t, state, permit);
                return null;
            });
        } catch (Exception e) {
            handleFailure(e, state, permit);
        }
    }

    private CompletableFuture<Void> manifestFlow(URI manifestUri, PackState state, ConsentPermit permit) {
        return manifestClient.fetch(manifestUri, cfg.assetName, permit).thenComposeAsync(manifest -> {

            permit.require();
            boolean upToDate = isCurrent(manifest, state, OttoExtraPaths.serverPackFile());
            if (upToDate) {
                DebugLog.debug("[resourcepack] Aktuell (Version {}) — kein Download.", state.version);
                markChecked(state, manifest.version(), permit);
                ensureActivation(state, permit);
                return CompletableFuture.completedFuture(null);
            }
            if (manifest.sizeBytes() != null && manifest.sizeBytes() > cfg.maxSizeBytes) {
                OttoExtra.LOGGER.warn("[resourcepack] Manifest meldet {} B > Limit {} B — uebersprungen.",
                        manifest.sizeBytes(), cfg.maxSizeBytes);
                markChecked(state, manifest.version(), permit);
                return CompletableFuture.completedFuture(null);
            }
            URI zip = URI.create(manifest.url());
            if (!zip.getScheme().equalsIgnoreCase("https")) {
                OttoExtra.LOGGER.warn("[resourcepack] Download-URL ist kein https — uebersprungen.");
                markChecked(state, manifest.version(), permit);
                return CompletableFuture.completedFuture(null);
            }
            OttoExtra.LOGGER.info("[resourcepack] Neue Version {} verfuegbar (lokal: {}) — lade herunter.",
                    manifest.version(), state.version != null ? state.version : "keine");
            return downloadService().download(zip, manifest.sha256(), permit, manifestUri).thenAccept(tmp ->
                    installAndMaybeActivate(tmp, manifest.version(), manifest.sha256(), state, manifest.notes(), null, permit));
        }, ioExecutor);
    }

    static boolean isCurrent(PackManifest manifest, PackState state, java.nio.file.Path file) {
        if (!PackInstaller.usablePack(file)) return false;
        return manifest.hasSha() ? state.matchesSha(manifest.sha256())
                : manifest.version() != null && manifest.version().equals(state.version);
    }

    private static boolean previouslyInstalled(PackState state) {
        return state.installedAt != null || state.version != null || state.sha256 != null;
    }

    private CompletableFuture<Void> directZipFlow(URI zipUri, PackState state, ConsentPermit permit) {
        if (isDirectCurrent(zipUri, state, OttoExtraPaths.serverPackFile())) {
            DebugLog.debug("[resourcepack] Direkt-Modus: Pack aus derselben Quelle vorhanden.");
            ensureActivation(state, permit);
            return CompletableFuture.completedFuture(null);
        }
        return downloadService().download(zipUri, null, permit, zipUri).thenAccept(tmp ->
                installAndMaybeActivate(tmp, "direct", null, state, null, zipUri.normalize().toString(), permit));
    }

    static boolean isDirectCurrent(URI source, PackState state, java.nio.file.Path file) {
        // Old states have no source: download once to establish a trustworthy identity.
        return source.normalize().toString().equals(state.directSource) && PackInstaller.usablePack(file);
    }

    private PackDownloadService downloadService() {
        return new PackDownloadService(http, Duration.ofMillis(Math.max(1_000, cfg.requestTimeoutMs)),
                cfg.maxSizeBytes, ioExecutor);
    }

    private void installAndMaybeActivate(java.nio.file.Path tmp, String version, String sha,
                                         PackState previous, String notes, String directSource, ConsentPermit permit) {
        try {
            permit.commitIo(() -> {
                PackInstaller.install(tmp);
                String now = Instant.now().toString();
                PackState state = new PackState();
                state.version = version;
                state.sha256 = sha;
                state.directSource = directSource;
                state.installedAt = now;
                state.lastCheckedAt = now;
                state.remoteVersion = version;
                state.enabled = previous.enabled;
                state.save();
                PackInstaller.requestReload(permit);
                if (cfg.autoEnable) {
                    PackInstaller.requestActivation(cfg.priorityTop, cfg.respectUserDisable,
                            previouslyInstalled(previous), permit);
                }
            });
            OttoExtra.LOGGER.info("[resourcepack] Installiert: Version {}", version);
        } catch (Exception error) {
            DebugLog.debug("[resourcepack] Installation abgebrochen: {}", error.getClass().getSimpleName());
        } finally {
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (java.io.IOException ignored) { }
        }
    }

    private void handleFailure(Throwable t, PackState state, ConsentPermit permit) {
        if (!permit.valid()) return;
        Throwable cause = (t.getCause() != null) ? t.getCause() : t;
        DebugLog.debug("[resourcepack] Update nicht moeglich ({}). Gecachter Pack bleibt aktiv.",
                cause.getMessage());
        markChecked(state, null, permit);
    }

    private void ensureActivation(PackState state, ConsentPermit permit) {
        if (!PackDownloadService.downloadsAllowed() || !cfg.autoEnable) {
            return;
        }

        permit.commit(() -> PackInstaller.requestActivation(cfg.priorityTop, cfg.respectUserDisable,
                previouslyInstalled(state), permit));
    }

    private void markChecked(PackState state, String remoteVersion, ConsentPermit permit) {
        permit.commit(() -> {
            state.lastCheckedAt = Instant.now().toString();
            if (remoteVersion != null) {
                state.remoteVersion = remoteVersion;
            }
            state.save();
        });
    }

    public void close() {
        config.consent.gate(ConsentPolicy.Purpose.RESOURCE_PACKS).invalidate();
        httpExecutor.shutdownNow();
        ioExecutor.shutdownNow();
        try {
            http.close();
        } catch (Throwable ignored) {

        }
    }
}
