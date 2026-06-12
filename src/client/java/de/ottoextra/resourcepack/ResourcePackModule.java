package de.ottoextra.resourcepack;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Modul: automatischer Server-Resourcepack-Downloader.
 *
 * <p>Läuft beim Client-Start (kein Server-Gate), damit der Ottonien-Look bereits
 * im Hauptmenü steht. Lädt via Manifest/URL den neuesten Pack und aktiviert ihn.
 * Kein Mixin nötig — nur öffentliche Vanilla-/Fabric-APIs.</p>
 */
public final class ResourcePackModule implements OttoExtraModule {

    private ResourcePackUpdater updater;

    @Override
    public String id() {
        return "resourcepack";
    }

    @Override
    public boolean enabled(OttoExtraConfig config) {
        return config.resourcepack.enabled;
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        OttoExtra.LOGGER.info("[resourcepack] initialisiert — pruefe auf Update.");
        this.updater = new ResourcePackUpdater(context.config());

        // Verzögerten Reload nur an sicheren Zeitpunkten ausführen (nicht im Startup),
        // um GLFW-/Render-Crashes in modded Umgebungen zu vermeiden.
        ClientTickEvents.END_CLIENT_TICK.register(PackInstaller::tick);

        this.updater.runAsync();
    }

    @Override
    public void onClientStop(OttoExtraContext context) {
        if (updater != null) {
            updater.close();
        }
    }
}
