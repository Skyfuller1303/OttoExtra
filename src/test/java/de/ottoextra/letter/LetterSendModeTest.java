package de.ottoextra.letter;

import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LetterSendModeTest {

    @Test
    void parsesAllModesAndFallsBackToPage() {
        assertEquals(LetterSendMode.LEGACY, LetterSendMode.from("legacy"));
        assertEquals(LetterSendMode.PAGE, LetterSendMode.from("PAGE"));
        assertEquals(LetterSendMode.V2, LetterSendMode.from(" v2 "));
        assertEquals(LetterSendMode.PAGE, LetterSendMode.from("unknown"));
    }

    @Test
    void announcementsUseCourierV2WhenSelected() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.letter.sendMode = "V2";
        LetterDraft draft = new LetterDraft();
        draft.pages = new ArrayList<>(java.util.List.of("Erste Seite", "Zweite Seite"));

        var plan = LetterServices.buildAnnouncementPlanForTest(config, draft);

        assertEquals("V2_CONTENT", plan.transports().getFirst());
        assertTrue(plan.transports().stream().noneMatch("PAGE"::equals));
    }

    @Test
    void v2PlanUsesCanonicalDocumentInsteadOfGeneratedEditorPages() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.letter.sendMode = "V2";
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("veraltete Darstellung"));
        draft.meta.flowingDocument = "Kanonischer Inhalt über mehrere Seiten";
        draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;

        var plan = LetterServices.buildAnnouncementPlanForTest(config, draft);

        assertEquals("Kanonischer Inhalt über mehrere Seiten",
                String.join(" ", plan.predictedPages()).replace('\n', ' '));
        assertEquals(plan.predictedPages(), LetterServices.previewPages(config, draft));
        assertTrue(plan.commands().stream().noneMatch(command -> command.contains("veraltete")));
    }

    @Test
    void importedPreviewIncludesExistingPagesAndOnlyUnlockedContinuation() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.letter.sendMode = "V2";
        LetterDraft draft = LetterDraft.empty();
        draft.meta.importedFromBook = true;
        draft.meta.lockedPages = 1;
        draft.meta.lockedOffset = "Bisher \n".length();
        draft.meta.courierInitialPages = new ArrayList<>(List.of("Alt", "Bisher "));
        draft.pages = new ArrayList<>(List.of("Alt", "Bisher \nFortsetzung"));

        assertEquals(List.of("Alt", "Bisher\nFortsetzung"), LetterServices.previewPages(config, draft));
        assertEquals(draft.pages, V2DraftFlow.sourcePages(draft));
    }

    @Test
    void allEditorModesUseTheSameCourierProtocolAndPreserveBlankLines() {
        OttoExtraConfig config = new OttoExtraConfig();
        LetterDraft draft = new LetterDraft();
        draft.pages = new ArrayList<>(java.util.List.of("A\n\nB"));

        for (String mode : List.of("LEGACY", "PAGE", "V2")) {
            config.letter.sendMode = mode;
            assertEquals(List.of("letter A \\n" + LetterServices.EMPTY_LINE + " \\nB"),
                    LetterServices.buildCalibrationCommands(config, draft), mode);
            assertEquals(List.of("A\n\nB"), LetterServices.previewPages(config, draft), mode);
        }
    }

    @Test
    void pageAndLegacyModesAlsoBlockLiteralBackslashNInsteadOfSilentlyChangingText() {
        OttoExtraConfig config = new OttoExtraConfig();
        LetterDraft draft = LetterDraft.empty();
        draft.pages = List.of("Literal \\n bleibt Text");
        for (String mode : List.of("LEGACY", "PAGE", "V2")) {
            config.letter.sendMode = mode;
            var plan = LetterServices.buildAnnouncementPlanForTest(config, draft);
            assertFalse(plan.sendable(), mode);
            assertTrue(plan.warnings().contains("LITERAL_BACKSLASH_N"), mode);
        }
    }

    @Test
    void validatesRecipientAsOneMinecraftAccountArgument() {
        assertTrue(LetterServices.validRecipient("Alice_123"));
        assertFalse(LetterServices.validRecipient("Alice Extra"));
        assertFalse(LetterServices.validRecipient("Alice\npost Bob"));
        assertFalse(LetterServices.validRecipient(""));
        assertFalse(LetterServices.validRecipient(null));
    }

    @Test
    void migratesExistingConfigToV2ExactlyOnce() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.letter.sendMode = "PAGE";
        config.letter.courierV2Migration = 0;

        config.repair();
        assertEquals("V2", config.letter.sendMode);
        assertEquals(1, config.letter.courierV2Migration);

        config.letter.sendMode = "PAGE";
        config.repair();
        assertEquals("PAGE", config.letter.sendMode);
    }
}
