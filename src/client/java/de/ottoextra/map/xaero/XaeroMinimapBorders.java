package de.ottoextra.map.xaero;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.map.LehenPolygonStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import xaero.common.HudMod;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.over.MinimapElementOverMapRendererHandler;

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

public final class XaeroMinimapBorders extends MinimapElementRenderer<XaeroMinimapBorders.Marker, Void> {

    public static final class Marker {
        static final Marker INSTANCE = new Marker();
    }

    private static final int COL_BORDER = 0xCCB8893A;
    private static final int COL_BORDER_INSIDE = 0xFFE6C8A9;

    private static boolean registered = false;

    private static volatile boolean lastCircle = false;
    private static volatile long lastCircleStampMs = 0;

    public static Boolean circleShape() {
        if (System.currentTimeMillis() - lastCircleStampMs > 5000L) {
            return null;
        }
        return lastCircle;
    }

    private static boolean minimapHasTile(xaero.common.minimap.region.MinimapChunk[][] blocks,
                                          int originX, int originZ, int ccx, int ccz) {
        int mcx = (ccx >> 2) - originX;
        int mcz = (ccz >> 2) - originZ;
        if (mcx < 0 || mcx >= blocks.length) {
            return false;
        }
        xaero.common.minimap.region.MinimapChunk[] column = blocks[mcx];
        if (column == null || mcz < 0 || mcz >= column.length) {
            return false;
        }
        xaero.common.minimap.region.MinimapChunk chunk = column[mcz];
        return chunk != null && chunk.getTile(ccx & 3, ccz & 3) != null;
    }

    private final OttoExtraConfig.Map cfg;
    private final BooleanSupplier visible;

    private final MinimapElementOverMapRendererHandler handler;
    private Field fPs;
    private Field fPc;
    private Field fZoom;
    private Field fHalfViewW;
    private Field fCircle;
    private boolean fieldsResolved = false;
    private boolean fieldsFailed = false;

    private XaeroMinimapBorders(OttoExtraConfig.Map cfg, BooleanSupplier visible,
                                MinimapElementOverMapRendererHandler handler) {
        super(new BorderReader(), new BorderProvider(cfg, visible), null);
        this.cfg = cfg;
        this.visible = visible;
        this.handler = handler;
    }

    public static boolean tryRegister(OttoExtraConfig.Map cfg, BooleanSupplier visible) {
        if (registered) {
            return true;
        }
        try {
            HudMod hud = HudMod.INSTANCE;
            if (hud == null || hud.getMinimap() == null) {
                return false;
            }
            MinimapElementOverMapRendererHandler over =
                    hud.getMinimap().getOverMapRendererHandler();
            if (over == null) {
                return false;
            }
            over.add(new XaeroMinimapBorders(cfg, visible, over));
            registered = true;
            OttoExtra.LOGGER.info("[map] Minimap-Grenzen registriert (Xaero Element-Pipeline).");
            return true;
        } catch (Throwable t) {
            OttoExtra.LOGGER.warn("[map] Minimap-Registrierung fehlgeschlagen: {}", t.toString());
            registered = true;
            return false;
        }
    }

    @Override
    public boolean shouldRender(MinimapElementRenderLocation location) {
        return location == MinimapElementRenderLocation.OVER_MINIMAP;
    }

    @Override
    public void preRender(MinimapElementRenderInfo renderInfo,
                          VertexConsumerProvider.Immediate vanillaBufferSource,
                          MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers) {
    }

    @Override
    public void postRender(MinimapElementRenderInfo renderInfo,
                           VertexConsumerProvider.Immediate vanillaBufferSource,
                           MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers) {
    }

