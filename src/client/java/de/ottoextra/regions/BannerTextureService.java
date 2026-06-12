package de.ottoextra.regions;

import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.config.OttoExtraPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lädt Fraktions-Wappen (Banner) über den zentralen API-Client, cached sie auf
 * Disk und registriert sie als dynamische Texturen.
 *
 * <p>Identifier-Schema: {@code ottoextra:dynamic/banners/<faction-uuid>}.
 * Disk-Cache: {@code config/ottoextra/cache/banners/<uuid>.png}. Downloads sind
 * durch den API-Client größenbegrenzt; zusätzlich PNG-Magic-Check hier.
 * Textur-Registrierung erfolgt ausschliesslich auf dem Render-Thread.</p>
 */
public final class BannerTextureService {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

    private final OttoExtraApiClient api;
    private final Map<String, Identifier> texturesByUuid = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> failed = ConcurrentHashMap.newKeySet();
    /** Pro Client-Lauf einmal neu vom Server laden (Wappen-Updates greifen so
     *  spaetestens beim naechsten Start; Disk-Cache bleibt Sofort-Fallback). */
    private final Set<String> refreshedThisSession = ConcurrentHashMap.newKeySet();

    public BannerTextureService(OttoExtraApiClient api) {
        this.api = api;
    }

    /**
     * Liefert die Banner-Textur einer Fraktion, falls bereits verfügbar.
     * Bei Miss wird der Download/Disk-Load asynchron angestossen (nächster
     * Aufruf liefert dann die Textur) — Render-Pfad bleibt nicht-blockierend.
     */
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
        if (failed.contains(uuid) || !inFlight.add(uuid)) {
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
            // Kein Server-Pfad, aber Banner-Name: gebündeltes Asset versuchen
            // (z. B. Holdern-Override -> custom_ottonien.png)
            Identifier bundled = bundledBanner(faction.banner_name());
            inFlight.remove(uuid);
            if (bundled != null) {
                texturesByUuid.put(uuid, bundled);
                return Optional.of(bundled);
            }
            failed.add(uuid);
            return Optional.empty();
        }
        return startDownload(uuid, relative);
    }

    /**
     * Banner über beliebigen Cache-Schlüssel + relativen Server-Pfad
     * (z. B. Region-Banner fraktionsloser Lehen). Nicht-blockierend wie
     * {@link #bannerFor(FactionRecord)}.
     */
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
        if (failed.contains(key) || !inFlight.add(key)) {
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
        Path cached = OttoExtraPaths.bannersCache().resolve(uuid + ".png");
        URI uri = api.routes().resolveRelative(relative);
        if (uri == null) {
            inFlight.remove(uuid);
            failed.add(uuid);
            return Optional.empty();
        }
        api.downloadBinary(uri).whenComplete((bytes, t) -> {
            if (t != null || bytes == null || !isPng(bytes)) {
                inFlight.remove(uuid);
                failed.add(uuid); // Fehler-Cache gegen Retry-Stürme
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
                OttoExtra.LOGGER.debug("[regions] Banner-Cache schreiben fehlgeschlagen: {}", e.getMessage());
            }
            writeSourceMarker(uuid, relative);
            refreshedThisSession.add(uuid);
            registerBytes(uuid, bytes);
        });
        return Optional.empty();
    }

    /**
     * Einmal pro Client-Lauf: Banner still neu herunterladen und Cache/Textur
     * ersetzen. Fehler lassen den vorhandenen Cache unangetastet; ein
     * geaenderter Server-Pfad wird im .src-Marker mitgefuehrt.
     */
    private void maybeRefresh(String key, String relativePath) {
        if (relativePath == null || relativePath.isBlank()
                || !refreshedThisSession.add(key)) {
            return;
        }
        URI uri = api.routes().resolveRelative(relativePath);
        if (uri == null) {
            return;
        }
        api.downloadBinary(uri).whenComplete((bytes, t) -> {
            if (t != null || bytes == null || !isPng(bytes)) {
                return; // alter Cache bleibt gueltig
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
                OttoExtra.LOGGER.debug("[regions] Banner-Refresh schreiben fehlgeschlagen: {}", e.getMessage());
            }
            inFlight.add(key); // registerBytes raeumt im finally wieder auf
            registerBytes(key, bytes);
        });
    }

    private static void writeSourceMarker(String key, String relativePath) {
        try {
            Path src = OttoExtraPaths.bannersCache().resolve(key + ".src");
            Files.createDirectories(src.getParent());
            Files.writeString(src, relativePath == null ? "" : relativePath);
        } catch (Exception ignored) {
            // Marker ist nur Diagnose/Zukunft — Fehler unkritisch
        }
    }

    /** Gebündeltes Banner-Asset nach Banner-Name, oder null wenn nicht vorhanden. */
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
                failed.add(uuid);
                return;
            }
            registerBytes(uuid, bytes);
        } catch (Exception e) {
            inFlight.remove(uuid);
            failed.add(uuid);
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
            } catch (Throwable t) {
                failed.add(uuid);
                OttoExtra.LOGGER.debug("[regions] Banner-Textur fehlgeschlagen ({}): {}", uuid, t.toString());
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
