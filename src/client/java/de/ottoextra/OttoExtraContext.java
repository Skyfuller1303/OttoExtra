package de.ottoextra;

import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.config.OttoExtraConfig;

import java.util.List;

/**
 * Geteilter Laufzeit-Kontext, der allen Modulen gereicht wird.
 *
 * <p>Bündelt die langlebigen Singletons (Config, API-Client) und den aktuellen
 * Server-Status. Module greifen hierüber auf gemeinsame Dienste zu, statt eigene
 * Clients zu erzeugen (eine API-Schicht, kein Duplikat).</p>
 */
public final class OttoExtraContext {

    private final OttoExtraConfig config;
    private final OttoExtraApiClient api;

    private List<OttoExtraModule> activeModules = List.of();
    private volatile boolean onOttonien = false;

    public OttoExtraContext(OttoExtraConfig config, OttoExtraApiClient api) {
        this.config = config;
        this.api = api;
    }

    public OttoExtraConfig config() {
        return config;
    }

    public OttoExtraApiClient api() {
        return api;
    }

    /** Registriert die Modulliste und merkt sich nur die laut Config aktiven. */
    void setModules(List<OttoExtraModule> all) {
        this.activeModules = all.stream().filter(m -> m.enabled(config)).toList();
    }

    public List<OttoExtraModule> activeModules() {
        return activeModules;
    }

    public boolean isOnOttonien() {
        return onOttonien;
    }

    void setOnOttonien(boolean value) {
        this.onOttonien = value;
    }
}
