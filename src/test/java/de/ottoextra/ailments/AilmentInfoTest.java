package de.ottoextra.ailments;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AilmentInfoTest {

    @Test
    void matchesAllThirteenNames() {
        assertEquals(13, AilmentNames.ALL.size());
        for (String name : AilmentNames.ALL) {
            assertEquals(Optional.of(name), AilmentNames.match(name), name);
            assertEquals(Optional.of(name), AilmentNames.match(name.toUpperCase()), name);
            assertEquals(Optional.of(name), AilmentNames.match(name.toLowerCase()), name);
        }
    }

    @Test
    void matchesUmlautNames() {
        assertEquals(Optional.of("Unterkühlung"), AilmentNames.match("unterkühlung"));
        assertEquals(Optional.of("Übermüdet"), AilmentNames.match("ÜBERMÜDET"));
        assertEquals(Optional.of("Übermüdet"), AilmentNames.match("übermüdet"));
        // decomposed (NFD) umlaut
        assertEquals(Optional.of("Übermüdet"), AilmentNames.match("Übermüdet"));
    }

    @Test
    void stripsFormattingCodes() {
        assertEquals(Optional.of("Verdreckt"), AilmentNames.match("§cVerdreckt"));
        assertEquals(Optional.of("Verdreckt"), AilmentNames.match("§c§lVerdreckt§r "));
        assertEquals(Optional.of("Übermüdet"), AilmentNames.match("§4§oÜbermüdet"));
    }

    @Test
    void rejectsOtherNames() {
        assertTrue(AilmentNames.match("").isEmpty());
        assertTrue(AilmentNames.match(null).isEmpty());
        assertTrue(AilmentNames.match("Zustand").isEmpty());
        assertTrue(AilmentNames.match("Verdreckt!").isEmpty());
        assertTrue(AilmentNames.match("Amethyst Shard").isEmpty());
    }

    @Test
    void detectsZustand() {
        assertTrue(AilmentNames.isZustand("Zustand"));
        assertTrue(AilmentNames.isZustand("§eZustand"));
        assertFalse(AilmentNames.isZustand("Verdreckt"));
        assertFalse(AilmentNames.isZustand(null));
    }

    @Test
    void parsesVerdrecktExample() {
        AilmentInfo info = AilmentInfo.parse("§cVerdreckt",
                List.of("§7Effekte:", "§c- Gestank", "§7Ich habe lange kein Wasser mehr gesehen.")).orElseThrow();
        assertEquals("Verdreckt", info.kind());
        assertEquals("Verdreckt", info.name());
        assertFalse(info.zustand());
        assertEquals(List.of("Effekte:", "- Gestank", "Ich habe lange kein Wasser mehr gesehen."), info.lore());
        assertEquals(List.of("Gestank"), info.effects());
    }

    @Test
    void parsesZustandExample() {
        AilmentInfo info = AilmentInfo.parse("Zustand",
                List.of("Was man auf den ersten Blick sieht:", "- Nichts Auffälliges")).orElseThrow();
        assertTrue(info.zustand());
        assertEquals("Zustand", info.kind());
        assertEquals(2, info.lore().size());
        assertTrue(info.effects().isEmpty(), "no Effekte: header, so no effects");
    }

    @Test
    void effectsStopAtFirstNonDashLine() {
        assertEquals(List.of("A", "B"), AilmentInfo.effects(
                List.of("Intro", "Effekte:", "- A", "- B", "Text", "- C")));
        assertTrue(AilmentInfo.effects(List.of("Effekte:")).isEmpty());
        assertTrue(AilmentInfo.effects(List.of()).isEmpty());
    }

    @Test
    void nonAilmentItemYieldsEmpty() {
        assertTrue(AilmentInfo.parse("Amethyst Shard", List.of("Effekte:", "- x")).isEmpty());
        assertTrue(AilmentInfo.parse("Pocken", null).orElseThrow().lore().isEmpty());
    }
}
