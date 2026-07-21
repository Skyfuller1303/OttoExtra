package de.ottoextra.playerlist;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListSortKeyTest {
    @Test
    void sortsByLehenThenRoleTitleHierarchyAndName() {
        PlayerListSortKey memberGraf = key("b-lehen", 2, 800, 0, "berta");
        PlayerListSortKey leaderPriest = key("b-lehen", 0, 700, 4, "ludwig");
        PlayerListSortKey deputyFreiherr = key("b-lehen", 1, 800, 2, "friedrich");
        PlayerListSortKey earlierLehenMember = key("a-lehen", 2, 100, 9, "anna");
        PlayerListSortKey unknown = new PlayerListSortKey(false, "", 0, 0,
                "", 0, "", "", "unknown", UUID.randomUUID());

        List<PlayerListSortKey> sorted = new ArrayList<>(List.of(
                unknown, memberGraf, deputyFreiherr, leaderPriest, earlierLehenMember));
        sorted.sort(PlayerListSortKey::compare);

        assertEquals(List.of(earlierLehenMember, leaderPriest, deputyFreiherr,
                memberGraf, unknown), sorted);
    }

    @Test
    void sortsHigherTitleGroupAndEarlierTitleFirstAfterRole() {
        PlayerListSortKey cleric = key("lehen", 2, 700, 0, "abt");
        PlayerListSortKey freiherr = key("lehen", 2, 800, 2, "freiherr");
        PlayerListSortKey graf = key("lehen", 2, 800, 0, "graf");

        assertTrue(PlayerListSortKey.compare(graf, freiherr) < 0);
        assertTrue(PlayerListSortKey.compare(freiherr, cleric) < 0);
        assertTrue(PlayerListSortKey.compare(graf, cleric) < 0);
    }

    @Test
    void unknownKeysRemainEqualForVanillaTieBreaker() {
        PlayerListSortKey first = new PlayerListSortKey(false, "", 0, 0,
                "", 0, "", "", "zeta", UUID.randomUUID());
        PlayerListSortKey second = new PlayerListSortKey(false, "", 0, 0,
                "", 0, "", "", "alpha", UUID.randomUUID());

        assertEquals(0, PlayerListSortKey.compare(first, second));
    }

    private static PlayerListSortKey key(String lehen, int roleOrder,
                                         int groupPriority, int titleOrder,
                                         String visibleName) {
        return new PlayerListSortKey(true, lehen, roleOrder, groupPriority,
                groupPriority == 800 ? "adel" : "klerus", titleOrder,
                "title", visibleName, visibleName, UUID.randomUUID());
    }
}
