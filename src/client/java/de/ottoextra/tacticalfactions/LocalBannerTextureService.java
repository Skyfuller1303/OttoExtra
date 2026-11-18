package de.ottoextra.tacticalfactions;

import de.ottoextra.logging.DebugLog;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

public final class LocalBannerTextureService {
    private static final long MAX_BYTES = 4L * 1024L * 1024L;
    private static final int MAX_DIMENSION = 2048;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};
    private static final LocalBannerTextureService INSTANCE = new LocalBannerTextureService();
    private final Map<String, Identifier> textures = new HashMap<>();
    private final Set<String> failed = new HashSet<>();

    private LocalBannerTextureService() { }

    public static LocalBannerTextureService instance() {
        return INSTANCE;
    }

    public Optional<Identifier> bannerFor(PoliticalStateService.FactionView view) {
        if (view == null) return Optional.empty();
        return bannerFor(view.id(), view.apiRecord(), view.bannerFile());
    }

    /** Banner of a local group by its id and stored file name (no API record). */
    public Optional<Identifier> bannerForCustom(String id, String bannerFile) {
        return bannerFor(id, null, bannerFile);
    }

    public Optional<Identifier> rootBannerFor(PoliticalStateService.FactionView view) {
        if (view == null) return Optional.empty();
        return bannerFor(view.rootId(), view.rootApiRecord(), view.rootBannerFile());
    }

    private Optional<Identifier> bannerFor(String id, de.ottoextra.api.model.FactionRecord apiRecord,
                                           String bannerFile) {
        if (apiRecord != null) {
            return RegionsServices.banners() != null
                    ? RegionsServices.banners().bannerFor(apiRecord) : Optional.empty();
        }
        if (bannerFile == null || bannerFile.isBlank() || id == null) return Optional.empty();
        if (failed.contains(id)) return Optional.empty();
        Identifier ready = textures.get(id);
        if (ready != null) return Optional.of(ready);
        Path file = TacticalConfig.bannerDirectory().resolve(Path.of(bannerFile).getFileName());
        if (!Files.isRegularFile(file)) return Optional.empty();
        try (InputStream stream = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            Identifier textureId = Identifier.of("ottoextra_tacticalfactions",
                    "dynamic/banners/" + safeId(id));
            MinecraftClient client = MinecraftClient.getInstance();
            client.getTextureManager().registerTexture(textureId,
                    new NativeImageBackedTexture(() -> "tacticalfactions-banner-" + id, image));
            textures.put(id, textureId);
            return Optional.of(textureId);
        } catch (Throwable error) {
            failed.add(id);
            DebugLog.debug("[tacticalfactions] Lokales Wappen konnte nicht geladen werden ({}): {}",
                    id, error.toString());
            return Optional.empty();
        }
    }

    public String importPng(String entityId, Path source) throws BannerImportException {
        return importPng(entityId, source, TacticalConfig.bannerDirectory());
    }

    String importPng(String entityId, Path source, Path directory) throws BannerImportException {
        if (source == null || !Files.isRegularFile(source)) throw new BannerImportException("missing");
        try {
            long size = Files.size(source);
            if (size <= 0 || size > MAX_BYTES) throw new BannerImportException("size");
            byte[] header = new byte[PNG_MAGIC.length];
            try (InputStream stream = Files.newInputStream(source)) {
                if (stream.read(header) != header.length) throw new BannerImportException("format");
            }
            for (int i = 0; i < PNG_MAGIC.length; i++) {
                if (header[i] != PNG_MAGIC[i]) throw new BannerImportException("format");
            }
            try (InputStream stream = Files.newInputStream(source); NativeImage image = NativeImage.read(stream)) {
                if (image.getWidth() < 1 || image.getHeight() < 1
                        || image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
                    throw new BannerImportException("dimensions");
                }
            }
            Files.createDirectories(directory);
            String fileName = safeId(entityId) + ".png";
            Files.copy(source, directory.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            invalidate(entityId);
            return fileName;
        } catch (BannerImportException error) {
            throw error;
        } catch (IOException | RuntimeException error) {
            throw new BannerImportException("read", error);
        }
    }

    public void delete(String entityId, String bannerFile) {
        invalidate(entityId);
        if (bannerFile == null || bannerFile.isBlank()) return;
        try {
            Path directory = TacticalConfig.bannerDirectory().toAbsolutePath().normalize();
            Path file = directory.resolve(Path.of(bannerFile).getFileName()).normalize();
            if (file.getParent().equals(directory)) Files.deleteIfExists(file);
        } catch (IOException | RuntimeException error) {
            DebugLog.debug("[tacticalfactions] Lokales Wappen konnte nicht gelöscht werden ({}): {}",
                    entityId, error.toString());
        }
    }

    public void invalidate(String entityId) {
        failed.remove(entityId);
        Identifier old = textures.remove(entityId);
        MinecraftClient client = MinecraftClient.getInstance();
        if (old != null && client != null) client.getTextureManager().destroyTexture(old);
    }

    private static String safeId(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    public static final class BannerImportException extends Exception {
        private final String reason;

        BannerImportException(String reason) {
            super(reason);
            this.reason = reason;
        }

        BannerImportException(String reason, Throwable cause) {
            super(reason, cause);
            this.reason = reason;
        }

        public String translationKey() {
            return "ottoextra_tacticalfactions.banner.error." + reason;
        }
    }
}
