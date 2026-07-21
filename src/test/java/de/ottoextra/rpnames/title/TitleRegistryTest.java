package de.ottoextra.rpnames.title;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TitleRegistryTest {
    @Test
    void exposesConfiguredGroupPriorityAndTitleOrder() {
        TitleRegistry.Group adel = new TitleRegistry.Group();
        adel.priority = 800;
        adel.titles = List.of("Graf", "Gräfin", "Freiherr");
        LinkedHashMap<String, TitleRegistry.Group> groups = new LinkedHashMap<>();
        groups.put("ADEL", adel);

        TitleRegistry registry = new TitleRegistry();
        registry.apply(groups);

        TitleRegistry.ResolvedTitle graf = registry.find("graf").orElseThrow();
        TitleRegistry.ResolvedTitle freiherr = registry.find("Freiherr").orElseThrow();
        assertEquals("ADEL", graf.groupKey());
        assertEquals(800, graf.group().priority);
        assertEquals(0, graf.titleIndex());
        assertEquals(2, freiherr.titleIndex());
    }
}
