package de.ottoextra.tweaks.lowhealth;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

/**
 * Spielt den Warden-Heartbeat lokal als wiederholten Einzel-Sound (kein Loop,
 * keine Überlagerung). Tempo, Lautstärke und Pitch skalieren mit der Intensität.
 */
public final class LowHealthSoundController {

    private long nextBeatAtMs = 0L;

    public void tick(MinecraftClient client, LowHealthState state,
                     OttoExtraConfig.Tweaks.LowHealth cfg) {
        if (!cfg.heartbeatEnabled || client.player == null) {
            nextBeatAtMs = 0L;
            return;
        }
        float intensity = state.intensity();
        if (intensity < cfg.heartbeatMinIntensity) {
            nextBeatAtMs = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextBeatAtMs) {
            return;
        }
        long interval = (long) LowHealthMath.lerp(
                cfg.heartbeatMaxIntervalMs, cfg.heartbeatMinIntervalMs, intensity);
        float volume = LowHealthMath.lerp(cfg.heartbeatMinVolume, cfg.heartbeatMaxVolume, intensity);
        float pitch = LowHealthMath.lerp(cfg.heartbeatMinPitch, cfg.heartbeatMaxPitch, intensity);

        // Lokal für den Client; Kategorie PLAYERS (persönliches Feedback).
        client.player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
        nextBeatAtMs = now + Math.max(120L, interval);
    }
}
