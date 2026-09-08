package de.ottoextra.letter.calibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterCalibrationStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void sessionRoundtripsAndWritesReadableReport() throws Exception {
        LetterCalibrationStore store = new LetterCalibrationStore(tempDir);
        LetterCalibrationSession session = new LetterCalibrationSession();
        session.sessionId = "test-session";
        session.serverAddress = "server.invalid";
        session.modVersion = "test";
        session.sendMode = "PAGE";
        LetterCalibrationSession.CaseResult result =
                new LetterCalibrationSession.CaseResult();
        result.id = "L01";
        result.purpose = "Basis";
        result.status = "CAPTURED";
        result.differences = List.of("PAGE_1_STYLE");
        session.cases.add(result);

        store.saveActiveSessionId(session.sessionId);
        store.save(session);

        assertEquals(session.sessionId, store.loadActiveSessionId());
        assertEquals("L01", store.load(session.sessionId).cases.getFirst().id);
        String report = Files.readString(tempDir.resolve(session.sessionId).resolve("report.md"));
        assertTrue(report.contains("PAGE_1_STYLE"));
        assertTrue(Files.isDirectory(tempDir.resolve(session.sessionId).resolve("screenshots")));
        store.clearActiveSession();
        assertFalse(Files.exists(tempDir.resolve("active-session.txt")));
    }

    @Test
    void keepsV1AndV2ActivePointersSeparate() {
        LetterCalibrationStore store = new LetterCalibrationStore(tempDir);
        store.saveActiveSessionId("LEGACY_16", "old-session");
        store.saveActiveSessionId("V2_HYBRID_8", "v2-session");

        assertEquals("old-session", store.loadActiveSessionId("LEGACY_16"));
        assertEquals("v2-session", store.loadActiveSessionId("V2_HYBRID_8"));
        assertEquals("V2_HYBRID_8", store.loadSelectedSuite());

        store.clearActiveSession("V2_HYBRID_8");
        assertEquals("old-session", store.loadActiveSessionId("LEGACY_16"));
        assertEquals("", store.loadActiveSessionId("V2_HYBRID_8"));
    }

    @Test
    void keepsRefinePointerSeparate() {
        LetterCalibrationStore store = new LetterCalibrationStore(tempDir);
        store.saveActiveSessionId("V2_REFINE_6", "refine-session");

        assertEquals("refine-session", store.loadActiveSessionId("V2_REFINE_6"));
        assertEquals("V2_REFINE_6", store.loadSelectedSuite());
        assertEquals("", store.loadActiveSessionId("V2_HYBRID_8"));
    }

    @Test
    void keepsFinalPointerSeparate() {
        LetterCalibrationStore store = new LetterCalibrationStore(tempDir);
        store.saveActiveSessionId("V2_FINAL_8", "final-session");

        assertEquals("final-session", store.loadActiveSessionId("V2_FINAL_8"));
        assertEquals("V2_FINAL_8", store.loadSelectedSuite());
        assertEquals("", store.loadActiveSessionId("V2_REFINE_6"));
    }

    @Test
    void keepsBoundaryPointerSeparate() {
        LetterCalibrationStore store = new LetterCalibrationStore(tempDir);
        store.saveActiveSessionId("V2_BOUNDARY_15", "boundary-session");

        assertEquals("boundary-session", store.loadActiveSessionId("V2_BOUNDARY_15"));
        assertEquals("V2_BOUNDARY_15", store.loadSelectedSuite());
        assertEquals("", store.loadActiveSessionId("V2_FINAL_8"));
    }
}
