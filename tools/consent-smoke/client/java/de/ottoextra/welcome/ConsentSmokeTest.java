package de.ottoextra.welcome;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.settings.OttoExtraSettingsScreen;
import de.ottoextra.privacy.ConsentPolicy;
import de.ottoextra.privacy.ConsentDetailsScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Isolated UI test. Never joins a server, authenticates, or grants an active network feature. */
public final class ConsentSmokeTest implements FabricClientGameTest {
    private ClientGameTestContext ctx;

    @Override public void runTest(ClientGameTestContext context) {
        ctx = context;
        ctx.runOnClient(client -> {
            checkAllDenied();
            String texture = java.util.Base64.getEncoder().encodeToString(
                    "{\"textures\":{\"SKIN\":{\"url\":\"https://unapproved.invalid/texture/test\"}}}"
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var properties = new com.mojang.authlib.properties.PropertyMap(com.google.common.collect.ImmutableMultimap.of(
                    "textures", new com.mojang.authlib.properties.Property("textures", texture)));
            var profile = new com.mojang.authlib.GameProfile(
                    java.util.UUID.fromString("bdfbf069-97ea-4e78-8762-764a4a2c19b7"), "ConsentFixture", properties);
            de.ottoextra.chat.SkinCache.remember(profile);
            check(de.ottoextra.chat.ChatHeads.skinForUuid(profile.id(), profile.name()) != null,
                    "Declined image permission must retain a local/default head");
            client.setScreen(new WelcomeScreen(new TitleScreen()));
        });
        ctx.takeScreenshot("consent-overview-de");
        click("ottoextra.consent.individual");
        ctx.takeScreenshot("consent-pack-de");
        for (int page = 1; page <= 5; page++) {
            if (page == 4) {
                ctx.runOnClient(client -> check(button("ottoextra.consent.allow").active,
                        "RP upload must offer an explicit user decision"));
                ctx.takeScreenshot("consent-rp-upload-onboarding-de");
            }
            if (page == 5) ctx.takeScreenshot("consent-online-de");
            click("ottoextra.consent.deny");
        }
        click("ottoextra.setup.rp.skip");
        ctx.runOnClient(client -> {
            checkAllDenied();
            OttoExtraConfig config = OttoExtraConfig.active();
            check(config.consent.onboardingVersion == ConsentPolicy.VERSION, "Onboarding decision not persisted");
            // Disable startup checks so a test-only pack grant cannot initiate networking.
            config.resourcepack.checkOnStartup = false;
            client.setScreen(new OttoExtraSettingsScreen(client.currentScreen));
            ConsentDetailsScreen.change(config, ConsentPolicy.Purpose.RESOURCE_PACKS, true);
            check(!ConsentPolicy.allowed(config, ConsentPolicy.Purpose.RESOURCE_PACKS), "Opening details granted consent");
        });
        ctx.takeScreenshot("consent-settings-details-de");
        click("ottoextra.consent.allow");
        ctx.runOnClient(client -> {
            OttoExtraConfig config = OttoExtraConfig.active();
            check(ConsentPolicy.allowed(config, ConsentPolicy.Purpose.RESOURCE_PACKS), "Explicit grant failed");
            ConsentDetailsScreen.change(config, ConsentPolicy.Purpose.RESOURCE_PACKS, false);
            client.currentScreen.close();
            checkAllDenied();
        });
        checkRpUploadDecision();
        checkP1Screens();
        checkBulkDecision(false, false);
        checkBulkDecision(true, false);
        checkBulkDecision(true, true);
        ctx.runOnClient(client -> {
            for (var purpose : ConsentPolicy.Purpose.values()) {
                String text = ConsentDetailsScreen.information(OttoExtraConfig.active(), purpose).getString();
                check(!text.contains("ottoextra.consent."), "Missing translation: " + purpose);
            }
            client.setScreen(new TitleScreen());
        });
    }

    private void checkBulkDecision(boolean allow, boolean changeSource) {
        var result = new java.util.concurrent.atomic.AtomicReference<java.util.Map<ConsentPolicy.Purpose, Boolean>>();
        var destination = new java.util.concurrent.atomic.AtomicReference<net.minecraft.client.gui.screen.Screen>();
        String originalApi = ctx.computeOnClient(client -> OttoExtraConfig.active().api.baseUrl);
        ctx.runOnClient(client -> {
            var config = OttoExtraConfig.active();
            checkAllDenied();
            config.rpnames.tablistEnabled = false;
            config.resourcepack.checkOnStartup = false;
            var parent = new net.minecraft.client.gui.screen.Screen(Text.literal("Bulk decision capture")) {
                @Override protected void init() {
                    var captured = new java.util.EnumMap<ConsentPolicy.Purpose, Boolean>(ConsentPolicy.Purpose.class);
                    for (var purpose : ConsentPolicy.Purpose.values()) captured.put(purpose, ConsentPolicy.allowed(config, purpose));
                    result.set(captured);
                    check(config.consent.onboardingVersion == ConsentPolicy.VERSION, "Bulk choice did not finish onboarding");
                    check(!config.rpnames.tablistEnabled, "Bulk choice overwrote roleplay settings");
                    // Capture and withdraw synchronously before the next tick can start an update check.
                    for (var purpose : ConsentPolicy.Purpose.values()) {
                        ConsentPolicy.decide(config, purpose, false, ConsentPolicy.scope(config, purpose));
                    }
                    config.save();
                }
            };
            destination.set(parent);
            client.setScreen(new WelcomeScreen(parent));
            String overview = ConsentDetailsScreen.overview(config).getString();
            check(!overview.contains("ottoextra.consent."), "Overview contains untranslated keys");
            for (var purpose : ConsentPolicy.Purpose.values()) {
                String heading = Text.translatable("ottoextra.consent.page." + ConsentDetailsScreen.page(purpose) + ".heading").getString();
                check(overview.contains(heading), "Overview omits purpose: " + purpose);
            }
            if (changeSource) config.api.baseUrl = "https://changed-after-display.invalid";
        });
        click(allow ? "ottoextra.consent.all.allow" : "ottoextra.consent.all.deny");
        ctx.runOnClient(client -> {
            check(client.currentScreen == destination.get(), "Bulk choice opened more setup pages");
            check(result.get() != null, "Bulk choice never completed");
            for (var purpose : ConsentPolicy.Purpose.values()) {
                boolean expected = allow && (!changeSource || purpose == ConsentPolicy.Purpose.RESOURCE_PACKS
                        || purpose == ConsentPolicy.Purpose.UPDATE_CHECKS);
                check(result.get().get(purpose) == expected, "Unexpected bulk grant: " + purpose);
            }
            OttoExtraConfig.active().api.baseUrl = originalApi;
            OttoExtraConfig.active().save();
            checkAllDenied();
            client.setScreen(new TitleScreen());
        });
    }

    private void checkRpUploadDecision() {
        ctx.runOnClient(client -> {
            var config = OttoExtraConfig.active();
            client.setScreen(new OttoExtraSettingsScreen(client.currentScreen));
            ConsentDetailsScreen.change(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS, true);
            check(!ConsentPolicy.allowed(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS), "Details must not grant uploads");
            check(button("ottoextra.consent.allow").active, "Upload grant button must be available");
            String text = ConsentDetailsScreen.information(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS).getString();
            check(text.contains("https://api.ottoextra.dev") && text.contains("sechs Kalendermonate")
                    && text.contains("private Vorschläge"), "Upload notice missing recipient, retention or publication model");
        });
        ctx.takeScreenshot("consent-rp-upload-settings-de");
        click("ottoextra.consent.deny");
        ctx.runOnClient(client -> {
            var config = OttoExtraConfig.active();
            checkAllDenied();
            ConsentDetailsScreen.change(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS, true);
        });
        click("ottoextra.consent.allow");
        ctx.runOnClient(client -> {
            var config = OttoExtraConfig.active();
            check(ConsentPolicy.allowed(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS), "Manual upload grant failed");
            check(!ConsentPolicy.allowed(config, ConsentPolicy.Purpose.ONLINE_SERVICES), "Upload choice silently enabled online services");
            var permit = ConsentPolicy.capture(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS);
            ConsentDetailsScreen.change(config, ConsentPolicy.Purpose.COMMUNITY_UPLOADS, false);
            check(!permit.valid(), "Manual withdrawal left old upload jobs authorized");
            checkAllDenied();
            client.currentScreen.close();
        });
    }

    private void checkP1Screens() {
        ctx.runOnClient(client -> {
            var config = OttoExtraConfig.active();
            check(!de.ottoextra.privacy.LocalPrivacy.chatAllowed(config, true), "Chat log default must be off");
            de.ottoextra.privacy.LocalChatConsentScreen.change(config, true);
        });
        ctx.takeScreenshot("p1-chat-consent-de");
        click("ottoextra.consent.allow");
        ctx.runOnClient(client -> {
            var config = OttoExtraConfig.active();
            check(de.ottoextra.privacy.LocalPrivacy.chatAllowed(config, true), "Chat consent missing");
            check(!de.ottoextra.privacy.LocalPrivacy.chatAllowed(config, false), "Scope must exclude other servers");
            de.ottoextra.privacy.LocalChatConsentScreen.change(config, false);
            check(!de.ottoextra.privacy.LocalPrivacy.chatAllowed(config, true), "Chat withdrawal failed");
        });
        var effects = new java.util.concurrent.atomic.AtomicInteger();
        ctx.runOnClient(client -> client.setScreen(new de.ottoextra.tweaks.vote.VoteConsentScreen(
                "ConsentFixture", java.util.List.of(java.net.URI.create("https://example.invalid/vote")), effects::incrementAndGet)));
        ctx.takeScreenshot("p1-vote-consent-de");
        ctx.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
        ctx.waitTick();
        check(effects.get() == 0, "ENTER must not open websites or change clipboard");
        click("gui.cancel");
        check(effects.get() == 0, "Cancel must have no external effect");
        ctx.runOnClient(client -> client.setScreen(new de.ottoextra.tweaks.vote.VoteConsentScreen(
                "ConsentFixture", java.util.List.of(java.net.URI.create("https://example.invalid/vote")), effects::incrementAndGet)));
        click("ottoextra.privacy.vote.open");
        check(effects.get() == 1, "Only explicit confirmation runs the action");
        // The fixture callback only increments a counter: it never opens a real website or clipboard.
        ctx.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.Screen(Text.literal("P1 low-health HUD fixture")) {
            private final de.ottoextra.tweaks.lowhealth.LowHealthState state = new de.ottoextra.tweaks.lowhealth.LowHealthState();
            private final OttoExtraConfig.Tweaks.LowHealth cfg = new OttoExtraConfig.Tweaks.LowHealth();
            { cfg.enabled = true; cfg.reducedMotion = false; cfg.vignetteMinAlpha = 1; cfg.vignetteMaxAlpha = 1;
              state.setForced(1); for (int i = 0; i < 60; i++) state.update(null, cfg); }
            @Override public void render(net.minecraft.client.gui.DrawContext context, int x, int y, float delta) {
                context.fill(0, 0, width, height, 0xFF303030);
                de.ottoextra.tweaks.lowhealth.LowHealthHudOverlay.render(context, state, cfg);
                context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2, 0xFFFFFFFF);
            }
        }));
        ctx.takeScreenshot("p1-lowhealth-max-hud");
        ctx.runOnClient(client -> client.setScreen(new TitleScreen()));
    }

    private void checkAllDenied() {
        for (var purpose : ConsentPolicy.Purpose.values()) {
            check(!ConsentPolicy.allowed(OttoExtraConfig.active(), purpose), "Unexpected grant: " + purpose);
        }
    }

    private ButtonWidget button(String key) {
        return net.minecraft.client.MinecraftClient.getInstance().currentScreen.children().stream()
                .filter(child -> child instanceof ButtonWidget).map(child -> (ButtonWidget) child)
                .filter(value -> value.getMessage().getString().equals(Text.translatable(key).getString()))
                .findFirst().orElseThrow();
    }

    private void click(String key) {
        double[] position = ctx.computeOnClient(client -> {
            ButtonWidget button = button(key);
            check(button.active && button.visible, "Unavailable button: " + key);
            check(button.getX() >= 0 && button.getY() >= 0
                    && button.getX() + button.getWidth() <= client.currentScreen.width
                    && button.getY() + button.getHeight() <= client.currentScreen.height, "Clipped button: " + key);
            var window = client.getWindow();
            return new double[]{(button.getX() + button.getWidth() / 2.0) * window.getWidth() / window.getScaledWidth(),
                    (button.getY() + button.getHeight() / 2.0) * window.getHeight() / window.getScaledHeight()};
        });
        ctx.getInput().setCursorPos(position[0], position[1]);
        ctx.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        ctx.waitTick();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
