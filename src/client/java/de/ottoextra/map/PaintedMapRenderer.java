package de.ottoextra.map;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.ottoextra.OttoExtra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Gemalte Ottonien-Karte auf der Xaero-Worldmap (Portierung des
 * OttoMap-Composite-Shaders auf die 1.21.11-GPU-Pipeline).
 *
 * <p>Ablauf pro Frame (nur bei offener Worldmap): Framebuffer in eine
 * Copy-Textur kopieren (Luma-Maske: schwarz = unerkundet), dann Fullscreen-Quad
 * mit 4 Samplern (Karte/Copy/Beschriftung/Karte-ohne-Details) + MapParams-UBO
 * zeichnen. Erkundetes Terrain bleibt sichtbar, unerkundetes zeigt die gemalte
 * Karte mit weichem Übergang. Draw-Muster nach Xaeros ImmediateRenderUtil
 * (RenderPass + DynamicTransforms + bindDefaultUniforms).</p>
 *
 * <p>Geo-Mapping exakt aus Legacy: Welt-Bounds X −10525..13559 / Z −7394..7108,
 * Quad-Affine cx−650/cz+150, hw·0.99/hz·0.98, Kamera-Adjust (65|−50, 0.98|1.01).</p>
 */
public final class PaintedMapRenderer {

    // Legacy-Geobounds der gemalten Karte
    private static final double MAP_MIN_X = -10525.0;
    private static final double MAP_MAX_X = 13559.0;
    private static final double MAP_MIN_Z = -7394.0;
    private static final double MAP_MAX_Z = 7108.0;

    private static final Identifier TEX_LOWER = OttoExtra.id("textures/map/otto-large_map_lower_layer.png");
    private static final Identifier TEX_NODETAILS = OttoExtra.id("textures/map/otto-large_map_lower_layer_nodetails.png");
    private static final Identifier TEX_UPPER = OttoExtra.id("textures/map/otto-large_map_upper_layer.png");
    private static final Identifier TEX_UPPER_HIRES = OttoExtra.id("textures/map/otto-large_map_upper_layer_high_res.png");

    private static volatile boolean disabled = false;
    /** Minimap-Karte separat abschaltbar (teilt NICHT das Worldmap-Flag). */
    private static volatile boolean minimapPaintedDisabled = false;
    /** Manueller Nutzer-Versatz (Blöcke), gesetzt von MapModule aus der Config. */
    private static volatile double userOffsetX = 0;
    private static volatile double userOffsetZ = 0;

    public static void setUserOffset(double x, double z) {
        userOffsetX = x;
        userOffsetZ = z;
    }

    private static RenderPipeline pipeline;
    private static GpuBuffer paramsBuffer;
    private static GpuTexture copyTexture;
    private static GpuTextureView copyView;
    private static GpuSampler copySampler;
    private static GpuSampler mapSampler;
    private static ProjectionMatrix2 guiOrtho;
    private static int copyW = -1;
    private static int copyH = -1;
    private static boolean texturesRegistered = false;

    private PaintedMapRenderer() {
    }

    public static boolean isDisabled() {
        return disabled;
    }

    /**
     * Zeichnet die gemalte Karte als Fullscreen-Composite. Aufruf aus dem
     * Worldmap-afterRender-Hook, VOR den Grenzlinien.
     *
     * @param view    Xaero-Sicht (Kamera/Zoom) — unangepasst; Affine passiert hier
     * @param screenW GUI-skalierte Screenbreite des Screens
     * @param screenH GUI-skalierte Screenhöhe
     */
    /** DIAGNOSE: true = Vanilla-Pipeline statt Composite (Plumbing-Bisektion). */
    private static final boolean DEBUG_SIMPLE_DRAW = false;

