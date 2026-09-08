package de.ottoextra.letter.calibration;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterCalibrationSafetyTest {

    @Test
    void acceptsOnlyConfiguredLetterRootAndRejectsDeliveryCommands() {
        OttoExtraConfig config = new OttoExtraConfig();
        assertTrue(LetterCalibrationService.safe(config, "letter OE-L01"));
        assertFalse(LetterCalibrationService.safe(config, "post Testkonto"));
        assertFalse(LetterCalibrationService.safe(config, "verkünden"));
        assertFalse(LetterCalibrationService.safe(config, "msg Testkonto OE-L01"));
        assertFalse(LetterCalibrationService.safe(config, "letter einzelnes § Zeichen"));
    }

    @Test
    void blocksCalibrationWhenLetterCommandEqualsPostCommand() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.letter.letterCommand = "post";
        assertFalse(LetterCalibrationService.safe(config, "post OE-L01"));
    }

    @Test
    void productionBuilderProducesOnlyLetterCommandsForCorpus() {
        OttoExtraConfig config = new OttoExtraConfig();
        for (var test : LetterCalibrationCorpus.all()) {
            LetterDraft draft = new LetterDraft();
            draft.pages = new java.util.ArrayList<>(test.pages());
            var plan = LetterServices.buildCalibrationPlan(config, draft);
            var commands = plan.commands();
            if (!plan.warnings().isEmpty()) {
                assertTrue(plan.warnings().contains("LITERAL_BACKSLASH_N"), test.id());
                assertTrue(commands.isEmpty(), test.id());
                continue;
            }
            assertFalse(commands.isEmpty(), test.id());
            assertTrue(commands.stream().allMatch(command ->
                    LetterCalibrationService.safe(config, command)),
                    test.id() + " " + commands);
            assertTrue(commands.stream().noneMatch(command -> command.startsWith("post ")),
                    test.id());
        }
    }

    @Test
    void v2BuilderProducesOnlySafeLetterCommandsForNewCorpus() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.letter.sendMode = "V2";
        for (var test : LetterCalibrationCorpus.v2()) {
            LetterDraft draft = new LetterDraft();
            draft.pages = new java.util.ArrayList<>(test.pages());
            var plan = LetterServices.buildCalibrationPlan(config, draft);
            if ("V206".equals(test.id())) {
                assertTrue(plan.warnings().contains("LITERAL_BACKSLASH_N"));
                assertFalse(plan.sendable());
                assertTrue(plan.commands().isEmpty());
                continue;
            }
            assertFalse(plan.commands().isEmpty(), test.id());
            assertTrue(plan.commands().stream().allMatch(command ->
                    LetterCalibrationService.safe(config, command)),
                    test.id() + " " + plan.commands());
            assertTrue(plan.commands().stream().allMatch(command -> command.length() <= 256),
                    test.id() + " " + plan.commands());
            assertTrue(plan.commands().size() == plan.transports().size(), test.id());
            if ("V206".equals(test.id())) {
                assertTrue(plan.warnings().contains("LITERAL_BACKSLASH_N"));
            } else {
                assertTrue(plan.warnings().isEmpty(), test.id() + " " + plan.warnings());
            }
        }
    }

    @Test
    void refineBuilderProducesOnlySafeLetterCommands() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.letter.sendMode = "V2";
        for (var test : LetterCalibrationCorpus.v2Refine()) {
            LetterDraft draft = new LetterDraft();
            draft.pages = new java.util.ArrayList<>(test.pages());
            var plan = LetterServices.buildCalibrationPlan(config, draft);
            assertFalse(plan.commands().isEmpty(), test.id());
            assertTrue(plan.commands().stream().allMatch(command ->
                    LetterCalibrationService.safe(config, command)), test.id());
            assertTrue(plan.commands().stream().allMatch(command -> command.length() <= 256),
                    test.id());
        }
    }
}
