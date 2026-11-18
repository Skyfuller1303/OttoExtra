package de.ottoextra.animalinfo;

import net.minecraft.util.PlayerInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalInfoRequestPlanTest {

    @Test
    void cleansMovementBeforeAndImmediatelyAfterSneakInteraction() {
        assertEquals(List.of(
                        AnimalInfoRequestPlan.Step.SYNC_SELECTED_SLOT,
                        AnimalInfoRequestPlan.Step.NEUTRAL_INPUT,
                        AnimalInfoRequestPlan.Step.STOP_SPRINTING,
                        AnimalInfoRequestPlan.Step.SNEAK_INTERACTION,
                        AnimalInfoRequestPlan.Step.NEUTRAL_INPUT,
                        AnimalInfoRequestPlan.Step.STOP_SPRINTING),
                AnimalInfoRequestPlan.steps());
    }

    @Test
    void fallbackSynchronizesSlotAndAssertsSneakBeforeInteraction() {
        assertEquals(List.of(
                        AnimalInfoRequestPlan.Step.SYNC_SELECTED_SLOT,
                        AnimalInfoRequestPlan.Step.ASSERT_SNEAK_INPUT,
                        AnimalInfoRequestPlan.Step.STOP_SPRINTING,
                        AnimalInfoRequestPlan.Step.SNEAK_INTERACTION,
                        AnimalInfoRequestPlan.Step.NEUTRAL_INPUT,
                        AnimalInfoRequestPlan.Step.STOP_SPRINTING),
                AnimalInfoRequestPlan.steps(AnimalInfoRequestPlan.Attempt.FALLBACK));
    }

    @Test
    void timeoutAlternatesToFallbackAndBackToPrimary() {
        assertEquals(AnimalInfoRequestPlan.Attempt.FALLBACK,
                AnimalInfoRequestPlan.Attempt.PRIMARY.afterTimeout());
        assertEquals(AnimalInfoRequestPlan.Attempt.PRIMARY,
                AnimalInfoRequestPlan.Attempt.FALLBACK.afterTimeout());
    }

    @Test
    void neutralInputKeepsMovementAndJumpButClearsSneakAndSprint() {
        PlayerInput input = new PlayerInput(true, false, true, false,
                true, true, true);

        PlayerInput neutral = AnimalInfoRequestPlan.neutralInput(input);

        assertTrue(neutral.forward());
        assertTrue(neutral.left());
        assertTrue(neutral.jump());
        assertFalse(neutral.backward());
        assertFalse(neutral.right());
        assertFalse(neutral.sneak());
        assertFalse(neutral.sprint());
    }

    @Test
    void anyLocalSneakOrSprintStateBlocksAutomaticQuery() {
        PlayerInput idle = new PlayerInput(false, false, false, false,
                false, false, false);
        PlayerInput sneaking = new PlayerInput(false, false, false, false,
                false, true, false);
        PlayerInput sprinting = new PlayerInput(false, false, false, false,
                false, false, true);

        assertFalse(AnimalInfoRequestPlan.movementBlocksQuery(idle, false, false));
        assertTrue(AnimalInfoRequestPlan.movementBlocksQuery(sneaking, false, false));
        assertTrue(AnimalInfoRequestPlan.movementBlocksQuery(sprinting, false, false));
        assertTrue(AnimalInfoRequestPlan.movementBlocksQuery(idle, true, false));
        assertTrue(AnimalInfoRequestPlan.movementBlocksQuery(idle, false, true));
    }

    @Test
    void mountedHorseOrDonkeyBlocksAutomaticQuery() {
        assertTrue(AnimalInfoRequestPlan.mountedEquineBlocksQuery(true));
        assertFalse(AnimalInfoRequestPlan.mountedEquineBlocksQuery(false));
    }

    @Test
    void heldLeadBlocksOnlyTheAutomaticInteraction() {
        assertTrue(AnimalInfoRequestPlan.heldLeadBlocksQuery(true));
        assertFalse(AnimalInfoRequestPlan.heldLeadBlocksQuery(false));
    }

    @Test
    void everyHorseStartsWithAssertedSneakEvenWhenClientMissesLeashState() {
        assertEquals(AnimalInfoRequestPlan.Attempt.FALLBACK,
                AnimalInfoRequestPlan.attemptForTarget(
                        AnimalInfoRequestPlan.Attempt.PRIMARY, true));
        assertEquals(AnimalInfoRequestPlan.Attempt.FALLBACK,
                AnimalInfoRequestPlan.attemptForTarget(
                        AnimalInfoRequestPlan.Attempt.FALLBACK, true));
        assertEquals(AnimalInfoRequestPlan.Attempt.PRIMARY,
                AnimalInfoRequestPlan.attemptForTarget(
                        AnimalInfoRequestPlan.Attempt.PRIMARY, false));
    }

    @Test
    void recognizesServerRejectionForLeashedHorse() {
        assertTrue(AnimalInfoRequestPlan.isLeashedHorseRejection(
                "[Info] Du musst Reitpferd erst losbinden um es zu reiten."));
        assertTrue(AnimalInfoRequestPlan.isLeashedHorseRejection(
                "You must untie the horse before you can mount it."));
        assertFalse(AnimalInfoRequestPlan.isLeashedHorseRejection(
                "Pferdeinfo - Reitpferd"));
    }

    @Test
    void delayedCleanupCreatesRealSneakInputEdgeAcrossTwoTicks() {
        PlayerInput moving = new PlayerInput(true, false, false, true,
                true, false, false);

        PlayerInput asserted = AnimalInfoRequestPlan.assertedSneakInput(moving);
        PlayerInput released = AnimalInfoRequestPlan.neutralInput(moving);

        assertTrue(asserted.sneak());
        assertFalse(asserted.sprint());
        assertFalse(released.sneak());
        assertFalse(released.sprint());
        assertEquals(AnimalInfoRequestPlan.CleanupPhase.RELEASE_SNEAK,
                AnimalInfoRequestPlan.CleanupPhase.ASSERT_SNEAK.next());
        assertEquals(AnimalInfoRequestPlan.CleanupPhase.NONE,
                AnimalInfoRequestPlan.CleanupPhase.RELEASE_SNEAK.next());
    }

    @Test
    void ridingAnythingBlocksQueries() {
        assertTrue(AnimalInfoRequestPlan.ridingBlocksQuery(true));
        assertFalse(AnimalInfoRequestPlan.ridingBlocksQuery(false));
    }

    @Test
    void anyItemThatCouldChangeTheAnimalBlocksQueries() {
        assertFalse(AnimalInfoRequestPlan.heldItemBlocksQuery(false, false, false, false, false, false));
        assertTrue(AnimalInfoRequestPlan.heldItemBlocksQuery(true, false, false, false, false, false));
        assertTrue(AnimalInfoRequestPlan.heldItemBlocksQuery(false, true, false, false, false, false));
        assertTrue(AnimalInfoRequestPlan.heldItemBlocksQuery(false, false, true, false, false, false));
        assertTrue(AnimalInfoRequestPlan.heldItemBlocksQuery(false, false, false, true, false, false));
        assertTrue(AnimalInfoRequestPlan.heldItemBlocksQuery(false, false, false, false, true, false));
        assertTrue(AnimalInfoRequestPlan.heldItemBlocksQuery(false, false, false, false, false, true));
    }

    @Test
    void existingProtectionsStillCoverSneakSprintAndMountedHorses() {
        PlayerInput idle = new PlayerInput(false, false, false, false, false, false, false);
        assertFalse(AnimalInfoRequestPlan.movementBlocksQuery(idle, false, false));
        assertTrue(AnimalInfoRequestPlan.movementBlocksQuery(
                new PlayerInput(false, false, false, false, false, true, false), false, false));
        assertTrue(AnimalInfoRequestPlan.movementBlocksQuery(idle, false, true));
        assertTrue(AnimalInfoRequestPlan.mountedEquineBlocksQuery(true));
        assertEquals(AnimalInfoRequestPlan.Attempt.FALLBACK,
                AnimalInfoRequestPlan.attemptForTarget(AnimalInfoRequestPlan.Attempt.PRIMARY, true));
    }
}
