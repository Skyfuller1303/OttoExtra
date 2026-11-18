package de.ottoextra.rpnames.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationTest {

    private static final Gson GSON = new Gson();

    @Test
    void legacyProfileWithoutRelationDefaultsToNone() {
        LocalRpProfile profile = GSON.fromJson("{\"accountName\":\"Ansgar_K\",\"rpName\":\"Ansgar\"}",
                LocalRpProfile.class);
        profile.repair();
        assertEquals(Relation.NONE, profile.relation);
    }

    @Test
    void relationSurvivesJsonRoundTrip() {
        LocalRpProfile profile = new LocalRpProfile();
        profile.accountName = "Wido_W";
        profile.relation = Relation.FRIEND;
        LocalRpProfile back = GSON.fromJson(GSON.toJson(profile), LocalRpProfile.class);
        back.repair();
        assertEquals(Relation.FRIEND, back.relation);
    }

    @Test
    void unknownOrNullRelationIsRepairedToNone() {
        LocalRpProfile profile = GSON.fromJson("{\"accountName\":\"x\",\"relation\":\"BESTIE\"}",
                LocalRpProfile.class);
        profile.repair();
        assertEquals(Relation.NONE, profile.relation);
        profile.relation = null;
        profile.repair();
        assertEquals(Relation.NONE, profile.relation);
    }
}
