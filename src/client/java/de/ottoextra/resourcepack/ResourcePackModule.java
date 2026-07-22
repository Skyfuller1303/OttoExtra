package de.ottoextra.resourcepack;

import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

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
        DebugLog.debug("[resourcepack] initialisiert — pruefe auf Update.");
        this.updater = new ResourcePackUpdater(context.config());

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
