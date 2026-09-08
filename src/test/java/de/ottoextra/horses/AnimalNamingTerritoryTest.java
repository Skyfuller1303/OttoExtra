package de.ottoextra.horses;

import com.google.gson.Gson;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.map.LehenPolygon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AnimalNamingTerritoryTest {
    private static final Gson GSON = new Gson();
    private static final List<LehenPolygon> LEHEN = List.of(
            square("lehen_1", 0, 0, 100, 100), square("lehen_2", 101, 0, 200, 100));

    @ParameterizedTest
    @ValueSource(strings = {"1", "lehen_1", " LEHEN_001 "})
    void unnamedAnimalCanOnlyBeNamedInsideOwnLehen(String membership) {
        PlayerRecord player = player(membership);
        assertTrue(AnimalNamingTerritory.allows(player, key -> null, location(20, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player, key -> null, location(150, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player, key -> null, location(250, 20), LEHEN));
    }

    @Test
    void resolvesFactionUuidWithoutGrantingVassalOrForeignLehenAccess() {
        String uuid = "00000000-0000-0000-0000-000000000051";
        FactionRecord faction = GSON.fromJson("{\"uuid\":\"" + uuid
                + "\",\"region_id\":\"1\",\"vassal_names\":[\"lehen_2\"]}", FactionRecord.class);
        Function<String, FactionRecord> factions = key -> uuid.equals(key) ? faction : null;

        assertTrue(AnimalNamingTerritory.allows(player(uuid), factions, location(20, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player(uuid), factions, location(150, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player("unknown"), factions, location(20, 20), LEHEN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "lehen_1", " LEHEN_001 "})
    void regionSummaryMembershipUsesFactionReferenceWhenRegionIdIsMissing(String reference) {
        String uuid = "00000000-0000-0000-0000-000000000051";
        // Region current_faction summaries omit region_id but retain faction_ref.
        FactionRecord faction = GSON.fromJson("{\"uuid\":\"" + uuid
                + "\",\"faction_ref\":" + GSON.toJson(reference)
                + ",\"vassal_names\":[\"lehen_2\"]}", FactionRecord.class);
        Function<String, FactionRecord> factions = key -> uuid.equals(key) ? faction : null;

        assertTrue(AnimalNamingTerritory.allows(player(uuid), factions, location(20, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player(uuid), factions, location(150, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player(uuid), factions, location(250, 20), LEHEN));
    }

    @Test
    void factionUuidWithoutTerritorialReferenceDoesNotGrantNaming() {
        String uuid = "00000000-0000-0000-0000-000000000051";
        FactionRecord faction = GSON.fromJson("{\"uuid\":\"" + uuid
                + "\",\"faction_ref\":\"" + uuid + "\",\"region_name\":\"Lehen 1\"}",
                FactionRecord.class);
        assertFalse(AnimalNamingTerritory.allows(player(uuid), key -> faction, location(20, 20), LEHEN));
    }

    @Test
    void missingMembershipLocationOrBoundariesNeverEnablesInitialNaming() {
        assertFalse(AnimalNamingTerritory.allows(null, key -> null, location(20, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player(""), key -> null, location(20, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player("lehen_1"), key -> null, null, LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player("lehen_1"), key -> null, location(20, 20), List.of()));
        assertFalse(AnimalNamingTerritory.allows(player("lehen_1"), key -> null,
                new AnimalBreedingHorseLocation("minecraft:the_nether", 20, 64, 20), LEHEN));
    }

    @Test
    void foreignEnclaveInsideOwnLehenRemainsReadOnly() {
        List<LehenPolygon> nested = List.of(square("lehen_1", 0, 0, 100, 100),
                square("lehen_2", 10, 10, 30, 30));
        assertEquals("lehen_2", AnimalNamingTerritory.regionAt(20, 20, nested));
        assertFalse(AnimalNamingTerritory.allows(player("lehen_1"), key -> null, location(20, 20), nested));
        assertTrue(AnimalNamingTerritory.allows(player("lehen_2"), key -> null, location(20, 20), nested));
    }

    @Test
    void polygonPartsAndBoundaryPointsUseTheServerRegionIdentity() {
        List<LehenPolygon> part = List.of(square("lehen_1#2", -100, -100, 0, 0));
        assertEquals("lehen_1", AnimalNamingTerritory.regionAt(-100, -100, part));
        assertTrue(AnimalNamingTerritory.allows(player("1"), key -> null, location(-100, -50), part));
        assertFalse(AnimalNamingTerritory.allows(player("1"), key -> null, location(-101, -50), part));
    }

    @Test
    void rechecksTheAnimalLocationAfterCrossingIntoAnotherLehen() {
        PlayerRecord player = player("lehen_1");
        assertTrue(AnimalNamingTerritory.allows(player, key -> null, location(99, 20), LEHEN));
        assertFalse(AnimalNamingTerritory.allows(player, key -> null, location(102, 20), LEHEN));
    }

    private static PlayerRecord player(String faction) {
        return GSON.fromJson("{\"faction\":" + GSON.toJson(faction) + "}", PlayerRecord.class);
    }

    private static AnimalBreedingHorseLocation location(int x, int z) {
        return new AnimalBreedingHorseLocation("minecraft:overworld", x, 64, z);
    }

    private static LehenPolygon square(String key, double x1, double z1, double x2, double z2) {
        return new LehenPolygon(key, new double[]{x1, x2, x2, x1}, new double[]{z1, z1, z2, z2},
                (x1 + x2) / 2, (z1 + z2) / 2, x1, z1, x2, z2, true);
    }
}
