package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LetterConfigDefaultsTest {
    @Test
    void announcementAutoOptimizeIsOptIn() {
        assertFalse(new OttoExtraConfig().letter.announcementAutoOptimizeEnabled);
    }
}
