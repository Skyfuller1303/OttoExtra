package de.ottoextra.letter.calibration;

import de.ottoextra.letter.format.LetterFormattingCodes;
import de.ottoextra.letter.send.LetterCommandPlan;
import de.ottoextra.letter.send.LetterV2Planner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Erzeugt die adaptive Rohmessung für die noch unbekannte Server-Pagination.
 * Die Payloads werden absichtlich nicht durch den produktiven V2-Planer geführt.
 */
public final class LetterBoundaryCalibration {

    public static final int CASE_COUNT = 15;
    public static final int INITIAL_KNOWN_FIT = 818;
    public static final int INITIAL_KNOWN_SPLIT = 887;
    public static final int MAX_ASSUMED_LINE_COST = 15;

    private static final int BOUNDARY_CASES = 7;
    private static final int LINE_COST_CASES = 4;
    private static final int[] LINE_COST_LINE_COUNTS = {10, 12, 11, 10};
    private static final char[] FILLER = {'x', 'f', 't', 'l', 'i', '.'};

    private LetterBoundaryCalibration() {
    }

    public static boolean isBoundarySuite(String suiteId) {
        return LetterCalibrationService.SUITE_V2_BOUNDARY.equals(suiteId);
    }

    public static void prepare(LetterCalibrationSession session, int index,
                               ToIntFunction<String> width) {
        if (session == null || index < 0 || index >= CASE_COUNT) {
            throw new IllegalArgumentException("Ungültige Grenzprobe");
        }
        refreshInference(session);
        LetterCalibrationSession.CaseResult result = session.cases.get(index);
        int target;
        int lineCount;
        int blankLine = -1;
        boolean formatted = false;

        result.probeKind = "BOUNDARY";
        result.hypothesizedLineCost = -1;
        if (index < BOUNDARY_CASES) {
            target = midpoint(session.boundaryFitWidth, session.boundarySplitWidth);
            lineCount = 9;
        } else if (index < BOUNDARY_CASES + LINE_COST_CASES) {
            int lineIndex = index - BOUNDARY_CASES;
            lineCount = LINE_COST_LINE_COUNTS[lineIndex];
            int hypothesis = midpoint(session.lineCostMin, session.lineCostMax);
            target = session.boundaryFitWidth
                    - (lineCount - 9) * hypothesis;
            result.probeKind = "LINE_COST";
            result.hypothesizedLineCost = hypothesis;
        } else {
            lineCount = 10;
            int lineCost = session.inferredLineBreakCost >= 0
                    ? session.inferredLineBreakCost : session.lineCostMin;
            target = session.boundaryFitWidth - lineCost;
            result.probeKind = switch (index) {
                case 11 -> "CONTROL_SINGLE";
                case 12 -> "CONTROL_THREE_COMMANDS";
                case 13 -> "CONTROL_BLANK_LINE";
                default -> "CONTROL_FORMATTED";
            };
            blankLine = index == 13 ? 4 : -1;
            formatted = index == 14;
        }

        String document = exactWidthDocument(result.id, lineCount, blankLine,
                formatted, target, width);
        result.targetRenderedWidth = target;
        result.logicalLineCount = lineCount;
        result.sourcePages = new ArrayList<>(List.of(document));
        result.sourceRuns = new ArrayList<>(List.of(
                new ArrayList<>(LetterMarkupSemantics.runs(document))));
        result.expectedTransportRenderedWidth = transportWidth(result, width);
        result.commandCount = expectedCommandCount(result);
    }

    public static void recordObservation(LetterCalibrationSession session,
                                         LetterCalibrationSession.CaseResult result) {
        if (session == null || result == null || !isBoundarySuite(session.suiteId)) {
            return;
        }
        result.observedPageCount = result.observedPages == null
                ? 0 : result.observedPages.size();
        refreshInference(session);
    }

    public static void refreshInference(LetterCalibrationSession session) {
        if (session == null || !isBoundarySuite(session.suiteId)) {
            return;
        }
        int fit = INITIAL_KNOWN_FIT;
        int split = INITIAL_KNOWN_SPLIT;
        for (int i = 0; i < Math.min(BOUNDARY_CASES, session.cases.size()); i++) {
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            if (!captured(result) || result.targetRenderedWidth <= 0) {
                continue;
            }
            if (result.observedPageCount <= 1) {
                fit = Math.max(fit, result.targetRenderedWidth);
            } else {
                split = Math.min(split, result.targetRenderedWidth);
            }
        }
        session.boundaryFitWidth = fit;
        session.boundarySplitWidth = split;

        int lineMin = 0;
        int lineMax = MAX_ASSUMED_LINE_COST;
        int end = Math.min(BOUNDARY_CASES + LINE_COST_CASES, session.cases.size());
        for (int i = BOUNDARY_CASES; i < end; i++) {
            LetterCalibrationSession.CaseResult result = session.cases.get(i);
            if (!captured(result) || result.hypothesizedLineCost < 0) {
                continue;
            }
            if (result.observedPageCount <= 1) {
                lineMax = Math.min(lineMax, result.hypothesizedLineCost);
            } else {
                lineMin = Math.max(lineMin, result.hypothesizedLineCost + 1);
            }
        }
        session.lineCostMin = lineMin;
        session.lineCostMax = lineMax;
        session.inferredLineBreakCost = lineMin == lineMax ? lineMin : -1;
    }

