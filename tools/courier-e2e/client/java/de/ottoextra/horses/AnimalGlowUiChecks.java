package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalInfoEvents;
import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.settings.OttoExtraSettingsScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exercises the actual glow mixin/render state, persisted toggle and registered key binding. */
public final class AnimalGlowUiChecks {
    private static final int FIRST_ID = -124_000;
    private AnimalGlowUiChecks() { }

    public static void run(ClientGameTestContext ctx) {
        String snapshot = ctx.computeOnClient(c -> OttoExtraConfig.active().snapshotJson());
        int scale = ctx.computeOnClient(c -> c.options.getGuiScale().getValue());
        float yaw = ctx.computeOnClient(c -> c.player.getYaw());
        float pitch = ctx.computeOnClient(c -> c.player.getPitch());
        var key = ctx.computeOnClient(c -> Arrays.stream(c.options.allKeys)
                .filter(k -> k.getId().equals("key.ottoextra.animal_gender_glow")).findFirst().orElseThrow());
        require(key.getDefaultKey().getCode() == GLFW.GLFW_KEY_F8, "Glow default key must be F8");
        String oldKey = ctx.computeOnClient(c -> key.getBoundKeyTranslationKey());
        AtomicBoolean gate = new AtomicBoolean(true);
        try {
            ctx.runOnClient(c -> {
                var api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                        new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> {
                            throw new AssertionError("Glow requested API data: " + method.getName());
                        });
                HorseRegistry.initialize(api, gate::get, true);
                HorseFinder.initialize(null, gate::get);
                AnimalGenderGlow.initialize(gate::get);
                c.setScreen(null);
                var config = OttoExtraConfig.active().animalInfo;
                config.nametagShowGender = config.nametagGenderColor = config.nametagIncludeUnregistered = false;
                var female = new CowEntity(EntityType.COW, c.world);
                var male = new DonkeyEntity(EntityType.DONKEY, c.world);
                var finder = new HorseEntity(EntityType.HORSE, c.world);
                var unknown = new CowEntity(EntityType.COW, c.world);
                var animals = List.of(female, male, finder, unknown);
                for (int i = 0; i < animals.size(); i++) {
                    var animal = animals.get(i);
                    animal.setId(FIRST_ID - i);
                    animal.setPosition(c.player.getX() + 2.5 - i * 1.7, c.player.getY(), c.player.getZ() + 5);
                    animal.setNoGravity(true);
                    animal.setAiDisabled(true);
                }
                observe(female, "Weiblich"); observe(male, "Männlich"); observe(finder, "Stute");
                require(HorseRegistry.name(female.getUuid()).isEmpty(), "Glow registered an observed animal");
                config.genderGlowEnabled = false;
                require(!female.isGlowing() && outline(c, female) == EntityRenderState.NO_OUTLINE, "Default glow is enabled");
                serverGlowFlag(unknown, true);
                int vanilla = outline(c, unknown);
                require(vanilla != EntityRenderState.NO_OUTLINE, "Vanilla glow fixture missing");
                config.genderGlowEnabled = true;
                require(female.isGlowing() && outline(c, female) == 0xFFFF79B8, "Female outline missing/wrong");
                require(male.isGlowing() && outline(c, male) == 0xFF68B8FF, "Male donkey outline missing/wrong");
                require(!female.isGlowingLocal() && !male.isGlowingLocal(), "Glow mutated local entity flags");
                require(!AnimalGenderGlow.shouldGlow(unknown) && outline(c, unknown) == vanilla, "Unknown animal lost vanilla glow");
                require(!AnimalGenderGlow.shouldGlow(c.player), "Player received animal glow");
                gate.set(false);
                require(!female.isGlowing() && outline(c, female) == EntityRenderState.NO_OUTLINE, "Glow crossed server gate");
                gate.set(true);
                double x = female.getX();
                female.setPosition(c.player.getX() + 64, c.player.getY(), c.player.getZ());
                require(!female.isGlowing() && outline(c, female) == EntityRenderState.NO_OUTLINE, "Glow exceeded distance limit");
                female.setPosition(x, c.player.getY(), c.player.getZ() + 5);
                female.setHealth(0);
                require(!AnimalGenderGlow.shouldGlow(female), "Dead animal highlighted");
                female.setHealth(10);
                HorseFinder.select(finder.getUuid()).join();
                require(outline(c, finder) == 0xFFFFC857, "Gender color replaced the golden finder");
                HorseFinder.remove(finder.getUuid()).join();
                require(outline(c, finder) == 0xFFFF79B8, "Removing finder lost gender outline");
                HorseFinder.select(finder.getUuid()).join();
                config.genderGlowEnabled = false;
                require(!female.isGlowing() && outline(c, female) == EntityRenderState.NO_OUTLINE, "Disabling glow left a stale outline");
                require(outline(c, finder) == 0xFFFFC857 && outline(c, unknown) == vanilla, "Toggle disabled unrelated outlines");
                serverGlowFlag(unknown, false);
                config.genderGlowEnabled = true;
                require(!unknown.isGlowing() && outline(c, unknown) == EntityRenderState.NO_OUTLINE, "Unknown gender was invented");
                observe(female, "Männlich");
                require(outline(c, female) == 0xFF68B8FF, "Fresh toast data did not update glow");
                HorseRegistry.clearConnection();
                require(!female.isGlowing() && outline(c, female) == EntityRenderState.NO_OUTLINE, "Glow reused disconnected observations");
                observe(female, "Weiblich"); observe(male, "Männlich"); observe(finder, "Stute");
                for (var animal : animals) c.world.addEntity(animal);
                c.player.setYaw(0); c.player.setPitch(5);
                c.options.getGuiScale().setValue(3);
                c.getToastManager().clear();
                key.setBoundKey(key.getDefaultKey());
                KeyBinding.updateKeysByCode();
            });
            ctx.getInput().resizeWindow(1920, 1080); ctx.waitTicks(5);
            ctx.takeScreenshot("animal-glow-colors");
            press(ctx);
            ctx.runOnClient(c -> {
                require(!OttoExtraConfig.active().animalInfo.genderGlowEnabled, "Registered key did not disable glow");
                require(outline(c, c.world.getEntityById(FIRST_ID)) == EntityRenderState.NO_OUTLINE, "Key left outline active");
                require(HorseFinder.targets().size() == 1, "Key changed finder targets");
            });
            press(ctx);
            ctx.runOnClient(c -> {
                require(OttoExtraConfig.active().animalInfo.genderGlowEnabled, "Registered key did not enable glow");
                c.setScreen(new OttoExtraSettingsScreen(null));
            });
            press(ctx);
            ctx.runOnClient(c -> {
                require(OttoExtraConfig.active().animalInfo.genderGlowEnabled, "Key toggled while menu was open");
                var screen = (OttoExtraSettingsScreen) c.currentScreen;
                try {
                    var field = OttoExtraSettingsScreen.class.getDeclaredField("searchField");
                    field.setAccessible(true);
                    ((TextFieldWidget) field.get(screen)).setText("animalInfo.genderGlowEnabled");
                } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
                var toggle = screen.children().stream().filter(e -> e instanceof ButtonWidget)
                        .map(e -> (ButtonWidget) e).filter(b -> b.visible && b.getMessage().getString()
                                .equals(Text.translatable("ottoextra.settings.on").getString())).findFirst().orElseThrow();
                toggle.onPress(new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0));
                require(!OttoExtraConfig.active().animalInfo.genderGlowEnabled, "Menu did not disable key-enabled glow");
                toggle.onPress(new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0));
                require(OttoExtraConfig.active().animalInfo.genderGlowEnabled, "Menu did not enable glow");
                require(!OttoExtraConfig.active().animalInfo.nametagGenderColor, "Glow changed nametag settings");
            });
            ctx.waitTick(); ctx.takeScreenshot("animal-glow-settings");
            ctx.runOnClient(c -> c.setScreen(null));
            gate.set(false);
            press(ctx);
            ctx.runOnClient(c -> require(OttoExtraConfig.active().animalInfo.genderGlowEnabled, "Key toggled outside Ottonien"));
        } finally {
            ctx.runOnClient(c -> {
                c.setScreen(null);
                for (int i = 0; i < 4; i++) c.world.removeEntity(FIRST_ID - i, Entity.RemovalReason.DISCARDED);
                HorseRegistry.shutdown();
                HorseFinder.shutdown();
                AnimalGenderGlow.initialize(() -> false);
                OttoExtraConfig.active().restoreFrom(snapshot);
                OttoExtraConfig.active().save();
                key.setBoundKey(InputUtil.fromTranslationKey(oldKey));
                KeyBinding.updateKeysByCode();
                c.player.setYaw(yaw); c.player.setPitch(pitch);
                c.options.getGuiScale().setValue(scale);
            });
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
        }
    }

    private static int outline(MinecraftClient c, Entity animal) {
        return c.getEntityRenderDispatcher().getRenderer(animal).getAndUpdateRenderState(animal, 0).outlineColor;
    }

    private static void observe(AnimalEntity animal, String gender) {
        AnimalInfoEvents.OBSERVED.invoker().onObserved(new AnimalInfoObservation(animal.getUuid(),
                new AnimalInfoSnapshot(animal.getType().getName().getString(), gender, "Ja"), Instant.now()));
    }

    private static void serverGlowFlag(Entity entity, boolean value) {
        // setGlowing() is a server-side setter; on clients isGlowing() reads tracked flags.
        try {
            var flag = Entity.class.getDeclaredField("GLOWING_FLAG_INDEX");
            flag.setAccessible(true);
            var setFlag = Entity.class.getDeclaredMethod("setFlag", int.class, boolean.class);
            setFlag.setAccessible(true);
            setFlag.invoke(entity, flag.getInt(null), value);
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }

    private static void press(ClientGameTestContext ctx) {
        ctx.runOnClient(c -> KeyBinding.onKeyPressed(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F8)));
        ctx.waitTicks(2);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
