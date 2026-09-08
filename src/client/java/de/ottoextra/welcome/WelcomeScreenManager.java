package de.ottoextra.welcome;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

import java.util.concurrent.atomic.AtomicBoolean;

/** Zeigt den Willkommensbildschirm genau einmal pro Installation. */
public final class WelcomeScreenManager {

    public static final int CONSENT_VERSION = 2;

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final AtomicBoolean ACCEPTED = new AtomicBoolean();
    private static final AtomicBoolean SHOWN_THIS_SESSION = new AtomicBoolean();
    private static OttoExtraConfig config;

    private WelcomeScreenManager() {
    }

    public static void initialize(OttoExtraConfig loadedConfig) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        config = loadedConfig;
        ACCEPTED.set(loadedConfig.consent.onboardingVersion >= CONSENT_VERSION);
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

    static void accept(boolean resourcePacks, boolean externalImages, boolean updateChecks,
                       boolean communityRpNameUploads, RoleplayChoices choices) {
        if (config == null) {
            return;
        }
        config.consent.automaticResourcePackDownloads = resourcePacks;
        config.consent.externalImageDownloads = externalImages;
        config.consent.updateChecks = updateChecks;
        config.consent.setCommunityRpNameUploads(communityRpNameUploads);
        if (choices != null) {
            choices.apply(config);
        }
        config.consent.onboardingVersion = CONSENT_VERSION;
        config.save();

        String version = installedVersion();
        ACCEPTED.set(true);
        DebugLog.debug(
                "OttoExtra-Einwilligungsassistent abgeschlossen (Version {}, Resourcepacks {}, Bilder {}, Update-Pruefung {}, RP-Namen-Upload {}).",
                version, resourcePacks, externalImages, updateChecks, communityRpNameUploads);
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
