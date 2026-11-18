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

    @Test void manualExportDirectoryGroupsByAuthorOnlyWhenEnabledAndSanitizesReservedNames() throws Exception {
        Path root = directory.resolve("letters");
        assertEquals(root, LetterTextArchive.manualExportDirectory(root, "GeicoGecko", false));
        Path geico = LetterTextArchive.manualExportDirectory(root, " GeicoGecko ", true);
        assertEquals(root.resolve("GeicoGecko"), geico);
        assertEquals(geico, LetterTextArchive.manualExportDirectory(root, "GeicoGecko", true));
        assertNotEquals(geico, LetterTextArchive.manualExportDirectory(root, "OtherPlayer", true));
        Path unsafe = LetterTextArchive.manualExportDirectory(root, "../escape", true);
        assertEquals(root, unsafe.getParent());
        assertEquals(root.resolve("player-CON"), LetterTextArchive.manualExportDirectory(root, "CON", true));
        assertEquals(root.resolve("player-SENT"), LetterTextArchive.manualExportDirectory(root, "SENT", true));

        var pages = List.of("Brief");
        Path first = LetterTextArchive.write(geico, "GeicoGecko", pages, "GeicoGecko", "", DATE,
                List.of("Titel"));
        Path second = LetterTextArchive.write(geico, "GeicoGecko", pages, "GeicoGecko", "", DATE,
                List.of("Titel"));
        assertNotEquals(first, second);
        assertEquals(pages, LetterTextArchive.read(first));
        assertEquals(pages, LetterTextArchive.read(second));
    }

    @Test void listsRootSentAndPlayerExportsButNotDeeperOrNonTxtFiles() throws Exception {
        Path root = directory.resolve("letters");
        Path sent = Files.createDirectories(root.resolve("sent"));
        Path player = Files.createDirectories(root.resolve("GeicoGecko"));
        Path deeper = Files.createDirectories(player.resolve("nested"));
        Path flatFile = Files.writeString(root.resolve("flat.TXT"), "flat");
        Path sentFile = Files.writeString(sent.resolve("sent.txt"), "sent");
        Path playerFile = Files.writeString(player.resolve("player.txt"), "player");
        Files.writeString(deeper.resolve("hidden.txt"), "hidden");
        Files.writeString(player.resolve("ignore.json"), "json");
        Files.createDirectory(player.resolve("directory.txt"));

        assertEquals(List.of(sentFile, playerFile, flatFile), LetterTextArchive.listFiles(root));
    }

    @Test void manualExportKeepsTooltipAndSeparatesExportTimeFromLetterDate() throws Exception {
        var pages = List.of("§cBrieftext\n\\f", "", "\fEnde");
        var tooltip = List.of("§7Brief von §6Absender", "Empfänger: Spieler", "Datum: 08.09.2026, 18:30 Uhr");
        Path file = LetterTextArchive.write(directory, "Autor", pages, "Autor", "", DATE, tooltip);
        assertTrue(file.getFileName().toString().matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}_Autor\\.txt"));
        String encoded = Files.readString(file);
        int separator = encoded.indexOf("\n\n");
        String header = encoded.substring(0, separator);
        assertTrue(header.contains("From: Autor"));
        assertTrue(header.contains("Tooltip: Brief von Absender"));
        assertFalse(header.contains("§"));
        assertTrue(header.contains("Tooltip: Empfänger: Spieler"));
        assertTrue(header.contains("Tooltip: Datum: 08.09.2026, 18:30 Uhr"));
        assertTrue(header.contains("Exported: " + DATE));
        assertFalse(header.contains("\nDate:"));
        assertEquals(pages, LetterTextArchive.read(file));
    }

    @Test void manualTooltipCannotInjectHeaderOrSeparatorAndEmptyLinesAreSkipped() throws Exception {
        var pages = List.of("Brieftext");
        String encoded = LetterTextArchive.encode(pages, "Autor", "", DATE,
                List.of("Titel\n\nInjected", "", "§c"));
        String header = encoded.substring(0, encoded.indexOf("\n\n"));
        assertTrue(header.contains("Tooltip: Titel  Injected"));
        assertEquals(1, header.split("Tooltip:", -1).length - 1);
        assertEquals(pages, LetterTextArchive.decode(encoded));
    }

    @Test void emptyTooltipStillWritesExportTimeAndLegacyApiKeepsDate() {
        String manual = LetterTextArchive.encode(List.of("Brief"), "Autor", "", DATE, List.of());
        assertTrue(manual.contains("Exported: " + DATE));
        assertFalse(manual.contains("\nDate:"));
        String automatic = LetterTextArchive.encode(List.of("Brief"), "Autor", "", DATE);
        assertTrue(automatic.contains("\nDate: " + DATE + "\n\n"));
        assertFalse(automatic.contains("Exported:"));
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

    @Test void fileSystemFailureReportsItsRealCauseAndKeepsExistingFile() throws Exception {
        Path blockedDirectory = directory.resolve("letters");
        Files.writeString(blockedDirectory, "Vorhandene Daten");
        IOException failure = assertThrows(IOException.class, () -> LetterTextArchive.write(
                blockedDirectory, "written", List.of("Brief"), "Autor", "", DATE));
        assertTrue(LetterTextArchive.failureReason(failure).startsWith(failure.getClass().getSimpleName()));
        assertEquals("Vorhandene Daten", Files.readString(blockedDirectory));
    }

    @Test void permissionAndValidationFailuresHaveDistinctDetails() {
        assertEquals("AccessDeniedException: letters/sent",
                LetterTextArchive.failureReason(new AccessDeniedException("letters/sent")));
        assertEquals("IOException: TXT exceeds 256 KiB",
                LetterTextArchive.failureReason(new IOException("TXT exceeds 256 KiB")));
        assertEquals("IOException", LetterTextArchive.failureReason(new IOException()));
        assertEquals("IOException: Zeile 1  Zeile 2",
                LetterTextArchive.failureReason(new IOException("Zeile 1\r\nZeile 2")));
    }
}
