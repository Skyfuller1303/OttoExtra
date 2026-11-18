package de.ottoextra.tacticalfactions;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TacticalFactionsAddonTest {
    @Test void removesTitlePrefixFromRpNameLine() {
        Text result = TacticalFactionsAddon.withoutRepeatedTitle(
                Text.literal("Freiherr Albrecht von Falken"), Text.literal("Freiherr"));
        assertEquals("Albrecht von Falken", result.getString());
    }

    @Test void keepsNamesThatOnlyContainTitleLetters() {
        Text result = TacticalFactionsAddon.withoutRepeatedTitle(
                Text.literal("Grafenwald"), Text.literal("Graf"));
        assertEquals("Grafenwald", result.getString());
    }
}
