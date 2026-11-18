package de.ottoextra.rpnames;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerProfileHeadClickTest {

    @Test
    void splitsTitleAndNameFromSiblings() {
        MutableText text = Text.empty();
        text.append(Text.literal("Großschmied "));
        text.append(Text.literal("Johann Nordligkatt"));
        var head = ServerProfileHeadClick.parseHeadName(text);
        assertEquals("Großschmied", head.title());
        assertEquals("Johann Nordligkatt", head.rpName());
    }

    @Test
    void rootContentCountsAsFirstPart() {
        MutableText text = Text.literal("Bote ");
        text.append(Text.literal("Ulrike"));
        var head = ServerProfileHeadClick.parseHeadName(text);
        assertEquals("Bote", head.title());
        assertEquals("Ulrike", head.rpName());
    }

    @Test
    void singlePartIsNameWithoutTitle() {
        var head = ServerProfileHeadClick.parseHeadName(Text.literal("Ada"));
        assertEquals("", head.title());
        assertEquals("Ada", head.rpName());
    }

    @Test
    void blankNameIsRejected() {
        assertNull(ServerProfileHeadClick.parseHeadName(Text.empty()));
        assertNull(ServerProfileHeadClick.parseHeadName(null));
    }

    @Test
    void screenTitleMustEqualRpName() {
        var head = new ServerProfileHeadClick.HeadName("Großschmied", "Johann Nordligkatt");
        assertTrue(ServerProfileHeadClick.matchesScreenTitle(head, Text.literal("Johann Nordligkatt")));
        assertFalse(ServerProfileHeadClick.matchesScreenTitle(head, Text.literal("Shop")));
        assertFalse(ServerProfileHeadClick.matchesScreenTitle(head, Text.empty()));
        assertFalse(ServerProfileHeadClick.matchesScreenTitle(head, null));
    }

    @Test
    void parsesLoreFields() {
        var lore = ServerProfileHeadClick.parseLore(java.util.List.of(
                Text.literal("Stand: Gemein"),
                Text.literal("Geschlecht: Männlich"),
                Text.literal("Alter: Erwachsener"),
                Text.literal(""),
                Text.literal("Beschreibung:"),
                Text.literal("Groß und breit."),
                Text.literal("Trägt eine Schürze."),
                Text.literal(""),
                Text.literal("Charakter erstellt am: 14.11.2025"),
                Text.literal("20:04:06")));
        assertEquals("Gemein", lore.state());
        assertEquals("Männlich", lore.gender());
        assertEquals("Erwachsener", lore.age());
        assertEquals("Groß und breit. Trägt eine Schürze.", lore.description());
    }

    @Test
    void missingLoreYieldsEmptyFields() {
        var lore = ServerProfileHeadClick.parseLore(java.util.List.of());
        assertEquals("", lore.state());
        assertEquals("", lore.description());
    }

    @Test
    void descriptionWithoutTrailingBlankLineIsKept() {
        var lore = ServerProfileHeadClick.parseLore(java.util.List.of(
                Text.literal("Beschreibung:"), Text.literal("Nur eine Zeile")));
        assertEquals("Nur eine Zeile", lore.description());
    }
}
