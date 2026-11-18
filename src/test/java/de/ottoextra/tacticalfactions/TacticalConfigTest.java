package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class TacticalConfigTest {
    @TempDir Path temp;

    @Test void defaultsAreSafeAndPersistentCombatModeSurvives() {
        Path file = temp.resolve("config.json");
        TacticalConfig config = TacticalConfig.load(file);
        assertFalse(config.combatMode);
        assertTrue(config.showPlayerNames);
        assertTrue(config.politicalColors);
        assertFalse(config.tablistHostilesFirst);
        assertEquals(BuildVariant.friendlyFireProtectionAvailable(), config.preventFriendlyFire);
        assertFalse(config.protectNeutralPlayers);
        assertTrue(config.retaliationMarkers);
        config.combatMode = true;
        config.protectNeutralPlayers = true;
        config.retaliationMarkers = false;
        config.tablistHostilesFirst = true;
        config.save(file);
        TacticalConfig loaded = TacticalConfig.load(file);
        assertTrue(loaded.combatMode);
        assertEquals(BuildVariant.friendlyFireProtectionAvailable(), loaded.protectNeutralPlayers);
        assertFalse(loaded.retaliationMarkers);
        assertTrue(loaded.tablistHostilesFirst);
    }

    @Test void invalidColorsAreRepairedWithoutLosingAssignments() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file, """
                {"allyColor":"blue","hostileColor":"#12345Z",
                 "accountRelations":{"SomePlayer":"ALLIED"},
                 "factionRelations":{"stable-id":"HOSTILE"}}
                """);
        TacticalConfig config = TacticalConfig.load(file);
        assertEquals("#5555FF", config.allyColor);
        assertEquals("#FF5555", config.hostileColor);
        assertEquals(Relation.ALLIED, config.accountRelations.get("someplayer"));
        assertEquals(Relation.HOSTILE, config.factionRelations.get("stable-id"));
    }

    @Test void olderPartialFileGetsMissingMapsAndFields() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file, "{\"combatMode\":true,\"factionRelations\":null}");
        TacticalConfig config = TacticalConfig.load(file);
        assertTrue(config.combatMode);
        assertNotNull(config.factionRelations);
        assertEquals("#5555FF", config.allyColor);
        assertEquals(TacticalConfig.CURRENT_SCHEMA, config.schemaVersion);
        assertNotNull(config.customEntities);
        assertNotNull(config.customPlayerMemberships);
        assertNotNull(config.squads);
        assertEquals(PoliticsView.REALMS, config.politicsView);
    }

    @Test void customGroupsAndMembershipsRoundTripWithStableNamespacedIds() {
        Path file = temp.resolve("config.json");
        TacticalConfig config = TacticalConfig.load(file);
        CustomPoliticalEntity group = new CustomPoliticalEntity("custom:stable-id",
                PoliticalEntityType.FOLLOWING, "Die Garde", "Beschreibung", "api-parent", "coat.png");
        config.customEntities.put(group.id, group);
        config.customPlayerMemberships.put("player-uuid", group.id);
        config.customAccountMemberships.put("SomePlayer", group.id);
        config.save(file);

        TacticalConfig loaded = TacticalConfig.load(file);
        assertEquals("Die Garde", loaded.customEntities.get(group.id).name);
        assertEquals("api-parent", loaded.customEntities.get(group.id).parentId);
        assertEquals(group.id, loaded.customPlayerMemberships.get("player-uuid"));
        assertEquals(group.id, loaded.customAccountMemberships.get("someplayer"));
    }

    @Test void brokenCustomCyclesAndUnsafeBannerPathsAreRepairedWithoutDroppingGroups() {
        TacticalConfig config = new TacticalConfig();
        config.customEntities = new LinkedHashMap<>();
        config.customEntities.put("custom:a", new CustomPoliticalEntity("custom:a",
                PoliticalEntityType.FIEF, "A", "", "custom:b", "../a.png"));
        config.customEntities.put("custom:b", new CustomPoliticalEntity("custom:b",
                PoliticalEntityType.FOLLOWING, "B", "", "custom:a", "not-an-image.txt"));
        config.customAccountMemberships.put("Ghost", "custom:missing");

        config.repair();

        assertEquals(2, config.customEntities.size());
        assertTrue(config.customEntities.get("custom:a").parentId == null
                || config.customEntities.get("custom:b").parentId == null);
        assertEquals("a.png", config.customEntities.get("custom:a").bannerFile);
        assertNull(config.customEntities.get("custom:b").bannerFile);
        assertTrue(config.customAccountMemberships.isEmpty());
    }
}
