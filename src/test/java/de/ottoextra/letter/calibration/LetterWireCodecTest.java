package de.ottoextra.letter.calibration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LetterWireCodecTest {

    @Test
    void decodesNewlinesBackslashesAndFormattingInOnePass() {
        assertEquals("§6Titel\nPfad C:\\Briefe und sichtbar \\n",
                LetterWireCodec.decodePagePayload(
                        "&6Titel\\nPfad C:\\\\Briefe und sichtbar \\\\n"));
    }

    @Test
    void unknownEscapesRemainVisible() {
        assertEquals("\\x", LetterWireCodec.decodePagePayload("\\x"));
    }
}
