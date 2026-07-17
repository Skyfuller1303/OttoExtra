package de.ottoextra;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.config.OttoExtraConfig;
import java.util.List;
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
