package de.ottoextra.tweaks.combat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CombatModeTest {
    @ParameterizedTest
    @EnumSource(CombatMode.class)
    void recognizesEveryTechniqueInFormattedActionbars(CombatMode mode) {
        assertEquals(mode, CombatMode.parse("§6Kampfmodus: §e" + mode.displayName() + " §7(II)").orElseThrow());
    }

    @Test
    void recognizesUmlautsAndWhitespaceVariants() {
        assertEquals(CombatMode.KUEHNER_STICH, CombatMode.parse("KUEHNER\u00a0STICH").orElseThrow());
        assertEquals(CombatMode.KUEHNER_STICH, CombatMode.parse("Ku\u0308hner Stich").orElseThrow());
        assertEquals(CombatMode.PFAEHLEN, CombatMode.parse("[Pfählen]").orElseThrow());
        assertEquals(CombatMode.PFAEHLEN, CombatMode.parse("Kampfmodus: Pfaehlen").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Willkommen im Lehen", "Wuchthaube", "Aderlass / Flinker Schnitt",
            "Du hast Aderlass noch nicht erlernt.", "Glockenschlag (Ungelernt)",
            "Pfeilhagel ist nicht verfügbar."})
    void ignoresUnrelatedAmbiguousAndRejectedModeMessages(String message) {
        assertTrue(CombatMode.parse(message).isEmpty());
    }
}
