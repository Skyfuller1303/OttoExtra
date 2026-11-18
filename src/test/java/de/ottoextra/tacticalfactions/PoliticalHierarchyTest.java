package de.ottoextra.tacticalfactions;

import de.ottoextra.api.model.FactionRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PoliticalHierarchyTest {
    @Test void resolvesRecursiveLiegesByFactionNameFromCurrentApiShape() {
        FactionRecord holdstewik = faction("holdstewik", "Holdstewik", "Bricketjosh", "");
        FactionRecord geldern = faction("geldern", "Geldern", "Rhobar", "Holdstewik");
        FactionRecord wyndham = faction("wyndham", "Wyndham", "Cadwgawn", "Geldern");
        Map<String, String> parents = new HashMap<>();

        PoliticalStateService.addNameBasedParents(List.of(wyndham, geldern, holdstewik), parents);

        assertEquals("holdstewik", parents.get("geldern"));
        assertEquals("geldern", parents.get("wyndham"));
        assertNull(parents.get("holdstewik"));
    }

    @Test void explicitUuidHierarchyRemainsAuthoritative() {
        FactionRecord geldern = faction("geldern", "Geldern", "Rhobar", "Holdstewik");
        FactionRecord holdstewik = faction("holdstewik", "Holdstewik", "Bricketjosh", "");
        Map<String, String> parents = new HashMap<>();
        parents.put("geldern", "explicit-parent");

        PoliticalStateService.addNameBasedParents(List.of(geldern, holdstewik), parents);

        assertEquals("explicit-parent", parents.get("geldern"));
    }

    private static FactionRecord faction(String uuid, String name, String leader, String lord) {
        return new FactionRecord(uuid, "lehen_1", uuid, name, "Vogtei", leader, lord,
                null, null, null, "external", null, name + " Region", 1, 1,
                null, null, null, 0, 0, null, null);
    }
}
