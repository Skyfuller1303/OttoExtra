package de.ottoextra.horses;

import de.ottoextra.animalinfo.AnimalInfoObservation;
import de.ottoextra.animalinfo.AnimalInfoSnapshot;
import de.ottoextra.config.OttoExtraConfig;
import com.google.gson.Gson;
import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.AnimalBreedingHorse;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class HorseRegistryNametagTest {
    @AfterEach
    void clear() {
        HorseRegistry.shutdown();
    }

    @Test
    void symbolAndNameColorAreIndependent() {
        for (boolean show : new boolean[]{false, true}) {
            for (boolean tint : new boolean[]{false, true}) {
                var config = new OttoExtraConfig.AnimalInfo();
                config.nametagShowGender = show;
                config.nametagGenderColor = tint;
                Text original = Text.literal("Lotte").formatted(Formatting.GOLD, Formatting.BOLD);
                Text label = HorseRegistryNametagRenderer.decorate(original, "Weiblich", config);
                assertEquals(show ? "Lotte ♀" : "Lotte", label.getString());
                assertEquals(tint ? 0xFF79B8 : Formatting.GOLD.getColorValue(), label.getStyle().getColor().getRgb());
                assertTrue(label.getStyle().isBold());
                assertEquals("Lotte", original.getString(), "Do not mutate the entity's text");
                assertEquals(Formatting.GOLD.getColorValue(), original.getStyle().getColor().getRgb());
                if (show) assertEquals(0xFF79B8, label.getSiblings().getLast().getStyle().getColor().getRgb());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Weiblich", "Female", "Stute", "♀", "Stute (Weiblich)",
            "Schaf (Weiblich)", "Sau (Weiblich)", "Henne (Weiblich)", "Neue Tierart (Female)"})
    void femaleAliasesUsePink(String gender) {
        assertGender(gender, "Lotte ♀", 0xFF79B8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Männlich", "Maennlich", "Male", "Hengst", "Wallach", "♂",
            "Hengst (Männlich)", "Bulle (Männlich)", "Bock (Männlich)", "Eber (Männlich)",
            "Hahn (Männlich)", "Neue Tierart (Maennlich)"})
    void maleAliasesUseBlue(String gender) {
        assertGender(gender, "Lotte ♂", 0x68B8FF);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Unbekannt", "unknown", "♀ ♂", "Stute (Männlich)",
            "Hengst (Weiblich)", "Not female", "unweiblich"})
    void unknownOrConflictingGenderKeepsOriginal(String gender) {
        var config = new OttoExtraConfig.AnimalInfo();
        config.nametagShowGender = config.nametagGenderColor = true;
        Text original = Text.literal("Lotte").formatted(Formatting.GOLD);
        assertSame(original, HorseRegistryNametagRenderer.decorate(original, gender, config));
        assertSame(original, HorseRegistryNametagRenderer.decorate(original, null, config));
    }

    @Test
    void matchingVanillaNameIsDecoratedOnceAndOtherNamesArePreserved() {
        var config = new OttoExtraConfig.AnimalInfo();
        config.nametagShowGender = config.nametagGenderColor = true;
        var state = new EntityRenderState();
        Text vanilla = Text.literal("Lotte").formatted(Formatting.WHITE);
        state.displayName = vanilla;
        HorseRegistryNametagRenderer.prepareLabel(state, "lotte", "Stute", config);
        assertEquals("Lotte ♀", state.displayName.getString());
        assertEquals(0xFF79B8, state.displayName.getStyle().getColor().getRgb());
        assertEquals("Lotte", vanilla.getString());
        state.displayName = Text.literal("Rotvieh Rind");
        HorseRegistryNametagRenderer.prepareLabel(state, "Lotte", "Stute", config);
        assertEquals("Rotvieh Rind", state.displayName.getString());
    }

    @Test
    void nameColorPreservesFormattingAcrossStyledSegments() {
        var config = new OttoExtraConfig.AnimalInfo();
        config.nametagGenderColor = true;
        Text original = Text.literal("Lotte").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal(" II").formatted(Formatting.WHITE, Formatting.ITALIC));
        Text label = HorseRegistryNametagRenderer.decorate(original, "Stute", config);
        assertEquals("Lotte II", label.getString());
        assertTrue(label.getSiblings().getFirst().getStyle().isBold());
        assertTrue(label.getSiblings().getLast().getStyle().isItalic());
        assertEquals(0xFF79B8, label.getSiblings().getLast().getStyle().getColor().getRgb());
        assertEquals(Formatting.WHITE.getColorValue(), original.getSiblings().getFirst().getStyle().getColor().getRgb());
    }

    @Test
    void protectedRecordFillsMissingGenderButKnownObservationTakesPriority() {
        UUID uuid = UUID.randomUUID();
        var record = new Gson().fromJson("{\"entityUuid\":\"" + uuid
                + "\",\"registryName\":\"Lotte\",\"gender\":\"Stute\"}", AnimalBreedingHorse.class);
        var api = (OttoExtraApiClient) Proxy.newProxyInstance(OttoExtraApiClient.class.getClassLoader(),
                new Class<?>[]{OttoExtraApiClient.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "animalBreedingRole" -> CompletableFuture.completedFuture(Optional.of("breeder"));
                    case "animalBreedingHorses" -> CompletableFuture.completedFuture(List.of(record));
                    default -> throw new AssertionError(method.getName());
                });
        HorseRegistry.initialize(api, () -> true, true);
        HorseRegistry.refreshDatabaseRecords().join();
        assertEquals("Stute", HorseRegistry.knownGender(uuid));
        HorseRegistry.observe(new AnimalInfoObservation(uuid,
                new AnimalInfoSnapshot("Kuh", "Unbekannt", "Ja"), Instant.now()));
        assertEquals("Stute", HorseRegistry.knownGender(uuid));
        HorseRegistry.observe(new AnimalInfoObservation(uuid,
                new AnimalInfoSnapshot("Kuh", "Männlich", "Ja"), Instant.now()));
        assertEquals("Männlich", HorseRegistry.knownGender(uuid));
    }

    @Test
    void knownGenderUsesMatchingObservationAndIsClearedOnDisconnect() {
        HorseRegistry.initialize(null, () -> true, true);
        UUID uuid = UUID.randomUUID();
        HorseRegistry.observe(new AnimalInfoObservation(uuid,
                new AnimalInfoSnapshot("Kuh", "Weiblich", "Ja"), Instant.now()));
        assertEquals("Weiblich", HorseRegistry.knownGender(uuid));
        assertEquals("", HorseRegistry.knownGender(UUID.randomUUID()));
        HorseRegistry.clearConnection();
        assertEquals("", HorseRegistry.knownGender(uuid));
    }

    private static void assertGender(String gender, String expected, int color) {
        var config = new OttoExtraConfig.AnimalInfo();
        config.nametagShowGender = config.nametagGenderColor = true;
        Text label = HorseRegistryNametagRenderer.decorate(Text.literal("Lotte"), gender, config);
        assertEquals(expected, label.getString());
        assertEquals(color, label.getStyle().getColor().getRgb());
    }

    @Test
    void unregisteredNamesRequireOptInKnownDataAndAnEnabledGenderOption() {
        HorseRegistry.initialize(null, () -> true, true);
        UUID uuid = UUID.randomUUID();
        var config = new OttoExtraConfig.AnimalInfo();
        config.nametagGenderColor = true;
        var state = new EntityRenderState();
        Text original = Text.literal("Kuh");
        state.displayName = original;
        HorseRegistry.observe(new AnimalInfoObservation(uuid,
                new AnimalInfoSnapshot("Rotvieh Rind", "Weiblich", "Ja"), Instant.now()));
        HorseRegistryNametagRenderer.prepareUnregisteredLabel(state, uuid, null, config);
        assertSame(original, state.displayName);
        config.nametagIncludeUnregistered = true;
        HorseRegistryNametagRenderer.prepareUnregisteredLabel(state, UUID.randomUUID(), null, config);
        assertSame(original, state.displayName);
        config.nametagGenderColor = false;
        HorseRegistryNametagRenderer.prepareUnregisteredLabel(state, uuid, null, config);
        assertSame(original, state.displayName);
        config.nametagGenderColor = true;
        HorseRegistryNametagRenderer.prepareUnregisteredLabel(state, uuid, null, config);
        assertEquals("Kuh", state.displayName.getString());
        assertEquals(0xFF79B8, state.displayName.getStyle().getColor().getRgb());
        assertTrue(HorseRegistry.name(uuid).isEmpty());
    }
}
