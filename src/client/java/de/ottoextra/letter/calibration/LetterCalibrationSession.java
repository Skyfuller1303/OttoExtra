package de.ottoextra.letter.calibration;

import java.util.ArrayList;
import java.util.List;

public final class LetterCalibrationSession {
    public int schemaVersion = 3;
    public String suiteId = "LEGACY_16";
    public String sessionId = "";
    public long startedAt;
    public long updatedAt;
    public String modVersion = "";
    public String serverAddress = "";
    public double guiScale;
    public String language = "";
    public String sendMode = "";
    public String previousSendMode = "";
    public int maxLinesPerPage;
    public int effectiveCharBudget;
    public int renderedWidthBudget;
    public int boundaryFitWidth = LetterBoundaryCalibration.INITIAL_KNOWN_FIT;
    public int boundarySplitWidth = LetterBoundaryCalibration.INITIAL_KNOWN_SPLIT;
    public int lineCostMin;
    public int lineCostMax = LetterBoundaryCalibration.MAX_ASSUMED_LINE_COST;
    public int inferredLineBreakCost = -1;
    public int currentCase;
    public boolean completed;
    public List<CaseResult> cases = new ArrayList<>();

    public static final class CaseResult {
        public String id = "";
        public String purpose = "";
        public String status = "PENDING";
        public List<String> sourcePages = new ArrayList<>();
        public List<String> commands = new ArrayList<>();
        public List<String> transports = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> intendedPages = new ArrayList<>();
        public List<String> observedPages = new ArrayList<>();
        public List<List<LetterMarkupSemantics.Run>> sourceRuns = new ArrayList<>();
        public List<List<LetterMarkupSemantics.Run>> observedRuns = new ArrayList<>();
        public List<String> differences = new ArrayList<>();
        public String probeKind = "";
        public int targetRenderedWidth;
        public int expectedTransportRenderedWidth;
        public int logicalLineCount;
        public int commandCount;
        public int observedPageCount;
        public int hypothesizedLineCost = -1;
        public String screenshot = "";
        public long sentAt;
        public long sendCompletedAt;
        public long capturedAt;
    }
}
