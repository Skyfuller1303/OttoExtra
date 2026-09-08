package de.ottoextra.letter.calibration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterMarkupSemanticsTest {

    @Test
    void colorResetsStylesLikeMinecraft() {
        var runs = LetterMarkupSemantics.runs("§lFett §cRot §lRotfett§r Normal");
        assertTrue(runs.get(0).bold());
        assertEquals('c', runs.get(1).color());
        assertFalse(runs.get(1).bold());
        assertEquals('c', runs.get(2).color());
        assertTrue(runs.get(2).bold());
        assertNull(runs.get(3).color());
        assertFalse(runs.get(3).bold());
    }

    @Test
    void ampersandCodesRemainLiteralInEditorSource() {
        assertEquals("Code &a bleibt sichtbar",
                LetterMarkupSemantics.plain("Code &a bleibt sichtbar"));
    }

    @Test
    void invalidSectionCodeRemainsLiteral() {
        assertEquals("§zTest", LetterMarkupSemantics.plain("§zTest"));
    }
}
