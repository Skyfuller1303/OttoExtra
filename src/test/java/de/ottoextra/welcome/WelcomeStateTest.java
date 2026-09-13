package de.ottoextra.welcome;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WelcomeStateTest {
    @TempDir Path dir;

    @Test void onePresentationPerReleaseSeriesNotPerHotfix() {
        Path file = dir.resolve("config/welcome.json");
        assertFalse(WelcomeState.wasSeen(file, "0.1.17-q"));
        WelcomeState.markSeen(file, "0.1.17-q");
        assertTrue(WelcomeState.wasSeen(file, "0.1.17"));
        assertTrue(WelcomeState.wasSeen(file, "0.1.17-r+build.2"));
        assertFalse(WelcomeState.wasSeen(file, "0.1.18"));
        assertFalse(Files.exists(file.resolveSibling("welcome.json.tmp")));
    }

    @Test void legacyAcceptedMarkerDoesNotHideNewRelease() throws Exception {
        Path file = dir.resolve("welcome.json");
        Files.writeString(file, "{\"schemaVersion\":1,\"accepted\":true,\"acceptedWithVersion\":\"0.1.17\"}");
        assertFalse(WelcomeState.wasSeen(file, "0.1.17-q"));
        WelcomeState.markSeen(file, "0.1.17-q");
        assertTrue(WelcomeState.wasSeen(file, "0.1.17-q"));
        assertFalse(Files.readString(file).contains("accepted"));
    }

    @Test void invalidAndOversizedMarkersShowIntroAgain() throws Exception {
        Path file = dir.resolve("welcome.json");
        for (String content : new String[]{"null", "{", "{}", "x".repeat(65537)}) {
            Files.writeString(file, content);
            assertFalse(WelcomeState.wasSeen(file, "0.1.17"));
        }
    }

    @Test void developmentVersionsRemainDistinct() {
        assertEquals("0.1.17", WelcomeState.releaseSeries(" 0.1.17-q "));
        assertEquals("dev-one", WelcomeState.releaseSeries("dev-one"));
        assertEquals("0.0.0", WelcomeState.releaseSeries(null));
    }
}
