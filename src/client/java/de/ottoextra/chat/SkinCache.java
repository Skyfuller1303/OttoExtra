package de.ottoextra.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.logging.DebugLog;

import java.net.URI;
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
            if (!Files.exists(f)) {
                return;
            }
            Map<String, Cached> raw = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, Cached>>() {}.getType());
            if (raw != null) {
                raw.forEach((id, c) -> {
                    UUID uuid = parse(id);
                    if (uuid != null && c != null && c.value != null) {
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
        if (prop == null || prop.value() == null || prop.value().isBlank()) {
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

    private static void ensurePng(UUID uuid, String base64Value, boolean changed) {
        if (uuid == null || base64Value == null) {
            return;
        }
        Path png = pngDir().resolve(uuid + ".png");
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
        CompletableFuture.runAsync(() -> {
            try {
                String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                String url = root.getAsJsonObject("textures").getAsJsonObject("SKIN")
                        .get("url").getAsString();
                HttpResponse<byte[]> resp = HTTP.send(
                        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200 && resp.body().length > 0) {
                    Files.createDirectories(png.getParent());
                    Path tmp = png.resolveSibling(uuid + ".png.tmp");
                    Files.write(tmp, resp.body());
                    Files.move(tmp, png, StandardCopyOption.REPLACE_EXISTING);
                    PNG_DOWNLOAD_FAILURES.remove(uuid);
                }
            } catch (Throwable t) {
                PNG_DONE.remove(uuid);
                if (PNG_DOWNLOAD_FAILURES.add(uuid)) {
                    DebugLog.debug("[skins] PNG-Download fehlgeschlagen: {}", t.toString());
                }
            }
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
        Path png = pngDir().resolve(uuid + ".png");
        if (!Files.exists(png)) {
            return null;
        }
        try {
            net.minecraft.client.texture.NativeImage img;
            try (var in = Files.newInputStream(png)) {
                img = net.minecraft.client.texture.NativeImage.read(in);
            }
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
            Files.writeString(tmp, GSON.toJson(out), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
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
