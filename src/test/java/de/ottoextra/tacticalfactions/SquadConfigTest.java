package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SquadConfigTest {
    @TempDir Path temp;

    @Test void freshConfigIsSchemaFiveWithNoSquadsAndNoLegacyField() throws Exception {
        Path file = temp.resolve("config.json");
        TacticalConfig config = TacticalConfig.load(file);
        assertEquals(5, TacticalConfig.CURRENT_SCHEMA);
        assertEquals(5, config.schemaVersion);
        assertTrue(config.squads.isEmpty());
        assertNull(config.playerLoginAlerts);
        assertFalse(Files.readString(file).contains("playerLoginAlerts"));
    }

    @Test void squadDefaultsAreAlliedAndTablistTopButNotFriendlyFireProtected() {
        Squad squad = Squad.create("Vorhut", "#55C46A");
        assertTrue(squad.alwaysAllied);
        assertTrue(squad.tablistTop);
        assertFalse(squad.friendlyFireProtection);
        assertTrue(squad.id.startsWith("squad:"));
    }

    @Test void repairValidatesNamesColorsMembersAndDeduplicates() {
        TacticalConfig config = new TacticalConfig();
        Squad broken = new Squad();
        broken.id = "  ";
        broken.name = "   ";
        broken.color = "green";
        broken.members = new ArrayList<>(List.of(
                new SquadMember(" Alice_1 ", "not-a-uuid", true),
                new SquadMember("alice_1", null, false),
                new SquadMember("ab", null, false),
                new SquadMember("has space", null, false),
                new SquadMember("WayTooLongAccountName1", null, false),
                new SquadMember("Bob", "00000000-0000-0000-0000-000000000002", false)));
        Squad twin = Squad.create("Hafenwache", "#abcdef");
        twin.id = "squad:same";
        Squad twin2 = Squad.create("Reserve", "#4FA3C7");
        twin2.id = "squad:same";
        twin2.members = null;
        config.squads = new ArrayList<>(List.of(broken, twin, twin2));
        config.squads.add(null);

        config.repair();

        assertEquals(3, config.squads.size());
        Squad first = config.squads.get(0);
        assertTrue(first.id.startsWith("squad:"));
        assertEquals("Trupp 1", first.name);
        assertEquals(TacticalConfig.SQUAD_COLORS[0], first.color);
        assertEquals(2, first.members.size());
        assertEquals("Alice_1", first.members.get(0).account);
        assertNull(first.members.get(0).uuid);
        assertTrue(first.members.get(0).leader);
        assertEquals("00000000-0000-0000-0000-000000000002", first.members.get(1).uuid);
        assertEquals("#ABCDEF", config.squads.get(1).color);
        assertEquals("squad:same", config.squads.get(1).id);
        assertNotEquals("squad:same", config.squads.get(2).id);
        assertNotNull(config.squads.get(2).members);
        assertTrue(config.squads.get(2).members.isEmpty());
    }

    @Test void squadsKeepOrderAndRoundTrip() {
        Path file = temp.resolve("config.json");
        TacticalConfig config = TacticalConfig.load(file);
        Squad a = Squad.create("Vorhut", "#55C46A");
        a.members.add(new SquadMember("Merle_1", null, true));
        a.friendlyFireProtection = true;
        Squad b = Squad.create("Reserve", "#B07AC9");
        b.alwaysAllied = false;
        config.squads.add(a);
        config.squads.add(b);
        config.save(file);

        TacticalConfig loaded = TacticalConfig.load(file);
        assertEquals(List.of("Vorhut", "Reserve"), loaded.squads.stream().map(s -> s.name).toList());
        assertEquals("Merle_1", loaded.squads.get(0).members.get(0).account);
        assertTrue(loaded.squads.get(0).members.get(0).leader);
        assertTrue(loaded.squads.get(0).friendlyFireProtection);
        assertFalse(loaded.squads.get(1).alwaysAllied);
        assertSame(loaded.squads.get(1), loaded.findSquad(b.id));
    }

    @Test void loginAlertsAreMigratedIntoOneSquadAndTheFieldIsDropped() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file, """
                {"schemaVersion":4,
                 "playerLoginAlerts":{"alice":"Alice","bob_2":"Bob_2","x":"x","dup":"ALICE"}}
                """);

        TacticalConfig config = TacticalConfig.load(file);

        assertEquals(1, config.squads.size());
        Squad wache = config.squads.get(0);
        assertEquals("Wache", wache.name);
        assertEquals(List.of("Alice", "Bob_2"), wache.members.stream().map(m -> m.account).toList());
        // The old feature only announced logins, so no squad effect may switch on by migration.
        assertFalse(wache.alwaysAllied);
        assertFalse(wache.tablistTop);
        assertFalse(wache.friendlyFireProtection);
        assertNull(config.playerLoginAlerts);
        assertEquals(5, config.schemaVersion);
        String saved = Files.readString(file);
        assertFalse(saved.contains("playerLoginAlerts"));

        // Loading the migrated file again must not add a second squad.
        assertEquals(1, TacticalConfig.load(file).squads.size());
    }

    @Test void emptyOrInvalidLoginAlertsCreateNoSquad() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file, "{\"schemaVersion\":4,\"playerLoginAlerts\":{\"x\":\"x\"}}");
        assertTrue(TacticalConfig.load(file).squads.isEmpty());
        Files.writeString(file, "{\"schemaVersion\":4,\"playerLoginAlerts\":{}}");
        assertTrue(TacticalConfig.load(file).squads.isEmpty());
    }

    @Test void migratedSquadIsAppendedAfterExistingSquads() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file, """
                {"squads":[{"id":"squad:one","name":"Vorhut","color":"#55C46A","members":[]}],
                 "playerLoginAlerts":{"alice":"Alice"}}
                """);
        TacticalConfig config = TacticalConfig.load(file);
        assertEquals(List.of("Vorhut", "Wache"), config.squads.stream().map(s -> s.name).toList());
    }

    @Test void politicsViewPersistsAndBrokenValuesFallBackToRealms() throws Exception {
        Path file = temp.resolve("config.json");
        TacticalConfig config = TacticalConfig.load(file);
        assertEquals(PoliticsView.REALMS, config.politicsView);
        config.politicsView = PoliticsView.BOARD;
        config.save(file);
        assertEquals(PoliticsView.BOARD, TacticalConfig.load(file).politicsView);

        Files.writeString(file, "{\"politicsView\":\"SIDEWAYS\"}");
        assertEquals(PoliticsView.REALMS, TacticalConfig.load(file).politicsView);
        Files.writeString(file, "{\"politicsView\":null}");
        assertEquals(PoliticsView.REALMS, TacticalConfig.load(file).politicsView);
    }

    @Test void membershipMatchesByUuidOrCaseInsensitiveAccount() {
        Squad squad = Squad.create("S", "#55C46A");
        squad.members.add(new SquadMember("Alice_1", "00000000-0000-0000-0000-000000000001", false));
        assertTrue(squad.contains("00000000-0000-0000-0000-000000000001", "Renamed"));
        assertTrue(squad.contains(null, "ALICE_1"));
        assertFalse(squad.contains(null, "Bob"));
        assertFalse(squad.contains(null, null));
    }

    @Test void onlineCountUsesOnlyNamesAndUuidsFromTheTabList() {
        Squad squad = Squad.create("S", "#55C46A");
        squad.members.add(new SquadMember("Alice_1", null, false));
        squad.members.add(new SquadMember("Bob_2", "00000000-0000-0000-0000-000000000002", false));
        squad.members.add(new SquadMember("Cara_3", null, false));
        assertEquals(2, squad.onlineCount(java.util.Set.of("alice_1",
                "00000000-0000-0000-0000-000000000002")));
        assertEquals(0, squad.onlineCount(java.util.Set.of()));
    }
}
