package de.ottoextra.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class PackRecoveryTest {
    @TempDir Path directory;

    @Test
    void matchingStateRequiresAnExistingUsableZip() throws Exception {
        var state = new PackState();
        state.version = "v1";
        state.sha256 = "abc";
        var manifest = new PackManifest("v1", "https://example.invalid/pack.zip", "abc", null, null, null, null);
        Path file = directory.resolve("Ottonien.zip");
        assertFalse(ResourcePackUpdater.isCurrent(manifest, state, file));
        Files.writeString(file, "not a zip");
        assertFalse(ResourcePackUpdater.isCurrent(manifest, state, file));
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("unrelated.txt"));
            zip.write(1);
        }
        assertFalse(ResourcePackUpdater.isCurrent(manifest, state, file));
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write("{\"pack\":{\"pack_format\":75,\"description\":\"test\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertTrue(ResourcePackUpdater.isCurrent(manifest, state, file));
        assertTrue(ResourcePackUpdater.isCurrent(new PackManifest("v1", null, null, null, null, null, null), state, file));
        state.version = "v0";
        assertFalse(ResourcePackUpdater.isCurrent(new PackManifest("v1", null, null, null, null, null, null), state, file));
    }

    @Test
    void replacingActivePackReloadsEvenWhenAutoEnableIsOff() {
        assertEquals(new PackInstaller.Decision(false, true), PackInstaller.decide(false, true, true, true, true, true));
        assertEquals(new PackInstaller.Decision(false, true), PackInstaller.decide(true, true, true, true, true, true));
        assertEquals(new PackInstaller.Decision(false, false), PackInstaller.decide(true, false, true, true, true, true));
    }

    @Test
    void persistedVanillaDisableWinsAfterInstallationButNotOnFirstInstall() {
        assertEquals(new PackInstaller.Decision(false, false), PackInstaller.decide(true, false, true, true, false, false));
        assertEquals(new PackInstaller.Decision(false, false), PackInstaller.decide(true, true, true, true, false, false));
        assertEquals(new PackInstaller.Decision(true, true), PackInstaller.decide(true, true, true, false, false, false));
        assertEquals(new PackInstaller.Decision(true, true), PackInstaller.decide(true, true, false, true, false, false));
    }
}
