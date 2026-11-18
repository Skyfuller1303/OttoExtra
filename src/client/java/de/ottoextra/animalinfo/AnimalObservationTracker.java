package de.ottoextra.animalinfo;

/*
 * Behaviour adapted from Viehzucht by yuyukey (MIT), https://github.com/yuyukey/OttonienUtils
 * See THIRD_PARTY_NOTICES.md in the repository root.
 */

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Records what the client sees happening to animals: hearts, newborns next to loving parents and sheep
 * that were just sheared. The results go to {@link AnimalLiveStore}. Everything runs on the client thread.
 */
public final class AnimalObservationTracker {

    /** A sheared sheep must look sheared this soon after the click. */
    private static final long SHEAR_CONFIRM_MS = 3_000L;
    private static final int MAX_WATCHED = 256;
    private static final byte LOVE_STATUS = 18;

    private static BooleanSupplier onOttonien = () -> false;
    private static final Map<AnimalEntity, Integer> WATCHED = new IdentityHashMap<>();
    private static final Map<SheepEntity, Long> SHEARING = new IdentityHashMap<>();

    private AnimalObservationTracker() {
    }

    static void init(BooleanSupplier serverGate) {
        onOttonien = serverGate == null ? () -> false : serverGate;
        reset();
    }

    static void reset() {
        WATCHED.clear();
        SHEARING.clear();
    }

    /** Entity status 18: hearts above an animal. */
    public static void onEntityStatus(Entity entity, byte status) {
        if (status == LOVE_STATUS && entity instanceof AnimalEntity animal && !animal.isBaby()
                && onOttonien.getAsBoolean()) {
            markLoved(animal);
        }
    }

    /** Heart particles sent by position instead of entity status: attribute them to the nearest adult. */
    public static void onParticle(ParticleEffect effect, double x, double y, double z) {
        if (effect == null || effect.getType() != ParticleTypes.HEART || !onOttonien.getAsBoolean()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Box box = new Box(x - 1.5, y - 2.5, z - 1.5, x + 1.5, y + 1.0, z + 1.5);
        List<AnimalEntity> adults = client.world.getEntitiesByClass(AnimalEntity.class, box, a -> !a.isBaby());
        double[] distances = new double[adults.size()];
        for (int i = 0; i < distances.length; i++) distances[i] = adults.get(i).squaredDistanceTo(x, y, z);
        int nearest = BreedingDecision.nearest(distances);
        if (nearest >= 0) markLoved(adults.get(nearest));
    }

    private static void markLoved(AnimalEntity animal) {
        long now = System.currentTimeMillis();
        AnimalLiveStore.current().update(animal.getUuid(), now, state -> state.withLoved(now));
    }

    /**
     * A new animal entered the world. Whether it is a baby is only known once its data arrived, so it is
     * watched for a short time instead of being judged right away.
     */
    static void onEntityLoad(Entity entity) {
        if (entity instanceof AnimalEntity animal && onOttonien.getAsBoolean()
                && WATCHED.size() < MAX_WATCHED) {
            WATCHED.put(animal, BreedingDecision.WATCH_TICKS);
        }
    }

    /** The player used an item on an animal: remember shears used on a sheep with wool. */
    static void onInteract(Entity entity, ItemStack held, boolean sneaking) {
        if (!sneaking && entity instanceof SheepEntity sheep && !sheep.isSheared() && !sheep.isBaby()
                && held.isOf(Items.SHEARS) && onOttonien.getAsBoolean()) {
            SHEARING.put(sheep, System.currentTimeMillis());
        }
    }

    static void tick() {
        if (WATCHED.isEmpty() && SHEARING.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<AnimalEntity, Integer>> it = WATCHED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<AnimalEntity, Integer> watched = it.next();
            AnimalEntity animal = watched.getKey();
            if (animal.isRemoved()) {
                it.remove();
            } else if (animal.isBaby()) {
                it.remove();
                onNewborn(animal, now);
            } else if (watched.getValue() <= 1) {
                it.remove();
            } else {
                watched.setValue(watched.getValue() - 1);
            }
        }
        for (Iterator<Map.Entry<SheepEntity, Long>> it = SHEARING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<SheepEntity, Long> shearing = it.next();
            SheepEntity sheep = shearing.getKey();
            if (sheep.isSheared()) {
                it.remove();
                AnimalLiveStore.current().update(sheep.getUuid(), now, state -> state.withSheared(now));
            } else if (sheep.isRemoved() || now - shearing.getValue() > SHEAR_CONFIRM_MS) {
                it.remove();
            }
        }
    }

    private static void onNewborn(AnimalEntity baby, long now) {
        AnimalLiveStore store = AnimalLiveStore.current();
        List<BreedingDecision.Neighbour> neighbours = new ArrayList<>();
        for (Entity other : baby.getEntityWorld().getOtherEntities(baby,
                baby.getBoundingBox().expand(BreedingDecision.BIRTH_RANGE))) {
            if (!(other instanceof AnimalEntity animal)) continue;
            AnimalLiveState state = store.get(animal.getUuid());
            neighbours.add(new BreedingDecision.Neighbour(animal.getUuid(),
                    animal.getType() == baby.getType(), !animal.isBaby(),
                    animal.squaredDistanceTo(baby), state == null ? 0L : state.lovedAt()));
        }
        List<java.util.UUID> parents = BreedingDecision.parents(neighbours, now);
        if (parents.isEmpty()) return;
        for (java.util.UUID parent : parents) {
            store.update(parent, now, state -> state.withBred(now));
        }
        store.update(baby.getUuid(), now, state -> state.withBorn(now));
    }
}
