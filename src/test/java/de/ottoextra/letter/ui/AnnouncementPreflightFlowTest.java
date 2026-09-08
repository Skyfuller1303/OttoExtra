package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.V2DraftFlow;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AnnouncementPreflightFlowTest {
    @Test
    void writesOnlyAfterConfirmationUsingOptimizedPagesInPageAndV2Mode() {
        for (String mode : List.of("PAGE", "V2")) {
            var config = new OttoExtraConfig();
            config.letter.sendMode = mode;
            config.letter.announcementSafeCharsPerLine = 17;
            var draft = LetterDraft.empty();
            draft.pages = new ArrayList<>(List.of("abcdefghijklmnopqr"));
            if (mode.equals("V2")) {
                draft.meta.flowingDocument = draft.pages.getFirst();
                draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;
            }
            var screen = new AnnouncementPreflightScreen.Flow(config, draft);
            AtomicInteger starts = new AtomicInteger();
            var before = LetterServices.buildPreflightAnnouncementPlan(config, draft);
            screen.optimize();
            assertEquals(0, starts.get());
            assertEquals(List.of("abcdefghijklmnopqr"), draft.pages,
                    "Preflight must not mutate the shared pending/editor draft");
            assertTrue(screen.confirm((cfg, finalDraft) -> {
                starts.incrementAndGet();
                assertNotSame(draft, finalDraft);
                assertNotSame(draft.meta, finalDraft.meta);
                assertEquals(List.of("abcdefghijklmnopq\nr"), finalDraft.pages);
                var plan = LetterServices.buildPreflightAnnouncementPlan(cfg, finalDraft);
                assertEquals(finalDraft.pages, plan.predictedPages());
                assertNotEquals(before.commands(), plan.commands());
                assertTrue(plan.commands().stream().anyMatch(c -> c.contains("abcdefghijklmnopq \\nr")));
                return true;
            }));
            assertEquals(1, starts.get());
        }
    }

    @Test
    void invalidFinalDraftDoesNotStartWriting() {
        var config = new OttoExtraConfig();
        var draft = LetterDraft.empty();
        var screen = new AnnouncementPreflightScreen.Flow(config, draft);
        assertFalse(screen.confirm((cfg, value) -> { fail("write started"); return true; }));
    }

    @Test
    void alreadyWrittenLetterUsesActualPagesAndCannotSilentlyOptimizeBeforeSubmit() {
        var config = new OttoExtraConfig();
        config.letter.announcementAutoOptimizeEnabled = true;
        config.letter.announcementSafeCharsPerLine = 17;
        // This cannot be replanned as a write, but submitting an existing book is valid.
        config.letter.letterCommand = "x".repeat(300);
        var pending = LetterDraft.empty();
        pending.pages = new ArrayList<>(List.of("Veralteter Entwurf"));
        var actualPages = List.of("abcdefghijklmnopqr ");
        var flow = new AnnouncementPreflightScreen.Flow(config, pending, ignored -> actualPages);

        assertFalse(flow.canOptimize());
        flow.optimize();
        assertEquals(List.of("Veralteter Entwurf"), pending.pages);
        assertTrue(flow.confirm((cfg, submitted) -> {
            assertEquals(List.of("abcdefghijklmnopqr"), submitted.pages,
                    "Preflight must submit the displayed existing book, without optimization");
            assertEquals(pending.meta.draftId, submitted.meta.draftId);
            return true;
        }));
    }

    @Test
    void changedPreparedBookInvalidatesThePreflightSnapshot() {
        var config = new OttoExtraConfig();
        var draft = LetterDraft.empty();
        var actual = new AtomicReference<>(List.of("Geschrieben "));
        var flow = new AnnouncementPreflightScreen.Flow(config, draft, ignored -> actual.get());

        actual.set(List.of("Anderer Inhalt "));
        assertFalse(flow.confirm((cfg, submitted) -> {
            fail("Changed prepared book must not be submitted under the old preflight");
            return true;
        }));
    }

    @Test
    void mutatedPreparedPageListCannotChangeTheFrozenPreflight() {
        var config = new OttoExtraConfig();
        var draft = LetterDraft.empty();
        var actual = new ArrayList<>(List.of("Geschrieben "));
        var flow = new AnnouncementPreflightScreen.Flow(config, draft, ignored -> actual);

        actual.set(0, "Nachträglich geändert ");
        assertFalse(flow.confirm((cfg, submitted) -> {
            fail("Prepared pages must be snapshotted, not shared by reference");
            return true;
        }));
    }

    @Test
    void absentPreparedSnapshotDoesNotNeedAMinecraftClient() {
        assertNull(LetterServices.snapshotPreparedPages(LetterDraft.empty()));
        assertNull(LetterServices.snapshotPreparedPages(null));
    }
}
