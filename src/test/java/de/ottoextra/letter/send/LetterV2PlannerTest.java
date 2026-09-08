package de.ottoextra.letter.send;

import de.ottoextra.letter.format.LetterFormattingCodes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterV2PlannerTest {

    private static final ToIntFunction<String> WIDTH = value -> {
        return LetterFormattingCodes.visibleLength(value) * 6;
    };

    @Test
    void preservesZeroOneAndTwoLeadingBlankLinesInPreviewAndCourierTransport() {
        for (int blanks = 0; blanks <= 2; blanks++) {
            String document = "\n".repeat(blanks) + "Hallo";
            for (LetterCommandPlan plan : List.of(
                    LetterV2Planner.plan("letter", List.of(document), WIDTH),
                    LetterV2Planner.planDocument("letter", List.of(document), WIDTH),
                    LetterV2Planner.planFlowing("letter", List.of(document), WIDTH))) {
                assertEquals(List.of(document), plan.predictedPages());
                String transported = (LetterV2Planner.EMPTY_LINE + "\n").repeat(blanks) + "Hallo";
                assertTrue(plan.commands().stream().allMatch(c -> c.startsWith("letter ")));
                assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(execute(plan, List.of())));
            }
        }
    }

    @Test
    void blankLinesSurviveAcrossTransportCommandsWithoutPadding() {
        String document = "\n".repeat(11) + "Hallo";
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter", List.of(document), WIDTH);
        assertEquals(List.of(document), plan.predictedPages());
        assertFalse(plan.transports().contains("V2_BOUNDARY_PADDING"));
        assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(execute(plan, List.of())));
        assertTrue(plan.sendable());
    }

    @Test
    void blankLinesAndFormattingUseActualServerPagination() {
        String document = "§6" + java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(i -> "Z" + i).collect(java.util.stream.Collectors.joining("\n")) + "\n\nHallo";
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter", List.of(document), WIDTH);
        assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(execute(plan, List.of())));
        assertTrue(plan.predictedPages().getLast().contains("Hallo"));
        assertTrue(plan.sendable());
    }

    @Test
    void singlePageUsesOneCourierContentCommand() {
        LetterCommandPlan plan = LetterV2Planner.plan("letter",
                List.of("Hallo Ottonien"), WIDTH);

        assertEquals(List.of("Hallo Ottonien"), plan.predictedPages());
        assertEquals(List.of("V2_CONTENT"), plan.transports());
        assertEquals("letter Hallo Ottonien", plan.commands().getFirst());
        assertTrue(plan.warnings().isEmpty());
    }

    @Test
    void courierDecidesPageBoundariesWithoutResetFillerCommands() {
        String document = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(i -> "Z" + i).collect(java.util.stream.Collectors.joining("\n"));
        LetterCommandPlan plan = LetterV2Planner.plan("letter", List.of(document), WIDTH);
        assertEquals(List.of(String.join("\n", java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> "Z" + i).toList()), "Z13\nZ14\nZ15"), plan.predictedPages());
        assertFalse(plan.transports().contains("V2_BOUNDARY_PADDING"));
        assertEquals(2, execute(plan, List.of()).size());
    }

    @Test
    void localEditorBudgetIsNotMisrepresentedAsServerPageBoundary() {
        String document = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> "Zeile " + i).collect(java.util.stream.Collectors.joining("\n"));
        assertEquals(2, LetterV2Planner.layoutDocumentPages("letter", List.of(document),
                value -> value.isEmpty() ? 0 : 100).size());
        LetterCommandPlan plan = LetterV2Planner.plan("letter", List.of(document),
                value -> value.isEmpty() ? 0 : 100);
        assertEquals(1, plan.predictedPages().size());
        assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(execute(plan, List.of())));
    }

    @Test
    void importedFullPageContinuesOnActualNextServerPage() {
        String full = String.join("\n", java.util.Collections.nCopies(12, "Alt"));
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter", List.of("Neu"), WIDTH, List.of(full));
        assertEquals(List.of(full, "Neu"), plan.predictedPages());
        assertEquals(1, plan.entries().getFirst().predictedPage());
        assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(execute(plan, List.of(full))));
    }

    @Test
    void courierMayDropOnlyItsTransportSuffixWhenTheWordStillFits() {
        String initial = String.join("\n", java.util.Collections.nCopies(11, "Alt"));
        String word = "x".repeat(18);
        var plan = LetterV2Planner.planDocument("letter", List.of(word), WIDTH, List.of(initial));

        assertTrue(plan.sendable());
        assertEquals(List.of("letter " + word), plan.commands());
        assertEquals(List.of(initial + "\n" + word), execute(plan, List.of(initial)));
        assertEquals(List.of(initial + "\n" + word), plan.predictedPages());
    }

    @Test
    void repeatedFullWidthLinesRemainCompleteAtCourierSuffixBoundary() {
        String lines = String.join("\n", java.util.Collections.nCopies(7, "x".repeat(18)));
        for (String marker : List.of("OE-F01", "§lOE-F04§r")) {
            String document = marker + "\n" + lines + "\n" + "i".repeat(10);
            var plan = LetterV2Planner.planDocument("letter", List.of(document), WIDTH);

            assertTrue(plan.sendable(), marker + ": " + plan.warnings());
            assertEquals(document, String.join("\n", plan.predictedPages()));
            assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(execute(plan, List.of())));
        }
    }

    @Test
    void ordinaryBackslashesAreNotDoubled() {
        LetterCommandPlan plan = LetterV2Planner.plan("letter",
                List.of("Pfad C:\\Ottonien\\Briefe"), WIDTH);

        assertTrue(plan.commands().getFirst().contains("C:\\Ottonien\\Briefe"));
        assertFalse(plan.commands().getFirst().contains("C:\\\\Ottonien"));
        assertTrue(plan.warnings().isEmpty());
    }

    @Test
    void literalBackslashNIsRejectedBeforeSending() {
        LetterCommandPlan plan = LetterV2Planner.plan("letter",
                List.of("Wörtlich \\n ist nicht darstellbar"), WIDTH);

        assertEquals(List.of(LetterV2Planner.LITERAL_NEWLINE_WARNING), plan.warnings());
        assertFalse(plan.sendable());
    }

    @Test
    void flowingModeRemovesSoftBreakButKeepsParagraphBreak() {
        LetterCommandPlan plan = LetterV2Planner.planFlowing("letter",
                List.of("Weicher\nUmbruch\n\nNeuer Absatz"), WIDTH);

        assertEquals(List.of("Weicher Umbruch\n\nNeuer Absatz"), plan.predictedPages());
        assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(execute(plan, List.of())));
    }

    @Test
    void canonicalDocumentPreservesManuallyEnteredLineBreaks() {
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter",
                List.of("Erste Zeile\nZweite Zeile"), WIDTH);

        assertEquals(List.of("Erste Zeile\nZweite Zeile"), plan.predictedPages());
        assertEquals("letter Erste Zeile \\nZweite Zeile", plan.commands().getFirst());
        assertEquals(List.of("Erste Zeile \nZweite Zeile "), execute(plan, List.of()));
    }

    @Test
    void canonicalDocumentPreservesEmptyParagraphInCourierBook() {
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter",
                List.of("Erster Absatz\n\nZweiter Absatz"), WIDTH);

        assertEquals(List.of("Erster Absatz\n\nZweiter Absatz"), plan.predictedPages());
        assertEquals("letter Erster Absatz \\n  \\nZweiter Absatz",
                plan.commands().getFirst());
        assertEquals(List.of("Erster Absatz \n  \nZweiter Absatz "),
                execute(plan, List.of()));
    }

    @Test
    void serverPlaceholderIsRejectedInsteadOfSilentlyChangingDocument() {
        assertFalse(LetterV2Planner.planDocument("letter", List.of("%prefix%"), WIDTH).sendable());
    }

    @Test
    void longCanonicalDocumentLosesNoNonWhitespaceCharacterAcrossPages() {
        String document = "Anrede\n\n"
                + java.util.stream.IntStream.rangeClosed(1, 160)
                .mapToObj(i -> "Wort" + i)
                .collect(java.util.stream.Collectors.joining(" "))
                + "\n\nGrußzeile";
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter",
                List.of(document), WIDTH);

        assertTrue(plan.predictedPages().size() > 1);
        assertTrue(plan.predictedPages().stream().anyMatch(page -> page.contains("\n\n")));
        assertEquals(document.replaceAll("\\s+", ""),
                String.join("", plan.predictedPages()).replaceAll("\\s+", ""));

        List<String> actual = execute(plan, List.of());
        assertEquals(plan.predictedPages().size(), actual.size());
        assertEquals(plan.predictedPages(), LetterV2Planner.displayPages(actual));
    }

    @Test
    void trailingBlankLineSurvivesEvenIfLocalLayoutUsesAnotherPage() {
        String document = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> "Z" + i).collect(java.util.stream.Collectors.joining("\n")) + "\n";
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter", List.of(document), WIDTH);
        assertEquals(List.of(document), plan.predictedPages());
    }

    @Test
    void formattingIsEncodedAsCourierAmpersandCode() {
        LetterCommandPlan plan = LetterV2Planner.plan("letter",
                List.of("§6§lGold§r normal"), WIDTH);

        assertEquals("letter &6&lGold&r normal", plan.commands().getFirst());
        assertEquals("§6§lGold§r normal ", execute(plan, List.of()).getFirst());
    }

    @Test
    void everyGeneratedCommandStaysWithinChatLimit() {
        LetterCommandPlan plan = LetterV2Planner.planFlowing("letter",
                List.of("fortlaufender Inhalt ".repeat(200)), WIDTH);

        assertTrue(plan.commands().stream()
                .allMatch(command -> command.length() <= LetterV2Planner.SAFE_CHAT_MESSAGE_LIMIT));
        assertTrue(plan.predictedPages().size() > 1);
    }

    @Test
    void splitsOversizedEditorPagesToObservedServerLimits() {
        String longPage = "i ".repeat(130).stripTrailing();
        LetterCommandPlan plan = LetterV2Planner.planFlowing("letter",
                List.of(longPage, longPage, longPage),
                value -> LetterFormattingCodes.visibleLength(value) * 2);

        assertTrue(plan.predictedPages().size() > 3);
        assertEquals(plan.predictedPages().size(), execute(plan, List.of()).size());
        assertTrue(plan.entries().stream().allMatch(e -> e.transport() == LetterCommandPlan.Transport.V2_CONTENT));
        assertTrue(plan.warnings().isEmpty());
    }

    @Test
    void continuesFromImportedCourierPageState() {
        LetterCommandPlan plan = LetterV2Planner.planFlowing("letter", List.of("Fortsetzung"),
                WIDTH, List.of("Bisher "));

        List<String> actual = execute(plan, List.of("Bisher "));
        assertEquals(1, actual.size());
        assertEquals("Bisher \nFortsetzung ", actual.getFirst());
        assertEquals(0, plan.entries().getFirst().predictedPage());
    }

    @Test
    void formattingSurvivesActualCourierPageBoundaries() {
        String document = "§6" + java.util.stream.IntStream.rangeClosed(1, 30)
                .mapToObj(i -> "Zeile" + i).collect(java.util.stream.Collectors.joining("\n"));
        LetterCommandPlan plan = LetterV2Planner.planDocument("letter", List.of(document), WIDTH);
        assertTrue(plan.sendable());
        assertTrue(plan.predictedPages().size() >= 3);
        for (String page : execute(plan, List.of())) {
            for (var run : de.ottoextra.letter.calibration.LetterMarkupSemantics.runs(page)) {
                if (!run.text().isBlank()) assertEquals('6', run.color());
            }
        }
    }

    @Test
    void importedFormattingContinuesOnNewServerPage() {
        String initial = "§6" + String.join("\n", java.util.Collections.nCopies(12, "Alt"));
        var plan = LetterV2Planner.planDocument("letter", List.of("Weiter"), WIDTH, List.of(initial));
        var actual = execute(plan, List.of(initial));
        assertEquals(2, actual.size());
        assertEquals("§6Weiter ", actual.getLast());
        var reset = LetterV2Planner.planDocument("letter", List.of("§rWeiter"), WIDTH, List.of(initial));
        var runs = de.ottoextra.letter.calibration.LetterMarkupSemantics.runs(execute(reset, List.of(initial)).getLast());
        assertTrue(runs.stream().allMatch(run -> run.color() == null));
    }

    @Test
    void independentSourcePageDoesNotInheritPreviousColorWhenCourierMergesIt() {
        var plan = LetterV2Planner.planDocument("letter", List.of("§6Gold", "Standard"), WIDTH);
        var runs = de.ottoextra.letter.calibration.LetterMarkupSemantics.runs(execute(plan, List.of()).getFirst());
        assertTrue(runs.stream().anyMatch(run -> run.text().contains("Standard") && run.color() == null));
    }

    @Test
    void formattingSurvivesServerSplittingInsideLongNarrowLine() {
        String initial = String.join("\n", java.util.Collections.nCopies(11, "Alt"));
        var plan = LetterV2Planner.planDocument("letter", List.of("§6eins zwei drei vier fünf sechs"),
                text -> LetterFormattingCodes.visibleLength(text), List.of(initial));
        assertTrue(plan.sendable());
        var actual = execute(plan, List.of(initial));
        for (int i = 1; i < actual.size(); i++) {
            for (var run : de.ottoextra.letter.calibration.LetterMarkupSemantics.runs(actual.get(i))) {
                if (!run.text().isBlank()) assertEquals('6', run.color());
            }
        }
        assertTrue(String.join(" ", LetterV2Planner.displayPages(actual)).contains("sechs"));
    }

    private static String materializeEmptyLinesForTransport(String value) {
        String[] lines = value.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (LetterFormattingCodes.visibleLength(lines[i]) == 0) {
                lines[i] += LetterV2Planner.EMPTY_LINE;
            }
        }
        return String.join("\n", lines);
    }

    private static List<String> execute(LetterCommandPlan plan, List<String> initial) {
        List<String> pages = new ArrayList<>(initial);
        for (String command : plan.commands()) {
            pages = CourierNewProtocol.apply(pages,
                    command.substring(command.indexOf(' ') + 1));
        }
        return pages;
    }
}
