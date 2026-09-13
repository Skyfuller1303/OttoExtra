package de.ottoextra.regions;

import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.OttoExtraPaths;
import de.ottoextra.privacy.ConsentPolicy;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Real native texture/render regression; no network requests or server join. */
public final class BannerCacheClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext ctx) {
        List<Path> files = new ArrayList<>();
        List<Identifier> textures = new ArrayList<>();
        String prefix = "banner-regression-" + UUID.randomUUID() + "-";
        OttoExtraApiClient api = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> { throw new AssertionError("Unexpected network call: " + method.getName()); });
        BannerTextureService banners = new BannerTextureService(api);
        try {
            Files.createDirectories(OttoExtraPaths.bannersCache());
            for (int i = 0; i < 80; i++) {
                BufferedImage png = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                int color = 0xff000000 | ((i * 41 + 50) & 255) << 16
                        | ((i * 83 + 90) & 255) << 8 | ((i * 137 + 70) & 255);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
                    png.setRGB(x, y, x == 7 || y == 7 ? 0xffeeeecc : color);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(png, "png", bytes);
                Path file = OttoExtraPaths.bannersCache().resolve(prefix + i + ".png");
                Files.write(file, bytes.toByteArray());
                files.add(file);
            }
            ctx.runOnClient(client -> {
                OttoExtraConfig config = OttoExtraConfig.active();
                ConsentPolicy.decide(config, ConsentPolicy.Purpose.EXTERNAL_IMAGES, false,
                        ConsentPolicy.scope(config, ConsentPolicy.Purpose.EXTERNAL_IMAGES));
                for (int i = 0; i < 80; i++) {
                    String key = prefix + i;
                    banners.bannerForPath(key, "uploads/local-fixture.png");
                    Identifier id = banners.bannerForPath(key, "uploads/local-fixture.png")
                            .orElseThrow(() -> new AssertionError("Local banner blocked at " + key));
                    if (!(client.getTextureManager().getTexture(id) instanceof NativeImageBackedTexture))
                        throw new AssertionError("Missing native banner texture");
                    textures.add(id);
                }
                // A full map's small banners must coexist, including the first loaded entries.
                for (int i = 0; i < 80; i++) if (banners.bannerForPath(prefix + i,
                        "uploads/local-fixture.png").isEmpty()) throw new AssertionError("Banner evicted too early");
                client.setScreen(new Grid(textures));
            });
            ctx.takeScreenshot("banner-cache-80");
            System.out.println("BANNER QA PASS: 80 cached banners coexist and render without network consent");
            for (int i = 0; i < 4; i++) {
                Path file = OttoExtraPaths.bannersCache().resolve(prefix + "lru-" + i + ".png");
                Files.copy(files.get(i), file);
                files.add(file);
            }
            ctx.runOnClient(client -> {
                BannerTextureService small = new BannerTextureService(api,
                        new BannerTextureBudget(3, 128 * 1024));
                for (int i = 0; i < 3; i++) small.bannerForPath(prefix + "lru-" + i, "uploads/local-fixture.png");
                Identifier victim = small.bannerForPath(prefix + "lru-1", "uploads/local-fixture.png").orElseThrow();
                NativeImageBackedTexture old = (NativeImageBackedTexture) client.getTextureManager().getTexture(victim);
                // Touch 0 and 2 so banner 1 is the least recently used.
                small.bannerForPath(prefix + "lru-0", "uploads/local-fixture.png");
                small.bannerForPath(prefix + "lru-2", "uploads/local-fixture.png");
                small.bannerForPath(prefix + "lru-3", "uploads/local-fixture.png");
                if (old.getImage() != null) throw new AssertionError("Eviction leaked native image/GPU texture");
                small.bannerForPath(prefix + "lru-1", "uploads/local-fixture.png");
                Identifier reloaded = small.bannerForPath(prefix + "lru-1", "uploads/local-fixture.png").orElseThrow();
                NativeImageBackedTexture fresh = (NativeImageBackedTexture) client.getTextureManager().getTexture(reloaded);
                if (fresh == old || fresh.getImage() == null) throw new AssertionError("Evicted banner could not reload from disk");
                for (int i = 0; i < 4; i++) textures.add(Identifier.of("ottoextra", "dynamic/banners/" + prefix + "lru-" + i));
            });
            System.out.println("BANNER QA PASS: eviction closes native images; evicted banners reload locally");
        } catch (Exception error) { throw new RuntimeException(error); }
        finally {
            ctx.runOnClient(client -> {
                client.setScreen(new TitleScreen());
                textures.forEach(client.getTextureManager()::destroyTexture);
            });
            for (Path file : files) try { Files.deleteIfExists(file); } catch (Exception ignored) { }
        }
    }

    private static final class Grid extends Screen {
        private final List<Identifier> textures;
        Grid(List<Identifier> textures) { super(Text.literal("80 lokale Banner")); this.textures = List.copyOf(textures); }
        @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xff172027);
            ctx.drawCenteredTextWithShadow(textRenderer, "80 lokale Banner · kein Download", width / 2, 12, 0xffffffff);
            for (int i = 0; i < textures.size(); i++) {
                int x = (width - 400) / 2 + (i % 10) * 40;
                int y = 35 + (i / 10) * 36;
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, textures.get(i), x, y, 0, 0, 24, 24, 24, 24);
                ctx.drawTextWithShadow(textRenderer, Integer.toString(i + 1), x, y + 25, 0xffffffff);
            }
        }
    }
}
