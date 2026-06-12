package de.ottoextra.map;

import com.mojang.blaze3d.vertex.VertexFormat;
import de.ottoextra.regions.RegionDataService;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

/**
 * Spieler-Aktivität auf der Worldmap (Portierung aus OttoPlus):
 * pulsierender Glow + phasenversetzter Ring am Lehen-Zentroid, wenn dort
 * Spieler versammelt sind ({@code player_gathering} aus der Regions-API,
 * Refresh alle 7 min im {@link RegionDataService}).
 *
 * <p>Formeln 1:1 aus Legacy: intensity = 1-0.7^count; Puls sin(t/900ms*PI);
 * Ring-Puls um 54 Grad versetzt — der Versatz erzeugt den "Wellen"-Effekt.
 * Geometrie als Quads (Fan/Strip-Äquivalent) über den immediate
 * GUI-Ortho-Pfad.</p>
 */
public final class ActivityRenderer {

    private static final int SEGMENTS = 48;

    private ActivityRenderer() {
    }

    public static void render(XaeroMapBridge.View view, float overallAlpha) {
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

        BufferBuilder buf = null;
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
            if (sx < -32 || sy < -32 || sx > view.width() + 32 || sy > view.height() + 32) {
                continue;
            }
            float intensity = (float) (1.0 - Math.pow(0.7, count));
            float baseR = (float) Math.max(6.0, Math.min(16.0,
                    6 + count * 2 + view.effScale() * 12.0));
            if (buf == null) {
                buf = Tessellator.getInstance().begin(
                        VertexFormat.DrawMode.QUADS, RenderPipelines.GUI.getVertexFormat());
            }
            // Glow: Fan als Quads (Mitte deckend, Rand transparent)
            float glowR = baseR * (0.78f + 0.22f * pulse);
            float centerA = intensity * (0.5f + 0.45f * pulse) * overallAlpha;
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
            // Ring: Strip als Quads
            float ringR = baseR * 1.4f * (0.72f + 0.28f * ringPulse);
            float ringThick = Math.max(1.5f, 2.5f * intensity);
            float ringA = intensity * (0.6f + 0.35f * ringPulse) * overallAlpha;
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
        if (buf == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        BufferBuilder finalBuf = buf;
        PaintedMapRenderer.withGuiOrtho(client, () ->
                PaintedMapRenderer.drawImmediate(client, finalBuf, RenderPipelines.GUI, null, null));
    }
}
