package de.ottoextra.horses;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.animalinfo.AnimalInfoEvents;
import de.ottoextra.horses.ui.BasicHorseOverviewScreen;
import de.ottoextra.horses.ui.BasicHorseRecordScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public final class HorseRegistryModule implements OttoExtraModule {

    private KeyBinding openRecordKey;
    private KeyBinding openOverviewKey;
    private KeyBinding genderGlowKey;

    @Override
    public String id() {
        return "horses";
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        boolean addonLoaded = FabricLoader.getInstance()
                .isModLoaded("ottoextra_animalbreeding");
        HorseRegistry.initialize(context.api(), context::isOnOttonien, !addonLoaded);
        HorseFinder.initialize(context.api(), context::isOnOttonien);
        AnimalGenderGlow.initialize(context::isOnOttonien);
        genderGlowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra.animal_gender_glow", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC));
        AnimalInfoEvents.OBSERVED.register(HorseRegistry::observe);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        if (addonLoaded) {
            OttoExtra.LOGGER.info("AnimalBreeding erkannt; Core-Finder aktiv, Core-Pferde-GUI und Nametag bleiben aus.");
            return;
        }
        openRecordKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra.horse_record", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B, KeyBinding.Category.MISC));
        openOverviewKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ottoextra.horse_overview", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, KeyBinding.Category.MISC));
    }

    private void tick(MinecraftClient client) {
        HorseFinder.tick(client);
        while (genderGlowKey != null && genderGlowKey.wasPressed()) {
            AnimalGenderGlow.toggle(client);
        }
        while (openRecordKey != null && openRecordKey.wasPressed()) {
            if (client.currentScreen == null) {
                openTargetRecord(client);
            }
        }
        while (openOverviewKey != null && openOverviewKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new BasicHorseOverviewScreen(null));
            }
        }
    }

    public static void openTargetRecord(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof AnimalEntity animal)
                || !animal.isAlive() || !RegistryAnimals.supports(animal)) {
            message(client, "ottoextra.horses.message.no_target");
            return;
        }
        var observation = HorseRegistry.observation(animal.getUuid());
        if (observation.isEmpty()) {
            message(client, "ottoextra.horses.message.no_observation");
            return;
        }
        client.setScreen(new BasicHorseRecordScreen(
                null, animal, observation.orElseThrow()));
    }

    private static void message(MinecraftClient client, String key) {
        if (client.player != null) {
            client.player.sendMessage(Text.translatable(key), true);
        }
    }

    @Override
    public void onServerJoin(OttoExtraContext context) {
        HorseFinder.pollNow();
        HorseRegistry.refresh().exceptionally(error -> {
            OttoExtra.LOGGER.debug("Tiernamen konnten nicht geladen werden: {}",
                    error.toString());
            return HorseRegistry.names();
        });
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        HorseRegistry.clearConnection();
        HorseFinder.reset();
    }

    @Override
    public void onClientStop(OttoExtraContext context) {
        HorseRegistry.shutdown();
        HorseFinder.shutdown();
    }
}
