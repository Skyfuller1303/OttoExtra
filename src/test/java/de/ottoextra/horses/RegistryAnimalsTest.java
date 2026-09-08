package de.ottoextra.horses;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryAnimalsTest {

    static Stream<Class<? extends Entity>> farmAnimals() {
        return Stream.of(CowEntity.class, SheepEntity.class, ChickenEntity.class,
                DonkeyEntity.class, GoatEntity.class, PigEntity.class,
                HorseEntity.class, MuleEntity.class);
    }

    static Stream<Class<? extends Entity>> otherEntities() {
        return Stream.of(WolfEntity.class, CatEntity.class, RabbitEntity.class,
                ZombieEntity.class, PlayerEntity.class);
    }

    @ParameterizedTest
    @MethodSource("farmAnimals")
    void acceptsAllRequestedFarmAnimalsAndExistingEquines(Class<? extends Entity> type) {
        assertTrue(RegistryAnimals.supportsType(type));
    }

    @ParameterizedTest
    @MethodSource("otherEntities")
    void rejectsPetsWildlifeMonstersAndPlayers(Class<? extends Entity> type) {
        assertFalse(RegistryAnimals.supportsType(type));
    }
}
