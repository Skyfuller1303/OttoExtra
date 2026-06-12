package de.ottoextra.letter;

import de.ottoextra.letter.announcement.AnnouncementCommandBuilder;
import de.ottoextra.letter.announcement.AnnouncementPreflightService;
import de.ottoextra.letter.model.AnnouncementPreflightResult;
import de.ottoextra.letter.model.AnnouncementSendProgress;
import de.ottoextra.letter.model.LetterPlaceholder;
import de.ottoextra.letter.model.PlaceholderResolveResult;
import de.ottoextra.letter.paste.PageSplitter;
import de.ottoextra.letter.paste.TextNormalizer;
import de.ottoextra.letter.placeholder.LetterPlaceholderParser;
import de.ottoextra.letter.placeholder.PlaceholderResolveService;
import de.ottoextra.letter.placeholder.RpIdentityResolver;
import de.ottoextra.letter.recovery.SendProgressStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit-Tests der MC-freien Letter-Kernlogik. */
class LetterCoreTest {

    // ---- TextNormalizer ------------------------------------------------------

    @Test
    void normalizerUnifiesLineEndingsAndStripsControl() {
        assertEquals("a\nb\nc", TextNormalizer.normalize("a\r\nb\rc"));
        assertEquals("x    y", TextNormalizer.normalize("x\ty"));
        assertEquals("ab", TextNormalizer.normalize("ab"));
        assertEquals("rot", TextNormalizer.normalize("§4rot"));
        assertEquals("a b", TextNormalizer.normalize("a b"));
    }

    // ---- PageSplitter --------------------------------------------------------

    @Test
    void splitterWrapsWordsAndPages() {
        PageSplitter splitter = new PageSplitter(10, 2);
        List<String> pages = splitter.split("eins zwei drei vier fuenf");
        assertTrue(pages.size() >= 2);
        String all = String.join(" ", pages).replace("\n", " ");
        assertTrue(all.contains("eins") && all.contains("fuenf"));
    }

    @Test
    void splitterNeverLosesLongWords() {
        PageSplitter splitter = new PageSplitter(5, 3);
        String word = "supercalifragilistic";
        String joined = String.join("", splitter.split(word)).replace("\n", "");
        assertEquals(word, joined);
    }

    // ---- Placeholder Parser ----------------------------------------------------

    @Test
    void parserFindsAllTypes() {
        String text = "Hallo {{name:Sky}} und {{ title : Burchard }} sowie {{full:X}} {{mc:Y}}";
        List<LetterPlaceholder> found = LetterPlaceholderParser.parse(text);
        assertEquals(4, found.size());
        assertEquals("name", found.get(0).type());
        assertEquals("Sky", found.get(0).playerName());
        assertEquals("title", found.get(1).type());
        assertEquals("Burchard", found.get(1).playerName());
    }

    @Test
    void parserNavigation() {
        String text = "a {{name:A}} b {{name:B}} c";
        var first = LetterPlaceholderParser.parse(text).get(0);
        var second = LetterPlaceholderParser.parse(text).get(1);
        assertEquals(first, LetterPlaceholderParser.at(text, first.start() + 2));
        assertEquals(second, LetterPlaceholderParser.next(text, first.end()));
        // wrap-around
        assertEquals(first, LetterPlaceholderParser.next(text, second.end()));
        assertEquals(first, LetterPlaceholderParser.previous(text, second.start()));
    }

    @Test
    void parserCountsInvalidOpenings() {
        assertEquals(0, LetterPlaceholderParser.countInvalidOpenings("ok {{name:A}}"));
        assertEquals(1, LetterPlaceholderParser.countInvalidOpenings("kaputt {{name:A"));
        assertEquals(1, LetterPlaceholderParser.countInvalidOpenings("{{quatsch:A}}"));
    }

    // ---- Resolver mit Fake-Cache -----------------------------------------------

