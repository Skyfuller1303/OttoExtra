package de.ottoextra.welcome;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.privacy.ConsentPolicy;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Actual client input/render checks. Isolated profile, no server or external test requests. */
public final class ReleaseWelcomeClientTest implements FabricClientGameTest {
    private ClientGameTestContext ctx;

    @Override public void runTest(ClientGameTestContext context) {
        ctx = context;
        ctx.runOnClient(client -> {
            System.out.println("RELEASE WELCOME initial screen: " + client.currentScreen.getClass().getSimpleName());
            // The isolated profile can still be on vanilla's accessibility/language onboarding.
            if (!(client.currentScreen instanceof ReleaseWelcomeScreen)) client.setScreen(new TitleScreen());
        });
        ctx.waitFor(client -> client.currentScreen instanceof ReleaseWelcomeScreen && client.getOverlay() == null, 1200);
        String initial = ctx.computeOnClient(client -> consentSnapshot());
        ctx.runOnClient(client -> {
            check(WelcomeScreenManager.blocksOtherPrompts(), "Updates must wait for intro");
            for (var purpose : ConsentPolicy.Purpose.values())
                check(!ConsentPolicy.allowed(OttoExtraConfig.active(), purpose), "Legacy grant survived reconciliation");
        });
        ctx.takeScreenshot("release-welcome-de");
        click("ottoextra.release.review");
        ctx.runOnClient(client -> check(client.currentScreen instanceof WelcomeScreen, "Review did not open wizard"));
        ctx.takeScreenshot("release-review-de");
        click("ottoextra.consent.back");
        ctx.runOnClient(client -> check(client.currentScreen instanceof ReleaseWelcomeScreen, "Back did not return to intro"));
        click("ottoextra.release.review");
        click("ottoextra.consent.individual");
        click("ottoextra.consent.allow");
        ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        ctx.waitTick();
        ctx.runOnClient(client -> {
            check(client.currentScreen instanceof ReleaseWelcomeScreen, "Escape did not discard unfinished wizard");
            check(initial.equals(consentSnapshot()), "Unfinished wizard changed consent");
        });
        click("ottoextra.release.skip");
        ctx.waitTicks(5);
        ctx.runOnClient(client -> {
            check(client.currentScreen instanceof TitleScreen, "Skip did not stay in main menu");
            check(initial.equals(consentSnapshot()), "Skip changed consent");
            check(!WelcomeScreenManager.blocksOtherPrompts(), "Skip kept other prompts blocked");
            check(WelcomeState.wasSeen(WelcomeScreenManager.installedVersion()), "Release marker not persisted");
            check(OttoExtraConfig.active().consent.onboardingVersion == ConsentPolicy.VERSION, "Skip did not finish onboarding");
        });

        // Preserve an existing grant and its permit/timestamps; revoke within the same tick.
        ctx.runOnClient(client -> {
            var config = OttoExtraConfig.active();
            var purpose = ConsentPolicy.Purpose.ONLINE_SERVICES;
            ConsentPolicy.decide(config, purpose, true, ConsentPolicy.scope(config, purpose));
            var permit = ConsentPolicy.capture(config, purpose);
            String before = consentSnapshot();
            client.setScreen(new ReleaseWelcomeScreen(new TitleScreen()));
            client.currentScreen.close();
            check(before.equals(consentSnapshot()) && permit.valid(), "Skipping invalidated an existing grant");
            ConsentPolicy.decide(config, purpose, false, ConsentPolicy.scope(config, purpose));
            config.save();
        });

        ctx.runOnClient(client -> {
            client.setScreen(new ReleaseWelcomeScreen(new TitleScreen()));
            client.getWindow().setWindowedSize(640, 480);
        });
        ctx.waitFor(client -> client.currentScreen.width == 320 && client.currentScreen.height == 240, 100);
        ctx.takeScreenshot("release-welcome-compact-de");
        click("ottoextra.release.review");
        ctx.takeScreenshot("release-review-compact-de");
        ctx.runOnClient(client -> client.currentScreen.children().stream()
                .filter(child -> child instanceof ButtonWidget).map(child -> (ButtonWidget) child)
                .forEach(button -> check(client.textRenderer.getWidth(button.getMessage()) <= button.getWidth() - 8,
                        "Compact wizard label clipped: " + button.getMessage().getString())));
        ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        ctx.waitTick();
        click("ottoextra.release.skip");
        ctx.runOnClient(client -> {
            client.getWindow().setWindowedSize(1280, 720);
            client.options.language = "en_us";
            client.getLanguageManager().setLanguage("en_us");
        });
        reload();
        ctx.runOnClient(client -> client.setScreen(new ReleaseWelcomeScreen(new TitleScreen())));
        ctx.takeScreenshot("release-welcome-en");
        ctx.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        ctx.waitTick();
        ctx.runOnClient(client -> {
            check(client.currentScreen instanceof TitleScreen, "Intro Escape must skip");
            client.options.language = "de_de";
            client.getLanguageManager().setLanguage("de_de");
        });
        reload();
        System.out.println("RELEASE WELCOME PASS: fresh skip, existing grants, back/escape, persistence, DE/EN, 320x240");
    }

    private void reload() {
        var reload = ctx.computeOnClient(client -> client.reloadResources());
        ctx.waitFor(client -> reload.isDone() && client.getOverlay() == null, 1800);
        reload.join();
    }

    private static String consentSnapshot() {
        var consent = OttoExtraConfig.active().consent;
        var flags = new java.util.EnumMap<ConsentPolicy.Purpose, Boolean>(ConsentPolicy.Purpose.class);
        for (var purpose : ConsentPolicy.Purpose.values()) flags.put(purpose, consent.flag(purpose));
        return flags + "|" + consent.policyVersion + "|" + new java.util.TreeMap<>(consent.acceptedScopes)
                + "|" + new java.util.TreeMap<>(consent.decidedAt);
    }

    private void click(String key) {
        double[] pos = ctx.computeOnClient(client -> {
            var button = client.currentScreen.children().stream().filter(child -> child instanceof ButtonWidget)
                    .map(child -> (ButtonWidget) child)
                    .filter(child -> child.getMessage().getString().equals(Text.translatable(key).getString()))
                    .findFirst().orElseThrow();
            check(button.active && button.visible, "Unavailable: " + key);
            check(button.getX() >= 0 && button.getY() >= 0 && button.getX() + button.getWidth() <= client.currentScreen.width
                    && button.getY() + button.getHeight() <= client.currentScreen.height, "Clipped: " + key);
            var window = client.getWindow();
            return new double[]{(button.getX() + button.getWidth() / 2.0) * window.getWidth() / window.getScaledWidth(),
                    (button.getY() + button.getHeight() / 2.0) * window.getHeight() / window.getScaledHeight()};
        });
        ctx.getInput().setCursorPos(pos[0], pos[1]);
        ctx.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        ctx.waitTick();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
