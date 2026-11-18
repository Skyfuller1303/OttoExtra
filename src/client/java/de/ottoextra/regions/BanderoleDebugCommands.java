package de.ottoextra.regions;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.regions.zonebanner.BanderoleTuning;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Debug commands to tweak the "Banderole" zone banner layout live:
 * <pre>
 * /ottoextra banderole                 list all values
 * /ottoextra banderole set KEY VALUE   change one value and show the banner held on screen
 * /ottoextra banderole reset [KEY]     restore defaults
 * /ottoextra banderole show            hold the banner on screen
 * /ottoextra banderole replay          play the full animation once
 * /ottoextra banderole hide            remove the banner
 * /ottoextra banderole export          print changed values as Java defaults (chat + log)
 * </pre>
 * Changes are in memory only.
 */
final class BanderoleDebugCommands {

    private static final String PREFIX = "§6[Banderole] §7";

    private BanderoleDebugCommands() {
    }

    static void register(Supplier<OttoExtraConfig.Regions> config) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("banderole")
                                .executes(c -> list(c.getSource()))
                                .then(ClientCommandManager.literal("set")
                                        .then(ClientCommandManager.argument("key", StringArgumentType.word())
                                                .suggests((c, b) -> CommandSource.suggestMatching(
                                                        BanderoleTuning.keys(), b))
                                                .executes(c -> describe(c.getSource(),
                                                        StringArgumentType.getString(c, "key")))
                                                .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg())
                                                        .executes(c -> set(c.getSource(), config,
                                                                StringArgumentType.getString(c, "key"),
                                                                FloatArgumentType.getFloat(c, "value"))))))
                                .then(ClientCommandManager.literal("reset")
                                        .executes(c -> {
                                            BanderoleTuning.resetAll();
                                            c.getSource().sendFeedback(Text.literal(PREFIX + "Alle Werte zurückgesetzt."));
                                            hold(config);
                                            return 1;
                                        })
                                        .then(ClientCommandManager.argument("key", StringArgumentType.word())
                                                .suggests((c, b) -> CommandSource.suggestMatching(
                                                        BanderoleTuning.keys(), b))
                                                .executes(c -> reset(c.getSource(), config,
                                                        StringArgumentType.getString(c, "key")))))
                                .then(ClientCommandManager.literal("show")
                                        .executes(c -> {
                                            hold(config);
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("replay")
                                        .executes(c -> {
                                            useBanderole(config);
                                            String[] target = target();
                                            RegionNotificationOverlay.show(target[0], target[1]);
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("hide")
                                        .executes(c -> {
                                            RegionNotificationOverlay.clear();
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("export")
                                        .executes(c -> export(c.getSource()))))));
    }

    private static int list(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal(PREFIX + "Werte (§eGelb§7 = geändert):"));
        for (BanderoleTuning t : BanderoleTuning.values()) {
            source.sendFeedback(Text.literal("§7  " + t.key() + " = "
                    + (t.isDefault() ? "§f" : "§e") + t.formatted() + " §8– " + t.description()));
        }
        return 1;
    }

    private static int describe(FabricClientCommandSource source, String key) {
        BanderoleTuning t = BanderoleTuning.byKey(key);
        if (t == null) return unknown(source, key);
        source.sendFeedback(Text.literal(PREFIX + t.key() + " = §f" + t.formatted()
                + " §8(" + fmt(t, t.min()) + " bis " + fmt(t, t.max()) + ") §7" + t.description()));
        return 1;
    }

    private static int set(FabricClientCommandSource source, Supplier<OttoExtraConfig.Regions> config,
                           String key, float value) {
        BanderoleTuning t = BanderoleTuning.byKey(key);
        if (t == null) return unknown(source, key);
        t.set(value);
        source.sendFeedback(Text.literal(PREFIX + t.key() + " = §e" + t.formatted()));
        hold(config);
        return 1;
    }

    private static int reset(FabricClientCommandSource source, Supplier<OttoExtraConfig.Regions> config,
                             String key) {
        BanderoleTuning t = BanderoleTuning.byKey(key);
        if (t == null) return unknown(source, key);
        t.reset();
        source.sendFeedback(Text.literal(PREFIX + t.key() + " zurückgesetzt auf §f" + t.formatted()));
        hold(config);
        return 1;
    }

    private static int export(FabricClientCommandSource source) {
        StringBuilder log = new StringBuilder("[banderole] Tuning-Export:");
        int changed = 0;
        for (BanderoleTuning t : BanderoleTuning.values()) {
            if (t.isDefault()) continue;
            changed++;
            String line = t.name() + " = " + t.formatted();
            log.append("\n  ").append(line);
            source.sendFeedback(Text.literal("§e  " + line));
        }
        if (changed == 0) {
            source.sendFeedback(Text.literal(PREFIX + "Keine Abweichungen von den Standardwerten."));
        } else {
            OttoExtra.LOGGER.info(log.toString());
            source.sendFeedback(Text.literal(PREFIX + changed + " geänderte Werte, auch im Log."));
        }
        return 1;
    }

    private static int unknown(FabricClientCommandSource source, String key) {
        source.sendError(Text.literal("Unbekannter Wert: " + key + ". Erlaubt: "
                + String.join(", ", BanderoleTuning.keys())));
        return 0;
    }

    /** Switches to the Banderole style (in memory only) and holds the banner fully shown. */
    private static void hold(Supplier<OttoExtraConfig.Regions> config) {
        useBanderole(config);
        String[] target = target();
        RegionNotificationOverlay.holdPreview(target[0], target[1]);
    }

    private static void useBanderole(Supplier<OttoExtraConfig.Regions> config) {
        OttoExtraConfig.Regions cfg = config.get();
        if (cfg != null) cfg.zoneBannerStyle = "banderole";
    }

    /** Last entered region and its hierarchy line, or a sample. */
    private static String[] target() {
        RegionState state = RegionMessageService.current();
        if (state == null || state.regionName() == null || state.regionName().isBlank()) {
            return new String[]{"Niederpölt", ""};
        }
        return new String[]{state.regionName(), state.hierarchyLine() == null ? "" : state.hierarchyLine()};
    }

    private static String fmt(BanderoleTuning t, float v) {
        return t.isFloat() ? String.format(Locale.ROOT, "%.2f", v) : Integer.toString(Math.round(v));
    }
}
