package de.ottoextra.map;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import java.util.HashMap;
import java.util.Map;
public final class ActivityRenderer {
    private static final int SEGMENTS = 48;
    private ActivityRenderer() {
    }
    public static void render(XaeroMapBridge.View view, float overallAlpha,
                              float perLehenAlpha, float groupAlpha) {
        if (overallAlpha <= 0.02f) {
            return;
        }
        RegionDataService data = RegionsServices.data();
        if (data == null) {
            return;
        }
        long t = System.currentTimeMillis();
        float pulse = (float) (Math.sin(t / 900.0 * Math.PI) * 0.5 + 0.5);
        float ringPulse = (float) (Math.sin(t / 900.0 * Math.PI + 0.9424778) * 0.5 + 0.5);
        float perA = perLehenAlpha * overallAlpha;
        float grpA = groupAlpha * overallAlpha;
        BufferBuilder buf = null;
        if (perA > 0.02f) {
            for (LehenPolygon poly : LehenPolygonStore.polygons()) {
                if (!poly.labelOwner()) {
                    continue;
                }
                int count = data.gatheringCount(poly.key());
                if (count <= 0) {
                    continue;
                }
                float sx = view.screenX(poly.centroidX());
                float sy = view.screenY(poly.centroidZ());
                if (offscreen(sx, sy, view)) {
                    continue;
                }
                buf = ensureBuffer(buf);
                emitActivity(buf, sx, sy, count, pulse, ringPulse, view.effScale(), perA);
            }
        }
        if (grpA > 0.02f) {
            var labels = PoliticalOverlay.groupLabels();
            Map<String, Integer> countByGroup = new HashMap<>();
            for (LehenPolygon poly : LehenPolygonStore.polygons()) {
                if (!poly.labelOwner()) {
                    continue;
                }
                int count = data.gatheringCount(poly.key());
                if (count <= 0) {
                    continue;
                }
                String group = PoliticalOverlay.groupDisplayName(poly.key());
                if (group == null) {
                    float sx = view.screenX(poly.centroidX());
                    float sy = view.screenY(poly.centroidZ());
                    if (offscreen(sx, sy, view)) {
                        continue;
                    }
                    buf = ensureBuffer(buf);
                    emitActivity(buf, sx, sy, count, pulse, ringPulse, view.effScale(), grpA);
                } else {
                    countByGroup.merge(group, count, Integer::sum);
                }
            }
            for (PoliticalOverlay.GroupLabel gl : labels) {
                Integer count = countByGroup.get(gl.displayName());
                if (count == null || count <= 0) {
                    continue;
                }
                float sx = view.screenX(gl.centerX());
                float sy = view.screenY(gl.centerZ());
                if (offscreen(sx, sy, view)) {
                    continue;
                }
                buf = ensureBuffer(buf);
                emitActivity(buf, sx, sy, count, pulse, ringPulse, view.effScale(), grpA);
            }
        }
        if (buf == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        BufferBuilder finalBuf = buf;
        PaintedMapRenderer.withGuiOrtho(client, () ->
                PaintedMapRenderer.drawImmediate(client, finalBuf, RenderPipelines.GUI, null, null));
    }
    private static boolean offscreen(float sx, float sy, XaeroMapBridge.View view) {
        return sx < -32 || sy < -32 || sx > view.width() + 32 || sy > view.height() + 32;
    }
    private static BufferBuilder ensureBuffer(BufferBuilder buf) {
        if (buf != null) {
            return buf;
        }
        return Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, RenderPipelines.GUI.getVertexFormat());
    }
    private static void emitActivity(BufferBuilder buf, float sx, float sy, int count,
                                     float pulse, float ringPulse, double effScale, float alpha) {
        float intensity = (float) (1.0 - Math.pow(0.7, count));
        float baseR = (float) Math.max(6.0, Math.min(16.0, 6 + count * 2 + effScale * 12.0));
        float glowR = baseR * (0.78f + 0.22f * pulse);
        float centerA = intensity * (0.5f + 0.45f * pulse) * alpha;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (i * 2.0 * Math.PI / SEGMENTS);
            float a1 = (float) ((i + 1) * 2.0 * Math.PI / SEGMENTS);
            buf.vertex(sx, sy, 0).color(1f, 1f, 1f, centerA);
            buf.vertex(sx + glowR * (float) Math.cos(a0), sy + glowR * (float) Math.sin(a0), 0)
                    .color(1f, 1f, 1f, 0f);
            buf.vertex(sx + glowR * (float) Math.cos(a1), sy + glowR * (float) Math.sin(a1), 0)
                    .color(1f, 1f, 1f, 0f);
            buf.vertex(sx, sy, 0).color(1f, 1f, 1f, centerA);
        }
        float ringR = baseR * 1.4f * (0.72f + 0.28f * ringPulse);
        float ringThick = Math.max(1.5f, 2.5f * intensity);
        float ringA = intensity * (0.6f + 0.35f * ringPulse) * alpha;
        float outerR = ringR + ringThick;
        float innerR = ringR - ringThick;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (i * 2.0 * Math.PI / SEGMENTS);
            float a1 = (float) ((i + 1) * 2.0 * Math.PI / SEGMENTS);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            buf.vertex(sx + outerR * c0, sy + outerR * s0, 0).color(1f, 1f, 1f, ringA);
            buf.vertex(sx + innerR * c0, sy + innerR * s0, 0).color(1f, 1f, 1f, ringA);
            buf.vertex(sx + innerR * c1, sy + innerR * s1, 0).color(1f, 1f, 1f, ringA);
            buf.vertex(sx + outerR * c1, sy + outerR * s1, 0).color(1f, 1f, 1f, ringA);
        }
    }
}