    public static void render(XaeroMapBridge.View view, int screenW, int screenH) {
        if (disabled || view == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            ensureResources(client);
            if (DEBUG_SIMPLE_DRAW) {
                renderBackground(view, screenW, screenH);
                return;
            }

            // 1) Framebuffer-Copy (Xaero-Terrain als Masken-Quelle)
            Framebuffer fb = client.getFramebuffer();
            int fbW = fb.textureWidth;
            int fbH = fb.textureHeight;
            ensureCopyTexture(fbW, fbH);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(fb.getColorAttachment(), copyTexture, 0, 0, 0, 0, 0, fbW, fbH);

            // 2) Kamera-/Skalen-Affine (Legacy)
            double eff = view.effScale();
            double adjCamX = (view.cameraX() - 65.0) / 0.98;
            double adjCamZ = (view.cameraZ() + 50.0) / 1.01;
            double adjScaleX = eff * 0.98;
            double adjScaleZ = eff * 1.01;

            // 3) Karten-Rect in Screen-Koordinaten -> UV-Mapping des Fullscreen-Quads
            double cx = (MAP_MIN_X + MAP_MAX_X) / 2.0 - 650.0 + userOffsetX;
            double cz = (MAP_MIN_Z + MAP_MAX_Z) / 2.0 + 150.0 + userOffsetZ;
            double hw = (MAP_MAX_X - MAP_MIN_X) / 2.0 * 0.99;
            double hz = (MAP_MAX_Z - MAP_MIN_Z) / 2.0 * 0.98;
            float x1 = (float) ((cx - hw - adjCamX) * adjScaleX + screenW / 2.0);
            float y1 = (float) ((cz - hz - adjCamZ) * adjScaleZ + screenH / 2.0);
            float x2 = (float) ((cx + hw - adjCamX) * adjScaleX + screenW / 2.0);
            float y2 = (float) ((cz + hz - adjCamZ) * adjScaleZ + screenH / 2.0);
            if (x2 - x1 < 1 || y2 - y1 < 1) {
                return;
            }
            float uLeft = (0.0f - x1) / (x2 - x1);
            float uRight = (screenW - x1) / (x2 - x1);
            float vTop = (0.0f - y1) / (y2 - y1);
            float vBottom = (screenH - y1) / (y2 - y1);

            // 4) Uniform-Werte (Legacy-Formeln)
            // 1.21.11: getSkyBrightness entfallen -> aus AmbientDarkness (0=Tag..11=Nacht) ableiten
            float skyBrightness = client.world != null
                    ? clamp01(1.0f - client.world.getAmbientDarkness() / 11.0f) : 1.0f;
            float night = 0.7f * (0.3f + 0.7f * skyBrightness);
            float detailBlend = clamp01(1.0f - (float) ((eff - 0.05) / 0.1));
            float upperAlpha = clamp01(1.0f - (float) ((eff - 0.3) / 0.2));
            float lowerAlpha = clamp01(1.0f - (float) ((eff - 0.5) / 0.2));
            float overallAlpha = clamp01(1.0f - (float) ((eff - 5.4) / 0.4));
            if (overallAlpha <= 0.0f) {
                return;
            }
            // HudGuard wie Legacy: 14px * GUI-Scale schützt Xaeros Koordinaten-/Zoom-Anzeige
            float hudGuard = (float) (14.0 * client.getWindow().getScaleFactor());
            writeParams((float) adjScaleX, hudGuard, night, detailBlend, upperAlpha, lowerAlpha, overallAlpha);

            // 5) Fullscreen-Quad zeichnen (Beschriftung zoomabhängig: Legacy-Schwelle 0.15)
            Identifier upperTex = eff > 0.15 ? TEX_UPPER_HIRES : TEX_UPPER;
            withGuiOrtho(client, () ->
                    drawQuad(client, upperTex, screenW, screenH, uLeft, uRight, vTop, vBottom));
        } catch (Throwable t) {
            disabled = true;
            OttoExtra.LOGGER.warn("[map] Gemalte Karte deaktiviert: {}", t.toString());
        }
    }

    // ---- Ressourcen ----------------------------------------------------------

