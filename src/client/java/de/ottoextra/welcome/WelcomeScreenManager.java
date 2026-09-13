package de.ottoextra.welcome;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

import java.util.concurrent.atomic.AtomicBoolean;

/** Presents each release series once; consent decisions have their own version. */
public final class WelcomeScreenManager {

    public static final int CONSENT_VERSION = de.ottoextra.privacy.ConsentPolicy.VERSION;

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final AtomicBoolean ACCEPTED = new AtomicBoolean();
    private static final AtomicBoolean RELEASE_SEEN = new AtomicBoolean();
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
        RELEASE_SEEN.set(WelcomeState.wasSeen(installedVersion()));
        ClientTickEvents.END_CLIENT_TICK.register(WelcomeScreenManager::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client == null || (ACCEPTED.get() && RELEASE_SEEN.get()) || SHOWN_THIS_SESSION.get()) {
            return;
        }
        if (!(client.currentScreen instanceof TitleScreen) || client.getOverlay() != null) {
            return;
        }
        if (!SHOWN_THIS_SESSION.compareAndSet(false, true)) {
            return;
        }

        client.setScreen(new ReleaseWelcomeScreen(client.currentScreen));
    }

    static void skipRelease() {
        if (config == null) return;
        // Do not call ConsentPolicy.decide: skipping preserves flags, scopes and decision times.
        if (config.consent.onboardingVersion < CONSENT_VERSION) {
            config.consent.onboardingVersion = CONSENT_VERSION;
            config.save();
        }
        ACCEPTED.set(true);
        markReleaseSeen();
    }

    private static void markReleaseSeen() {
        WelcomeState.markSeen(installedVersion());
        RELEASE_SEEN.set(true);
    }

    static void accept(boolean resourcePacks, boolean externalImages, boolean updateChecks,
                       boolean communityRpNameUploads, boolean onlineServices,
                       java.util.Map<de.ottoextra.privacy.ConsentPolicy.Purpose, String> shownScopes,
                       RoleplayChoices choices) {
        if (config == null) {
            return;
        }
        for (var purpose : de.ottoextra.privacy.ConsentPolicy.Purpose.values()) {
            boolean allow = switch (purpose) {
                case RESOURCE_PACKS -> resourcePacks;
                case EXTERNAL_IMAGES -> externalImages;
                case UPDATE_CHECKS -> updateChecks;
                case COMMUNITY_UPLOADS -> communityRpNameUploads;
                case ONLINE_SERVICES -> onlineServices;
            };
            de.ottoextra.privacy.ConsentPolicy.decide(config, purpose, allow, shownScopes.get(purpose));
        }
        if (choices != null) {
            choices.apply(config);
        }
        config.consent.onboardingVersion = CONSENT_VERSION;
        config.save();

        String version = installedVersion();
        ACCEPTED.set(true);
        markReleaseSeen();
        DebugLog.debug(
                "OttoExtra-Einwilligungsassistent abgeschlossen (Version {}, Resourcepacks {}, Bilder {}, Update-Pruefung {}, RP-Namen-Upload {}).",
                version, resourcePacks, externalImages, updateChecks, communityRpNameUploads);
    }

    /**
     * Andere einmalige Hauptmenue-Hinweise warten, bis der Willkommensbildschirm
     * bestaetigt wurde. So erscheint der Update-Dialog niemals davor.
     */
    public static boolean blocksOtherPrompts() {
        return !ACCEPTED.get() || !RELEASE_SEEN.get();
    }

    public static String installedVersion() {
        return FabricLoader.getInstance()
                .getModContainer(OttoExtra.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }
}
