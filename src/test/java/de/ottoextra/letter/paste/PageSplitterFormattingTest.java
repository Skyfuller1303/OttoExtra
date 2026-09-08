package de.ottoextra.letter.paste;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageSplitterFormattingTest {

    @Test
    void hardWrapNeverSeparatesSectionCodePair() {
        PageSplitter splitter = new PageSplitter(18, 20);
        var lines = splitter.wrapLines("§mdurchgestrichen§r");

        assertTrue(lines.size() > 1);
        for (String line : lines) {
            assertFalse(line.endsWith("§"), lines.toString());
            assertFalse(line.startsWith("r") && !line.startsWith("§r"), lines.toString());
        }
        assertTrue(String.join("", lines).contains("§r"));
    }
}
