package de.ottoextra.regions;

import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.privacy.ConsentPolicy;
import de.ottoextra.privacy.ConsentPermit;
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

    private static final long FAILED_RETRY_NANOS = Duration.ofSeconds(45).toNanos();
    private static final long REFRESH_INTERVAL_NANOS = Duration.ofMinutes(20).toNanos();

    private final OttoExtraApiClient api;
    private final Map<String, Identifier> texturesByUuid = new ConcurrentHashMap<>();
    private final BannerTextureBudget textureBudget;
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> retryAfterNanos = new ConcurrentHashMap<>();
    private final Map<String, Long> refreshedAtNanos = new ConcurrentHashMap<>();
    // Source path each registered texture was built from; absent when unknown.
    private final Map<String, String> sourceByKey = new ConcurrentHashMap<>();

    public BannerTextureService(OttoExtraApiClient api) {
        this(api, new BannerTextureBudget());
    }

    BannerTextureService(OttoExtraApiClient api, BannerTextureBudget textureBudget) {
        this.api = api;
        this.textureBudget = textureBudget;
    }

    public void onServerJoin() {
        invalidateRefresh();
    }

    /** Lets the next lookup refresh without waiting for the regular interval or a failure backoff. */
    public void invalidateRefresh() {
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
        if (!uuid.matches("[a-z0-9_-]{1,128}")) return Optional.empty();
        String relative = faction.effectiveBannerPath();
        boolean bannerRemoved = relative == null || relative.isBlank();
        Identifier ready = texturesByUuid.get(uuid);
        if (ready != null && bannerRemoved && isDynamicBanner(ready)) {
            evictRemoved(uuid);
            ready = null;
        }
        if (ready != null) {
            textureBudget.touch(uuid);
            maybeRefresh(uuid, relative);
            return Optional.of(ready);
        }
        if (!beginLoad(uuid)) {
            return Optional.empty();
        }

        Path cached = OttoExtraPaths.bannersCache().resolve(uuid + ".png");
        if (Files.exists(cached)) {
            if (bannerRemoved) {
                deleteCacheFiles(uuid);
            } else {
                registerFromDisk(uuid, cached);
                maybeRefresh(uuid, relative);
                return Optional.empty();
            }
        }

        if (bannerRemoved
                && faction.banner_name() != null && !faction.banner_name().isBlank()) {

            Identifier bundled = bundledBanner(faction.banner_name());
            inFlight.remove(uuid);
            if (bundled != null) {
                MinecraftClient.getInstance().execute(() -> retainTexture(uuid, bundled, 0));
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
            textureBudget.touch(key);
            maybeRefresh(key, relativePath);
            return Optional.of(ready);
        }
        if (!beginLoad(key)) {
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
        var permit = ConsentPolicy.capture(ConsentPolicy.Purpose.EXTERNAL_IMAGES);
        if (!permit.valid()) {
            inFlight.remove(uuid);
            return Optional.empty();
        }
        download(uuid, relative, permit);
        return Optional.empty();
    }

    private void maybeRefresh(String key, String relativePath) {
        var permit = ConsentPolicy.capture(ConsentPolicy.Purpose.EXTERNAL_IMAGES);
        if (!permit.valid() || relativePath == null || relativePath.isBlank()) return;
        boolean changed = BannerSourcePolicy.needsImmediateRefresh(sourceByKey.get(key), relativePath);
        if (!beginRefresh(key, changed)) return;
        download(key, relativePath, permit);
    }

    private static boolean isDynamicBanner(Identifier id) {
        return "ottoextra".equals(id.getNamespace()) && id.getPath().startsWith("dynamic/banners/");
    }

    /** Drops a downloaded banner whose source was removed; bundled textures stay untouched. */
    private void evictRemoved(String key) {
        Identifier evicted = texturesByUuid.remove(key);
        sourceByKey.remove(key);
        refreshedAtNanos.remove(key);
        retryAfterNanos.remove(key);
        textureBudget.release(key);
        deleteCacheFiles(key);
        if (evicted != null && isDynamicBanner(evicted)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.execute(() -> client.getTextureManager().destroyTexture(evicted));
        }
    }

    private static void deleteCacheFiles(String key) {
        try {
            Path dir = OttoExtraPaths.bannersCache();
            Files.deleteIfExists(dir.resolve(key + ".png"));
            Files.deleteIfExists(dir.resolve(key + ".src"));
        } catch (Exception ignored) {
            // Best effort; a leftover file is ignored while the API reports no banner.
        }
    }

    /** Source path stored next to a cached banner; null when missing or unreadable. */
    private static String readSourceMarker(String key) {
        try {
            Path src = OttoExtraPaths.bannersCache().resolve(key + ".src");
            if (!Files.isRegularFile(src) || Files.size(src) > 1024) return null;
            String source = BannerSourcePolicy.normalize(
                    Files.readString(src, java.nio.charset.StandardCharsets.UTF_8));
            return source.isEmpty() ? null : source;
        } catch (Exception unreadable) {
            return null;
        }
    }

    private void download(String key, String relativePath, ConsentPermit permit) {
        URI uri = api.routes().resolveRelative(relativePath);
        if (uri == null) {
            markFailed(key);
            return;
        }
        permit.start(() -> api.downloadBinary(uri)).whenComplete((bytes, error) -> {
            if (!permit.valid()) {
                inFlight.remove(key);
                refreshedAtNanos.remove(key);
                return;
            }
            if (error != null || !isPng(bytes)) {
                markFailed(key);
                return;
            }
            try {
                Path cached = OttoExtraPaths.bannersCache().resolve(key + ".png");
                permit.commitIo(() -> de.ottoextra.privacy.CacheBudget.write(cached, bytes.length, () -> {
                    Files.createDirectories(cached.getParent());
                    Path tmp = Files.createTempFile(cached.getParent(), key + "-", ".tmp");
                    try {
                        Files.write(tmp, bytes);
                        try {
                            Files.move(tmp, cached, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                            Files.move(tmp, cached, StandardCopyOption.REPLACE_EXISTING);
                        }
                        writeSourceMarker(key, relativePath);
                        refreshedAtNanos.put(key, System.nanoTime());
                    } finally { Files.deleteIfExists(tmp); }
                }));
                registerBytes(key, bytes, permit, relativePath);
            } catch (Exception failure) {
                inFlight.remove(key);
                refreshedAtNanos.remove(key);
            }
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
        refreshedAtNanos.remove(key);
        retryAfterNanos.put(key, System.nanoTime() + FAILED_RETRY_NANOS);
    }

    private boolean beginLoad(String key) {
        synchronized (inFlight) {
            return inFlight.size() < 4 && !retryBlocked(key) && inFlight.add(key);
        }
    }

    private boolean beginRefresh(String key, boolean immediate) {
        long now = System.nanoTime();
        Long previous = refreshedAtNanos.get(key);
        if (!immediate && previous != null && now - previous < REFRESH_INTERVAL_NANOS) {
            return false;
        }
        if (!beginLoad(key)) {
            return false;
        }
        refreshedAtNanos.put(key, now);
        return true;
    }

    private static void writeSourceMarker(String key, String relativePath) {
        try {
            Path src = OttoExtraPaths.bannersCache().resolve(key + ".src");
            Files.createDirectories(src.getParent());
            String source = relativePath == null ? "" : relativePath;
            de.ottoextra.privacy.CacheBudget.write(src, source.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                    () -> Files.writeString(src, source));
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
            byte[] bytes = de.ottoextra.privacy.PngLimits.read(file, 5 * 1024 * 1024, 1024);
            if (!isPng(bytes)) {
                inFlight.remove(uuid);
                markFailed(uuid);
                return;
            }
            // Reading an existing local cache needs no network consent.
            registerBytes(uuid, bytes, null, readSourceMarker(uuid));
        } catch (Exception e) {
            inFlight.remove(uuid);
            markFailed(uuid);
        }
    }

    private void registerBytes(String uuid, byte[] bytes, ConsentPermit permit, String source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            inFlight.remove(uuid);
            return;
        }
        client.execute(() -> {
            try {
                if (permit != null) permit.require();
                de.ottoextra.privacy.PngLimits.check(bytes, 5 * 1024 * 1024, 1024);
                NativeImage image = NativeImage.read(bytes);
                NativeImageBackedTexture texture =
                        new NativeImageBackedTexture(() -> "ottoextra-banner-" + uuid, image);
                Identifier id = OttoExtra.id("dynamic/banners/" + uuid);
                long decodedBytes = (long) image.getWidth() * image.getHeight() * 4;
                Runnable register = () -> {
                    client.getTextureManager().registerTexture(id, texture);
                    retainTexture(uuid, id, decodedBytes);
                    String known = BannerSourcePolicy.normalize(source);
                    if (known.isEmpty()) sourceByKey.remove(uuid); else sourceByKey.put(uuid, known);
                    retryAfterNanos.remove(uuid);
                };
                try {
                    if (permit == null) register.run(); else permit.commit(register);
                } catch (RuntimeException failure) {
                    texture.close();
                    throw failure;
                }
            } catch (Throwable t) {
                markFailed(uuid);
                DebugLog.debug("[regions] Banner-Textur fehlgeschlagen ({}): {}", uuid, t.toString());
            } finally {
                inFlight.remove(uuid);
            }
        });
    }

    /** Called on the render thread; bundled resources remain owned by Minecraft. */
    private void retainTexture(String key, Identifier id, long decodedBytes) {
        texturesByUuid.put(key, id);
        for (String victim : textureBudget.retain(key, decodedBytes)) {
            Identifier evicted = texturesByUuid.remove(victim);
            sourceByKey.remove(victim);
            refreshedAtNanos.remove(victim);
            retryAfterNanos.remove(victim);
            if (evicted != null && isDynamicBanner(evicted)) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(evicted);
            }
        }
    }

    private static boolean isPng(byte[] bytes) {
        try {
            de.ottoextra.privacy.PngLimits.check(bytes, 5 * 1024 * 1024, 1024);
            return true;
        } catch (java.io.IOException invalid) { return false; }
    }
}
