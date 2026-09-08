package de.ottoextra.regions;

import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BannerTextureService {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};
    private static final long FAILED_RETRY_NANOS = Duration.ofSeconds(45).toNanos();
    private static final long REFRESH_INTERVAL_NANOS = Duration.ofMinutes(20).toNanos();

    private final OttoExtraApiClient api;
    private final Map<String, Identifier> texturesByUuid = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> retryAfterNanos = new ConcurrentHashMap<>();
    private final Map<String, Long> refreshedAtNanos = new ConcurrentHashMap<>();

    public BannerTextureService(OttoExtraApiClient api) {
        this.api = api;
    }

    public void onServerJoin() {
        retryAfterNanos.clear();
        refreshedAtNanos.clear();
    }

    /** Uses explicit/suggested region banners even when there is no player faction. */
    public Optional<Identifier> bannerForRegion(RegionDataService data, String regionName) {
        return RegionBannerResolver.resolve(data, regionName, this::bannerForPath, this::bannerFor);
    }

    public Optional<Identifier> bannerFor(FactionRecord faction) {
        if (faction == null || faction.uuid() == null || faction.uuid().isBlank()) {
            return Optional.empty();
        }
        String uuid = faction.uuid().toLowerCase(Locale.ROOT);
        Identifier ready = texturesByUuid.get(uuid);
        if (ready != null) {
            maybeRefresh(uuid, faction.effectiveBannerPath());
            return Optional.of(ready);
        }
        if (retryBlocked(uuid) || !inFlight.add(uuid)) {
            return Optional.empty();
        }

        Path cached = OttoExtraPaths.bannersCache().resolve(uuid + ".png");
        if (Files.exists(cached)) {
            registerFromDisk(uuid, cached);
            maybeRefresh(uuid, faction.effectiveBannerPath());
            return Optional.empty();
        }

        String relative = faction.effectiveBannerPath();
        if ((relative == null || relative.isBlank())
                && faction.banner_name() != null && !faction.banner_name().isBlank()) {

            Identifier bundled = bundledBanner(faction.banner_name());
            inFlight.remove(uuid);
            if (bundled != null) {
                texturesByUuid.put(uuid, bundled);
                return Optional.of(bundled);
            }
            markFailed(uuid);
            return Optional.empty();
        }
        return startDownload(uuid, relative);
    }

    public Optional<Identifier> bannerForPath(String cacheKey, String relativePath) {
        if (cacheKey == null || cacheKey.isBlank()
                || relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }
        String key = RegionNameKeys.sanitizeFileStem(cacheKey).toLowerCase(Locale.ROOT);
        Identifier ready = texturesByUuid.get(key);
        if (ready != null) {
            maybeRefresh(key, relativePath);
            return Optional.of(ready);
        }
        if (retryBlocked(key) || !inFlight.add(key)) {
            return Optional.empty();
        }
        Path cached = OttoExtraPaths.bannersCache().resolve(key + ".png");
        if (Files.exists(cached)) {
            registerFromDisk(key, cached);
            maybeRefresh(key, relativePath);
            return Optional.empty();
        }
        return startDownload(key, relativePath);
    }

    private Optional<Identifier> startDownload(String uuid, String relative) {
        if (!OttoExtraConfig.active().consent.externalImageDownloads) {
            inFlight.remove(uuid);
            return Optional.empty();
        }
        Path cached = OttoExtraPaths.bannersCache().resolve(uuid + ".png");
        URI uri = api.routes().resolveRelative(relative);
        if (uri == null) {
            inFlight.remove(uuid);
            markFailed(uuid);
            return Optional.empty();
        }
        api.downloadBinary(uri).whenComplete((bytes, t) -> {
            if (t != null || bytes == null || !isPng(bytes)) {
                DebugLog.debug("[regions] Banner-Download fehlgeschlagen ({}): {}",
                        uri, t != null ? t.toString() : "kein PNG");
                inFlight.remove(uuid);
                markFailed(uuid);
                return;
            }
            try {
                Files.createDirectories(cached.getParent());
                Path tmp = cached.resolveSibling(uuid + ".png.tmp");
                Files.write(tmp, bytes);
                try {
                    Files.move(tmp, cached, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception atomicUnsupported) {
                    Files.move(tmp, cached, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                DebugLog.debug("[regions] Banner-Cache schreiben fehlgeschlagen: {}", e.getMessage());
            }
            writeSourceMarker(uuid, relative);
            refreshedAtNanos.put(uuid, System.nanoTime());
            registerBytes(uuid, bytes);
        });
        return Optional.empty();
    }

    private void maybeRefresh(String key, String relativePath) {
        if (!OttoExtraConfig.active().consent.externalImageDownloads) {
            return;
        }
        if (relativePath == null || relativePath.isBlank() || !beginRefresh(key)) {
            return;
        }
        URI uri = api.routes().resolveRelative(relativePath);
        if (uri == null) {
            inFlight.remove(key);
            return;
        }
        api.downloadBinary(uri).whenComplete((bytes, t) -> {
            if (t != null || bytes == null || !isPng(bytes)) {
                inFlight.remove(key);
                return;
            }
            try {
                Path cached = OttoExtraPaths.bannersCache().resolve(key + ".png");
                Files.createDirectories(cached.getParent());
                Path tmp = cached.resolveSibling(key + ".png.tmp");
                Files.write(tmp, bytes);
                try {
                    Files.move(tmp, cached, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception atomicUnsupported) {
                    Files.move(tmp, cached, StandardCopyOption.REPLACE_EXISTING);
                }
                writeSourceMarker(key, relativePath);
            } catch (Exception e) {
                DebugLog.debug("[regions] Banner-Refresh schreiben fehlgeschlagen: {}", e.getMessage());
            }
            registerBytes(key, bytes);
        });
    }

    private boolean retryBlocked(String key) {
        Long retryAfter = retryAfterNanos.get(key);
        if (retryAfter == null) {
            return false;
        }
        if (System.nanoTime() - retryAfter < 0) {
            return true;
        }
        retryAfterNanos.remove(key, retryAfter);
        return false;
    }

    private void markFailed(String key) {
        inFlight.remove(key);
        retryAfterNanos.put(key, System.nanoTime() + FAILED_RETRY_NANOS);
    }

    private boolean beginRefresh(String key) {
        long now = System.nanoTime();
        Long previous = refreshedAtNanos.get(key);
        if (previous != null && now - previous < REFRESH_INTERVAL_NANOS) {
            return false;
        }
        if (!inFlight.add(key)) {
            return false;
        }
        refreshedAtNanos.put(key, now);
        return true;
    }

    private static void writeSourceMarker(String key, String relativePath) {
        try {
            Path src = OttoExtraPaths.bannersCache().resolve(key + ".src");
            Files.createDirectories(src.getParent());
            Files.writeString(src, relativePath == null ? "" : relativePath);
        } catch (Exception ignored) {

        }
    }

    private static Identifier bundledBanner(String bannerName) {
        String stem = RegionNameKeys.sanitizeFileStem(bannerName).toLowerCase(Locale.ROOT);
        Identifier id = OttoExtra.id("textures/banners/custom_" + stem + ".png");
        boolean exists = MinecraftClient.getInstance().getResourceManager()
                .getResource(id).isPresent();
        return exists ? id : null;
    }

    private void registerFromDisk(String uuid, Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (!isPng(bytes)) {
                inFlight.remove(uuid);
                markFailed(uuid);
                return;
            }
            registerBytes(uuid, bytes);
        } catch (Exception e) {
            inFlight.remove(uuid);
            markFailed(uuid);
        }
    }

    private void registerBytes(String uuid, byte[] bytes) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            inFlight.remove(uuid);
            return;
        }
        client.execute(() -> {
            try {
                NativeImage image = NativeImage.read(bytes);
                NativeImageBackedTexture texture =
                        new NativeImageBackedTexture(() -> "ottoextra-banner-" + uuid, image);
                Identifier id = OttoExtra.id("dynamic/banners/" + uuid);
                client.getTextureManager().registerTexture(id, texture);
                texturesByUuid.put(uuid, id);
                retryAfterNanos.remove(uuid);
            } catch (Throwable t) {
                markFailed(uuid);
                DebugLog.debug("[regions] Banner-Textur fehlgeschlagen ({}): {}", uuid, t.toString());
            } finally {
                inFlight.remove(uuid);
            }
        });
    }

    private static boolean isPng(byte[] bytes) {
        if (bytes == null || bytes.length < PNG_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (bytes[i] != PNG_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
