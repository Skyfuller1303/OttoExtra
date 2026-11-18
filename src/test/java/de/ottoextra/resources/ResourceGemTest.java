package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ResourceGemTest {
    @Test
    void everyKindHasItsColour() {
        assertEquals(0xFFC9A58A, ResourceGem.light("IRON_MINE"));
        assertEquals(0xFFC9A58A, ResourceGem.light("IRON_DEPOSIT"));
        assertEquals(0xFF55555F, ResourceGem.light("COAL_MINE"));
        assertEquals(0xFFE6E2EC, ResourceGem.light("SALT_DEPOSIT"));
        assertEquals(0xFF7A5A3A, ResourceGem.light("PEAT_BOG"));
        assertEquals(0xFF9AA3B5, ResourceGem.light("CLAY_PIT"));
        assertEquals(0xFF6FB35A, ResourceGem.light("MONASTERY_GARDEN"));
        assertEquals(0xFFE0B43A, ResourceGem.light("APIARY"));
        assertEquals(0xFFB56FC7, ResourceGem.light("MARKER"));
    }

    @Test
    void unknownTypeGetsAGreyBlue() {
        assertEquals(0xFF9AA6B5, ResourceGem.light("NOPE"));
        assertEquals(0xFF9AA6B5, ResourceGem.light(null));
    }

    @Test
    void darkIsSixtyPercentPerChannelAndOpaque() {
        // 0xC9 = 201 -> 120 (0x78), 0xA5 = 165 -> 99 (0x63), 0x8A = 138 -> 82 (0x52)
        assertEquals(0xFF786352, ResourceGem.dark(0xFFC9A58A));
        assertEquals(0xFF000000, ResourceGem.dark(0xFF000000));
        assertEquals(0xFF999999, ResourceGem.dark(0xFFFFFFFF));
    }

    @Test
    void kindsAreDistinguishable() {
        java.util.Set<Integer> colours = new java.util.HashSet<>();
        for (String base : ResourcePolicy.BASES) colours.add(ResourceGem.light(ResourcePolicy.compose(base, true)));
        assertEquals(ResourcePolicy.BASES.size(), colours.size());
        assertNotEquals(ResourceGem.light("IRON_MINE"), ResourceGem.dark(ResourceGem.light("IRON_MINE")));
    }
}
