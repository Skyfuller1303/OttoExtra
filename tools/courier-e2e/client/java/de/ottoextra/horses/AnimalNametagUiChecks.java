package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoEvents;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.AnimalBreedingHorseName;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.config.settings.OttoExtraSettingsScreen;
import de.ottoextra.config.settings.SettingsRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Runs against real Minecraft render-state preparation; server data stays in this test fixture. */
public final class AnimalNametagUiChecks {
    private AnimalNametagUiChecks() { }

    public static void run(ClientGameTestContext ctx) {
        ctx.runOnClient(c -> {
            var config = OttoExtraConfig.active().animalInfo;
            boolean show = config.nametagShowGender, tint = config.nametagGenderColor;
            boolean include = config.nametagIncludeUnregistered;
            var previousTarget = c.crosshairTarget;
            boolean hudHidden = c.options.hudHidden;
            var labels = new ArrayList<Text>();
            var queue = (OrderedRenderCommandQueue) Proxy.newProxyInstance(
                    OrderedRenderCommandQueue.class.getClassLoader(), new Class<?>[]{OrderedRenderCommandQueue.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("submitLabel")) labels.add((Text) args[3]);
                        return null;
                    });
            var cow = new CowEntity(EntityType.COW, c.world);
            cow.setPosition(c.player.getX() + 2, c.player.getY(), c.player.getZ());
            var api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                    new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> {
                        if (method.getName().equals("animalBreedingHorseNames")) {
                            return CompletableFuture.completedFuture(List.of(new AnimalBreedingHorseName(
                                    cow.getUuidAsString(), "Lotte", 1)));
                        }
                        throw new AssertionError("Unexpected API request from renderer: " + method.getName());
                    });
            try {
                HorseRegistry.initialize(api, () -> true, true);
                HorseRegistry.refresh().join();
                HorseRegistry.observe(new AnimalInfoObservation(cow.getUuid(),
                        new AnimalInfoSnapshot("Rotvieh Rind", "Weiblich", "Ja"), Instant.now()));
                c.setScreen(null);
                c.options.hudHidden = false;
                c.crosshairTarget = new EntityHitResult(cow);
                var renderer = c.getEntityRenderDispatcher().getRenderer(cow);
                for (boolean symbol : new boolean[]{false, true}) {
                    for (boolean color : new boolean[]{false, true}) {
                        config.nametagShowGender = symbol;
                        config.nametagGenderColor = color;
                        labels.clear();
                        var state = renderer.getAndUpdateRenderState(cow, 0);
                        submit(state, queue);
                        require(labels.size() == 1, "Expected exactly one registry nametag");
                        require(labels.getFirst().getString().equals(symbol ? "Lotte ♀" : "Lotte"), "Wrong gender suffix");
                        require(labels.getFirst().getStyle().getColor().getRgb()
                                == (color ? 0xFF79B8 : Formatting.GOLD.getColorValue()), "Wrong name color");
                    }
                }
                cow.setCustomName(Text.literal("Lotte"));
                cow.setCustomNameVisible(true);
                labels.clear();
                var state = renderer.getAndUpdateRenderState(cow, 0);
                submit(state, queue);
                require(labels.isEmpty() && state.displayName.getString().equals("Lotte ♀"), "Duplicate vanilla/registry nametag");
                require(cow.getCustomName().getString().equals("Lotte"), "Changed entity name data");
                cow.setCustomName(null);
                state = renderer.getAndUpdateRenderState(cow, 0);
                c.options.hudHidden = true;
                labels.clear(); submit(state, queue);
                require(labels.isEmpty(), "Nametag visible with HUD hidden");
                c.options.hudHidden = false;
                c.crosshairTarget = null;
                labels.clear(); submit(renderer.getAndUpdateRenderState(cow, 0), queue);
                require(labels.isEmpty(), "Reused state kept previous target label");
                c.crosshairTarget = new EntityHitResult(cow);
                state.squaredDistanceToCamera = 65 * 65;
                HorseRegistryNametagRenderer.prepare(cow, state, 0);
                labels.clear(); submit(state, queue);
                require(labels.isEmpty(), "Distant nametag bypassed limit");

                var unregistered = new CowEntity(EntityType.COW, c.world);
                unregistered.setPosition(c.player.getX() + 3, c.player.getY(), c.player.getZ());
                c.crosshairTarget = new EntityHitResult(unregistered);
                config.nametagIncludeUnregistered = true;
                labels.clear(); submit(renderer.getAndUpdateRenderState(unregistered, 0), queue);
                require(labels.isEmpty(), "Unknown animal gained invented data");
                AnimalInfoEvents.OBSERVED.invoker().onObserved(new AnimalInfoObservation(unregistered.getUuid(),
                        new AnimalInfoSnapshot("Rotvieh Rind", "Weiblich", "Ja"), Instant.now()));
                require(HorseRegistry.observation(unregistered.getUuid()).isPresent(), "Toast event did not populate the local cache");
                require(HorseRegistry.name(unregistered.getUuid()).isEmpty(), "Observation created a register entry");
                config.nametagIncludeUnregistered = false;
                labels.clear(); submit(renderer.getAndUpdateRenderState(unregistered, 0), queue);
                require(labels.isEmpty(), "Unregistered nametag ignored opt-out");
                config.nametagIncludeUnregistered = true;
                for (boolean symbol : new boolean[]{false, true}) {
                    for (boolean color : new boolean[]{false, true}) {
                        config.nametagShowGender = symbol;
                        config.nametagGenderColor = color;
                        labels.clear(); submit(renderer.getAndUpdateRenderState(unregistered, 0), queue);
                        if (!symbol && !color) {
                            require(labels.isEmpty(), "Disabled gender options still create an unregistered nametag");
                        } else {
                            require(labels.size() == 1, "Expected one locally observed nametag");
                            require(labels.getFirst().getString().equals(symbol ? "Rotvieh Rind ♀" : "Rotvieh Rind"), "Wrong observed name");
                            require(labels.getFirst().getStyle().getColor().getRgb()
                                    == (color ? 0xFF79B8 : Formatting.GOLD.getColorValue()), "Wrong unregistered name color");
                        }
                    }
                }
                unregistered.setCustomName(Text.literal("Wilde Lotte"));
                unregistered.setCustomNameVisible(true);
                labels.clear(); state = renderer.getAndUpdateRenderState(unregistered, 0); submit(state, queue);
                require(labels.isEmpty() && state.displayName.getString().equals("Wilde Lotte ♀"), "Custom name was replaced or duplicated");
                require(unregistered.getCustomName().getString().equals("Wilde Lotte"), "Local display modified entity data");
                unregistered.setCustomName(null);
                unregistered.setCustomNameVisible(false);
                c.crosshairTarget = null;
                labels.clear(); submit(renderer.getAndUpdateRenderState(unregistered, 0), queue);
                require(labels.isEmpty(), "Unregistered label survived target loss");
                c.crosshairTarget = new EntityHitResult(unregistered);
                labels.clear(); submit(renderer.getAndUpdateRenderState(unregistered, 0), queue);
                require(labels.size() == 1, "Refocusing lost locally cached data");
                AnimalInfoEvents.OBSERVED.invoker().onObserved(new AnimalInfoObservation(unregistered.getUuid(),
                        new AnimalInfoSnapshot("Rotvieh Rind", "Männlich", "Ja"), Instant.now()));
                labels.clear(); submit(renderer.getAndUpdateRenderState(unregistered, 0), queue);
                require(labels.getFirst().getString().equals("Rotvieh Rind ♂")
                        && labels.getFirst().getStyle().getColor().getRgb() == 0x68B8FF, "Fresh toast data did not update the nametag");
                c.crosshairTarget = new EntityHitResult(cow);
                labels.clear(); submit(renderer.getAndUpdateRenderState(cow, 0), queue);
                require(labels.getFirst().getString().equals("Lotte ♀"), "Cached animal type replaced the registered name");
                var wolf = new WolfEntity(EntityType.WOLF, c.world);
                wolf.setPosition(c.player.getX() + 3, c.player.getY(), c.player.getZ());
                c.crosshairTarget = new EntityHitResult(wolf);
                AnimalInfoEvents.OBSERVED.invoker().onObserved(new AnimalInfoObservation(wolf.getUuid(),
                        new AnimalInfoSnapshot("Wolf", "Männlich", "Ja"), Instant.now()));
                labels.clear(); submit(c.getEntityRenderDispatcher().getRenderer(wolf).getAndUpdateRenderState(wolf, 0), queue);
                require(labels.size() == 1 && labels.getFirst().getString().equals("Wolf ♂"), "Non-register animal did not use toast data");
                HorseRegistry.clearConnection();
                labels.clear(); submit(c.getEntityRenderDispatcher().getRenderer(wolf).getAndUpdateRenderState(wolf, 0), queue);
                require(labels.isEmpty(), "Cached nametag crossed a disconnect boundary");
            } finally {
                config.nametagShowGender = show;
                config.nametagGenderColor = tint;
                config.nametagIncludeUnregistered = include;
                c.crosshairTarget = previousTarget;
                c.options.hudHidden = hudHidden;
                HorseRegistry.shutdown();
            }
        });
        boolean[] initial = ctx.computeOnClient(c -> new boolean[]{OttoExtraConfig.active().animalInfo.nametagShowGender,
                OttoExtraConfig.active().animalInfo.nametagGenderColor, OttoExtraConfig.active().animalInfo.nametagIncludeUnregistered});
        try {
            ctx.getInput().resizeWindow(1920, 1080); ctx.waitTick();
            ctx.runOnClient(c -> {
                c.getToastManager().clear();
                OttoExtraConfig.active().animalInfo.nametagShowGender = false;
                OttoExtraConfig.active().animalInfo.nametagGenderColor = false;
                OttoExtraConfig.active().animalInfo.nametagIncludeUnregistered = false;
                var screen = new OttoExtraSettingsScreen(null);
                c.setScreen(screen);
                try {
                    var registryField = OttoExtraSettingsScreen.class.getDeclaredField("registry");
                    registryField.setAccessible(true);
                    var registry = (SettingsRegistry) registryField.get(screen);
                    int index = 0;
                    while (!registry.modules().get(index).id().equals("animalinfo")) index++;
                    var select = OttoExtraSettingsScreen.class.getDeclaredMethod("selectModuleTab", int.class);
                    select.setAccessible(true);
                    select.invoke(screen, index);
                    screen.mouseScrolled(screen.width / 2.0, screen.height / 2.0, 0, -20);
                    var toggles = screen.children().stream().filter(e -> e instanceof ButtonWidget)
                            .map(e -> (ButtonWidget) e).filter(b -> b.visible
                                    && b.getMessage().getString().equals(Text.translatable("ottoextra.settings.off").getString()))
                            .sorted(java.util.Comparator.comparingInt(ButtonWidget::getY)).toList();
                    require(toggles.size() == 3, "All three nametag switches must be visible");
                    var enter = new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0);
                    toggles.get(0).onPress(enter); toggles.get(1).onPress(enter);
                    require(OttoExtraConfig.active().animalInfo.nametagShowGender
                            && OttoExtraConfig.active().animalInfo.nametagGenderColor, "Settings buttons did not enable both options");
                    toggles.get(0).onPress(enter);
                    require(!OttoExtraConfig.active().animalInfo.nametagShowGender
                            && OttoExtraConfig.active().animalInfo.nametagGenderColor, "Name color is not independent of the symbol");
                    toggles.get(2).onPress(enter);
                    require(OttoExtraConfig.active().animalInfo.nametagIncludeUnregistered
                            && !OttoExtraConfig.active().animalInfo.nametagShowGender
                            && OttoExtraConfig.active().animalInfo.nametagGenderColor, "Unregistered option changed another switch");
                } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
            });
            ctx.waitTick(); ctx.takeScreenshot("animal-nametag-settings");
        } finally {
            ctx.runOnClient(c -> {
                c.setScreen(null);
                OttoExtraConfig.active().animalInfo.nametagShowGender = initial[0];
                OttoExtraConfig.active().animalInfo.nametagGenderColor = initial[1];
                OttoExtraConfig.active().animalInfo.nametagIncludeUnregistered = initial[2];
            });
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
        }
    }

    private static void submit(EntityRenderState state, OrderedRenderCommandQueue queue) {
        HorseRegistryNametagRenderer.submit(state, new MatrixStack(), queue, new CameraRenderState());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
