package de.ottoextra.horses.ui;

import com.google.gson.Gson;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.ottoextra.horses.ui.HorseOverviewView.AnimalKind.*;
import static org.junit.jupiter.api.Assertions.*;

class HorseOverviewFilterTest {
    @Test
    void defaultsOfferAllHorsesAndDonkeysAndClassifyServerAliases() {
        var config = new OttoExtraConfig.AnimalInfo();
        assertEquals(List.of(ALL, HORSES, DONKEYS), HorseOverviewView.filters(config));
        var aliases = Map.ofEntries(Map.entry("Packesel", DONKEYS), Map.entry("Lastesel", DONKEYS),
                Map.entry("Maultier", DONKEYS), Map.entry("minecraft:donkey", DONKEYS),
                Map.entry("Mule", DONKEYS), Map.entry("Streitross", HORSES), Map.entry("Ackergaul", HORSES),
                Map.entry("Reitpferd", HORSES), Map.entry("Rotvieh Rind", COWS), Map.entry("Brakel", CHICKENS),
                Map.entry("Hausschwein", PIGS), Map.entry("Ziege", GOATS), Map.entry("Zaupel", SHEEP));
        aliases.forEach((name, kind) -> assertEquals(kind, HorseOverviewView.kind(record(name)), name));
    }

    @Test
    void optionalTypesFilterIndependentlyWithoutChangingSourceRecords() {
        var config = new OttoExtraConfig.AnimalInfo();
        var records = List.of(record("Reitpferd"), record("Packesel"), record("Rotvieh Rind"), record("Brakel"),
                record("Hausschwein"), record("Ziege"), record("Zaupel"));
        assertEquals(2, HorseOverviewView.filter(records, ALL, config).size());
        assertEquals(List.of(records.get(1)), HorseOverviewView.filter(records, DONKEYS, config));
        config.overviewShowCows = true;
        assertEquals(List.of(ALL, HORSES, DONKEYS, COWS), HorseOverviewView.filters(config));
        assertEquals(3, HorseOverviewView.filter(records, ALL, config).size());
        config.overviewShowChickens = config.overviewShowPigs = config.overviewShowGoats = config.overviewShowSheep = true;
        assertEquals(8, HorseOverviewView.filters(config).size());
        assertEquals(7, HorseOverviewView.filter(records, ALL, config).size());
        for (var kind : HorseOverviewView.filters(config).subList(1, 8)) {
            assertEquals(1, HorseOverviewView.filter(records, kind, config).size());
        }
        config.overviewShowCows = false;
        assertTrue(HorseOverviewView.filter(records, COWS, config).isEmpty());
        assertEquals(7, records.size());
    }

    @Test
    void unknownLegacyTypesRemainInAllAndNamesUseQuestionMarksWithUuidSearch() {
        var unknown = record("");
        var config = new OttoExtraConfig.AnimalInfo();
        assertEquals(OTHER, HorseOverviewView.kind(unknown));
        assertEquals(List.of(unknown), HorseOverviewView.filter(List.of(unknown), ALL, config));
        assertEquals("???", HorseOverviewView.displayName(unknown));
        assertEquals(1, HorseOverviewView.groups(List.of(unknown), unknown.entityUuid(),
                HorseOverviewView.SortOrder.FARMS_ASCENDING).size());
        assertEquals("", unknown.registryName());
    }

    private static AnimalBreedingHorse record(String type) {
        return new Gson().fromJson("{\"entityUuid\":\"00000000-0000-0000-0000-000000000001\",\"animalType\":\""
                + type + "\"}", AnimalBreedingHorse.class);
    }
}
