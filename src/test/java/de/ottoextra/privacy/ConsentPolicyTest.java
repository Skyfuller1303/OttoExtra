package de.ottoextra.privacy;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.ottoextra.privacy.ConsentPolicy.Purpose.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsentPolicyTest {
    @TempDir Path directory;

    private static void decide(OttoExtraConfig config, ConsentPolicy.Purpose purpose, boolean allow) {
        ConsentPolicy.decide(config, purpose, allow, ConsentPolicy.scope(config, purpose));
    }

    @Test void freshAndLegacyFlagsNeverAuthorizeNetwork() {
        OttoExtraConfig config = new OttoExtraConfig();
        for (var purpose : ConsentPolicy.Purpose.values()) {
            assertFalse(ConsentPolicy.allowed(config, purpose));
            config.consent.flag(purpose, true);
        }
        config.consent.onboardingVersion = 2;
        ConsentPolicy.reconcile(config);
        for (var purpose : ConsentPolicy.Purpose.values()) {
            assertFalse(ConsentPolicy.allowed(config, purpose));
            assertFalse(config.consent.flag(purpose));
        }
    }

    @Test void permissionsAreIndependentAndUploadsRequireTheirOwnDecision() {
        OttoExtraConfig config = new OttoExtraConfig();
        decide(config, EXTERNAL_IMAGES, true);
        assertTrue(ConsentPolicy.allowed(config, EXTERNAL_IMAGES));
        assertFalse(ConsentPolicy.allowed(config, ONLINE_SERVICES));
        assertFalse(ConsentPolicy.allowed(config, RESOURCE_PACKS));
        assertFalse(ConsentPolicy.allowed(config, COMMUNITY_UPLOADS));
        decide(config, COMMUNITY_UPLOADS, true);
        assertTrue(ConsentPolicy.allowed(config, COMMUNITY_UPLOADS));
        assertTrue(config.consent.communityRpNameUploads);
        assertFalse(ConsentPolicy.allowed(config, ONLINE_SERVICES));
        decide(config, COMMUNITY_UPLOADS, false);
        assertFalse(ConsentPolicy.allowed(config, COMMUNITY_UPLOADS));
        assertFalse(config.consent.communityRpNameUploads);
    }

    @Test void earlierUploadScopeCannotSilentlyEnableNewRelease() {
        OttoExtraConfig config = new OttoExtraConfig();
        decide(config, ONLINE_SERVICES, true);
        decide(config, COMMUNITY_UPLOADS, true);
        config.consent.acceptedScopes = java.util.Map.of(
                ONLINE_SERVICES.name(), ConsentPolicy.scope(config, ONLINE_SERVICES),
                COMMUNITY_UPLOADS.name(), ConsentPolicy.apiSource(config) + "|authenticated-rp-proposals-v1");
        ConsentPolicy.reconcile(config);
        assertFalse(ConsentPolicy.allowed(config, COMMUNITY_UPLOADS));
        assertFalse(config.consent.communityRpNameUploads);
        assertTrue(ConsentPolicy.allowed(config, ONLINE_SERVICES));
        decide(config, COMMUNITY_UPLOADS, true);
        assertTrue(ConsentPolicy.allowed(config, COMMUNITY_UPLOADS));
    }

    @Test void withdrawalCancelsFutureAndClosesStreamAndCannotBeUndoneForOldJobs() throws Exception {
        OttoExtraConfig config = new OttoExtraConfig();
        decide(config, EXTERNAL_IMAGES, true);
        ConsentPermit old = ConsentPolicy.capture(config, EXTERNAL_IMAGES);
        CompletableFuture<String> pending = old.start(CompletableFuture::new);
        AtomicBoolean closed = new AtomicBoolean();
        var body = old.guard(new ByteArrayInputStream(new byte[]{1}) {
            @Override public void close() { closed.set(true); }
        });
        decide(config, EXTERNAL_IMAGES, false);
        decide(config, EXTERNAL_IMAGES, true);
        assertTrue(pending.isCancelled());
        assertTrue(closed.get());
        assertFalse(old.valid());
        assertThrows(CancellationException.class, body::read);
        Path cache = directory.resolve("late.png");
        assertThrows(CancellationException.class, () -> old.commitIo(() -> Files.writeString(cache, "late")));
        assertFalse(Files.exists(cache));
        assertTrue(ConsentPolicy.capture(config, EXTERNAL_IMAGES).valid());
    }

    @Test void initiallyDeniedPermitNeverBecomesUsable() {
        ConsentPermit.Gate gate = new ConsentPermit.Gate();
        AtomicBoolean allowed = new AtomicBoolean(false);
        ConsentPermit denied = gate.capture(allowed::get);
        allowed.set(true);
        assertFalse(denied.valid());
        assertTrue(gate.capture(allowed::get).valid());
    }

    @Test void newProviderRequiresDecisionAndOldScopeCannotGrantIt() {
        OttoExtraConfig config = new OttoExtraConfig();
        decide(config, ONLINE_SERVICES, true);
        String shown = ConsentPolicy.scope(config, ONLINE_SERVICES);
        ConsentPermit old = ConsentPolicy.capture(config, ONLINE_SERVICES);
        config.api.baseUrl = "https://new.example.invalid";
        assertFalse(old.valid());
        assertFalse(ConsentPolicy.allowed(config, ONLINE_SERVICES));
        ConsentPolicy.decide(config, ONLINE_SERVICES, true, shown);
        assertFalse(ConsentPolicy.allowed(config, ONLINE_SERVICES));
        decide(config, ONLINE_SERVICES, true);
        assertTrue(ConsentPolicy.allowed(config, ONLINE_SERVICES));
        assertFalse(old.valid());
    }

    @Test void packActivationAndSizeChangesNeedFreshDecision() {
        OttoExtraConfig config = new OttoExtraConfig();
        decide(config, RESOURCE_PACKS, true);
        config.resourcepack.autoEnable = !config.resourcepack.autoEnable;
        assertFalse(ConsentPolicy.allowed(config, RESOURCE_PACKS));
        decide(config, RESOURCE_PACKS, true);
        config.resourcepack.maxSizeBytes++;
        assertFalse(ConsentPolicy.allowed(config, RESOURCE_PACKS));
    }

    @Test void missingApiValueUsesEffectiveDefaultWithoutCrashingMigration() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.api.baseUrl = null;
        assertTrue(ConsentPolicy.scope(config, ONLINE_SERVICES).startsWith("https://api.ottoextra.dev|"));
        ConsentPolicy.reconcile(config);
    }
}
