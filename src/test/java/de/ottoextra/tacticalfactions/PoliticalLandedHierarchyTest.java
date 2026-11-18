package de.ottoextra.tacticalfactions;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.RegionRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Shapes taken from the live API: no vassal lists, hierarchy only via lord_name, region holders = landed. */
class PoliticalLandedHierarchyTest {
    private static FactionRecord faction(String id, String name, String lord) {
        return new FactionRecord(id, "ref_" + id, id, name, "Dorf", "Leader_" + id, lord,
                null, null, null, "external", null, name + " Region", 1, 1,
                null, null, null, 0, 0, null, null);
    }

    private static RegionRecord region(String name, FactionRecord holder) {
        return new RegionRecord(name, name, name, name, null, null, "external", holder,
                null, null, List.of(), null, null, null, null);
    }

    private final FactionRecord holdstewik = faction("hold", "Holdstewik", "");
    private final FactionRecord mayenburg = faction("mayen", "Mayenburg", "Holdstewik");
    private final FactionRecord nordhaven = faction("nord", "Nordhaven", "Holdstewik");
    private final FactionRecord aegidius = faction("aeg", "SanktAegidius", "Nordhaven");
    private final FactionRecord freyford = faction("frey", "Freyford", "");
    private final FactionRecord limmenthal = faction("limm", "Limmenthal", "Freyford");
    private final FactionRecord auenfels = faction("auen", "Auenfels", "Holdstewik");
    private final List<FactionRecord> all = List.of(limmenthal, aegidius, mayenburg, holdstewik,
            nordhaven, freyford, auenfels);
    private final List<RegionRecord> regions = List.of(region("Merdorf", holdstewik),
            region("Dunberg", mayenburg), region("Oberpoelt", nordhaven), region("Ae", aegidius),
            region("Merkdorf", freyford), region("Leer", null));

    @Test void landedAreExactlyTheCurrentRegionHolders() {
        Set<String> landed = PoliticalHierarchy.landedIds(regions);
        assertEquals(Set.of("hold", "mayen", "nord", "aeg", "frey"), landed);
        assertTrue(PoliticalHierarchy.isLanded(holdstewik, landed));
        // Stale region_name ("Limmenthal Region") must not count as land.
        assertFalse(PoliticalHierarchy.isLanded(limmenthal, landed));
    }

    @Test void withoutRegionDataTheOldRecordHeuristicIsUsed() {
        assertNull(PoliticalHierarchy.landedIds(List.of()));
        assertNull(PoliticalHierarchy.landedIds(null));
        assertEquals(limmenthal.isLanded(), PoliticalHierarchy.isLanded(limmenthal, null));
    }

    @Test void holdstewikGetsItsLandedVassalsViaLordName() {
        Map<String, String> parents = PoliticalHierarchy.parents(all, PoliticalHierarchy.landedIds(regions));
        assertEquals("hold", parents.get("mayen"));
        assertEquals("hold", parents.get("nord"));
        assertNull(parents.get("hold"));
    }

    @Test void vassalOfVassalKeepsDepthTwo() {
        Map<String, String> parents = PoliticalHierarchy.parents(all, PoliticalHierarchy.landedIds(regions));
        assertEquals("nord", parents.get("aeg"));
        assertEquals("hold", parents.get(parents.get("aeg")));
    }

    @Test void unlandedFactionsGetNoParentEvenWithStaleLordName() {
        Map<String, String> parents = PoliticalHierarchy.parents(all, PoliticalHierarchy.landedIds(regions));
        assertFalse(parents.containsKey("limm"));
        assertFalse(parents.containsKey("auen"));
        assertEquals(3, parents.size());
    }

    @Test void landedFactionWithUnlandedLiegeHasNoParent() {
        FactionRecord orphan = faction("orph", "Orphan", "Limmenthal");
        List<RegionRecord> withOrphan = List.of(region("O", orphan), region("F", freyford));
        Map<String, String> parents = PoliticalHierarchy.parents(List.of(limmenthal, orphan, freyford),
                PoliticalHierarchy.landedIds(withOrphan));
        assertTrue(parents.isEmpty());
    }

    @Test void legacyVassalListsStillWorkButOnlyForLandedFactions() {
        FactionRecord lord = new FactionRecord("hold", "r", "hold", "Holdstewik", "Dorf", "L", "",
                null, null, null, "external", null, "X", 1, 1,
                null, List.of("mayen", "limm"), List.of("mayen", "limm"), 0, 0, null, null);
        Map<String, String> parents = PoliticalHierarchy.parents(List.of(lord, mayenburg, limmenthal),
                Set.of("hold", "mayen"));
        assertEquals("hold", parents.get("mayen"));
        assertFalse(parents.containsKey("limm"));
    }
}
