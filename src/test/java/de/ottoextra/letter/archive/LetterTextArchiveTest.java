package de.ottoextra.letter.archive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class LetterTextArchiveTest {
    @TempDir Path directory;
    private static final Instant DATE = Instant.parse("2026-09-08T16:30:45Z");

    @Test void roundTripKeepsUnicodeFormattingEmptyPagesAndLiteralEscapes() throws Exception {
        var pages = List.of("§cGrüße 😀§r\n\nMit & und \\n und \\f.", "", "\f\nFormfeed", "Ende\n", "");
        Path file = LetterTextArchive.write(directory, "letter", pages, "Skyfuller1303", "CourierAudit", DATE);
        assertEquals(pages, LetterTextArchive.read(file));
        assertTrue(Files.readString(file).contains("From: Skyfuller1303"));
        assertTrue(file.getFileName().toString().matches("letter_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.txt"));
    }

    @Test void repeatedExportsNeverOverwriteExistingFile() throws Exception {
        Path first = LetterTextArchive.write(directory, "Player", List.of("Erster Brief"), "", "", DATE);
        Path second = LetterTextArchive.write(directory, "Player", List.of("Zweiter Brief"), "", "", DATE);
        assertNotEquals(first, second);
        assertEquals(List.of("Erster Brief"), LetterTextArchive.read(first));
        assertEquals(List.of("Zweiter Brief"), LetterTextArchive.read(second));
    }

    @Test void unsafeSenderCannotEscapeDirectoryOrInjectHeader() throws Exception {
        Path file = LetterTextArchive.write(directory, "../CON:§cX\\..", List.of("Inhalt"), "A\n\nInjected", "", DATE);
        assertEquals(directory, file.getParent());
        assertFalse(file.getFileName().toString().contains(".."));
        assertEquals(List.of("Inhalt"), LetterTextArchive.read(file));
    }

    @Test void readsPlainUtf8WithBomWindowsLinesAndPageBreaks() throws Exception {
        assertEquals(List.of("§6Hallo\nWelt\n", "Seite 2\\n"),
                LetterTextArchive.decode("\uFEFF§6Hallo\r\nWelt\r\n\fSeite 2\\n"));
    }

    @Test void archivedWindowsLineEndingsAreAccepted() throws Exception {
        var pages = List.of("Hallo\nWelt", "Zwei");
        assertEquals(pages, LetterTextArchive.decode(LetterTextArchive.encode(pages, "a", "b", DATE).replace("\n", "\r\n")));
    }

    @Test void rejectsUnknownOrMalformedArchive() {
        assertThrows(IOException.class, () -> LetterTextArchive.decode("# OttoExtra Letter TXT 2\n\ntext"));
        assertThrows(IOException.class, () -> LetterTextArchive.decode("# OttoExtra Letter TXT 1\nmissing"));
        assertThrows(IOException.class, () -> LetterTextArchive.decode("# OttoExtra Letter TXT 1\n\n\\x"));
        assertThrows(IOException.class, () -> LetterTextArchive.decode("# OttoExtra Letter TXT 1\n\n\\"));
    }

    @Test void rejectsOversizedInputAndTooManyPages() throws Exception {
        Path file = directory.resolve("large.txt");
        Files.write(file, new byte[LetterTextArchive.MAX_BYTES + 1]);
        assertThrows(IOException.class, () -> LetterTextArchive.read(file));
        assertThrows(IOException.class, () -> LetterTextArchive.decode("x".repeat(32768)));
        assertThrows(IOException.class, () -> LetterTextArchive.decode("\f".repeat(100)));
    }

    @Test void rejectsInvalidUtf8WithoutReplacingCharacters() throws Exception {
        Path file = directory.resolve("broken.txt");
        Files.write(file, new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThrows(IOException.class, () -> LetterTextArchive.read(file));
    }
}
