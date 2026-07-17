package de.ottoextra;
import de.ottoextra.config.OttoExtraConfig;
public interface OttoExtraModule {
    String id();
    default boolean enabled(OttoExtraConfig config) {
        return true;
    }
    void onInitializeClient(OttoExtraContext context);
    default void onServerJoin(OttoExtraContext context) {
    }
    default void onDisconnect(OttoExtraContext context) {
    }
    default void onClientStop(OttoExtraContext context) {
    }
}