    private static final RpIdentityResolver FAKE = new RpIdentityResolver() {
        private final Map<String, String> names = Map.of("Sky", "Burchard Geestner");
        private final Map<String, String> titles = Map.of("Sky", "Abt");

        @Override
        public Optional<String> rpName(String playerName) {
            return Optional.ofNullable(names.get(playerName));
        }

        @Override
        public Optional<String> title(String playerName) {
            return Optional.ofNullable(titles.get(playerName));
        }

        @Override
        public boolean accountKnown(String playerName) {
            return names.containsKey(playerName);
        }
    };

    @Test
    void resolverResolvesAndApplies() {
        PlaceholderResolveService service = new PlaceholderResolveService(FAKE);
        String text = "Gruss von {{full:Sky}}!";
        LetterPlaceholder p = LetterPlaceholderParser.parse(text).get(0);
        PlaceholderResolveResult r = service.resolve(p);
        assertTrue(r.ok());
        assertEquals("Abt Burchard Geestner", r.resolved());
        assertEquals("Gruss von Abt Burchard Geestner!", service.apply(text, r));
    }

    @Test
    void resolverFailsUnknownPlayer() {
        PlaceholderResolveService service = new PlaceholderResolveService(FAKE);
        LetterPlaceholder p = LetterPlaceholderParser.parse("{{name:Unbekannt}}").get(0);
        PlaceholderResolveResult r = service.resolve(p);
        assertFalse(r.ok());
        assertEquals("{{name:Unbekannt}}", service.apply("{{name:Unbekannt}}", r));
    }

    // ---- Announcement Command Builder ------------------------------------------

    @Test
    void builderChunksWithinCommandLimit() {
        AnnouncementCommandBuilder b = new AnnouncementCommandBuilder(
                "verk start", "verk page", "verk commit");
        String longPage = "x".repeat(900);
        List<String> commands = b.buildCommands("d1", List.of(longPage, "kurz"));
        assertTrue(commands.get(0).startsWith("verk start d1 2 "));
        assertEquals("verk commit d1", commands.get(commands.size() - 1));
        for (String c : commands) {
            assertTrue(c.length() <= AnnouncementCommandBuilder.COMMAND_CHAR_LIMIT,
                    "Command über Limit: " + c.length());
        }
        // Payload-Rekonstruktion: alle Chunks von Seite 1 ergeben den Originaltext
        StringBuilder page1 = new StringBuilder();
        for (String c : commands) {
            if (c.startsWith("verk page d1 1 ")) {
                String[] parts = c.split(" ", 8);
                page1.append(parts[7]);
            }
        }
        assertEquals(longPage, page1.toString());
    }

    // ---- Preflight -----------------------------------------------------------------

    @Test
    void preflightFlagsProblems() {
        AnnouncementCommandBuilder b = new AnnouncementCommandBuilder(
                "verk start", "verk page", "verk commit");
        AnnouncementPreflightService service = new AnnouncementPreflightService(b);
        AnnouncementPreflightResult result = service.check("d1", List.of(
                "Saubere Seite.",
                "",
                "Mit {{name:Wer}} offen",
                "y".repeat(2500)));
        assertFalse(result.ok());
        assertTrue(result.pages().get(0).ok());
        assertFalse(result.pages().get(1).ok()); // leer
        assertFalse(result.pages().get(2).ok()); // Platzhalter offen
        assertFalse(result.pages().get(3).ok()); // über Discord-Limit
        assertNotNull(result.totalChecksum());
    }

    // ---- Recovery Store -----------------------------------------------------------

    @TempDir
    Path tempDir;

    @Test
    void storeRoundtripAndClear() {
        SendProgressStore<AnnouncementSendProgress> store = new SendProgressStore<>(
                tempDir.resolve("active-announcement-send.json"), AnnouncementSendProgress.class);
        AnnouncementSendProgress progress = new AnnouncementSendProgress();
        progress.draftId = "d1";
        progress.pendingCommands = List.of("a", "b", "c");
        progress.sentCommands = 1;
        store.save(progress);
        AnnouncementSendProgress loaded = store.load();
        assertNotNull(loaded);
        assertEquals("d1", loaded.draftId);
        assertEquals(1, loaded.sentCommands);
        assertEquals(3, loaded.pendingCommands.size());
        store.clear();
        assertNull(store.load());
    }
}
