package de.ottoextra.welcome;

import de.ottoextra.OttoExtra;
import de.ottoextra.logging.DebugLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

import java.util.concurrent.atomic.AtomicBoolean;

/** Zeigt den Willkommensbildschirm genau einmal pro Installation. */
public final class WelcomeScreenManager {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final AtomicBoolean ACCEPTED = new AtomicBoolean();
    private static final AtomicBoolean SHOWN_THIS_SESSION = new AtomicBoolean();

    private WelcomeScreenManager() {
    }

    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        ACCEPTED.set(WelcomeState.wasAccepted());
        ClientTickEvents.END_CLIENT_TICK.register(WelcomeScreenManager::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client == null || ACCEPTED.get() || SHOWN_THIS_SESSION.get()) {
            return;
        }
        if (!(client.currentScreen instanceof TitleScreen)) {
            return;
        }
        if (!SHOWN_THIS_SESSION.compareAndSet(false, true)) {
            return;
        }

        client.setScreen(new WelcomeScreen(client.currentScreen));
    }

    static void accept() {
        String version = installedVersion();
        WelcomeState.markAccepted(version);
        ACCEPTED.set(true);
        DebugLog.debug("OttoExtra-Willkommensbildschirm bestaetigt (Version {}).", version);
    }

    /**
     * Andere einmalige Hauptmenue-Hinweise warten, bis der Willkommensbildschirm
     * bestaetigt wurde. So erscheint der Update-Dialog niemals davor.
     */
    public static boolean blocksOtherPrompts() {
        return !ACCEPTED.get();
    }

    public static String installedVersion() {
        return FabricLoader.getInstance()
                .getModContainer(OttoExtra.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }
}
