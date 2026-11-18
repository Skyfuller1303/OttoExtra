package de.ottoextra.crafting.werkstatt;

import de.ottoextra.crafting.CraftingParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Only the server's own messages may steer a job; player chat never does. */
class ServerMessageTest {

    @Test
    void refusalsAreWholeServerSentences() {
        assertTrue(WerkstattText.isRefusal("§cDu hast nicht genug Eisenerz!"));
        assertTrue(WerkstattText.isRefusal("[Info] Die Station ist nicht bereit."));
        assertTrue(WerkstattText.isRefusal("Zu wenig Brennstoff"));
        assertFalse(WerkstattText.isRefusal("Ich glaube, ich habe heute einfach nicht genug geschlafen, Leute"));
        assertFalse(WerkstattText.isRefusal("Steve: nicht genug Holz"));
        assertFalse(WerkstattText.isRefusal("<Steve> kein Feuer hier"));
        assertFalse(WerkstattText.isRefusal("[Gilde] Alex » zu wenig Leute"));
        assertFalse(WerkstattText.isRefusal(""));
        assertFalse(WerkstattText.isRefusal(null));
    }

    @Test
    void refusalNeedsTheActionBarOrTheServerPrefix() {
        assertTrue(WerkstattText.isServerRefusal("Du hast nicht genug Eisenerz!", true));
        assertTrue(WerkstattText.isServerRefusal("[Info] Du hast nicht genug Eisenerz!", false));
        assertTrue(WerkstattText.isServerRefusal("§c[Info] Die Herstellung ist fehlgeschlagen!", false));
        // Same sentence as plain chat (no prefix): could be typed by a player.
        assertFalse(WerkstattText.isServerRefusal("Du hast nicht genug Eisenerz!", false));
        assertFalse(WerkstattText.isServerRefusal("Die Herstellung wurde nicht abgeschlossen", false));
        assertFalse(WerkstattText.isServerRefusal("[Info] Schönes Wetter heute", false));
    }

    @Test
    void completionAndFailureLinesAreAnchored() {
        assertTrue(CraftingParser.isCompletion("§7Die Herstellung ist erfolgreich!"));
        assertTrue(CraftingParser.isCompletion("[Info] Die Herstellung ist erfolgreich."));
        assertFalse(CraftingParser.isCompletion("Steve: Die Herstellung ist erfolgreich!"));
        assertFalse(CraftingParser.isCompletion("<Steve> Die Herstellung ist erfolgreich"));
        assertFalse(CraftingParser.isCompletion("Die Herstellung ist erfolgreich? Nein"));
        assertTrue(CraftingParser.isCraftFailed("§7Die Herstellung ist schiefgegangen..."));
        assertFalse(CraftingParser.isCraftFailed("Steve: Die Herstellung ist schiefgegangen..."));
        assertFalse(CraftingParser.isCraftFailed("Bei mir: die herstellung ist misslungen, lol"));
    }

    @Test
    void damageAndMovingStillStopWhileChatting() {
        assertEquals(StopReason.DAMAGE, StopRules.check(StopRules.Settings.ALL,
                StopRules.Observation.whileChatting(0, true)));
        assertEquals(StopReason.MOVED, StopRules.check(StopRules.Settings.ALL,
                StopRules.Observation.whileChatting(StopRules.MAX_DISTANCE + 1, false)));
        // The chat itself is neither a foreign GUI nor a focus loss.
        assertNull(StopRules.check(StopRules.Settings.ALL, StopRules.Observation.whileChatting(0, false)));
    }
}
