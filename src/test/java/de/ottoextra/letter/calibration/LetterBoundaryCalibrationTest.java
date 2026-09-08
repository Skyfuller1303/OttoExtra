package de.ottoextra.letter.calibration;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.send.LetterV2Planner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterBoundaryCalibrationTest {

    private static final ToIntFunction<String> TEST_WIDTH =
            LetterBoundaryCalibrationTest::formattedWidth;

    @Test
    void adaptiveSeriesResolvesBoundaryAndLineCost() {
        LetterCalibrationSession session = session();

        for (int i = 0; i < 7; i++) {
            LetterBoundaryCalibration.prepare(session, i, TEST_WIDTH);
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            assertEquals(9, result.logicalLineCount);
            capture(session, result, result.targetRenderedWidth <= 856);
        }

        assertEquals(856, session.boundaryFitWidth);
        assertEquals(857, session.boundarySplitWidth);

        for (int i = 7; i < 11; i++) {
            LetterBoundaryCalibration.prepare(session, i, TEST_WIDTH);
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            int maximumForLineCount = 856 - (result.logicalLineCount - 9) * 2;
            capture(session, result, result.targetRenderedWidth <= maximumForLineCount);
        }

        assertEquals(2, session.lineCostMin);
        assertEquals(2, session.lineCostMax);
        assertEquals(2, session.inferredLineBreakCost);
    }

    @Test
    void controlsUseExactWidthsAndBypassProductionPagination() {
        LetterCalibrationSession session = resolvedSession();

        for (int i = 11; i < 15; i++) {
            LetterBoundaryCalibration.prepare(session, i, TEST_WIDTH);
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            String document = result.sourcePages.getFirst();
            int baseline = LetterBoundaryCalibration.renderedSourceWidth(document, TEST_WIDTH)
                    + TEST_WIDTH.applyAsInt(" ");
            assertEquals(result.targetRenderedWidth, baseline, result.id);
            assertTrue(document.split("\n", -1).length <= 14, result.id);
            for (String line : document.split("\n", -1)) {
                assertTrue(TEST_WIDTH.applyAsInt(line) <= LetterV2Planner.BOOK_PAGE_WIDTH,
                        result.id + " " + line);
            }

            var plan = LetterBoundaryCalibration.rawPlan("letter", result);
            assertEquals(result.commandCount, plan.commands().size(), result.id);
            assertTrue(plan.commands().stream().allMatch(command -> command.length() <= 256),
                    result.id + " " + plan.commands());
            assertTrue(plan.commands().stream().noneMatch(command -> command.indexOf('§') >= 0),
                    result.id);
        }

        assertEquals(1, session.cases.get(11).commandCount);
        assertEquals(3, session.cases.get(12).commandCount);
        assertEquals(3, session.cases.get(13).commandCount);
        assertTrue(session.cases.get(13).sourcePages.getFirst().contains("\n\n"));
        assertTrue(session.cases.get(14).sourcePages.getFirst().contains("§l"));
        assertTrue(session.cases.get(12).expectedTransportRenderedWidth
                > session.cases.get(11).targetRenderedWidth);
    }

    @Test
    void eachGeneratedProbeContainsItsMarkerAndIsSendable() {
        LetterCalibrationSession session = resolvedSession();
        OttoExtraConfig config = new OttoExtraConfig();
        for (int i = 11; i < 15; i++) {
            LetterBoundaryCalibration.prepare(session, i, TEST_WIDTH);
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            assertTrue(result.sourcePages.getFirst().contains("OE-" + result.id));
            var commands = LetterBoundaryCalibration.rawPlan("letter", result).commands();
            assertFalse(commands.isEmpty());
            assertTrue(commands.stream().allMatch(command ->
                    LetterCalibrationService.safe(config, command)), result.id);
        }
    }

    @Test
    void firstBoundaryProbeStaysOneRawCommandAboveOld816Limit() {
        LetterCalibrationSession session = session();
        LetterBoundaryCalibration.prepare(session, 0, TEST_WIDTH);
        LetterCalibrationSession.CaseResult result = session.cases.getFirst();

        assertEquals(852, result.targetRenderedWidth);
        assertEquals(1, LetterBoundaryCalibration.rawPlan("letter", result).commands().size());
        assertEquals(1, LetterBoundaryCalibration.rawPlan("letter", result)
                .predictedPages().size());
    }

    private static LetterCalibrationSession resolvedSession() {
        LetterCalibrationSession session = session();
        for (int i = 0; i < 7; i++) {
            LetterBoundaryCalibration.prepare(session, i, TEST_WIDTH);
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            capture(session, result, result.targetRenderedWidth <= 856);
        }
        for (int i = 7; i < 11; i++) {
            LetterBoundaryCalibration.prepare(session, i, TEST_WIDTH);
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            int maximumForLineCount = 856 - (result.logicalLineCount - 9) * 2;
            capture(session, result, result.targetRenderedWidth <= maximumForLineCount);
        }
        return session;
    }

    private static LetterCalibrationSession session() {
        LetterCalibrationSession session = new LetterCalibrationSession();
        session.suiteId = LetterCalibrationService.SUITE_V2_BOUNDARY;
        for (var test : LetterCalibrationCorpus.v2Boundary()) {
            LetterCalibrationSession.CaseResult result =
                    new LetterCalibrationSession.CaseResult();
            result.id = test.id();
            result.purpose = test.purpose();
            result.sourcePages = new ArrayList<>(test.pages());
            session.cases.add(result);
        }
        return session;
    }

    private static void capture(LetterCalibrationSession session,
                                LetterCalibrationSession.CaseResult result, boolean fits) {
        result.observedPages = fits ? List.of(result.sourcePages.getFirst())
                : List.of("Seite 1", "Seite 2");
        result.status = "CAPTURED";
        LetterBoundaryCalibration.recordObservation(session, result);
    }

    private static int formattedWidth(String value) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '§' && i + 1 < value.length()) {
                char code = Character.toLowerCase(value.charAt(++i));
                if (code == 'l') {
                    bold = true;
                } else if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) {
                    bold = false;
                }
                continue;
            }
            int glyph = switch (ch) {
                case 'i', '.', '!' -> 2;
                case 'l' -> 3;
                case 't', ' ' -> 4;
                case 'f' -> 5;
                case '\u2007' -> 4;
                default -> 6;
            };
            width += glyph + (bold && ch != ' ' ? 1 : 0);
        }
        return width;
    }
}
