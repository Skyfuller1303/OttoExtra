package de.ottoextra.letter.paste;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextNormalizerTest {

    @Test
    void preservesOrdinarySpacesWhenPasting() {
        assertEquals("Ein Text mit Leerzeichen ",
                TextNormalizer.normalize("Ein Text mit Leerzeichen "));
    }

    @Test
    void acceptsWindowsMacAndUnixLineEndings() {
        assertEquals("Windows\nMac\nUnix\nEnde",
                TextNormalizer.normalize("Windows\r\nMac\rUnix\nEnde"));
    }

    @Test
    void convertsClipboardWhitespaceToBookSafeCharacters() {
        assertEquals("A B C D    E",
                TextNormalizer.normalize("A\u00a0B\u2007C\u202fD\tE"));
    }
}
