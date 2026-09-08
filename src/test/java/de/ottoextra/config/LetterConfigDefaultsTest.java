package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LetterConfigDefaultsTest {
    @Test
    void pageIsTheDefaultSendModeAndSurvivesRepair() {
        OttoExtraConfig config = new OttoExtraConfig();

        assertEquals("PAGE", config.letter.sendMode);
        config.repair();
        assertEquals("PAGE", config.letter.sendMode);
    }

    @Test
    void announcementAutoOptimizeIsOptIn() {
        assertFalse(new OttoExtraConfig().letter.announcementAutoOptimizeEnabled);
    }
}
