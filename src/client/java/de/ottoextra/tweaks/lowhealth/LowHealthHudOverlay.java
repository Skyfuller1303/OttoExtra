package de.ottoextra.tweaks.lowhealth;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Roter Adrenalin-Rand (Blut-Vignette) als HUD-Overlay. Prozedural gezeichnet
 * (kein Texture-Asset nötig): an jeder Bildschirmkante ein nach innen
 * ausblendender roter Verlauf; die Ecken überlagern sich zu einem Vignette-Look.
 * Leichtes Pulsieren simuliert den Adrenalinpuls.
 */
public final class LowHealthHudOverlay {

    /** Dunkelrot (ohne Alpha; Alpha kommt aus der Intensität). */
    private static final int RED = 0x7A0000;

    private LowHealthHudOverlay() {
    }

    public static void render(DrawContext ctx, LowHealthState state,
                              OttoExtraConfig.Tweaks.LowHealth cfg) {
        if (!cfg.vignetteEnabled) {
            return;
        }
        float intensity = state.intensity();
        if (intensity <= 0.01f) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();

        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        float pulse = 0.92f + (float) Math.sin(time * (2.0f + intensity * 5.0f)) * 0.08f;
        float alpha = (cfg.vignetteMinAlpha + cfg.vignetteMaxAlpha * intensity) * pulse;
        if (cfg.reduceWhenScreenOpen && client.currentScreen != null) {
            alpha *= cfg.screenOpenMultiplier;
        }
        alpha = LowHealthMath.clamp(alpha, 0.0f, 1.0f);
        if (alpha <= 0.003f) {
            return;
        }

        // Randbreite wächst leicht mit der Intensität (Tunnelblick-Gefühl).
        int edgeX = Math.round(w * (0.20f + 0.18f * intensity));
        int edgeY = Math.round(h * (0.24f + 0.20f * intensity));

        for (int y = 0; y < edgeY; y++) {
            int a = alphaAt(alpha, y, edgeY);
            if (a <= 0) {
                continue;
            }
            int color = (a << 24) | RED;
            ctx.fill(0, y, w, y + 1, color);            // oben
            ctx.fill(0, h - 1 - y, w, h - y, color);    // unten
        }
        for (int x = 0; x < edgeX; x++) {
            int a = alphaAt(alpha, x, edgeX);
            if (a <= 0) {
                continue;
            }
            int color = (a << 24) | RED;
            ctx.fill(x, 0, x + 1, h, color);            // links
            ctx.fill(w - 1 - x, 0, w - x, h, color);    // rechts
        }
    }

    /** Alpha-Byte für Abstand {@code i} von der Kante (0 = Kante, voll). */
    private static int alphaAt(float baseAlpha, int i, int edge) {
        float f = 1.0f - (float) i / edge;
        return (int) (baseAlpha * f * f * 255.0f); // quadratisch -> weicherer Verlauf
    }
}
