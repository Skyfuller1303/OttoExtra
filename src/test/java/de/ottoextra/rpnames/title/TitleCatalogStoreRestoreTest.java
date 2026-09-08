package de.ottoextra.rpnames.title;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleCatalogStoreRestoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void invalidRestoredCatalogKeepsPreviousUsableModelAndIndex() throws Exception {
        Path file = temporaryDirectory.resolve("title-catalog.json");
        Files.writeString(file, """
                {"titles":[{"id":"graefin","title":"Gräfin","variants":["Graefin"]}]}
                """);
        TitleCatalogStore catalog = new TitleCatalogStore();
        catalog.reloadAfterRestore(file);
        var previousEntries = catalog.all();
        var previousTitle = catalog.find("Graefin").orElseThrow();

        for (String invalid : new String[]{"{\"titles\":[null]}",
                "{\"titles\":null}", "{\"categories\":null}",
                "{\"categories\":{\"adel\":null}}", "null", ""}) {
            Files.writeString(file, invalid);

            assertThrows(IllegalStateException.class, () -> catalog.reloadAfterRestore(file));

            assertSame(previousEntries, catalog.all());
            assertSame(previousTitle, catalog.find("Graefin").orElseThrow());
            assertEquals("Gräfin", catalog.displayForm("Graefin"));
            assertEquals(invalid, Files.readString(file));
        }
    }

    @Test
    void validRestoredCatalogRefreshesTitleIndex() throws Exception {
        Path file = temporaryDirectory.resolve("title-catalog.json");
        TitleCatalogStore catalog = new TitleCatalogStore();
        Files.writeString(file, "{\"titles\":[{\"id\":\"alt\",\"title\":\"Alt\"}]}");
        catalog.reloadAfterRestore(file);
        Files.writeString(file, "{\"titles\":[{\"id\":\"neu\",\"title\":\"Neu\"}]}");

        catalog.reloadAfterRestore(file);

        assertTrue(catalog.find("Alt").isEmpty());
        assertEquals("Neu", catalog.find("Neu").orElseThrow().title);
    }
}