    @Override
    public boolean renderElement(Marker element, boolean highlighted, boolean outOfBounds,
                                 double optionalDepth, float optionalScale,
                                 double partialX, double partialY,
                                 MinimapElementRenderInfo info, MinimapElementGraphics graphics,
                                 VertexConsumerProvider.Immediate vanillaBufferSource) {
        if (!LehenPolygonStore.isLoaded()) {
            LehenPolygonStore.ensureLoaded();
            return false;
        }
        if (!resolveHandlerFields()) {
            return false;
        }
        try {
            double ps = fPs.getDouble(handler);
            double pc = fPc.getDouble(handler);
            double zoom = fZoom.getDouble(handler);
            int halfView = fHalfViewW.getInt(handler);
            boolean circle = fCircle.getBoolean(handler);
            lastCircle = circle;
            lastCircleStampMs = System.currentTimeMillis();
            if (!(zoom > 0) || halfView <= 0) {
                return false;
            }
            double camX = info.renderPos.x;
            double camZ = info.renderPos.z;

            double worldHalf = halfView / zoom + 64;
            double qMinX = camX - worldHalf;
            double qMaxX = camX + worldHalf;
            double qMinZ = camZ - worldHalf;
            double qMaxZ = camZ + worldHalf;

            MinecraftClient client = MinecraftClient.getInstance();
            double playerX = client.player != null ? client.player.getX() : Double.NaN;
            double playerZ = client.player != null ? client.player.getZ() : Double.NaN;
            String insideKey = de.ottoextra.map.MapOverlayRenderer.insidePolygonKey(playerX, playerZ);

            MatrixStack pose = graphics.pose();
            int width = Math.max(1, cfg.borderWidthPx);

            if (cfg.minimapPainted && cfg.paintedMap) {
                xaero.hud.minimap.module.MinimapSession session =
                        xaero.hud.minimap.BuiltInHudModules.MINIMAP.getCurrentSession();
                xaero.common.minimap.write.MinimapWriter writer =
                        session != null ? session.getProcessor().getMinimapWriter() : null;
                xaero.common.minimap.region.MinimapChunk[][] blocks =
                        writer != null ? writer.getLoadedBlocks() : null;
                if (blocks != null) {
                    final int originX = writer.getLoadedMapChunkX();
                    final int originZ = writer.getLoadedMapChunkZ();
                    de.ottoextra.map.PaintedMapRenderer.renderMinimap(
                            new org.joml.Matrix4f(pose.peek().getPositionMatrix()),
                            camX, camZ, ps, pc, zoom, halfView, circle,
                            (ccx, ccz) -> minimapHasTile(blocks, originX, originZ, ccx, ccz));
                }
            }

            if (cfg.minimapPolitical && cfg.politicalFill) {
                for (de.ottoextra.map.LehenPolygon poly : LehenPolygonStore.polygons()) {
                    if (!poly.intersects(qMinX, qMinZ, qMaxX, qMaxZ)) {
                        continue;
                    }
                    fillPolygon(graphics, poly, camX, camZ, ps, pc, zoom, halfView, circle,
                            de.ottoextra.map.PoliticalOverlay.fillTintFor(poly.key()));
                }
            }

            for (de.ottoextra.map.BorderSegment seg : LehenPolygonStore.segments()) {
                if (!seg.intersects(qMinX, qMinZ, qMaxX, qMaxZ)) {
                    continue;
                }
                boolean highlight = insideKey != null && seg.ownerKeys().contains(insideKey);
                int color = highlight ? COL_BORDER_INSIDE : COL_BORDER;

                double ox1 = seg.x1() - camX;
                double oy1 = seg.z1() - camZ;
                double ox2 = seg.x2() - camX;
                double oy2 = seg.z2() - camZ;
                double x1 = (ps * ox1 - pc * oy1) * zoom;
                double y1 = (pc * ox1 + ps * oy1) * zoom;
                double x2 = (ps * ox2 - pc * oy2) * zoom;
                double y2 = (pc * ox2 + ps * oy2) * zoom;
                drawClippedSegment(graphics, pose, x1, y1, x2, y2, halfView, circle, color, width, cfg);
            }
        } catch (Throwable t) {
            OttoExtra.LOGGER.debug("[map] Minimap-Grenzen Renderfehler: {}", t.toString());
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 50;
    }

    private boolean resolveHandlerFields() {
        if (fieldsResolved) {
            return true;
        }
        if (fieldsFailed) {
            return false;
        }
        try {
            Class<?> base = handler.getClass();
            fPs = findField(base, "ps");
            fPc = findField(base, "pc");
            fZoom = findField(base, "zoom");
            fHalfViewW = findField(base, "halfViewW");
            fCircle = findField(base, "circle");
            fieldsResolved = true;
            return true;
        } catch (Throwable t) {
            fieldsFailed = true;
            OttoExtra.LOGGER.warn("[map] Minimap-Handler-Felder nicht lesbar: {} — Minimap-Grenzen aus.",
                    t.toString());
            return false;
        }
    }

    private static Field findField(Class<?> start, String name) throws NoSuchFieldException {
        Class<?> c = start;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void drawClippedSegment(MinimapElementGraphics graphics, MatrixStack pose,
                                           double x1, double y1, double x2, double y2,
                                           int half, boolean circle, int color, int widthPx,
                                           OttoExtraConfig.Map cfg) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double t0 = 0;
        double t1 = 1;
        if (circle) {

            double r = half;
            double a = dx * dx + dy * dy;
            double b = 2 * (x1 * dx + y1 * dy);
            double cQ = x1 * x1 + y1 * y1 - r * r;
            if (a < 1e-9) {
                if (cQ > 0) {
                    return;
                }
            } else {
                double disc = b * b - 4 * a * cQ;
                if (disc < 0) {
                    return;
                }
                double sq = Math.sqrt(disc);
                double tEnter = (-b - sq) / (2 * a);
                double tExit = (-b + sq) / (2 * a);
                t0 = Math.max(0, tEnter);
                t1 = Math.min(1, tExit);
                if (t0 >= t1) {
                    return;
                }
            }
        } else {
            double[] p = {-dx, dx, -dy, dy};
            double[] q = {x1 + half, half - x1, y1 + half, half - y1};
            for (int k = 0; k < 4; k++) {
                if (p[k] == 0) {
                    if (q[k] < 0) {
                        return;
                    }
                } else {
                    double r = q[k] / p[k];
                    if (p[k] < 0) {
                        if (r > t1) {
                            return;
                        }
                        if (r > t0) {
                            t0 = r;
                        }
                    } else {
                        if (r < t0) {
                            return;
                        }
                        if (r < t1) {
                            t1 = r;
                        }
                    }
                }
            }
        }
        float cx1 = (float) (x1 + t0 * dx);
        float cy1 = (float) (y1 + t0 * dy);
        float cx2 = (float) (x1 + t1 * dx);
        float cy2 = (float) (y1 + t1 * dy);
        float sdx = cx2 - cx1;
        float sdy = cy2 - cy1;
        float len = (float) Math.sqrt(sdx * sdx + sdy * sdy);
        if (len < 0.5f) {
            return;
        }
        pose.push();
        pose.translate(cx1, cy1, 0);
        pose.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(sdy, sdx)));
        int h = widthPx / 2;
        if (cfg.dashedBorders && cfg.dashGapPx > 0) {
            int dash = Math.max(2, cfg.dashLengthPx);
            int step = dash + cfg.dashGapPx;
            for (float s = 0; s < len; s += step) {
                int end = Math.round(Math.min(s + dash, len));
                graphics.fill(Math.round(s), -h, end, -h + widthPx, color);
            }
        } else {
            graphics.fill(0, -h, Math.round(len) + 1, -h + widthPx, color);
        }
        pose.pop();
    }

    private static void fillPolygon(MinimapElementGraphics graphics,
                                    de.ottoextra.map.LehenPolygon poly,
                                    double camX, double camZ, double ps, double pc,
                                    double zoom, int half, boolean circle, int color) {
        int n = poly.pointCount();
        double[] mx = new double[n];
        double[] my = new double[n];
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double ox = poly.xs()[i] - camX;
            double oy = poly.zs()[i] - camZ;
            mx[i] = (ps * ox - pc * oy) * zoom;
            my[i] = (pc * ox + ps * oy) * zoom;
            minY = Math.min(minY, my[i]);
            maxY = Math.max(maxY, my[i]);
        }
        int yStart = (int) Math.max(Math.floor(minY), -half);
        int yEnd = (int) Math.min(Math.ceil(maxY), half - 1);
        double[] xs = new double[n];
        for (int y = yStart; y <= yEnd; y++) {
            double yf = y + 0.5;
            int count = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double y1 = my[i];
                double y2 = my[j];
                if ((y1 <= yf && y2 > yf) || (y2 <= yf && y1 > yf)) {
                    xs[count++] = mx[i] + (yf - y1) / (y2 - y1) * (mx[j] - mx[i]);
                }
            }
            if (count < 2) {
                continue;
            }
            java.util.Arrays.sort(xs, 0, count);
            double rowMin = -half;
            double rowMax = half;
            if (circle) {
                double w = half * (double) half - yf * yf;
                if (w <= 0) {
                    continue;
                }
                double cw = Math.sqrt(w);
                rowMin = -cw;
                rowMax = cw;
            }
            for (int k = 0; k + 1 < count; k += 2) {
                int x1 = (int) Math.max(Math.floor(xs[k]), rowMin);
                int x2 = (int) Math.min(Math.ceil(xs[k + 1]), rowMax);
                if (x1 < x2) {
                    graphics.fill(x1, y, x2, y + 1, color);
                }
            }
        }
    }

    private static final class BorderReader extends MinimapElementReader<Marker, Void> {
        @Override
        public boolean isHidden(Marker e, Void ctx) {
            return false;
        }

        @Override
        public double getRenderX(Marker e, Void ctx, float partial) {
            MinecraftClient mc = MinecraftClient.getInstance();
            return mc.getCameraEntity() != null ? mc.getCameraEntity().getX() : 0;
        }

        @Override
        public double getRenderY(Marker e, Void ctx, float partial) {
            return 0;
        }

        @Override
        public double getRenderZ(Marker e, Void ctx, float partial) {
            MinecraftClient mc = MinecraftClient.getInstance();
            return mc.getCameraEntity() != null ? mc.getCameraEntity().getZ() : 0;
        }

        @Override
        public int getInteractionBoxLeft(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getInteractionBoxRight(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getInteractionBoxTop(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getInteractionBoxBottom(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getRenderBoxLeft(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getRenderBoxRight(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getRenderBoxTop(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getRenderBoxBottom(Marker e, Void ctx, float p) {
            return 0;
        }

        @Override
        public int getLeftSideLength(Marker e, MinecraftClient mc) {
            return 0;
        }

        @Override
        public boolean shouldScaleBoxWithOptionalScale() {
            return false;
        }

        @Override
        public String getMenuName(Marker e) {
            return "OttoExtra";
        }

        @Override
        public String getFilterName(Marker e) {
            return "OttoExtra";
        }

        @Override
        public int getMenuTextFillLeftPadding(Marker e) {
            return 0;
        }

        @Override
        public int getRightClickTitleBackgroundColor(Marker e) {
            return 0;
        }
    }

    private static final class BorderProvider extends MinimapElementRenderProvider<Marker, Void> {
        private final OttoExtraConfig.Map cfg;
        private final BooleanSupplier visible;
        private boolean served;

        private BorderProvider(OttoExtraConfig.Map cfg, BooleanSupplier visible) {
            this.cfg = cfg;
            this.visible = visible;
        }

        @Override
        public void begin(MinimapElementRenderLocation location, Void ctx) {
            served = !(cfg.minimapBorders && visible.getAsBoolean());
        }

        @Override
        public boolean hasNext(MinimapElementRenderLocation location, Void ctx) {
            return !served;
        }

        @Override
        public Marker getNext(MinimapElementRenderLocation location, Void ctx) {
            served = true;
            return Marker.INSTANCE;
        }

        @Override
        public void end(MinimapElementRenderLocation location, Void ctx) {
        }
    }
}
