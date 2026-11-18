package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SquadHudModelTest {
    private static Squad squad(String name, String... accounts) {
        Squad squad = Squad.create(name, "#55C46A");
        for (String account : accounts) squad.members.add(new SquadMember(account, null, false));
        return squad;
    }

    private final List<Squad> squads = List.of(squad("Vorhut", "Alice_1", "Bob_2", "Cara_3"),
            squad("Leer"), squad("Reserve", "Dan_4"));

    @Test void keepsSquadOrderAndHidesSquadsWithoutMembers() {
        var lines = SquadHudModel.build(squads, Set.of("alice_1"), true, false);
        assertEquals(List.of("Vorhut", "Vorhut", "Vorhut", "Vorhut", "Reserve", "Reserve"),
                lines.stream().map(l -> l.squad().name).toList());
        assertTrue(lines.get(0).header());
        assertTrue(lines.get(4).header());
        assertTrue(SquadHudModel.build(List.of(squad("Leer")), Set.of(), true, false).isEmpty());
        assertTrue(SquadHudModel.build(null, Set.of(), true, false).isEmpty());
    }

    @Test void headersCarryOnlineCountAndTotal() {
        var lines = SquadHudModel.build(squads, Set.of("alice_1", "cara_3"), true, false);
        assertEquals(2, lines.get(0).onlineCount());
        assertEquals(3, lines.get(0).total());
        assertEquals(0, lines.get(4).onlineCount());
        assertEquals(1, lines.get(4).total());
        assertEquals(2, SquadHudModel.totalOnline(lines));
        assertEquals(4, SquadHudModel.totalMembers(lines));
    }

    @Test void memberRowsMarkOnlineAndCanBeSwitchedOff() {
        var lines = SquadHudModel.build(squads, Set.of("bob_2"), true, false);
        assertFalse(lines.get(1).online());
        assertTrue(lines.get(2).online());
        var headersOnly = SquadHudModel.build(squads, Set.of("bob_2"), false, false);
        assertEquals(2, headersOnly.size());
        assertTrue(headersOnly.stream().allMatch(SquadHudModel.Line::header));
    }

    @Test void onlyOnlineFilterKeepsHeadersButDropsOfflineMembers() {
        var lines = SquadHudModel.build(squads, Set.of("bob_2"), true, true);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).header());
        assertEquals("Bob_2", lines.get(1).member().account);
        assertTrue(lines.get(2).header());
        assertEquals(0, lines.get(2).onlineCount());
    }

    @Test void configDefaultsAndRepairForHudFields() {
        TacticalConfig config = new TacticalConfig();
        assertFalse(config.squadHudEnabled);
        assertTrue(config.squadHudMembers);
        assertFalse(config.squadHudOnlyOnline);
        assertEquals(HudCorner.TOP_RIGHT, config.squadHudCorner);
        config.squadHudCorner = null;
        config.repair();
        assertEquals(HudCorner.TOP_RIGHT, config.squadHudCorner);
        assertEquals(HudCorner.TOP_RIGHT, HudCorner.BOTTOM_RIGHT.next().next());
        assertEquals(HudCorner.BOTTOM_RIGHT, HudCorner.TOP_RIGHT.flipVertical());
    }

    private static Squad big() {
        Squad squad = squad("Big", "A_1", "B_2", "C_3", "D_4", "E_5");
        squad.members.get(4).leader = true;
        return squad;
    }

    @Test void hiddenSquadsAreSkippedAndDoNotCountInTotals() {
        Squad hidden = squad("Hidden", "Zed_1");
        hidden.showInHud = false;
        var lines = SquadHudModel.build(List.of(squads.get(0), hidden, squads.get(2)),
                Set.of("zed_1", "alice_1"), true, false);
        assertTrue(lines.stream().noneMatch(l -> l.squad() == hidden));
        assertEquals(1, SquadHudModel.totalOnline(lines));
        assertEquals(4, SquadHudModel.totalMembers(lines));
        assertTrue(new Squad().showInHud);
    }

    @Test void truncationKeepsOnlineFirstInListOrderAndAddsMoreRow() {
        var lines = SquadHudModel.build(List.of(big()), Set.of("b_2", "d_4"), true, false, 3);
        // header + 3 members + "+2" row; leader E_5 (offline) is kept by displacing the last pick.
        assertEquals(5, lines.size());
        assertEquals(List.of("B_2", "D_4", "E_5"), lines.subList(1, 4).stream().map(l -> l.member().account).toList());
        assertTrue(lines.get(4).moreRow());
        assertEquals(2, lines.get(4).more());
        assertEquals(5, lines.get(0).total());
        assertEquals(2, lines.get(0).onlineCount());
    }

    @Test void truncationWithoutLeaderTakesOnlineThenOfflineInListOrder() {
        Squad squad = squad("S", "A_1", "B_2", "C_3", "D_4");
        var lines = SquadHudModel.build(List.of(squad), Set.of("c_3"), true, false, 2);
        assertEquals(List.of("C_3", "A_1"), lines.subList(1, 3).stream().map(l -> l.member().account).toList());
        assertEquals(2, lines.get(3).more());
    }

    @Test void noMoreRowWhenEverythingFitsOrUnlimited() {
        assertTrue(SquadHudModel.build(List.of(big()), Set.of(), true, false, 5).stream().noneMatch(SquadHudModel.Line::moreRow));
        assertTrue(SquadHudModel.build(List.of(big()), Set.of(), true, false, 0).stream().noneMatch(SquadHudModel.Line::moreRow));
        assertEquals(6, SquadHudModel.build(List.of(big()), Set.of(), true, false, 0).size());
    }

    @Test void maxMembersInteractsWithOnlyOnlineFilter() {
        // Only online members are candidates: 3 online, limit 2 -> 1 hidden; offline leader is not shown.
        var lines = SquadHudModel.build(List.of(big()), Set.of("a_1", "b_2", "c_3"), true, true, 2);
        assertEquals(List.of("A_1", "B_2"), lines.subList(1, 3).stream().map(l -> l.member().account).toList());
        assertEquals(1, lines.get(3).more());
        // Limit above the online count: no "+n" row.
        var all = SquadHudModel.build(List.of(big()), Set.of("a_1", "b_2", "c_3"), true, true, 3);
        assertEquals(4, all.size());
    }

    @Test void maxMembersIsRepairedIntoRange() {
        TacticalConfig config = new TacticalConfig();
        config.squadHudMaxMembers = 99;
        config.repair();
        assertEquals(20, config.squadHudMaxMembers);
        config.squadHudMaxMembers = -5;
        config.repair();
        assertEquals(0, config.squadHudMaxMembers);
    }
}
