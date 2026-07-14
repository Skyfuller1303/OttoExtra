package de.ottoextra.letter.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterFormattingCodesTest {

    @Test
    void isValidCodeWhitelist() {
        assertTrue(LetterFormattingCodes.isValidCode('0'));
        assertTrue(LetterFormattingCodes.isValidCode('a'));
        assertTrue(LetterFormattingCodes.isValidCode('f'));
        assertTrue(LetterFormattingCodes.isValidCode('l'));
        assertTrue(LetterFormattingCodes.isValidCode('r'));
        assertTrue(LetterFormattingCodes.isValidCode('k'));
        assertFalse(LetterFormattingCodes.isValidCode('z'));
        assertFalse(LetterFormattingCodes.isValidCode('x'));
        assertFalse(LetterFormattingCodes.isValidCode('g'));
    }

    @Test
    void sectionToAmpersandConvertsOnlyValid() {
        assertEquals("&6Hallo", LetterFormattingCodes.sectionToAmpersand("§6Hallo"));
        assertEquals("&lFett&r", LetterFormattingCodes.sectionToAmpersand("§lFett§r"));
        assertEquals("§zTest", LetterFormattingCodes.sectionToAmpersand("§zTest"));
        assertEquals("&6&lTitel&r",
                LetterFormattingCodes.sectionToAmpersand("§6§lTitel§r"));
    }

    @Test
    void ampersandToSectionConvertsOnlyValid() {
        assertEquals("§6Hallo", LetterFormattingCodes.ampersandToSection("&6Hallo"));
        assertEquals("§lFett§r", LetterFormattingCodes.ampersandToSection("&lFett&r"));
        assertEquals("&zTest", LetterFormattingCodes.ampersandToSection("&zTest"));
    }

    @Test
    void roundtripPreservesText() {
        String editor = "§6§lBekanntmachung§r normal";
        String wire = LetterFormattingCodes.sectionToAmpersand(editor);
        assertEquals("&6&lBekanntmachung&r normal", wire);
        assertEquals(editor, LetterFormattingCodes.ampersandToSection(wire));
    }

    @Test
    void activePrefixCarriesColorAndStyle() {
        String doc = "§6§lHallo Welt";
        assertEquals("§6§l",
                LetterFormattingCodes.activePrefixBefore(doc, doc.indexOf("Welt")));
    }

    @Test
    void activePrefixResetClearsEverything() {
        String doc = "§6§lHallo §rWelt";
        assertEquals("", LetterFormattingCodes.activePrefixBefore(doc, doc.indexOf("Welt")));
    }

    @Test
    void activePrefixColorAfterStyleResetsStyle() {
        String doc = "§6§lHallo §cWelt";
        assertEquals("§c", LetterFormattingCodes.activePrefixBefore(doc, doc.indexOf("Welt")));
    }

    @Test
    void resetAndRestoreAtRestoresOuterColor() {
        String doc = "§eAlles gelb, auch danach";
        int selectionEnd = doc.indexOf(",");
        assertEquals("§r§e", LetterFormattingCodes.resetAndRestoreAt(doc, selectionEnd));
    }

    @Test
    void resetAndRestoreAtRestoresColorAndStyle() {
        String doc = "§e§oGelb und kursiv";
        assertEquals("§r§e§o", LetterFormattingCodes.resetAndRestoreAt(doc, doc.length()));
    }

    @Test
    void resetAndRestoreAtLeavesNormalTextNormal() {
        assertEquals("§r", LetterFormattingCodes.resetAndRestoreAt("Normal", 3));
    }

    @Test
    void visibleLengthIgnoresCodes() {
        assertEquals(5, LetterFormattingCodes.visibleLength("§6Hallo"));
        assertEquals(5, LetterFormattingCodes.visibleLength("&6Hallo"));
        assertEquals(7, LetterFormattingCodes.visibleLength("§zHallo"));
    }
}
