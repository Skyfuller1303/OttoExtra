package de.ottoextra.animalinfo;

import net.minecraft.util.PlayerInput;

import java.util.List;
import java.util.Locale;

/** Paketfolge und Bewegungsregeln einer automatischen Tierinfo-Abfrage. */
final class AnimalInfoRequestPlan {

    private static final List<Step> PRIMARY_STEPS = List.of(
            Step.SYNC_SELECTED_SLOT,
            Step.NEUTRAL_INPUT,
            Step.STOP_SPRINTING,
            Step.SNEAK_INTERACTION,
            Step.NEUTRAL_INPUT,
            Step.STOP_SPRINTING);
    private static final List<Step> FALLBACK_STEPS = List.of(
            Step.SYNC_SELECTED_SLOT,
            Step.ASSERT_SNEAK_INPUT,
            Step.STOP_SPRINTING,
            Step.SNEAK_INTERACTION,
            Step.NEUTRAL_INPUT,
            Step.STOP_SPRINTING);

    private AnimalInfoRequestPlan() {
    }

    static List<Step> steps() {
        return steps(Attempt.PRIMARY);
    }

    static List<Step> steps(Attempt attempt) {
        return attempt == Attempt.FALLBACK ? FALLBACK_STEPS : PRIMARY_STEPS;
    }

    static boolean movementBlocksQuery(PlayerInput input,
                                       boolean entitySneaking,
                                       boolean entitySprinting) {
        return input == null || input.sneak() || input.sprint()
                || entitySneaking || entitySprinting;
    }

    static boolean mountedEquineBlocksQuery(boolean mountedOnEquine) {
        return mountedOnEquine;
    }

    /** Riding anything (boat, minecart, horse) must not start a query. */
    static boolean ridingBlocksQuery(boolean hasVehicle) {
        return hasVehicle;
    }

    /** The animal the player rides (directly or through a chain of vehicles) gets no info panel. */
    static boolean ownMountHidden(boolean hideOwnMount, boolean targetIsOwnMount) {
        return hideOwnMount && targetIsOwnMount;
    }

    /**
     * The synthetic click is a real interaction. Breeding food, a lead, shears, a bucket, a name tag, a
     * saddle or a container item in either hand could change the animal, so no query is sent.
     */
    static boolean heldItemBlocksQuery(boolean breedingItem, boolean lead, boolean shears,
                                       boolean bucket, boolean nameTag, boolean saddleOrContainer) {
        return breedingItem || lead || shears || bucket || nameTag || saddleOrContainer;
    }

    /** A synthetic main-hand click must never use a lead held by the player. */
    static boolean heldLeadBlocksQuery(boolean leadInAutomaticInteractionHand) {
        return leadInAutomaticInteractionHand;
    }

    /**
     * Protokollbrücken werten den Sneak-Wert des Interaktionspakets bei
     * Pferden teils nicht aus und versuchen stattdessen aufzusteigen. Der
     * Client kennt den Leinenzustand dabei nicht immer rechtzeitig. Deshalb
     * verwenden alle Pferde den expliziten Sneak-Pfad.
     */
    static Attempt attemptForTarget(Attempt planned, boolean horseTarget) {
        return horseTarget ? Attempt.FALLBACK
                : planned == null ? Attempt.PRIMARY : planned;
    }

    static boolean isLeashedHorseRejection(String message) {
        String normalized = message == null
                ? "" : message.toLowerCase(Locale.ROOT);
        boolean german = normalized.contains("losbinden")
                && (normalized.contains("reiten") || normalized.contains("besteigen"));
        boolean english = (normalized.contains("unleash") || normalized.contains("untie"))
                && (normalized.contains("ride") || normalized.contains("mount"));
        return german || english;
    }

    static PlayerInput neutralInput(PlayerInput input) {
        return withSneak(input, false);
    }

    static PlayerInput assertedSneakInput(PlayerInput input) {
        return withSneak(input, true);
    }

    private static PlayerInput withSneak(PlayerInput input, boolean sneak) {
        if (input == null) {
            input = PlayerInput.DEFAULT;
        }
        return new PlayerInput(
                input.forward(), input.backward(), input.left(), input.right(),
                input.jump(), sneak, false);
    }

    enum Step {
        SYNC_SELECTED_SLOT,
        NEUTRAL_INPUT,
        STOP_SPRINTING,
        ASSERT_SNEAK_INPUT,
        SNEAK_INTERACTION
    }

    enum Attempt {
        PRIMARY,
        FALLBACK;

        Attempt afterTimeout() {
            return this == PRIMARY ? FALLBACK : PRIMARY;
        }
    }

    enum CleanupPhase {
        NONE,
        ASSERT_SNEAK,
        RELEASE_SNEAK;

        CleanupPhase next() {
            return switch (this) {
                case NONE, RELEASE_SNEAK -> NONE;
                case ASSERT_SNEAK -> RELEASE_SNEAK;
            };
        }
    }
}
