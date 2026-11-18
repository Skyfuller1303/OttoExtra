package de.ottoextra.tacticalfactions;

import de.ottoextra.tacticalfactions.PoliticalStateService.FactionView;
import de.ottoextra.tacticalfactions.RealmGrouping.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RealmGroupingTest {
    private static FactionView view(String id, String rootId, Relation relation, boolean custom) {
        return new FactionView(id, id, id, rootId, rootId, new Resolution(relation, "faction"), custom,
                custom ? PoliticalEntityType.FIEF : PoliticalEntityType.API, "", 0, null, null, null, null,
                0xFF777777, true, null, id, id.equals(rootId) ? 0 : 1);
    }

    @Test void groupsByTopLiegeKeepingRealmAndVassalOrder() {
        List<FactionView> views = List.of(
                view("hold", "hold", Relation.ALLIED, false),
                view("geldern", "hold", Relation.ALLIED, false),
                view("kelter", "kelter", Relation.HOSTILE, false),
                view("wyndham", "hold", Relation.ALLIED, false),
                view("suedwall", "kelter", Relation.HOSTILE, false),
                view("lonely", "lonely", Relation.NEUTRAL, false));

        List<Realm> realms = RealmGrouping.group(views);

        assertEquals(List.of("hold", "kelter", "lonely"), realms.stream().map(r -> r.root().id()).toList());
        assertEquals(List.of("geldern", "wyndham"), realms.get(0).vassals().stream().map(FactionView::id).toList());
        assertEquals(2, realms.get(0).total());
        assertEquals(1, realms.get(1).total());
        assertTrue(realms.get(2).vassals().isEmpty());
        assertEquals(3, RealmGrouping.totalVassals(realms));
    }

    @Test void emptyInputGivesNoRealms() {
        assertTrue(RealmGrouping.group(List.of()).isEmpty());
        assertTrue(RealmGrouping.group(null).isEmpty());
    }

    @Test void localGroupsFormTheirOwnRealmsAndCanBeVassalsOfApiRealms() {
        List<FactionView> views = List.of(
                view("hold", "hold", Relation.ALLIED, false),
                view("custom:a", "hold", Relation.ALLIED, true),
                view("custom:b", "custom:b", Relation.HOSTILE, true));
        List<Realm> realms = RealmGrouping.group(views);
        assertEquals(2, realms.size());
        assertEquals("custom:a", realms.get(0).vassals().get(0).id());
        assertEquals("custom:b", realms.get(1).root().id());
    }

    @Test void missingRootPromotesFirstMemberInsteadOfDroppingEntries() {
        List<Realm> realms = RealmGrouping.group(List.of(
                view("b", "ghost", Relation.NEUTRAL, false),
                view("c", "ghost", Relation.NEUTRAL, false)));
        assertEquals(1, realms.size());
        assertEquals("b", realms.get(0).root().id());
        assertEquals(List.of("c"), realms.get(0).vassals().stream().map(FactionView::id).toList());
    }

    @Test void filterKeepsHeaderForMatchingVassalsAndDropsOthers() {
        List<Realm> realms = RealmGrouping.group(List.of(
                view("hold", "hold", Relation.ALLIED, false),
                view("geldern", "hold", Relation.ALLIED, false),
                view("rabenfurt", "hold", Relation.NEUTRAL, false),
                view("kelter", "kelter", Relation.HOSTILE, false),
                view("suedwall", "kelter", Relation.HOSTILE, false)));

        List<Realm> neutral = RealmGrouping.filter(realms,
                v -> v.resolution().relation() == Relation.NEUTRAL);

        assertEquals(1, neutral.size());
        assertEquals("hold", neutral.get(0).root().id());
        assertEquals(List.of("rabenfurt"), neutral.get(0).vassals().stream().map(FactionView::id).toList());
        // The header still reports the real number of fiefs, not just the matching ones.
        assertEquals(2, neutral.get(0).total());

        List<Realm> allied = RealmGrouping.filter(realms, v -> v.resolution().relation() == Relation.ALLIED);
        assertEquals(List.of("geldern"), allied.get(0).vassals().stream().map(FactionView::id).toList());

        assertTrue(RealmGrouping.filter(realms, v -> false).isEmpty());
        assertEquals(2, RealmGrouping.filter(realms, v -> true).size());
    }
}
