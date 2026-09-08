package de.ottoextra.letter.recovery;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LetterSendContextTest {
    @Test
    void bindsRecoveryToTheOriginalServerPlayerAndHotbarSlot() {
        LetterSendContext context = new LetterSendContext();
        context.serverAddress = "Example.org:25565";
        context.playerUuid = "player-one";
        context.selectedSlot = 2;

        assertTrue(context.matches("example.org:25565", "player-one", 2));
        assertFalse(context.matches("elsewhere.org:25565", "player-one", 2));
        assertFalse(context.matches("example.org:25565", "player-two", 2));
        assertFalse(context.matches("example.org:25565", "player-one", 3));
        assertFalse(new LetterSendContext().matches("", "", -1));
    }

    @Test
    void pageMatchingAllowsRedundantStyleCodesButRejectsDifferentTextStyleOrPages() {
        LetterSendContext context = new LetterSendContext();
        context.expectedPages = List.of("§6Hallo §6Ottonien ");

        assertTrue(context.matchesPages(List.of("§6Hallo Ottonien ")));
        assertFalse(context.matchesPages(List.of("§6Anderer Brief ")));
        assertFalse(context.matchesPages(List.of("§lHallo Ottonien ")));
        assertFalse(context.matchesPages(List.of("§6Hallo ", "§6Ottonien ")));
        assertFalse(context.matchesPages(null));
    }

    @Test
    void sentWriteRequiresWrittenBookBeforePostAndTracksSubmissionSeparately() {
        LetterSendContext context = new LetterSendContext();
        context.letterCommand = "letter";
        assertFalse(context.writtenExpected);

        context.sent("letter Hallo");
        assertTrue(context.writtenExpected);
        assertFalse(context.submitted);
        assertTrue(context.matchesPages(List.of("Hallo ")));
        context.sent("post Alice");
        assertTrue(context.submitted);
    }

    @Test
    void rejectsMalformedPersistedCommandState() {
        assertTrue(LetterSendContext.validCommands(List.of("letter Hallo", "post Alice"), 1));
        assertFalse(LetterSendContext.validCommands(null, 0));
        assertFalse(LetterSendContext.validCommands(List.of("letter Hallo"), -1));
        assertFalse(LetterSendContext.validCommands(List.of("letter Hallo"), 2));
        assertFalse(LetterSendContext.validCommands(List.of("letter Hallo\npost Alice"), 0));
        assertFalse(LetterSendContext.validCommands(List.of("letter " + "x".repeat(256)), 0));
    }
}
