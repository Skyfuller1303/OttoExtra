package de.ottoextra.horses;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class AnimalNameCatalogTest {
    @Test void expandedCatalogLoadsAndRollsDifferentNames() {
        var names = AnimalNameCatalog.loadDefault();
        assertTrue(names.sizeFor("Stute") >= 160);
        assertTrue(names.sizeFor("Hengst") >= 160);
        String current = "";
        for (int i = 0; i < 100; i++) {
            String next = names.next("Weiblich", current, new Random(i));
            assertFalse(next.isBlank()); assertNotEquals(current, next); assertTrue(next.length() <= 64);
            current = next;
        }
    }

    @Test void respectsGenderAndAvoidsAlreadyKnownNamesWhenAvailable() {
        var names = new AnimalNameCatalog(List.of("Ada", "Berta", "Clara"), List.of("Alrik", "Bodo"));
        assertEquals("Clara", names.next("♀", "", "", "Ada", List.of("berta"), new Random(1)));
        assertEquals("Bodo", names.next("Männlich", "Alrik", new Random(1)));
        assertEquals("Bodo", names.next("♂", "Alrik", new Random(1)));
    }
}
