package de.ottoextra.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraPaths;

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

/**
 * Persistenter Skin-Cache je UUID: merkt sich die signierte {@code textures}-
 * Property der Spieler, die online auf dem Server gesehen werden, und speichert
 * sie lokal ({@code config/ottoextra/cache/skins.json}). Damit lässt sich der
 * echte Skin überall (z. B. Chat-Köpfe) auch dann auflösen, wenn der Spieler
 * offline ist — der Skin-Provider lädt das PNG aus der gespeicherten URL und
 * cached es selbst.
 */
public final class SkinCache {

    /** GSON-direkt: name + signierte Textur-Property. */
    static final class Cached {
        String name;
        String value;
        String signature;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<UUID, Cached> MAP = new ConcurrentHashMap<>();
    private static volatile boolean dirty;

    /** Laufende/erledigte PNG-Downloads je UUID (kein Doppel-Download). */
    private static final Set<UUID> PNG_DONE = ConcurrentHashMap.newKeySet();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private SkinCache() {
    }

    private static Path file() {
        return OttoExtraPaths.cacheDir().resolve("skins.json");
    }

    /** Ordner mit den lokal gespeicherten Skin-PNGs ({@code <uuid>.png}). */
    public static Path pngDir() {
        return OttoExtraPaths.cacheDir().resolve("skins");
    }

    /** Beim Client-Start einmal laden. */
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
            OttoExtra.LOGGER.info("[skins] {} Skins aus Cache geladen.", MAP.size());
            // Fehlende PNGs lokal nachladen (Gegentest / Offline-Persistenz).
            MAP.forEach((uuid, c) -> ensurePng(uuid, c.value, false));
        } catch (Exception e) {
            OttoExtra.LOGGER.warn("[skins] skins.json unlesbar: {}", e.toString());
        }
    }

    /** Skin eines online gesehenen GameProfiles merken (nur bei Änderung). */
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

    /** Skin-PNG lokal speichern, wenn es fehlt oder sich der Skin geändert hat. */
    private static void ensurePng(UUID uuid, String base64Value, boolean changed) {
        if (uuid == null || base64Value == null) {
            return;
        }
        Path png = pngDir().resolve(uuid + ".png");
        if (!changed && (PNG_DONE.contains(uuid) || Files.exists(png))) {
            return;
        }
        if (!PNG_DONE.add(uuid) && !changed) {
            return; // Download läuft bereits
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
                }
            } catch (Throwable t) {
                PNG_DONE.remove(uuid); // erneuter Versuch später möglich
                OttoExtra.LOGGER.debug("[skins] PNG-Download fehlgeschlagen ({}): {}", uuid, t.toString());
            }
        });
    }

    /**
     * GameProfile mit der gecachten Textur-Property (falls vorhanden), sonst nur
     * UUID+Name. Damit löst der Skin-Provider den echten Skin auf.
     */
    public static GameProfile profileFor(UUID uuid, String name) {
        Cached c = uuid != null ? MAP.get(uuid) : null;
        String n = c != null && c.name != null && !c.name.isBlank() ? c.name : name;
        GameProfile gp = new GameProfile(uuid, n == null ? "" : n);
        if (c != null && c.value != null) {
            gp.properties().put("textures", new Property("textures", c.value, c.signature));
        }
        return gp;
    }

    /** Hat der Cache einen Skin für diese UUID? */
    public static boolean has(UUID uuid) {
        return uuid != null && MAP.containsKey(uuid);
    }

    /** Bereits als Textur registrierte lokale Skins (UUID -> SkinTextures). */
    private static final Map<UUID, net.minecraft.entity.player.SkinTextures> LOADED =
            new ConcurrentHashMap<>();

    /**
     * SkinTextures aus dem lokal gespeicherten PNG ({@code cache/skins/<uuid>.png}),
     * als Textur registriert — nutzt also den lokalen Cache statt Mojang. Null,
     * wenn kein PNG vorhanden ist oder das Laden scheitert. Auf dem Render-Thread
     * aufrufen (Textur-Registrierung).
     */
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
            // texturePath == die registrierte Runtime-ID (kein Resource-Pfad ableiten),
            // sonst sucht der Renderer eine nicht existierende Ressource.
            net.minecraft.entity.player.SkinTextures st = new net.minecraft.entity.player.SkinTextures(
                    new net.minecraft.util.AssetInfo.TextureAssetInfo(id, id), null, null, modelFor(uuid), true);
            LOADED.put(uuid, st);
            return st;
        } catch (Throwable t) {
            OttoExtra.LOGGER.debug("[skins] lokales PNG laden fehlgeschlagen ({}): {}", uuid, t.toString());
            return null;
        }
    }

    /** Modell (SLIM/WIDE) aus der gecachten Textur-Metadaten, Default WIDE. */
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
            // Default unten
        }
        return net.minecraft.entity.player.PlayerSkinType.WIDE;
    }

    /** Geänderten Cache auf die Platte schreiben (debounced über das dirty-Flag). */
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
