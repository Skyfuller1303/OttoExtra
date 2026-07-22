package de.ottoextra.tweaks;

import com.mojang.brigadier.arguments.FloatArgumentType;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.tweaks.lowhealth.LowHealthHudOverlay;
import de.ottoextra.tweaks.lowhealth.LowHealthMath;
import de.ottoextra.tweaks.lowhealth.LowHealthSoundController;
import de.ottoextra.tweaks.lowhealth.LowHealthState;
import de.ottoextra.tweaks.toolprotect.ToolProtectHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;

public final class TweaksModule implements OttoExtraModule {

    private final LowHealthState lowHealth = new LowHealthState();
    private final LowHealthSoundController heartbeat = new LowHealthSoundController();

    private static LowHealthState activeState;
    private static OttoExtraConfig.Tweaks.LowHealth activeConfig;

    @Override
    public String id() {
        return "tweaks";
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        OttoExtraConfig config = context.config();
        activeState = lowHealth;
        activeConfig = config.tweaks.lowHealth;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            lowHealth.update(client, config.tweaks.lowHealth);
            heartbeat.tick(client, lowHealth, config.tweaks.lowHealth);
        });

        HudRenderCallback.EVENT.register((ctx, tick) ->
                LowHealthHudOverlay.render(ctx, lowHealth, config.tweaks.lowHealth));

        ToolProtectHandler.register(config);

        registerCommands(config);

        DebugLog.debug("[tweaks] initialisiert (Low-Health-Effekt + Werkzeugschutz + Test-Commands).");
    }

    private void registerCommands(OttoExtraConfig config) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("tweaks")
                                .then(ClientCommandManager.literal("lowhealth")
                                        .then(ClientCommandManager.literal("test")
                                                .executes(c -> {
                                                    forceTest(0.6f);
                                                    return 1;
                                                })
                                                .then(ClientCommandManager.argument("intensity",
                                                                FloatArgumentType.floatArg(0.0f, 1.0f))
                                                        .executes(c -> {
                                                            forceTest(FloatArgumentType.getFloat(c, "intensity"));
                                                            return 1;
                                                        })))
                                        .then(ClientCommandManager.literal("stop")
                                                .executes(c -> {
                                                    lowHealth.clearForced();
                                                    msg("§a[Tweaks]§7 Low-Health-Test gestoppt.");
                                                    return 1;
                                                }))
                                        .then(ClientCommandManager.literal("on")
                                                .executes(c -> {
                                                    config.tweaks.lowHealth.enabled = true;
                                                    config.save();
                                                    msg("§a[Tweaks]§7 Low-Health-Effekt §aAN§7.");
                                                    return 1;
                                                }))
                                        .then(ClientCommandManager.literal("off")
                                                .executes(c -> {
                                                    config.tweaks.lowHealth.enabled = false;
                                                    lowHealth.clearForced();
                                                    config.save();
                                                    msg("§a[Tweaks]§7 Low-Health-Effekt §cAUS§7.");
                                                    return 1;
                                                }))))));
    }

    public static int lowHealthBlurPasses() {
        LowHealthState state = activeState;
        OttoExtraConfig.Tweaks.LowHealth cfg = activeConfig;
        if (state == null || cfg == null || !cfg.blurEnabled || cfg.blurStrength <= 0.01f) {
            return 0;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return 0;
        }
        float t;
        if (state.isForced()) {
            t = state.intensity();
        } else {
            if (client.player == null) {
                return 0;
            }
            float startHealth = Math.max(2.0f, cfg.blurStartHearts * 2.0f);
            float health = client.player.getHealth();
            t = LowHealthMath.smoothstep(
                    LowHealthMath.clamp((startHealth - health) / startHealth, 0.0f, 1.0f));
        }
        if (t <= 0.02f) {
            return 0;
        }
        int passes = Math.round(t * cfg.blurStrength * 4.0f);
        return Math.max(0, Math.min(8, passes));
    }

    public static float lowHealthFovBoost() {
        LowHealthState state = activeState;
        OttoExtraConfig.Tweaks.LowHealth cfg = activeConfig;
        if (state == null || cfg == null || !cfg.fovEnabled) {
            return 0.0f;
        }
        return state.intensity() * cfg.fovMaxDegrees;
    }

    private void forceTest(float value) {
        lowHealth.setForced(value);
        msg("§a[Tweaks]§7 Low-Health-Test-Intensität: §e"
                + String.format(Locale.ROOT, "%.2f", value)
                + " §8(/ottoextra tweaks lowhealth stop)");
    }

    private static void msg(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(text), false);
        }
    }
}
