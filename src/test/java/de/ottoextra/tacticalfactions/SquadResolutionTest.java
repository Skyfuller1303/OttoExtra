package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SquadResolutionTest {
    private static final UUID EVE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private static PoliticalStateService service(TacticalConfig config, boolean freshSnapshot) throws Exception {
        PoliticalStateService service = new PoliticalStateService(config);
        PlayerAffiliation eve = new PlayerAffiliation(EVE.toString(), "Eve_1", "enemy");
        PoliticalStateService.Snapshot snapshot = new PoliticalStateService.Snapshot(
                Map.of("enemy", new FactionNode("enemy", "Enemy", null)), Map.of(),
                Map.of(EVE.toString(), eve), Map.of("eve_1", eve), List.of(), null,
                freshSnapshot ? System.currentTimeMillis() : 0L);
        Field field = PoliticalStateService.class.getDeclaredField("snapshot");
        field.setAccessible(true);
        field.set(service, snapshot);
        return service;
    }

    private static Squad squad(String name, boolean allied, String... accounts) {
        Squad squad = Squad.create(name, "#55C46A");
        squad.alwaysAllied = allied;
        for (String account : accounts) squad.members.add(new SquadMember(account, null, false));
        return squad;
    }

    @Test void squadMemberIsAlliedDespiteHostileFactionAndManualRelation() throws Exception {
        TacticalConfig config = new TacticalConfig();
        config.factionRelations.put("enemy", Relation.HOSTILE);
        config.accountRelations.put("eve_1", Relation.HOSTILE);
        config.playerRelations.put(EVE.toString(), Relation.HOSTILE);
        config.squads.add(squad("Vorhut", true, "Eve_1"));
        PoliticalStateService service = service(config, true);

        Resolution protectionPath = service.resolve(EVE, "Eve_1");
        assertEquals(Relation.ALLIED, protectionPath.relation());
        assertEquals("squad:Vorhut", protectionPath.source());
        assertEquals(Relation.ALLIED, service.resolveForDisplay(EVE, "Eve_1").relation());
        // Squads are local data: they do not depend on the API snapshot being fresh.
        assertEquals(Relation.ALLIED, service(config, false).resolve(EVE, "eve_1").relation());
    }

    @Test void nonMembersAreUnaffected() throws Exception {
        TacticalConfig config = new TacticalConfig();
        config.factionRelations.put("enemy", Relation.HOSTILE);
        config.squads.add(squad("Vorhut", true, "Someone_Else"));
        PoliticalStateService service = service(config, true);
        assertEquals(Relation.HOSTILE, service.resolve(EVE, "Eve_1").relation());
        assertNull(service.squadFor(EVE, "Eve_1"));
    }

    @Test void firstSquadInListOrderWins() throws Exception {
        TacticalConfig config = new TacticalConfig();
        config.factionRelations.put("enemy", Relation.HOSTILE);
        Squad first = squad("Erster", false, "Eve_1");
        Squad second = squad("Zweiter", true, "Eve_1");
        config.squads.add(first);
        config.squads.add(second);
        PoliticalStateService service = service(config, true);

        assertSame(first, service.squadFor(EVE, "Eve_1"));
        // The first squad does not force an alliance, and the later one must not take over.
        assertEquals(Relation.HOSTILE, service.resolve(EVE, "Eve_1").relation());

        config.squads.clear();
        config.squads.add(second);
        config.squads.add(first);
        assertEquals("squad:Zweiter", service.resolve(EVE, "Eve_1").source());
    }

    @Test void squadColorOnlyAppliesToSquadResolutions() throws Exception {
        TacticalConfig config = new TacticalConfig();
        Squad squad = squad("Vorhut", true, "Eve_1");
        squad.color = "#123456";
        config.squads.add(squad);
        PoliticalStateService service = service(config, true);
        assertEquals("#123456", service.squadColor(service.resolve(EVE, "Eve_1"), EVE, "Eve_1"));
        assertNull(service.squadColor(new Resolution(Relation.ALLIED, "faction"), EVE, "Eve_1"));
    }

    @Test void tablistTopRanksSquadsInSquadOrderAheadOfRelations() throws Exception {
        TacticalConfig config = new TacticalConfig();
        Squad a = squad("A", true, "Alpha_1");
        Squad b = squad("B", true, "Bravo_1");
        b.tablistTop = false;
        Squad c = squad("C", true, "Charlie_1");
        config.squads.addAll(List.of(a, b, c));
        PoliticalStateService service = service(config, true);
        assertEquals(0, service.tablistSquadIndex(null, "alpha_1"));
        assertEquals(-1, service.tablistSquadIndex(null, "Bravo_1"));
        assertEquals(2, service.tablistSquadIndex(null, "Charlie_1"));
        assertEquals(-1, service.tablistSquadIndex(null, "Nobody"));

        assertTrue(TablistRelations.groupRank(2, Relation.NEUTRAL, false)
                < TablistRelations.groupRank(-1, Relation.ALLIED, false));
        assertTrue(TablistRelations.groupRank(0, Relation.NEUTRAL, true)
                < TablistRelations.groupRank(2, Relation.HOSTILE, true));
        assertEquals(TablistRelations.relationRank(Relation.HOSTILE, true),
                TablistRelations.groupRank(-1, Relation.HOSTILE, true));
    }
}
