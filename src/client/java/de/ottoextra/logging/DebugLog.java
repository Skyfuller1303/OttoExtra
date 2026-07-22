package de.ottoextra.logging;

import de.ottoextra.OttoExtra;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DebugLog {

    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    private DebugLog() {
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static void setEnabled(boolean enabled) {
        ENABLED.set(enabled);
    }

    public static boolean isLogging() {
        return isEnabled() && OttoExtra.LOGGER.isDebugEnabled();
    }

    public static void debug(String message, Object... arguments) {
        if (isLogging()) {
            OttoExtra.LOGGER.debug(message, arguments);
        }
    }

    public static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("debug")
                                .executes(context -> sendStatus(context.getSource()))
                                .then(ClientCommandManager.literal("status")
                                        .executes(context -> sendStatus(context.getSource())))
                                .then(ClientCommandManager.literal("on")
                                        .executes(context -> {
                                            setEnabled(true);
                                            context.getSource().sendFeedback(Text.literal(
                                                    "§a[OttoExtra] Debug-Logs aktiviert. §7Ausgabe: logs/debug.log. "
                                                            + "Diagnosen können Chat- und Identitätsdaten enthalten."));
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("off")
                                        .executes(context -> {
                                            setEnabled(false);
                                            context.getSource().sendFeedback(Text.literal(
                                                    "§a[OttoExtra] §7Debug-Logs deaktiviert."));
                                            return 1;
                                        })))));
    }

    private static int sendStatus(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("§a[OttoExtra] §7Debug-Logs: "
                + (isEnabled() ? "§aAN" : "§cAUS")));
        return 1;
    }
}