    private static void ensureResources(MinecraftClient client) {
        if (pipeline == null) {
            pipeline = RenderPipeline.builder()
                    .withLocation(OttoExtra.id("pipeline/map_composite"))
                    .withVertexShader(OttoExtra.id("core/map_composite"))
                    .withFragmentShader(OttoExtra.id("core/map_composite"))
                    .withSampler("Sampler0")
                    .withSampler("Sampler1")
                    .withSampler("Sampler2")
                    .withSampler("Sampler3")
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .withUniform("MapParams", UniformType.UNIFORM_BUFFER)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build();
        }
        if (paramsBuffer == null) {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "ottoextra map params",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);
        }
        if (!texturesRegistered) {
            // ReloadableTexture-Registrierung lädt mid-game NICHT (erst beim
            // Resource-Reload) -> selbst dekodieren + sofort hochladen
            // (bewährtes Muster aus BannerTextureService).
            loadMapTexture(client, TEX_LOWER);
            loadMapTexture(client, TEX_NODETAILS);
            loadMapTexture(client, TEX_UPPER);
            loadMapTexture(client, TEX_UPPER_HIRES);
            texturesRegistered = true;
            OttoExtra.LOGGER.info("[map] Gemalte-Karte-Texturen geladen.");
        }
        if (mapSampler == null) {
            mapSampler = RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
        }
    }

    private static void loadMapTexture(MinecraftClient client, Identifier id) {
        try (var stream = client.getResourceManager().getResource(id)
                .orElseThrow(() -> new IllegalStateException("Resource fehlt: " + id))
                .getInputStream()) {
            NativeImage image = NativeImage.read(stream);
            client.getTextureManager().registerTexture(id,
                    new NativeImageBackedTexture(id::toString, image));
        } catch (Exception e) {
            throw new IllegalStateException("Karten-Textur " + id + ": " + e.getMessage(), e);
        }
    }

    private static void ensureCopyTexture(int w, int h) {
        if (copyTexture != null && w == copyW && h == copyH) {
            return;
        }
        if (copyTexture != null) {
            copyTexture.close();
        }
        copyTexture = RenderSystem.getDevice().createTexture(
                () -> "ottoextra map screen copy",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, w, h, 1, 1);
        copyView = RenderSystem.getDevice().createTextureView(copyTexture);
        if (copySampler == null) {
            copySampler = RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        }
        copyW = w;
        copyH = h;
    }

    /** Wiederverwendeter DIREKTER Buffer — die GPU-API verlangt Off-Heap-Speicher. */
    private static final ByteBuffer PARAMS_STAGING =
            ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());

    private static void writeParams(float fadeScale, float hudGuard, float night,
                                    float detail, float upper, float lower, float overall) {
        PARAMS_STAGING.clear();
        PARAMS_STAGING.putFloat(fadeScale).putFloat(hudGuard).putFloat(night).putFloat(detail)
                .putFloat(upper).putFloat(lower).putFloat(overall).putFloat(0f);
        PARAMS_STAGING.flip();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToBuffer(paramsBuffer.slice(0, 32), PARAMS_STAGING);
    }

    // ---- Draw (Muster: Xaero ImmediateRenderUtil) ------------------------------

    private static void drawQuad(MinecraftClient client, Identifier upperTexId, int screenW, int screenH,
                                 float uL, float uR, float vT, float vB) {
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(0, 0, 0).texture(uL, vT);
        buffer.vertex(0, screenH, 0).texture(uL, vB);
        buffer.vertex(screenW, screenH, 0).texture(uR, vB);
        buffer.vertex(screenW, 0, 0).texture(uR, vT);

        TextureManager tm = client.getTextureManager();
        AbstractTexture lower = tm.getTexture(TEX_LOWER);
        AbstractTexture nodetails = tm.getTexture(TEX_NODETAILS);
        AbstractTexture upper = tm.getTexture(upperTexId);

        Framebuffer target = client.getFramebuffer();
        GpuTextureView colorTarget = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : target.getColorAttachmentView();
        GpuTextureView depthTarget = target.useDepthAttachment
                ? (RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride : target.getDepthAttachmentView())
                : null;

        try (BuiltBuffer mesh = buffer.end()) {
            RenderSystem.ShapeIndexBuffer shapeIndex =
                    RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
            GpuBuffer indexBuffer = shapeIndex.getIndexBuffer(mesh.getDrawParameters().indexCount());
            GpuBuffer vertexBuffer = pipeline.getVertexFormat()
                    .uploadImmediateVertexBuffer(mesh.getBuffer());

            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1f, 1f, 1f, 1f),
                    new Vector3f(),
                    new Matrix4f());

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                    .createRenderPass(() -> "ottoextra painted map", colorTarget,
                            OptionalInt.empty(), depthTarget, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("MapParams", paramsBuffer.slice(0, 32));
                pass.bindTexture("Sampler0", lower.getGlTextureView(), mapSampler);
                pass.bindTexture("Sampler1", copyView, copySampler);
                pass.bindTexture("Sampler2", upper.getGlTextureView(), mapSampler);
                pass.bindTexture("Sampler3", nodetails.getGlTextureView(), mapSampler);
                pass.setIndexBuffer(indexBuffer, shapeIndex.getIndexType());
                pass.setVertexBuffer(0, vertexBuffer);
                pass.drawIndexed(0, 0, mesh.getDrawParameters().indexCount(), 1);
            }
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /**
     * Gemalte Karte in der MINIMAP — v3: Maske aus Xaeros Tile-Daten statt
     * Framebuffer (FBO-Kopien sind im Minimap-Pass nicht erlaubt). Pro
     * unerkundetem Chunk ein texturiertes Quad im Element-Raum; Chunks mit
     * erkundeten Nachbarn bekommen reduziertes Alpha = weicher Rand.
     * {@code explored} liefert, ob Xaero fuer (chunkX, chunkZ) Daten hat.
     */
    public static void renderMinimap(Matrix4f poseMatrix, double camX, double camZ,
                                     double ps, double pc, double zoom,
                                     int halfView, boolean circle,
                                     java.util.function.BiPredicate<Integer, Integer> explored) {
        if (minimapPaintedDisabled || zoom <= 0 || halfView <= 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            ensureResources(client);
            double cx = (MAP_MIN_X + MAP_MAX_X) / 2.0 - 650.0 + userOffsetX;
            double cz = (MAP_MIN_Z + MAP_MAX_Z) / 2.0 + 150.0 + userOffsetZ;
            double hw = (MAP_MAX_X - MAP_MIN_X) / 2.0 * 0.99;
            double hz = (MAP_MAX_Z - MAP_MIN_Z) / 2.0 * 0.98;

            float skyBrightness = client.world != null
                    ? clamp01(1.0f - client.world.getAmbientDarkness() / 11.0f) : 1.0f;
            float night = 0.7f * (0.3f + 0.7f * skyBrightness);

            int camChunkX = (int) Math.floor(camX / 16.0);
            int camChunkZ = (int) Math.floor(camZ / 16.0);
            int range = (int) Math.ceil(halfView / zoom / 16.0) + 1;
            double clipR = circle ? halfView + 16 * zoom : halfView * Math.sqrt(2) + 16 * zoom;

            BufferBuilder buf = null;
            for (int dcx = -range; dcx <= range; dcx++) {
                for (int dcz = -range; dcz <= range; dcz++) {
                    int ccx = camChunkX + dcx;
                    int ccz = camChunkZ + dcz;
                    if (explored.test(ccx, ccz)) {
                        continue;
                    }
                    // Chunk-Mitte im Element-Raum: grobes Cull
                    double mx = ccx * 16 + 8 - camX;
                    double mz = ccz * 16 + 8 - camZ;
                    double ex = (ps * mx - pc * mz) * zoom;
                    double ey = (pc * mx + ps * mz) * zoom;
                    if (ex * ex + ey * ey > clipR * clipR) {
                        continue;
                    }
                    // weicher Rand: erkundete 4er-Nachbarn senken das Alpha
                    int exploredNeighbors = 0;
                    if (explored.test(ccx + 1, ccz)) exploredNeighbors++;
                    if (explored.test(ccx - 1, ccz)) exploredNeighbors++;
                    if (explored.test(ccx, ccz + 1)) exploredNeighbors++;
                    if (explored.test(ccx, ccz - 1)) exploredNeighbors++;
                    float alpha = exploredNeighbors == 0 ? 1.0f
                            : Math.max(0.35f, 1.0f - 0.25f * exploredNeighbors);
                    if (buf == null) {
                        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED.getVertexFormat());
                    }
                    emitClippedChunkQuad(buf, poseMatrix, ccx, ccz, camX, camZ, ps, pc, zoom,
                            cx, cz, hw, hz, night, alpha, halfView, circle);
                }
            }
            if (buf == null) {
                return;
            }
            TextureManager tm = client.getTextureManager();
            AbstractTexture lower = tm.getTexture(TEX_LOWER);
            // Aktive Projection/ModelView des Minimap-Passes nutzen (kein Ortho)
            drawImmediate(client, buf,
                    net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                    lower.getGlTextureView(), mapSampler);
        } catch (Throwable t) {
            minimapPaintedDisabled = true;
            OttoExtra.LOGGER.warn("[map] Minimap-Karte deaktiviert: {}", t.toString());
        }
    }

    /** Kreis-Approximation fürs Clipping (Halbebenen-Anzahl). */
    private static final int CIRCLE_CLIP_SIDES = 32;

    /**
     * Chunk-Quad gegen die Minimap-Kontur clippen (Sutherland-Hodgman gegen
     * Quadrat bzw. 32-Eck) und als Fan-Quads emittieren (letzter Vertex
     * doppelt = Dreieck, DrawMode bleibt QUADS).
     */
    private static void emitClippedChunkQuad(BufferBuilder buf, Matrix4f pose,
                                             int ccx, int ccz,
                                             double camX, double camZ, double ps, double pc, double zoom,
                                             double cx, double cz, double hw, double hz,
                                             float night, float alpha, int halfView, boolean circle) {
        double wx0 = ccx * 16;
        double wz0 = ccz * 16;
        // Quad-Ecken in den Element-Raum transformieren
        double[] xs = new double[40];
        double[] ys = new double[40];
        int n = 0;
        for (int corner = 0; corner < 4; corner++) {
            double wx = wx0 + ((corner == 2 || corner == 3) ? 16 : 0);
            double wz = wz0 + ((corner == 1 || corner == 2) ? 16 : 0);
            double ox = wx - camX;
            double oy = wz - camZ;
            xs[n] = (ps * ox - pc * oy) * zoom;
            ys[n] = (pc * ox + ps * oy) * zoom;
            n++;
        }
        // Schnellpfad: komplett innerhalb -> ungeclippt
        boolean inside = true;
        for (int i = 0; i < 4 && inside; i++) {
            if (circle) {
                inside = xs[i] * xs[i] + ys[i] * ys[i] <= (double) halfView * halfView;
            } else {
                inside = Math.abs(xs[i]) <= halfView && Math.abs(ys[i]) <= halfView;
            }
        }
        if (!inside) {
            double[] tx = new double[40];
            double[] ty = new double[40];
            if (circle) {
                for (int s = 0; s < CIRCLE_CLIP_SIDES && n > 2; s++) {
                    double ang = 2 * Math.PI * s / CIRCLE_CLIP_SIDES;
                    n = clipHalfPlane(xs, ys, n, Math.cos(ang), Math.sin(ang), halfView, tx, ty);
                    double[] swap = xs; xs = tx; tx = swap;
                    swap = ys; ys = ty; ty = swap;
                }
            } else {
                double[][] planes = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (double[] p : planes) {
                    if (n <= 2) break;
                    n = clipHalfPlane(xs, ys, n, p[0], p[1], halfView, tx, ty);
                    double[] swap = xs; xs = tx; tx = swap;
                    swap = ys; ys = ty; ty = swap;
                }
            }
            if (n < 3) {
                return;
            }
        }
        // Fan-Zerlegung in Quads (Dreieck = letzter Vertex doppelt)
        for (int i = 1; i + 1 < n; i++) {
            emitElementVertex(buf, pose, xs[0], ys[0], camX, camZ, ps, pc, zoom, cx, cz, hw, hz, night, alpha);
            emitElementVertex(buf, pose, xs[i], ys[i], camX, camZ, ps, pc, zoom, cx, cz, hw, hz, night, alpha);
            emitElementVertex(buf, pose, xs[i + 1], ys[i + 1], camX, camZ, ps, pc, zoom, cx, cz, hw, hz, night, alpha);
            emitElementVertex(buf, pose, xs[i + 1], ys[i + 1], camX, camZ, ps, pc, zoom, cx, cz, hw, hz, night, alpha);
        }
    }

    /** Polygon gegen Halbebene a*x + b*y <= c clippen; Ergebnis in tx/ty. */
    private static int clipHalfPlane(double[] xs, double[] ys, int n,
                                     double a, double b, double c,
                                     double[] tx, double[] ty) {
        int out = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double di = a * xs[i] + b * ys[i] - c;
            double dj = a * xs[j] + b * ys[j] - c;
            if (di <= 0) {
                tx[out] = xs[i];
                ty[out] = ys[i];
                out++;
            }
            if ((di < 0) != (dj < 0) && di != dj) {
                double t = di / (di - dj);
                tx[out] = xs[i] + t * (xs[j] - xs[i]);
                ty[out] = ys[i] + t * (ys[j] - ys[i]);
                out++;
            }
        }
        return out;
    }

    /** Vertex im Element-Raum; UV über inverse Minimap-Rotation aus der Welt. */
    private static void emitElementVertex(BufferBuilder buf, Matrix4f pose,
                                          double ex, double ey,
                                          double camX, double camZ, double ps, double pc, double zoom,
                                          double cx, double cz, double hw, double hz,
                                          float night, float alpha) {
        double ox = (ps * ex + pc * ey) / zoom;
        double oy = (-pc * ex + ps * ey) / zoom;
        // Welt -> Kartenraum: gleiche Legacy-Kalibrierung wie die Worldmap
        // (adjCam (x-65)/0.98 bzw. (z+50)/1.01), sonst liegt das Bild daneben
        double kx = (camX + ox - 65.0) / 0.98;
        double kz = (camZ + oy + 50.0) / 1.01;
        float u = (float) ((kx - (cx - hw)) / (2 * hw));
        float v = (float) ((kz - (cz - hz)) / (2 * hz));
        buf.vertex(pose, (float) ex, (float) ey, 0f).texture(u, v).color(night, night, night, alpha);
    }

    /**
     * GuiMap stellt nach seinem Terrain-Render die Welt-Projection wieder her
     * (Xaero Misc.minecraftOrtho-Backup) — unser afterRender-Hook läuft also
     * OHNE GUI-Ortho. Immediate-Draws brauchen deshalb eigene Ortho +
     * Identity-ModelView (Parameter wie Vanilla-GuiRenderer: near 1000, far
     * 11000, invertY; z-Shift −11000 wie Xaero).
     */
    static void withGuiOrtho(MinecraftClient client, Runnable draw) {
        com.mojang.blaze3d.buffers.GpuBufferSlice projBackup = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType projTypeBackup = RenderSystem.getProjectionType();
        if (guiOrtho == null) {
            guiOrtho = new ProjectionMatrix2("ottoextra map ortho", 1000.0f, 11000.0f, true);
        }
        var window = client.getWindow();
        float orthoW = (float) (window.getFramebufferWidth() / window.getScaleFactor());
        float orthoH = (float) (window.getFramebufferHeight() / window.getScaleFactor());
        RenderSystem.setProjectionMatrix(guiOrtho.set(orthoW, orthoH), ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        modelView.translate(0.0f, 0.0f, -11000.0f);
        try {
            draw.run();
        } finally {
            modelView.popMatrix();
            RenderSystem.setProjectionMatrix(projBackup, projTypeBackup);
        }
    }

    /**
     * Gemalte Karte als IMMEDIATE-Hintergrund (beforeRender): eigene RenderPass-
     * Draws mit Vanilla-GUI-Pipelines, damit Xaeros (ebenfalls immediate)
     * Terrain/Marker DANACH darüber liegen. Kein Masken-Shader nötig.
     */
    public static void renderBackground(XaeroMapBridge.View view, int screenW, int screenH) {
        if (disabled || view == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            ensureResources(client);

            double eff = view.effScale();
            double adjCamX = (view.cameraX() - 65.0) / 0.98;
            double adjCamZ = (view.cameraZ() + 50.0) / 1.01;
            double adjScaleX = eff * 0.98;
            double adjScaleZ = eff * 1.01;

            double cx = (MAP_MIN_X + MAP_MAX_X) / 2.0 - 650.0 + userOffsetX;
            double cz = (MAP_MIN_Z + MAP_MAX_Z) / 2.0 + 150.0 + userOffsetZ;
            double hw = (MAP_MAX_X - MAP_MIN_X) / 2.0 * 0.99;
            double hz = (MAP_MAX_Z - MAP_MIN_Z) / 2.0 * 0.98;
            float x1 = (float) ((cx - hw - adjCamX) * adjScaleX + screenW / 2.0);
            float y1 = (float) ((cz - hz - adjCamZ) * adjScaleZ + screenH / 2.0);
            float x2 = (float) ((cx + hw - adjCamX) * adjScaleX + screenW / 2.0);
            float y2 = (float) ((cz + hz - adjCamZ) * adjScaleZ + screenH / 2.0);
            if (x2 - x1 < 1 || y2 - y1 < 1) {
                return;
            }

            float detailBlend = clamp01(1.0f - (float) ((eff - 0.05) / 0.1));
            float upperAlpha = clamp01(1.0f - (float) ((eff - 0.3) / 0.2));
            float overallAlpha = clamp01(1.0f - (float) ((eff - 5.4) / 0.4));
            if (overallAlpha <= 0f) {
                return;
            }

            TextureManager tm = client.getTextureManager();
            AbstractTexture lowerTex = tm.getTexture(detailBlend > 0.5f ? TEX_LOWER : TEX_NODETAILS);
            AbstractTexture upperTex = tm.getTexture(eff > 0.15 ? TEX_UPPER_HIRES : TEX_UPPER);

            withGuiOrtho(client, () -> {
                // Meeres-/Außenfläche (#6E7B8B, volle Helligkeit)
                drawImmediateColorQuad(client, 0, 0, screenW, screenH,
                        packColor(0x6E, 0x7B, 0x8B, overallAlpha));
                // Untere Kartenebene (volle Helligkeit — keine Doppel-Abdunklung mehr)
                drawImmediateTexturedQuad(client, lowerTex, x1, y1, x2, y2,
                        packColor(0xFF, 0xFF, 0xFF, overallAlpha));
                // Beschriftungs-Ebene
                if (upperAlpha > 0.02f) {
                    drawImmediateTexturedQuad(client, upperTex, x1, y1, x2, y2,
                            packColor(0xFF, 0xFF, 0xFF, upperAlpha * overallAlpha));
                }
            });
        } catch (Throwable t) {
            disabled = true;
            OttoExtra.LOGGER.warn("[map] Gemalte Karte deaktiviert: {}", t.toString());
        }
    }

    private static int packColor(int r, int g, int b, float a) {
        int ai = Math.max(0, Math.min(255, Math.round(a * 255)));
        return (ai << 24) | (r << 16) | (g << 8) | b;
    }

    private static void drawImmediateTexturedQuad(MinecraftClient client, AbstractTexture tex,
                                                  float x1, float y1, float x2, float y2, int color) {
        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderPipeline pl = net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, pl.getVertexFormat());
        buf.vertex(x1, y1, 0).texture(0f, 0f).color(r, g, b, a);
        buf.vertex(x1, y2, 0).texture(0f, 1f).color(r, g, b, a);
        buf.vertex(x2, y2, 0).texture(1f, 1f).color(r, g, b, a);
        buf.vertex(x2, y1, 0).texture(1f, 0f).color(r, g, b, a);
        drawImmediate(client, buf, pl, tex.getGlTextureView(), mapSampler);
    }

    private static void drawImmediateColorQuad(MinecraftClient client,
                                               float x1, float y1, float x2, float y2, int color) {
        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderPipeline pl = net.minecraft.client.gl.RenderPipelines.GUI;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, pl.getVertexFormat());
        buf.vertex(x1, y1, 0).color(r, g, b, a);
        buf.vertex(x1, y2, 0).color(r, g, b, a);
        buf.vertex(x2, y2, 0).color(r, g, b, a);
        buf.vertex(x2, y1, 0).color(r, g, b, a);
        drawImmediate(client, buf, pl, null, null);
    }

    /** Gemeinsamer Immediate-Draw (Muster: Xaero ImmediateRenderUtil). */
    static void drawImmediate(MinecraftClient client, BufferBuilder buffer,
                              RenderPipeline pl, GpuTextureView texView, GpuSampler sampler) {
        Framebuffer target = client.getFramebuffer();
        GpuTextureView colorTarget = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : target.getColorAttachmentView();
        GpuTextureView depthTarget = target.useDepthAttachment
                ? (RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride : target.getDepthAttachmentView())
                : null;

        try (BuiltBuffer mesh = buffer.end()) {
            RenderSystem.ShapeIndexBuffer shapeIndex =
                    RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
            GpuBuffer indexBuffer = shapeIndex.getIndexBuffer(mesh.getDrawParameters().indexCount());
            GpuBuffer vertexBuffer = pl.getVertexFormat().uploadImmediateVertexBuffer(mesh.getBuffer());

            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1f, 1f, 1f, 1f),
                    new Vector3f(),
                    new Matrix4f());

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                    .createRenderPass(() -> "ottoextra painted bg", colorTarget,
                            OptionalInt.empty(), depthTarget, OptionalDouble.empty())) {
                pass.setPipeline(pl);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                RenderSystem.bindDefaultUniforms(pass);
                if (texView != null) {
                    pass.bindTexture("Sampler0", texView, sampler);
                }
                pass.setIndexBuffer(indexBuffer, shapeIndex.getIndexType());
                pass.setVertexBuffer(0, vertexBuffer);
                pass.drawIndexed(0, 0, mesh.getDrawParameters().indexCount(), 1);
            }
        }
    }

    // ---- Einfacher Deferred-Pfad (Hintergrund unter Xaero, ohne Masken-Shader) ----

    /**
     * Zeichnet die gemalte Karte als normalen (deferred) GUI-Draw — gedacht für
     * {@code beforeRender}: liegt dadurch UNTER Xaeros Terrain-Tiles. Kein
     * Composite-Shader/FB-Copy nötig; erkundetes Terrain deckt die Karte ab.
     */
    public static void renderSimple(net.minecraft.client.gui.DrawContext ctx,
                                    XaeroMapBridge.View view, int screenW, int screenH) {
        if (disabled || view == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            ensureResources(client);

            double eff = view.effScale();
            double adjCamX = (view.cameraX() - 65.0) / 0.98;
            double adjCamZ = (view.cameraZ() + 50.0) / 1.01;
            double adjScaleX = eff * 0.98;
            double adjScaleZ = eff * 1.01;

            double cx = (MAP_MIN_X + MAP_MAX_X) / 2.0 - 650.0 + userOffsetX;
            double cz = (MAP_MIN_Z + MAP_MAX_Z) / 2.0 + 150.0 + userOffsetZ;
            double hw = (MAP_MAX_X - MAP_MIN_X) / 2.0 * 0.99;
            double hz = (MAP_MIN_Z - MAP_MAX_Z) / -2.0 * 0.98;
            int x1 = (int) Math.floor((cx - hw - adjCamX) * adjScaleX + screenW / 2.0);
            int y1 = (int) Math.floor((cz - hz - adjCamZ) * adjScaleZ + screenH / 2.0);
            int x2 = (int) Math.ceil((cx + hw - adjCamX) * adjScaleX + screenW / 2.0);
            int y2 = (int) Math.ceil((cz + hz - adjCamZ) * adjScaleZ + screenH / 2.0);
            int w = x2 - x1;
            int h = y2 - y1;
            if (w < 1 || h < 1) {
                return;
            }

            float skyBrightness = client.world != null
                    ? clamp01(1.0f - client.world.getAmbientDarkness() / 11.0f) : 1.0f;
            float night = 0.7f * (0.3f + 0.7f * skyBrightness);
            float detailBlend = clamp01(1.0f - (float) ((eff - 0.05) / 0.1));
            float upperAlpha = clamp01(1.0f - (float) ((eff - 0.3) / 0.2));
            float overallAlpha = clamp01(1.0f - (float) ((eff - 5.4) / 0.4));
            if (overallAlpha <= 0f) {
                return;
            }

            // Füllfarbe hinter der Karte (Meer/ausserhalb der Geobounds)
            int nb = (int) (255 * night);
            int fill = 0xFF000000 | (tint(0x6E, night) << 16) | (tint(0x7B, night) << 8) | tint(0x8B, night);
            ctx.fill(0, 0, screenW, screenH, withOverall(fill, overallAlpha));

            // Untere Ebene: Details vs. ohne Details (zoomabhängig)
            Identifier lowerTex = detailBlend > 0.5f ? TEX_LOWER : TEX_NODETAILS;
            int tintCol = tintColor(night, overallAlpha);
            ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, lowerTex,
                    x1, y1, 0f, 0f, w, h, w, h, tintCol);
            // Obere Ebene (Beschriftung) mit eigenem Alpha
            if (upperAlpha > 0.02f) {
                ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                        eff > 0.15 ? TEX_UPPER_HIRES : TEX_UPPER,
                        x1, y1, 0f, 0f, w, h, w, h, tintColor(night, upperAlpha * overallAlpha));
            }
        } catch (Throwable t) {
            disabled = true;
            OttoExtra.LOGGER.warn("[map] Gemalte Karte deaktiviert: {}", t.toString());
        }
    }

    private static int tint(int channel, float f) {
        return Math.max(0, Math.min(255, Math.round(channel * f)));
    }

    private static int tintColor(float night, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        int c = Math.max(0, Math.min(255, Math.round(night * 255)));
        return (a << 24) | (c << 16) | (c << 8) | c;
    }

    private static int withOverall(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