    public static LetterCommandPlan rawPlan(String letterCommand,
                                            LetterCalibrationSession.CaseResult result) {
        String root = letterCommand == null ? "" : letterCommand.trim();
        String document = String.join("\n", result.sourcePages);
        List<LetterCommandPlan.PlannedCommand> entries = new ArrayList<>();
        if ("CONTROL_THREE_COMMANDS".equals(result.probeKind)) {
            String[] lines = document.split("\n", -1);
            int first = Math.min(4, lines.length);
            int second = Math.min(7, lines.length);
            addPage(entries, root, join(lines, 0, first));
            addPage(entries, root, join(lines, first, second));
            addPage(entries, root, join(lines, second, lines.length));
        } else if ("CONTROL_BLANK_LINE".equals(result.probeKind)) {
            String[] lines = document.split("\n", -1);
            int blank = firstBlank(lines);
            addPage(entries, root, join(lines, 0, blank));
            entries.add(new LetterCommandPlan.PlannedCommand(
                    root + " " + LetterV2Planner.EMPTY_LINE,
                    LetterCommandPlan.Transport.V2_LEGACY_EMPTY, 0));
            addPage(entries, root, join(lines, blank + 1, lines.length));
        } else {
            addPage(entries, root, document);
        }
        return new LetterCommandPlan(entries, List.copyOf(result.sourcePages), List.of());
    }

    static int renderedSourceWidth(String document, ToIntFunction<String> width) {
        int rendered = 0;
        for (String line : document.split("\n", -1)) {
            rendered += width.applyAsInt(line);
        }
        return rendered;
    }

    private static String exactWidthDocument(String id, int lineCount, int blankLine,
                                             boolean formatted, int target,
                                             ToIntFunction<String> width) {
        int sourceTarget = target - width.applyAsInt(" ");
        List<StringBuilder> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            if (i == blankLine) {
                lines.add(new StringBuilder());
            } else if (i == 0) {
                String marker = "OE-" + id;
                lines.add(new StringBuilder(formatted
                        ? "§6§l" + marker + "§r" : marker));
            } else {
                lines.add(new StringBuilder("i"));
            }
        }

        int current = lines.stream().mapToInt(line -> width.applyAsInt(line.toString())).sum();
        if (current > sourceTarget) {
            throw new IllegalStateException("Zielbreite zu klein für " + id);
        }
        while (current < sourceTarget) {
            int remaining = sourceTarget - current;
            AppendChoice choice = findChoice(lines, remaining, width);
            if (choice == null) {
                throw new IllegalStateException("Zielbreite " + target
                        + " für " + id + " nicht exakt darstellbar (Rest "
                        + remaining + ")");
            }
            lines.get(choice.line()).append(choice.glyph());
            current += choice.delta();
        }

        String document = String.join("\n", lines.stream().map(StringBuilder::toString).toList());
        int measured = renderedSourceWidth(document, width) + width.applyAsInt(" ");
        if (measured != target) {
            throw new IllegalStateException("Grenzprobe " + id + " misst " + measured
                    + " statt " + target);
        }
        return document;
    }

    private static AppendChoice findChoice(List<StringBuilder> lines, int remaining,
                                           ToIntFunction<String> width) {
        AppendChoice best = null;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            StringBuilder line = lines.get(lineIndex);
            if (line.isEmpty()) {
                continue;
            }
            String before = line.toString();
            int beforeWidth = width.applyAsInt(before);
            for (char glyph : FILLER) {
                int afterWidth = width.applyAsInt(before + glyph);
                int delta = afterWidth - beforeWidth;
                if (delta <= 0 || delta > remaining || afterWidth > LetterV2Planner.BOOK_PAGE_WIDTH
                        || remaining - delta == 1) {
                    continue;
                }
                if (best == null || delta > best.delta()) {
                    best = new AppendChoice(lineIndex, glyph, delta);
                }
            }
        }
        return best;
    }

    private static int transportWidth(LetterCalibrationSession.CaseResult result,
                                      ToIntFunction<String> width) {
        String document = String.join("\n", result.sourcePages);
        int value = renderedSourceWidth(document, width);
        if ("CONTROL_THREE_COMMANDS".equals(result.probeKind)) {
            return value + 3 * width.applyAsInt(" ");
        }
        if ("CONTROL_BLANK_LINE".equals(result.probeKind)) {
            return value + width.applyAsInt(LetterV2Planner.EMPTY_LINE)
                    + 3 * width.applyAsInt(" ");
        }
        return value + width.applyAsInt(" ");
    }

    private static int expectedCommandCount(LetterCalibrationSession.CaseResult result) {
        return "CONTROL_THREE_COMMANDS".equals(result.probeKind)
                || "CONTROL_BLANK_LINE".equals(result.probeKind) ? 3 : 1;
    }

    private static int midpoint(int lower, int upper) {
        return lower + (upper - lower) / 2;
    }

    private static boolean captured(LetterCalibrationSession.CaseResult result) {
        return result != null && result.status != null && result.status.startsWith("CAPTURED")
                && result.observedPageCount > 0;
    }

    private static int firstBlank(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                return i;
            }
        }
        throw new IllegalStateException("Leerzeilenprobe enthält keine Leerzeile");
    }

    private static String join(String[] lines, int from, int to) {
        return String.join("\n", java.util.Arrays.copyOfRange(lines, from, to));
    }

    private static void addPage(List<LetterCommandPlan.PlannedCommand> entries,
                                String root, String payload) {
        String ampersand = LetterFormattingCodes.sectionToAmpersand(payload);
        entries.add(new LetterCommandPlan.PlannedCommand(
                root + " " + encodePagePayload(ampersand),
                LetterCommandPlan.Transport.V2_PAGE_BLOCK, 0));
    }

    private static String encodePagePayload(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\\') {
                out.append("\\\\");
            } else if (ch == '\n') {
                out.append("\\n");
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private record AppendChoice(int line, char glyph, int delta) {
    }
}
