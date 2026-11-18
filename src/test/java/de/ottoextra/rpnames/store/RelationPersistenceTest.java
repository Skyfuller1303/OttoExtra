package de.ottoextra.rpnames.store;

import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.Relation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationPersistenceTest {

    @Test
    void serverGuiImportNeverOverwritesRelation() {
        LocalRpIdentityStore store = new LocalRpIdentityStore(false);
        store.importServerGui("Acc", "u-1", "Johann", "Schmied", "Gemein");
        store.find("u-1", "Acc").orElseThrow().relation = Relation.FOE;

        store.importServerGui("Acc", "u-1", "Johann II", "Großschmied", "Adelig");

        assertEquals(Relation.FOE, store.find("u-1", "Acc").orElseThrow().relation);
    }

    @Test
    void manualEditKeepsRelationUnlessExplicitlyChanged() {
        LocalRpIdentityStore store = new LocalRpIdentityStore(false);
        LocalRpProfile profile = store.ensureSeen("Acc2", "u-2",
                de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
        profile.relation = Relation.FRIEND;

        LocalRpProfile updated = store.updateManual("Acc2", "u-2", p -> p.notes = "Notiz", false);

        assertEquals(Relation.FRIEND, updated.relation);
    }
}
