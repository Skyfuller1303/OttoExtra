package de.ottoextra.tweaks.lowhealth;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
public final class LowHealthState {
    private float previousHealth = -1.0f;
    private float smoothedIntensity = 0.0f;
    private long lastDamageAtMs = 0L;
    private float forcedIntensity = -1.0f;
    public void setForced(float value) {
        forcedIntensity = LowHealthMath.clamp(value, 0.0f, 1.0f);
    }
    public void clearForced() {
        forcedIntensity = -1.0f;
    }
    public boolean isForced() {
        return forcedIntensity >= 0.0f;
    }
    public void update(MinecraftClient client, OttoExtraConfig.Tweaks.LowHealth cfg) {
        float target;
        if (forcedIntensity >= 0.0f) {
            target = forcedIntensity;
        } else if (client.player == null || client.world == null || !cfg.enabled) {
            previousHealth = -1.0f;
            smoothedIntensity += (0.0f - smoothedIntensity) * 0.2f;
            if (smoothedIntensity < 0.001f) {
                smoothedIntensity = 0.0f;
            }
            return;
        } else {
            float health = client.player.getHealth();
            if (previousHealth >= 0.0f && health < previousHealth - 0.01f) {
                lastDamageAtMs = System.currentTimeMillis();
            }
            previousHealth = health;
            float start = Math.max(1.0f, cfg.startHealth);
            float raw = LowHealthMath.clamp((start - health) / start, 0.0f, 1.0f);
            float smooth = LowHealthMath.smoothstep(raw);
            if (cfg.calmAfterNoDamage) {
                long quietForMs = System.currentTimeMillis() - lastDamageAtMs;
                if (quietForMs > cfg.calmAfterNoDamageSeconds * 1000L && health > cfg.calmMinHealth) {
                    smooth *= cfg.calmVisualMultiplier;
                }
            }
            target = LowHealthMath.clamp(smooth * cfg.intensityScale, 0.0f, 1.0f);
        }
        float fade = LowHealthMath.clamp(cfg.fadeSpeed, 0.01f, 1.0f);
        smoothedIntensity += (target - smoothedIntensity) * fade;
    }
    public float intensity() {
        return smoothedIntensity;
    }
}
