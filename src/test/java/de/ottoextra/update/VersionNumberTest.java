package de.ottoextra.update;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class VersionNumberTest {
    @Test
    void comparesNormalVersions() {
        assertTrue(VersionNumber.parse("0.1.14").isNewerThan(VersionNumber.parse("0.1.13")));
        assertFalse(VersionNumber.parse("0.1.13").isNewerThan(VersionNumber.parse("0.1.14")));
        assertFalse(VersionNumber.parse("0.1.13").isNewerThan(VersionNumber.parse("0.1.13")));
    }
    @Test
    void acceptsGitHubTagPrefixAndBuildMetadata() {
        VersionNumber version = VersionNumber.parse("v0.1.14+mc1.21.11");
        assertEquals("0.1.14", version.normalized());
        assertTrue(version.isNewerThan(VersionNumber.parse("0.1.13")));
    }
    @Test
    void stableReleaseIsNewerThanPrerelease() {
        assertTrue(VersionNumber.parse("0.1.14")
                .isNewerThan(VersionNumber.parse("0.1.14-beta.2")));
        assertTrue(VersionNumber.parse("0.1.14-beta.2")
                .isNewerThan(VersionNumber.parse("0.1.14-beta.1")));
    }
    @Test
    void missingComponentsAreTreatedAsZero() {
        assertEquals(0, VersionNumber.parse("1.2").compareTo(VersionNumber.parse("1.2.0")));
    }
}
