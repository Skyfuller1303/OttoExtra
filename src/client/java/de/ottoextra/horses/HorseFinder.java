package de.ottoextra.horses;

import de.ottoextra.OttoExtra;
import de.ottoextra.api.OttoExtraApiClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Shared finder used by OttoExtra Core and the AnimalBreeding addon. */
public final class HorseFinder {

    private static final int FINDER_OUTLINE = 0xFFFFC857;
    private static final AtomicBoolean POLLING = new AtomicBoolean();
    private static final Object LOCK = new Object();

    private static volatile OttoExtraApiClient api;
    private static volatile BooleanSupplier onOttonien = () -> false;
    private static volatile Set<UUID> targets = Set.of();
    private static boolean locallyChanged;
    private static long generation;
    private static int pollTicks;

    private HorseFinder() {
    }

    static void initialize(OttoExtraApiClient client, BooleanSupplier serverGate) {
        api = client;
        onOttonien = serverGate == null ? () -> false : serverGate;
        reset();
    }

    public static Optional<UUID> target() {
        return targets.stream().findFirst();
    }

    public static Set<UUID> targets() {
        return targets;
    }

    public static boolean isTarget(UUID entityUuid) {
        return entityUuid != null && targets.contains(entityUuid);
    }

    public static boolean isTarget(Entity entity) {
        return RegistryAnimals.supports(entity) && isTarget(entity.getUuid());
    }

    public static CompletableFuture<Void> toggle(UUID entityUuid) {
        return isTarget(entityUuid) ? remove(entityUuid) : select(entityUuid);
    }

    /** Activates the glow immediately; API persistence happens asynchronously. */
    public static CompletableFuture<Void> select(UUID entityUuid) {
        if (entityUuid == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Finder-Pferd fehlt"));
        }
        long selectedGeneration;
        synchronized (LOCK) {
            if (targets.contains(entityUuid)) {
                return CompletableFuture.completedFuture(null);
            }
            generation++;
            selectedGeneration = generation;
            LinkedHashSet<UUID> updated = new LinkedHashSet<>(targets);
            updated.add(entityUuid);
            targets = Set.copyOf(updated);
            locallyChanged = true;
        }
        OttoExtraApiClient current = api;
        if (current == null || !onOttonien.getAsBoolean()) {
            return CompletableFuture.completedFuture(null);
        }
        return current.addAnimalBreedingFinderTarget(entityUuid)
                .handle((ignored, error) -> {
                    if (error != null) {
                        OttoExtra.LOGGER.warn(
                                "Pferdefinder bleibt lokal aktiv; API-Auswahl fehlgeschlagen: {}",
                                unwrap(error).toString());
                        return null;
                    }
                    synchronized (LOCK) {
                        if (generation == selectedGeneration) {
                            locallyChanged = false;
                        }
                    }
                    return null;
                });
    }

    /** Removes only this horse; all other selected horses keep glowing. */
    public static CompletableFuture<Void> remove(UUID entityUuid) {
        if (entityUuid == null) {
            return CompletableFuture.completedFuture(null);
        }
        long removedGeneration;
        synchronized (LOCK) {
            if (!targets.contains(entityUuid)) {
                return CompletableFuture.completedFuture(null);
            }
            generation++;
            removedGeneration = generation;
            LinkedHashSet<UUID> updated = new LinkedHashSet<>(targets);
            updated.remove(entityUuid);
            targets = Set.copyOf(updated);
            locallyChanged = true;
        }
        OttoExtraApiClient current = api;
        if (current == null || !onOttonien.getAsBoolean()) {
            return CompletableFuture.completedFuture(null);
        }
        return current.removeAnimalBreedingFinderTarget(entityUuid).handle((ignored, error) -> {
            if (error != null) {
                OttoExtra.LOGGER.debug("Pferdefinder-Ziel konnte serverseitig nicht entfernt werden: {}",
                        unwrap(error).toString());
                return null;
            }
            synchronized (LOCK) {
                if (generation == removedGeneration) locallyChanged = false;
            }
            return null;
        });
    }

    public static CompletableFuture<Void> clear() {
        synchronized (LOCK) {
            generation++;
            targets = Set.of();
            locallyChanged = false;
        }
        OttoExtraApiClient current = api;
        if (current == null || !onOttonien.getAsBoolean()) {
            return CompletableFuture.completedFuture(null);
        }
        return current.clearAnimalBreedingFinderTarget().handle((ignored, error) -> {
            if (error != null) {
                OttoExtra.LOGGER.debug("Pferdefinder konnte serverseitig nicht beendet werden: {}",
                        unwrap(error).toString());
            }
            return null;
        });
    }

    public static CompletableFuture<Void> pollNow() {
        OttoExtraApiClient current = api;
        if (current == null || !onOttonien.getAsBoolean()
                || !POLLING.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        long requestedGeneration;
        synchronized (LOCK) {
            requestedGeneration = generation;
        }
        return current.animalBreedingFinderTargets().handle((remote, error) -> {
            POLLING.set(false);
            if (error != null) {
                OttoExtra.LOGGER.debug("Pferdefinder konnte nicht aktualisiert werden: {}",
                        unwrap(error).toString());
                return null;
            }
            Set<UUID> values = remote == null ? Set.of() : Set.copyOf(remote);
            synchronized (LOCK) {
                if (generation != requestedGeneration) {
                    return null;
                }
                if (locallyChanged) {
                    if (values.equals(targets)) {
                        locallyChanged = false;
                    }
                    return null;
                }
                targets = values;
            }
            return null;
        });
    }

    static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null
                || !onOttonien.getAsBoolean()) {
            pollTicks = 0;
            return;
        }
        if (++pollTicks >= 100) {
            pollTicks = 0;
            pollNow();
        }
        Set<UUID> selected = targets;
        if (selected.isEmpty()) {
            return;
        }
        for (UUID entityUuid : selected) {
            Entity entity = client.world.getEntity(entityUuid);
            if (!RegistryAnimals.supports(entity)
                    || client.player.squaredDistanceTo(entity) >= 4.0D) {
                continue;
            }
            String name = HorseRegistry.name(entityUuid)
                    .orElse(entityUuid.toString().substring(0, 8));
            remove(entityUuid);
            client.player.sendMessage(Text.translatable(
                    "ottoextra.horses.finder.found", name), true);
        }
    }

    public static void prepare(Entity entity, EntityRenderState state) {
        if (isTarget(entity)) {
            state.outlineColor = FINDER_OUTLINE;
        }
    }

    static void reset() {
        synchronized (LOCK) {
            generation++;
            targets = Set.of();
            locallyChanged = false;
        }
        pollTicks = 0;
        POLLING.set(false);
    }

    static void shutdown() {
        reset();
        api = null;
        onOttonien = () -> false;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
