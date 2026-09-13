package de.ottoextra.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class PackRecoveryTest {
    @TempDir Path directory;

    @Test
    void directPackCacheRequiresTheInstalledSourceIncludingAfterStateRoundTrip() throws Exception {
        Path file = directory.resolve("Ottonien.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        var sourceA = java.net.URI.create("https://example.invalid/a.zip");
        var sourceB = java.net.URI.create("https://example.invalid/b.zip");
        var state = new PackState();
        state.version = "direct";
        assertFalse(ResourcePackUpdater.isDirectCurrent(sourceA, state, file),
                "Old installs with an unknown source need one download");
        state.directSource = sourceA.toString();
        state = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(state), PackState.class);
        assertTrue(ResourcePackUpdater.isDirectCurrent(sourceA, state, file));
        assertFalse(ResourcePackUpdater.isDirectCurrent(sourceB, state, file));
        assertFalse(ResourcePackUpdater.isDirectCurrent(sourceA, state, directory.resolve("missing.zip")));
        Files.writeString(file, "broken");
        assertFalse(ResourcePackUpdater.isDirectCurrent(sourceA, state, file));
    }

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
        assertEquals(new PackInstaller.Decision(false, false), PackInstaller.decide(true, true, false, true, false, false));
    }
}
