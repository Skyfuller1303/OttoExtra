package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraConfig;

import java.util.HashMap;
import java.util.Objects;

public final class ConsentPolicy {
    public static final int VERSION = 3;
    // Availability never grants consent: each client still needs its own current decision.
    public static final boolean COMMUNITY_UPLOADS_AVAILABLE = true;

    public enum Purpose { ONLINE_SERVICES, RESOURCE_PACKS, EXTERNAL_IMAGES, UPDATE_CHECKS, COMMUNITY_UPLOADS }

    private ConsentPolicy() { }

    public static String apiSource(OttoExtraConfig config) {
        return new de.ottoextra.api.OttoExtraApiRoutes(config.api.baseUrl).baseUrl();
    }

    public static String scope(OttoExtraConfig config, Purpose purpose) {
        return switch (purpose) {
            case ONLINE_SERVICES -> apiSource(config) + "|mcp-api.ottonien.com|mojang-session|ottoextra.dev-account-link";
            case RESOURCE_PACKS -> config.resourcepack.effectiveSource() + "|" + config.resourcepack.assetName
                    + "|" + config.resourcepack.maxSizeBytes + "|autoEnable=" + config.resourcepack.autoEnable;
            case EXTERNAL_IMAGES -> "textures.minecraft.net|" + apiSource(config);
            case UPDATE_CHECKS -> "api.github.com/repos/Skyfuller1303/OttoExtra/releases/latest";
            case COMMUNITY_UPLOADS -> apiSource(config) + "|authenticated-rp-proposals-v2";
        };
    }

    public static boolean allowed(OttoExtraConfig config, Purpose purpose) {
        if (config == null || config.consent == null) return false;
        var consent = config.consent;
        return (purpose != Purpose.COMMUNITY_UPLOADS || COMMUNITY_UPLOADS_AVAILABLE)
                && (purpose != Purpose.RESOURCE_PACKS || config.resourcepack.enabled)
                && consent.policyVersion == VERSION && consent.flag(purpose)
                && consent.acceptedScopes != null
                && Objects.equals(consent.acceptedScopes.get(purpose.name()), scope(config, purpose));
    }

    public static ConsentPermit capture(OttoExtraConfig config, Purpose purpose) {
        var consent = config.consent;
        String expected = scope(config, purpose);
        return consent.gate(purpose).capture(() -> config.consent == consent
                && allowed(config, purpose) && expected.equals(scope(config, purpose)));
    }

    public static ConsentPermit capture(Purpose purpose) {
        return capture(OttoExtraConfig.active(), purpose);
    }

    public static void decide(OttoExtraConfig config, Purpose purpose, boolean allow, String shownScope) {
        var consent = config.consent;
        Runnable cancel;
        synchronized (consent) {
            boolean granted = allow && Objects.equals(shownScope, scope(config, purpose))
                    && (purpose != Purpose.COMMUNITY_UPLOADS || COMMUNITY_UPLOADS_AVAILABLE);
            var scopes = new HashMap<String, String>();
            if (consent.acceptedScopes != null) scopes.putAll(consent.acceptedScopes);
            if (granted) scopes.put(purpose.name(), shownScope); else scopes.remove(purpose.name());
            var times = new HashMap<String, Long>();
            if (consent.decidedAt != null) times.putAll(consent.decidedAt);
            times.put(purpose.name(), System.currentTimeMillis());
            cancel = consent.gate(purpose).change(() -> {
                consent.acceptedScopes = java.util.Map.copyOf(scopes);
                consent.decidedAt = java.util.Map.copyOf(times);
                consent.policyVersion = VERSION;
                consent.flag(purpose, granted);
            });
        }
        cancel.run();
    }

    /** Reconcile imports, source changes and legacy consent before anything can start networking. */
    public static void reconcile(OttoExtraConfig config) {
        for (Purpose purpose : Purpose.values()) {
            if (config.consent.flag(purpose) && !allowed(config, purpose)) {
                decide(config, purpose, false, scope(config, purpose));
            }
        }
    }

    public static void invalidateAll(OttoExtraConfig config) {
        for (Purpose purpose : Purpose.values()) config.consent.gate(purpose).invalidate();
    }
}
