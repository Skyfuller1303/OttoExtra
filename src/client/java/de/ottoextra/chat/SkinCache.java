package de.ottoextra.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.privacy.ConsentHttp;
import de.ottoextra.privacy.ConsentPolicy;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinCache {

    static final class Cached {
        String name;
        String value;
        String signature;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<UUID, Cached> MAP = new ConcurrentHashMap<>();
    private static volatile boolean dirty;
    private static final java.util.concurrent.Semaphore DOWNLOAD_SLOTS = new java.util.concurrent.Semaphore(4);
    private static final Map<UUID, Long> RETRY_AT = new ConcurrentHashMap<>();

    private static final Set<UUID> PNG_IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PNG_DONE = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PNG_DOWNLOAD_FAILURES = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PNG_LOAD_FAILURES = ConcurrentHashMap.newKeySet();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private SkinCache() {
    }

    private static Path file() {
        return OttoExtraPaths.cacheDir().resolve("skins.json");
    }

    public static Path pngDir() {
        return OttoExtraPaths.cacheDir().resolve("skins");
    }

    public static void load() {
        Path f = file();
        try {
            if (!Files.exists(f) || Files.size(f) > 8 * 1024 * 1024) {
                return;
            }
            Map<String, Cached> raw = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, Cached>>() {}.getType());
            if (raw != null) {
                raw.forEach((id, c) -> {
                    UUID uuid = parse(id);
                    if (uuid != null && c != null && c.value != null && c.value.length() <= 16384 && MAP.size() < 4096) {
                        MAP.put(uuid, c);
                    }
                });
            }
            DebugLog.debug("[skins] {} Skins aus Cache geladen.", MAP.size());

            MAP.forEach((uuid, c) -> ensurePng(uuid, c.value, false));
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[skins] skins.json unlesbar: {}", e.toString());
        }
    }

    public static void remember(GameProfile gp) {
        if (gp == null || gp.id() == null) {
            return;
        }
        Property prop = gp.properties().get("textures").stream().findFirst().orElse(null);
        if (prop == null || prop.value() == null || prop.value().isBlank() || prop.value().length() > 16384
                || !MAP.containsKey(gp.id()) && MAP.size() >= 4096) {
            return;
        }
        Cached old = MAP.get(gp.id());
        boolean changed = old == null || !Objects.equals(old.value, prop.value())
                || !Objects.equals(old.signature, prop.signature());
        if (changed) {
            Cached c = new Cached();
            c.name = gp.name();
            c.value = prop.value();
            c.signature = prop.signature();
            MAP.put(gp.id(), c);
            dirty = true;
        }
        ensurePng(gp.id(), prop.value(), changed);
    }

    public static void requestPng(UUID uuid) {
        Cached cached = uuid == null ? null : MAP.get(uuid);
        if (cached != null) ensurePng(uuid, cached.value, false);
    }

    private static void ensurePng(UUID uuid, String base64Value, boolean changed) {
        var permit = ConsentPolicy.capture(ConsentPolicy.Purpose.EXTERNAL_IMAGES);
        if (!permit.valid()) {
            return;
        }
        if (uuid == null || base64Value == null) {
            return;
        }
        Path png = pngDir().resolve(uuid + ".png");
        if (PNG_IN_FLIGHT.contains(uuid)) return;
        if (changed) {
            PNG_DOWNLOAD_FAILURES.remove(uuid);
            PNG_LOAD_FAILURES.remove(uuid);
        }
        if (!changed && (PNG_DONE.contains(uuid) || Files.exists(png))) {
            return;
        }
        if (!PNG_DONE.add(uuid) && !changed) {
            return;
        }
        Long retryAt = RETRY_AT.get(uuid);
        if (retryAt != null && System.nanoTime() < retryAt || !DOWNLOAD_SLOTS.tryAcquire()) {
            PNG_DONE.remove(uuid);
            return;
        }
        if (!PNG_IN_FLIGHT.add(uuid)) {
            DOWNLOAD_SLOTS.release();
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                String url = root.getAsJsonObject("textures").getAsJsonObject("SKIN")
                        .get("url").getAsString();
                HttpResponse<byte[]> resp = ConsentHttp.send(HTTP,
                        HttpRequest.newBuilder(ConsentHttp.skinTarget(url)).timeout(Duration.ofSeconds(15)).GET().build(),
                        de.ottoextra.privacy.BoundedBody.bytes(1024 * 1024), permit).join();
                if (resp.statusCode() == 200 && resp.body().length > 0) {
                    de.ottoextra.privacy.PngLimits.check(resp.body(), 1024 * 1024, 256);
                    permit.commitIo(() -> de.ottoextra.privacy.CacheBudget.write(png, resp.body().length, () -> {
                        Files.createDirectories(png.getParent());
                        Path tmp = png.resolveSibling(uuid + ".png.tmp");
                        try {
                            Files.write(tmp, resp.body());
                            Files.move(tmp, png, StandardCopyOption.REPLACE_EXISTING);
                        } finally { Files.deleteIfExists(tmp); }
                        PNG_DOWNLOAD_FAILURES.remove(uuid);
                    }));
                } else {
                    RETRY_AT.put(uuid, System.nanoTime() + Duration.ofSeconds(45).toNanos());
                    PNG_DONE.remove(uuid);
                }
            } catch (Throwable t) {
                RETRY_AT.put(uuid, System.nanoTime() + Duration.ofSeconds(45).toNanos());
                PNG_DONE.remove(uuid);
                if (PNG_DOWNLOAD_FAILURES.add(uuid)) {
                    DebugLog.debug("[skins] PNG-Download fehlgeschlagen: {}", t.toString());
                }
            } finally { PNG_IN_FLIGHT.remove(uuid); DOWNLOAD_SLOTS.release(); }
        });
    }

    public static GameProfile profileFor(UUID uuid, String name) {
        Cached c = uuid != null ? MAP.get(uuid) : null;
        String n = c != null && c.name != null && !c.name.isBlank() ? c.name : name;
        GameProfile gp = new GameProfile(uuid, n == null ? "" : n);
        if (c != null && c.value != null) {
            gp.properties().put("textures", new Property("textures", c.value, c.signature));
        }
        return gp;
    }

    public static boolean has(UUID uuid) {
        return uuid != null && MAP.containsKey(uuid);
    }

    private static final Map<UUID, net.minecraft.entity.player.SkinTextures> LOADED =
            new ConcurrentHashMap<>();

    public static net.minecraft.entity.player.SkinTextures localSkin(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        net.minecraft.entity.player.SkinTextures cached = LOADED.get(uuid);
        if (cached != null) {
            return cached;
        }
        if (LOADED.size() >= 128 || PNG_LOAD_FAILURES.contains(uuid)) return null;
        Path png = pngDir().resolve(uuid + ".png");
        if (!Files.exists(png)) {
            return null;
        }
        try {
            net.minecraft.client.texture.NativeImage img;
            img = net.minecraft.client.texture.NativeImage.read(
                    de.ottoextra.privacy.PngLimits.read(png, 1024 * 1024, 256));
            net.minecraft.util.Identifier id =
                    OttoExtra.id("cached_skin/" + uuid.toString().toLowerCase());
            net.minecraft.client.MinecraftClient.getInstance().getTextureManager().registerTexture(id,
                    new net.minecraft.client.texture.NativeImageBackedTexture(
                            () -> "ottoextra-skin-" + uuid, img));

            net.minecraft.entity.player.SkinTextures st = new net.minecraft.entity.player.SkinTextures(
                    new net.minecraft.util.AssetInfo.TextureAssetInfo(id, id), null, null, modelFor(uuid), true);
            LOADED.put(uuid, st);
            PNG_LOAD_FAILURES.remove(uuid);
            return st;
        } catch (Throwable t) {
            if (PNG_LOAD_FAILURES.add(uuid)) {
                DebugLog.debug("[skins] Lokales PNG konnte nicht geladen werden: {}", t.toString());
            }
            return null;
        }
    }

    private static net.minecraft.entity.player.PlayerSkinType modelFor(UUID uuid) {
        try {
            Cached c = MAP.get(uuid);
            if (c != null && c.value != null) {
                String json = new String(Base64.getDecoder().decode(c.value), StandardCharsets.UTF_8);
                JsonObject skin = GSON.fromJson(json, JsonObject.class)
                        .getAsJsonObject("textures").getAsJsonObject("SKIN");
                if (skin.has("metadata")) {
                    String model = skin.getAsJsonObject("metadata").get("model").getAsString();
                    return net.minecraft.entity.player.PlayerSkinType.byModelMetadata(model);
                }
            }
        } catch (Throwable ignored) {

        }
        return net.minecraft.entity.player.PlayerSkinType.WIDE;
    }

    public static void flush() {
        if (!dirty) {
            return;
        }
        dirty = false;
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Map<String, Cached> out = new HashMap<>();
            MAP.forEach((uuid, c) -> out.put(uuid.toString(), c));
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            String json = GSON.toJson(out);
            if (json.getBytes(StandardCharsets.UTF_8).length > 8 * 1024 * 1024) return;
            de.ottoextra.privacy.CacheBudget.write(f, json.getBytes(StandardCharsets.UTF_8).length, () -> {
                Files.writeString(tmp, json, StandardCharsets.UTF_8);
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
            });
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[skins] skins.json speichern fehlgeschlagen: {}", e.toString());
        }
    }

    private static UUID parse(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }
}
