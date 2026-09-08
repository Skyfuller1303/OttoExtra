package de.ottoextra.rpnames.store;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.ui.MeetPersonScreen;
import de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Real UI callbacks against an isolated in-memory identity store, never the live API. */
public final class PeopleLockUiChecks {
    private PeopleLockUiChecks() { }

    public static void run(ClientGameTestContext ctx) {
        LocalRpIdentityStore temporary = new LocalRpIdentityStore(false);
        LocalRpIdentityStore previous = ctx.computeOnClient(c -> {
            RpNamesServices.ensureInitialized(OttoExtraConfig.active().rpnames);
            var original = RpNamesServices.store();
            setStore(temporary);
            c.setScreen(new MeetPersonScreen(null, "LockFixture", null, "Bekannter", "Knappe"));
            return original;
        });
        try {
            ctx.waitTick();
            ctx.runOnClient(c -> {
                press(c.currentScreen, "ottoextra.meet.yes");
                var profile = temporary.findByName("LockFixture").orElseThrow();
                require(!profile.locked && !profile.titleLocked, "Meeting a player locked their profile");
                require(temporary.updateTitleIfChanged("LockFixture", null, "Ritter"), "Saved player cannot receive new title");
                c.setScreen(new RpNamesPeopleBookScreen(null, "LockFixture"));
            });
            ctx.waitTick();
            ctx.runOnClient(c -> {
                Screen screen = c.currentScreen;
                PeopleLockUiChecks.<TextFieldWidget>field(screen, "rpNameField").setText("Persönlicher Name");
                PeopleLockUiChecks.<TextFieldWidget>field(screen, "titleField").setText("Freiherr");
                PeopleLockUiChecks.<TextFieldWidget[]>field(screen, "colorFields")[1].setText("#123456");
                require(!PeopleLockUiChecks.<CheckboxWidget>field(screen, "lockCheckbox").isChecked(), "Name/color edit enabled full lock");
                require(!PeopleLockUiChecks.<CheckboxWidget>field(screen, "titleLockCheckbox").isChecked(), "Title edit enabled title lock");
                press(screen, "ottoextra.rpbook.save");
                var profile = temporary.findByName("LockFixture").orElseThrow();
                require(!profile.locked && !profile.titleLocked, "Saving enabled a hidden lock");
                temporary.importApi("LockFixture", null, "API Name", "Graf", "Adlige");
                require(profile.title.equals("Graf") && profile.rpName.equals("Persönlicher Name"), "Unlocked title/name policy wrong");
                c.setScreen(new RpNamesPeopleBookScreen(null, "LockFixture"));
            });
            ctx.waitTick();
            ctx.takeScreenshot("people-saved-without-locks");
            ctx.runOnClient(c -> {
                // A checkbox alone must become dirty and persist when closing the book.
                PeopleLockUiChecks.<CheckboxWidget>field(c.currentScreen, "titleLockCheckbox").onPress(null);
                c.currentScreen.close();
                require(temporary.findByName("LockFixture").orElseThrow().titleLocked, "Title checkbox was not saved on close");
                require(!temporary.updateTitleIfChanged("LockFixture", null, "Herzog"), "Explicit title lock was ignored");
                c.setScreen(new RpNamesPeopleBookScreen(null, "LockFixture"));
            });
            ctx.waitTick();
            ctx.takeScreenshot("people-explicit-title-lock");
            ctx.runOnClient(c -> {
                PeopleLockUiChecks.<CheckboxWidget>field(c.currentScreen, "titleLockCheckbox").onPress(null);
                PeopleLockUiChecks.<CheckboxWidget>field(c.currentScreen, "lockCheckbox").onPress(null);
                c.currentScreen.close();
                require(temporary.findByName("LockFixture").orElseThrow().locked, "Explicit profile lock was not saved");
                // Saving through Meet must preserve an existing explicit lock.
                c.setScreen(new MeetPersonScreen(null, "LockFixture", null, "Persönlicher Name", "Graf"));
                press(c.currentScreen, "ottoextra.meet.yes");
                require(temporary.findByName("LockFixture").orElseThrow().locked, "Meeting again cleared an existing lock");
                c.setScreen(new RpNamesPeopleBookScreen(null, "LockFixture"));
                PeopleLockUiChecks.<CheckboxWidget>field(c.currentScreen, "lockCheckbox").onPress(null);
                c.currentScreen.close();
                require(!temporary.findByName("LockFixture").orElseThrow().locked, "Profile cannot be unlocked");
                require(temporary.updateTitleIfChanged("LockFixture", null, "Herzog"), "Cleared locks still blocked title updates");
                c.setScreen(new RpNamesPeopleBookScreen(null, "LockFixture"));
                PeopleLockUiChecks.<TextFieldWidget>field(c.currentScreen, "titleField").setText("");
                PeopleLockUiChecks.<CheckboxWidget>field(c.currentScreen, "titleLockCheckbox").onPress(null);
                c.currentScreen.close();
                var profile = temporary.findByName("LockFixture").orElseThrow();
                require(profile.titleLocked && profile.title.isEmpty(), "Explicit empty title lock was dropped");
            });
        } finally {
            ctx.runOnClient(c -> { c.setScreen(null); setStore(previous); temporary.shutdown(); });
        }
    }

    private static void press(Screen screen, String key) {
        screen.children().stream().filter(child -> child instanceof ButtonWidget).map(child -> (ButtonWidget) child)
                .filter(button -> button.getMessage().getString().equals(Text.translatable(key).getString()))
                .findFirst().orElseThrow().onPress(new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0));
    }

    private static void setStore(LocalRpIdentityStore store) {
        try {
            var field = RpNamesServices.class.getDeclaredField("store");
            field.setAccessible(true); field.set(null, store);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object instance, String name) {
        try {
            var field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true); return (T) field.get(instance);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
