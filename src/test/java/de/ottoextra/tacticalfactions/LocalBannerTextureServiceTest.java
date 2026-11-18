package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalBannerTextureServiceTest {
    @TempDir Path temp;

    @Test void rejectsNonPngInputWithoutCreatingAFile() throws Exception {
        Path source = temp.resolve("wrong.txt");
        Files.writeString(source, "not an image");
        Path target = temp.resolve("banners");

        LocalBannerTextureService.BannerImportException error = assertThrows(
                LocalBannerTextureService.BannerImportException.class,
                () -> LocalBannerTextureService.instance().importPng("custom:test", source, target));

        assertEquals("ottoextra_tacticalfactions.banner.error.format", error.translationKey());
    }

    @Test void importsAValidSmallPngUnderASanitizedStableName() throws Exception {
        Path source = temp.resolve("coat.png");
        Files.write(source, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
        Path target = temp.resolve("banners");

        String file = LocalBannerTextureService.instance().importPng("custom:House One", source, target);

        assertEquals("custom_house_one.png", file);
        assertTrue(Files.isRegularFile(target.resolve(file)));
    }
}
