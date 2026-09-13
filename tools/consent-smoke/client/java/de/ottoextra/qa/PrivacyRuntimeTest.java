package de.ottoextra.qa;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.privacy.ConsentPolicy;
import de.ottoextra.privacy.ConsentDetailsScreen;
import de.ottoextra.resourcepack.PackInstaller;
import de.ottoextra.chat.SkinCache;
import de.ottoextra.regions.BannerTextureService;
import de.ottoextra.api.HttpOttoExtraApiClient;
import de.ottoextra.tweaks.TweaksModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Real client/render/network tests with synthetic data and loopback-only HTTP fixtures. */
public final class PrivacyRuntimeTest implements FabricClientGameTest {
    private ClientGameTestContext ctx;
    private final List<String> passed = new ArrayList<>();
    @Override public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("ottoextra.privacyQa")) return;
        ctx = context;
        try {
            englishScreens();
            imageDownloads();
            packs();
            world();
            Path report = FabricLoader.getInstance().getGameDir().resolve("privacy-qa-results.txt");
            Files.write(report, passed);
        } catch (Exception error) { throw new RuntimeException(error); }
        finally {
            RuntimeProbe.skinEndpoint = null;
            RuntimeProbe.simulateOttonien = false;
            ctx.runOnClient(client -> {
                for (var p : ConsentPolicy.Purpose.values()) decision(p, false);
                OttoExtraConfig.active().tweaks.lowHealth.enabled = false;
                client.setScreen(new TitleScreen());
            });
        }
    }
    private void pass(String message) { passed.add(message); System.out.println("PRIVACY QA PASS: " + message); }
    private static void check(boolean yes, String message) { if (!yes) throw new AssertionError(message); }
    private static void decision(ConsentPolicy.Purpose p, boolean allow) {
        var c = OttoExtraConfig.active();
        ConsentPolicy.decide(c, p, allow, ConsentPolicy.scope(c, p));
    }
    private void reload() {
        var reload = ctx.computeOnClient(client -> client.reloadResources());
        ctx.waitFor(client -> reload.isDone() && client.getOverlay() == null, 1800);
        reload.join();
    }
    private void englishScreens() {
        ctx.runOnClient(client -> {
            client.options.language = "en_us";
            client.getLanguageManager().setLanguage("en_us");
        });
        reload();
        for (var purpose : ConsentPolicy.Purpose.values()) {
            ctx.runOnClient(client -> {
                client.setScreen(new TitleScreen());
                ConsentDetailsScreen.change(OttoExtraConfig.active(), purpose, true);
                check(!ConsentDetailsScreen.information(OttoExtraConfig.active(), purpose).getString().contains("ottoextra.consent."), "Untranslated English consent: " + purpose);
            });
            ctx.takeScreenshot("privacy-en-" + purpose.name().toLowerCase(Locale.ROOT));
            ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            ctx.runOnClient(client -> check(!ConsentPolicy.allowed(OttoExtraConfig.active(), purpose), "Escape granted consent"));
        }
        ctx.runOnClient(client -> de.ottoextra.privacy.LocalChatConsentScreen.change(OttoExtraConfig.active(), true));
        ctx.takeScreenshot("privacy-en-chat");
        ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        ctx.runOnClient(client -> client.setScreen(new de.ottoextra.tweaks.vote.VoteConsentScreen("PrivacyFixture", List.of(URI.create("https://example.invalid/vote")), () -> { throw new AssertionError("Unconfirmed vote effect"); })));
        ctx.takeScreenshot("privacy-en-vote");
        ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        pass("English: seven consent dialogs rendered; Escape grants nothing");
    }
    private void imageDownloads() throws Exception {
        String oldBase = OttoExtraConfig.active().api.baseUrl;
        try (SlowImage server = new SlowImage()) {
            ctx.runOnClient(client -> OttoExtraConfig.active().api.baseUrl = server.base());
            var api = new HttpOttoExtraApiClient(OttoExtraConfig.active());
            try {
                var banners = new BannerTextureService(api);
                var denyId = UUID.randomUUID();
                RuntimeProbe.skinEndpoint = URI.create(server.base() + "deny");
                ctx.runOnClient(client -> {
                    decision(ConsentPolicy.Purpose.EXTERNAL_IMAGES, false);
                    banners.bannerForPath("denied-" + denyId, "deny");
                    SkinCache.remember(profile(denyId));
                });
                ctx.waitTicks(20);
                check(server.requests.get() == 0, "Denied image request reached HTTP server");
                pass("Images denied: skin and banner produce zero HTTP requests");
                for (boolean skin : new boolean[]{false, true}) {
                    for (boolean revoke : new boolean[]{false, true}) {
                        UUID id = UUID.randomUUID();
                        String key = "qa-" + id;
                        var body = server.add(key, !revoke);
                        RuntimeProbe.skinEndpoint = URI.create(server.base() + key);
                        ctx.runOnClient(client -> {
                            decision(ConsentPolicy.Purpose.EXTERNAL_IMAGES, true);
                            if (skin) SkinCache.remember(profile(id)); else banners.bannerForPath(key, key);
                        });
                        ctx.waitFor(client -> body.started.getCount() == 0, 400);
                        if (revoke) {
                            ctx.runOnClient(client -> client.setScreen(new de.ottoextra.config.settings.OttoExtraSettingsScreen(new TitleScreen())));
                            ctx.waitTick();
                        }
                        if (revoke) ctx.runOnClient(client -> {
                            client.setScreen(new TitleScreen());
                            decision(ConsentPolicy.Purpose.EXTERNAL_IMAGES, false);
                            decision(ConsentPolicy.Purpose.EXTERNAL_IMAGES, true);
                        });
                        body.release.countDown();
                        ctx.waitFor(client -> body.done.getCount() == 0, 400);
                        Path file = skin ? SkinCache.pngDir().resolve(id + ".png") : OttoExtraPaths.bannersCache().resolve(key + ".png");
                        ctx.waitFor(client -> skin ? !((Set<?>) field(SkinCache.class, null, "PNG_IN_FLIGHT")).contains(id)
                                : !((Set<?>) field(BannerTextureService.class, banners, "inFlight")).contains(key), 400);
                        ctx.waitTicks(5);
                        ctx.runOnClient(client -> {
                            check(Files.exists(file) != revoke, "Image cache result incorrect: skin=" + skin + ", revoke=" + revoke);
                            boolean texture = skin ? SkinCache.localSkin(id) != null
                                    : ((Map<?, ?>) field(BannerTextureService.class, banners, "texturesByUuid")).containsKey(key);
                            check(texture != revoke, "Late/missing native texture registration");
                            if (revoke) check(!Files.exists(file.resolveSibling(file.getFileName() + ".tmp")), "Late temporary image file");
                        });
                        pass((skin ? "Skin" : "Banner") + (revoke ? ": mid-body revoke/regrant prevents cache and native texture" : ": granted HTTP download, PNG cache and native texture succeed"));
                    }
                }
            } finally { api.close(); }
        } finally {
            RuntimeProbe.skinEndpoint = null;
            ctx.runOnClient(client -> {
                decision(ConsentPolicy.Purpose.EXTERNAL_IMAGES, false);
                OttoExtraConfig.active().api.baseUrl = oldBase;
            });
        }
    }
    private void packs() throws Exception {
        Path zip = Files.createTempFile(FabricLoader.getInstance().getGameDir(), "privacy-pack-", ".zip");
        try (var out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
            out.write("{\"pack\":{\"pack_format\":75,\"min_format\":75,\"max_format\":75,\"description\":\"Privacy QA local fixture\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new java.util.zip.ZipEntry("assets/ottoextra/qa/marker.txt"));
            out.write("privacy-qa-resource-loaded".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.closeEntry();
        }
        PackInstaller.install(zip);
        ctx.runOnClient(client -> {
            decision(ConsentPolicy.Purpose.RESOURCE_PACKS, true);
            var stale = ConsentPolicy.capture(ConsentPolicy.Purpose.RESOURCE_PACKS);
            PackInstaller.requestActivation(true, true, false, stale);
            decision(ConsentPolicy.Purpose.RESOURCE_PACKS, false);
            decision(ConsentPolicy.Purpose.RESOURCE_PACKS, true);
            PackInstaller.tick(client);
            check(!client.options.resourcePacks.contains(PackInstaller.packId()), "Revoked pending activation revived");
            PackInstaller.requestActivation(true, true, false, ConsentPolicy.capture(ConsentPolicy.Purpose.RESOURCE_PACKS));
            PackInstaller.tick(client);
            decision(ConsentPolicy.Purpose.RESOURCE_PACKS, false);
        });
        ctx.waitFor(client -> client.getOverlay() == null, 1800);
        ctx.runOnClient(client -> check(client.getResourcePackManager().getEnabledIds().contains(PackInstaller.packId()), "Previously authorized pack reload failed"));
        ctx.runOnClient(client -> {
            var resource = client.getResourceManager().getResource(net.minecraft.util.Identifier.of("ottoextra", "qa/marker.txt")).orElseThrow(() -> new AssertionError("Pack selected but resource not actually loaded"));
            try (var input = resource.getInputStream()) {
                check(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).equals("privacy-qa-resource-loaded"), "Unexpected resource bytes");
            }
        });
        ctx.takeScreenshot("privacy-pack-reload");
        pass("Resource pack: stale pending activation blocked; authorized actual reload survives later download-consent withdrawal");
        ctx.runOnClient(client -> {
            var enabled = new ArrayList<>(client.getResourcePackManager().getEnabledIds());
            enabled.remove(PackInstaller.packId());
            client.getResourcePackManager().setEnabledProfiles(enabled);
            client.options.resourcePacks.remove(PackInstaller.packId());
        });
        reload();
        ctx.runOnClient(client -> {
            decision(ConsentPolicy.Purpose.RESOURCE_PACKS, true);
            PackInstaller.requestActivation(true, false, true, ConsentPolicy.capture(ConsentPolicy.Purpose.RESOURCE_PACKS));
            PackInstaller.requestReload(ConsentPolicy.capture(ConsentPolicy.Purpose.RESOURCE_PACKS));
            PackInstaller.tick(client);
            check(!client.options.resourcePacks.contains(PackInstaller.packId()) && client.getOverlay() == null, "Manually disabled pack reenabled/reloaded");
            decision(ConsentPolicy.Purpose.RESOURCE_PACKS, false);
        });
        pass("Resource pack: manual disable respected even with legacy respectDisable=false");
    }
    private void world() {
        ctx.runOnClient(client -> { for (var purpose : ConsentPolicy.Purpose.values()) decision(purpose, false); });
        RuntimeProbe.simulateOttonien = true;
        net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave saved;
        try (var world = ctx.worldBuilder().create()) {
            saved = world.getWorldSave();
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("weather clear");
            ctx.waitFor(client -> client.player != null && client.world != null && client.currentScreen == null, 1200);
            world.getServer().runOnServer(server -> server.getPlayerManager().getPlayerList().forEach(player -> {
                player.setHealth(2); player.getHungerManager().setFoodLevel(0);
            }));
            ctx.waitFor(client -> client.player.getHealth() <= 2, 400);
            ctx.runOnClient(client -> {
                var cfg = OttoExtraConfig.active().tweaks.lowHealth;
                cfg.enabled = true; cfg.reducedMotion = false; cfg.blurEnabled = true; cfg.blurStrength = 3; cfg.blurStartHearts = 10; cfg.startHealth = 40;
                cfg.fovEnabled = true; cfg.fovMaxDegrees = 25; cfg.heartbeatEnabled = true;
                cfg.vignetteMaxAlpha = 1; cfg.heartbeatMaxVolume = 1;
            });
            ctx.waitTicks(60);
            ctx.runOnClient(client -> {
                check(TweaksModule.lowHealthBlurPasses() > 0, "Real health produces no blur");
                check(TweaksModule.lowHealthFovBoost() > 0, "Real health produces no FOV change");
            });
            ctx.waitFor(client -> RuntimeProbe.heartbeats.get() > 0, 200);
            ctx.runOnClient(client -> ((de.ottoextra.tweaks.lowhealth.LowHealthState) field(TweaksModule.class, null, "activeState")).setForced(1));
            ctx.waitTicks(40);
            ctx.runOnClient(client -> check(TweaksModule.lowHealthBlurPasses() == 8 && TweaksModule.lowHealthFovBoost() > 24.9f, "Maximum preview settings not reached"));
            ctx.takeScreenshot("privacy-world-lowhealth-full");
            var before = ctx.computeOnClient(client -> client.player.getEntityPos());
            ctx.getInput().holdKeyFor(options -> options.forwardKey, 15);
            ctx.runOnClient(client -> check(client.player.getEntityPos().squaredDistanceTo(before) > 0.001, "Movement fixture did not move"));
            ctx.getInput().holdKey(options -> options.forwardKey);
            world.getServer().runCommand("damage @p 0.5 minecraft:generic");
            ctx.waitFor(client -> client.player.getHealth() < 2, 200);
            ctx.runOnClient(client -> {
                OttoExtraConfig.active().tweaks.lowHealth.reducedMotion = true;
                check(TweaksModule.lowHealthBlurPasses() == 0 && TweaksModule.lowHealthFovBoost() == 0, "Reduced motion must stop dynamic effects immediately");
            });
            int sounds = RuntimeProbe.heartbeats.get();
            ctx.waitTicks(25);
            check(RuntimeProbe.heartbeats.get() == sounds, "Heartbeat continues in reduced mode");
            ctx.takeScreenshot("privacy-world-lowhealth-reduced");
            ctx.runOnClient(client -> OttoExtraConfig.active().tweaks.lowHealth.enabled = false);
            ctx.waitTicks(25);
            check(RuntimeProbe.heartbeats.get() == sounds, "Heartbeat continues after disable");
            ctx.takeScreenshot("privacy-world-lowhealth-off");
            ctx.getInput().releaseKey(options -> options.forwardKey);
            pass("World: real low health and damage while moving, maximum preview blur=8/FOV=25/heartbeat active; reduced mode and disable stop dynamic effects");
            if (FabricLoader.getInstance().isModLoaded("ottoextra_animalbreeding")) {
                ctx.runOnClient(client -> OttoExtraConfig.active().animalInfo.breederModeEnabled = true);
                ctx.waitTicks(5);
                ctx.getInput().pressKey(GLFW.GLFW_KEY_J);
                ctx.waitFor(client -> client.currentScreen != null && client.currentScreen.getClass().getName()
                        .equals("de.ottoextra.animalbreeding.ui.HorseFarmOverviewScreen"), 200);
                ctx.waitTicks(10);
                double[] syncButton = ctx.computeOnClient(client -> {
                    var button = client.currentScreen.children().stream()
                            .filter(child -> child instanceof net.minecraft.client.gui.widget.ButtonWidget)
                            .map(child -> (net.minecraft.client.gui.widget.ButtonWidget) child)
                            .filter(child -> child.getMessage().getString().equals("Sync"))
                            .findFirst().orElseThrow();
                    var window = client.getWindow();
                    return new double[]{(button.getX() + button.getWidth() / 2.0) * window.getWidth() / window.getScaledWidth(),
                            (button.getY() + button.getHeight() / 2.0) * window.getHeight() / window.getScaledHeight()};
                });
                ctx.getInput().setCursorPos(syncButton[0], syncButton[1]);
                ctx.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
                ctx.waitTicks(5);
                ctx.takeScreenshot("privacy-animalbreeding-overview");
                ctx.runOnClient(client -> {
                    Object addon = FabricLoader.getInstance().getEntrypoints("client", net.fabricmc.api.ClientModInitializer.class)
                            .stream().filter(entry -> entry.getClass().getName()
                                    .equals("de.ottoextra.animalbreeding.AnimalBreedingClient"))
                            .findFirst().orElseThrow();
                    Object sync = field(addon.getClass(), addon, "syncService");
                    try {
                        Object status = sync.getClass().getMethod("status").invoke(sync);
                        check(status.getClass().getMethod("state").invoke(status).toString().equals("LOCAL_ONLY"),
                                "AnimalBreeding must stay local when online consent is denied");
                    } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                });
                ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
                ctx.waitFor(client -> client.currentScreen == null, 200);
                pass("AnimalBreeding 0.5.16: breeder overview opens/renders; manual Sync with denied online consent stays LOCAL_ONLY");
            }
            if (FabricLoader.getInstance().isModLoaded("xaeroworldmap")) {
                ctx.getInput().pressKey(GLFW.GLFW_KEY_M);
                ctx.waitFor(client -> client.currentScreen != null && client.currentScreen.getClass().getName().equals("xaero.map.gui.GuiMap"), 400);
                ctx.waitFor(client -> RuntimeProbe.overlayCalls.get() > 0, 400);
                ctx.takeScreenshot("privacy-xaero-map");
                ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
                pass("Xaero world map opens and production overlay injection executes");
            } else pass("Minimal client: no Xaero loaded; world/render path succeeds");
        }
        ctx.waitFor(client -> client.world == null && client.currentScreen instanceof TitleScreen, 400);
        pass("Disconnect returns to title without failure");
        try (var reopened = saved.open()) {
            ctx.waitTicks(40);
            ctx.runOnClient(client -> {
                check(de.ottoextra.OttoExtraGate.isOnOttonien(client), "Ottonien scope fixture inactive");
                for (var purpose : ConsentPolicy.Purpose.values()) check(!ConsentPolicy.allowed(OttoExtraConfig.active(), purpose), "Reconnect revived consent");
            });
        }
        check(RuntimeProbe.deniedHttpAttempts.get() == 0, "OttoExtra HTTP attempted on denied simulated Ottonien join/reconnect");
        RuntimeProbe.simulateOttonien = false;
        pass("Simulated Ottonien scope: local world join/reconnect with all grants denied causes zero ConsentHttp requests");
    }
    private static Object field(Class<?> type, Object owner, String name) {
        try { var f = type.getDeclaredField(name); f.setAccessible(true); return f.get(owner); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static com.mojang.authlib.GameProfile profile(UUID id) {
        String encoded = Base64.getEncoder().encodeToString("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/PrivacyQa\"}}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new com.mojang.authlib.GameProfile(id, "PrivacyFixture", new com.mojang.authlib.properties.PropertyMap(
                com.google.common.collect.ImmutableMultimap.of("textures", new com.mojang.authlib.properties.Property("textures", encoded))));
    }
    private static final class SlowImage implements AutoCloseable {
        private final com.sun.net.httpserver.HttpServer http;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final Map<String, Body> bodies = new ConcurrentHashMap<>();
        private final java.util.concurrent.atomic.AtomicInteger requests = new java.util.concurrent.atomic.AtomicInteger();
        private final byte[] png;
        private static final class Body {
            final CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        }
        SlowImage() throws Exception {
            var out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", out);
            png = out.toByteArray();
            http = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            http.setExecutor(executor);
            http.createContext("/", exchange -> {
                requests.incrementAndGet();
                Body body = bodies.get(exchange.getRequestURI().getPath().substring(1));
                try (exchange) {
                    if (body == null) { exchange.sendResponseHeaders(404, -1); return; }
                    exchange.sendResponseHeaders(200, png.length);
                    exchange.getResponseBody().write(png, 0, 24); exchange.getResponseBody().flush();
                    body.started.countDown();
                    if (!body.release.await(20, TimeUnit.SECONDS)) throw new java.io.IOException("Fixture release timeout");
                    exchange.getResponseBody().write(png, 24, png.length - 24);
                } catch (Exception expectedOnCancel) { /* Client withdrawal may close the socket. */ }
                finally { if (body != null) body.done.countDown(); }
            });
            http.start();
        }
        String base() { return "http://127.0.0.1:" + http.getAddress().getPort() + "/"; }
        Body add(String path, boolean immediate) { var body = new Body(); if (immediate) body.release.countDown(); bodies.put(path, body); return body; }
        @Override public void close() { bodies.values().forEach(body -> body.release.countDown()); http.stop(0); executor.shutdownNow(); }
    }
}
