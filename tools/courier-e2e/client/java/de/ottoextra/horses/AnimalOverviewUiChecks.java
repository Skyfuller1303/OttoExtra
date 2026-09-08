package de.ottoextra.horses;

import com.google.gson.Gson;
import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseName;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.horses.ui.BasicHorseOverviewScreen;
import de.ottoextra.horses.ui.BasicHorseRecordScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** Real overview widgets and model rendering with an isolated animal-register API. */
public final class AnimalOverviewUiChecks {
    private static final Gson JSON = new Gson();
    private static final int ENTITY_ID = -123_457;
    private AnimalOverviewUiChecks() { }

    public static void run(ClientGameTestContext ctx) {
        String snapshot = ctx.computeOnClient(c -> OttoExtraConfig.active().snapshotJson());
        int guiScale = ctx.computeOnClient(c -> c.options.getGuiScale().getValue());
        var oldTarget = ctx.computeOnClient(c -> c.crosshairTarget);
        AtomicReference<AnimalBreedingHorse> saved = new AtomicReference<>();
        List<AnimalBreedingHorse> records = new ArrayList<>();
        UUID donkeyId = UUID.fromString("900ad347-0000-4000-8000-000000000001");
        records.add(record(donkeyId, "Packesel", ""));
        records.add(record(UUID.fromString("b21bdae6-0000-4000-8000-000000000002"), "Reitpferd", ""));
        records.add(record(UUID.randomUUID(), "Streitross", "Askan"));
        records.add(record(UUID.randomUUID(), "Esel", "Abendrot"));
        for (String type : List.of("Rotvieh Rind", "Brakel", "Hausschwein", "Ziege", "Zaupel")) {
            records.add(record(UUID.randomUUID(), type, type));
        }
        try {
            ctx.runOnClient(c -> c.options.getGuiScale().setValue(3));
            ctx.getInput().resizeWindow(1920, 1080); ctx.waitTick();
            ctx.runOnClient(c -> {
                var config = OttoExtraConfig.active().animalInfo;
                config.overviewShowCows = config.overviewShowChickens = config.overviewShowPigs
                        = config.overviewShowGoats = config.overviewShowSheep = false;
                var api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                        new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> switch (method.getName()) {
                            case "animalBreedingRole" -> CompletableFuture.completedFuture(Optional.of("breeder"));
                            case "animalBreedingHorses" -> CompletableFuture.completedFuture(List.copyOf(records));
                            case "animalBreedingHorseNames" -> CompletableFuture.completedFuture(records.stream()
                                    .filter(r -> !r.registryName().isBlank()).map(r -> new AnimalBreedingHorseName(
                                            r.entityUuid(), r.registryName(), r.revision())).toList());
                            case "player" -> CompletableFuture.completedFuture(null);
                            case "putAnimalBreedingHorseName" -> {
                                var draft = (AnimalBreedingHorse) args[0];
                                require(draft.uuid().equals(donkeyId) && draft.animalType().equals("Packesel"), "Donkey saved as another animal");
                                require(args[1] != null && draft.coatColor().isEmpty(), "Donkey location/appearance incorrect");
                                var data = JSON.toJsonTree(draft).getAsJsonObject();
                                data.addProperty("currentFarm", "Kallberg Kloster");
                                data.addProperty("canEdit", true);
                                data.addProperty("canManage", true);
                                data.addProperty("revision", 2);
                                var stored = JSON.fromJson(data, AnimalBreedingHorse.class);
                                records.replaceAll(r -> r.uuid().equals(donkeyId) ? stored : r);
                                saved.set(stored);
                                yield CompletableFuture.completedFuture(stored);
                            }
                            default -> throw new AssertionError("Unexpected API method: " + method.getName());
                        });
                HorseRegistry.initialize(api, () -> true, true);
                HorseRegistry.refreshDatabaseRecords().join();
                var donkey = new DonkeyEntity(EntityType.DONKEY, c.world);
                require(c.world.getEntityById(ENTITY_ID) == null, "Fixture entity ID is already in use");
                donkey.setId(ENTITY_ID);
                donkey.setUuid(donkeyId);
                donkey.setPosition(c.player.getX() + 2, c.player.getY(), c.player.getZ());
                c.world.addEntity(donkey);
                c.crosshairTarget = new EntityHitResult(donkey);
                HorseRegistry.observe(new AnimalInfoObservation(donkeyId,
                        new AnimalInfoSnapshot("Packesel", "Stute", "72/100", "Ja", "", "", "1,46 Blöcke", "13,67 Blöcke/s"), Instant.now()));
                c.getToastManager().clear();
                c.setScreen(new BasicHorseOverviewScreen(null));
            });
            ctx.waitTick(); assertVisible(ctx, "ALL", 4); ctx.takeScreenshot("animal-overview-all");
            cycle(ctx); assertVisible(ctx, "HORSES", 2); ctx.takeScreenshot("animal-overview-horses");
            cycle(ctx); assertVisible(ctx, "DONKEYS", 2); ctx.takeScreenshot("animal-overview-donkeys");
            ctx.runOnClient(c -> {
                var screen = (BasicHorseOverviewScreen) c.currentScreen;
                var add = AnimalOverviewUiChecks.<ButtonWidget>field(screen, "addButton");
                require(add.active, "Observed donkey cannot be registered from overview");
                add.onPress(enter());
                require(c.currentScreen instanceof BasicHorseRecordScreen, "Registration did not open the animal record");
            });
            ctx.waitTick();
            ctx.runOnClient(c -> {
                var record = (BasicHorseRecordScreen) c.currentScreen;
                require(AnimalOverviewUiChecks.<UUID>field(record, "entityUuid").equals(donkeyId), "Wrong registration target");
                AnimalOverviewUiChecks.<TextFieldWidget>field(record, "nameField").setText("Testesel");
                var save = AnimalOverviewUiChecks.<ButtonWidget>field(record, "saveButton");
                require(save.active, "Allowed donkey cannot be saved");
                save.onPress(enter());
            });
            ctx.waitTick();
            require(saved.get() != null && saved.get().registryName().equals("Testesel"), "Donkey was not saved");
            assertVisible(ctx, "DONKEYS", 2);
            ctx.runOnClient(c -> {
                var cfg = OttoExtraConfig.active().animalInfo;
                cfg.overviewShowCows = cfg.overviewShowChickens = cfg.overviewShowPigs
                        = cfg.overviewShowGoats = cfg.overviewShowSheep = true;
            });
            for (String kind : List.of("COWS", "CHICKENS", "PIGS", "GOATS", "SHEEP")) {
                cycle(ctx); assertVisible(ctx, kind, 1); ctx.takeScreenshot("animal-overview-" + kind.toLowerCase());
            }
            cycle(ctx); assertVisible(ctx, "ALL", 9);
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
            ctx.takeScreenshot("animal-overview-compact");
            ctx.runOnClient(c -> {
                AnimalOverviewUiChecks.<TextFieldWidget>field(c.currentScreen, "searchField").setText("900ad347");
            });
            ctx.waitTick(); assertVisible(ctx, "ALL", 1);
        } finally {
            ctx.runOnClient(c -> {
                c.setScreen(null);
                if (c.world != null && c.world.getEntityById(ENTITY_ID) != null
                        && c.world.getEntityById(ENTITY_ID).getUuid().equals(donkeyId)) {
                    c.world.removeEntity(ENTITY_ID, Entity.RemovalReason.DISCARDED);
                }
                c.crosshairTarget = oldTarget;
                OttoExtraConfig.active().restoreFrom(snapshot);
                c.options.getGuiScale().setValue(guiScale);
                HorseRegistry.shutdown();
            });
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
        }
    }

    private static void cycle(ClientGameTestContext ctx) {
        ctx.runOnClient(c -> AnimalOverviewUiChecks.<ButtonWidget>field(c.currentScreen, "filterButton").onPress(enter()));
        ctx.waitTick();
    }

    private static void assertVisible(ClientGameTestContext ctx, String kind, int count) {
        ctx.runOnClient(c -> {
            require(c.currentScreen instanceof BasicHorseOverviewScreen, "Overview closed unexpectedly");
            require(AnimalOverviewUiChecks.<Enum<?>>field(c.currentScreen, "filter").name().equals(kind), "Wrong animal filter");
            require(AnimalOverviewUiChecks.<List<?>>field(c.currentScreen, "visibleHorses").size() == count, "Wrong filtered animal count for " + kind);
        });
    }

    private static AnimalBreedingHorse record(UUID uuid, String type, String name) {
        var data = JSON.toJsonTree(Map.of("entityUuid", uuid.toString(), "registryName", name, "animalType", type,
                "gender", "Stute", "origin", "Kallberg Kloster", "currentFarm", "Kallberg Kloster", "canEdit", true,
                "canManage", true, "revision", 1)).getAsJsonObject();
        if (type.equals("Packesel") || type.equals("Esel") || type.equals("Reitpferd") || type.equals("Streitross")) {
            data.addProperty("speed", "13,67 Blöcke/s"); data.addProperty("jumpHeight", "1,46 Blöcke");
        }
        return JSON.fromJson(data, AnimalBreedingHorse.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    private static KeyInput enter() { return new KeyInput(GLFW.GLFW_KEY_ENTER, 0, 0); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
