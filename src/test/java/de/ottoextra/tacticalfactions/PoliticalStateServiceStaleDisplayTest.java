package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoliticalStateServiceStaleDisplayTest {
    private static final UUID PLAYER = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Test
    void staleSnapshotRemainsAvailableToTablistButNotProtectionLogic() throws Exception {
        TacticalConfig config = new TacticalConfig();
        config.factionRelations.put("faction", Relation.HOSTILE);
        PoliticalStateService service = new PoliticalStateService(config);
        PlayerAffiliation affiliation = new PlayerAffiliation(
                PLAYER.toString(), "Player", "faction");
        PoliticalStateService.Snapshot stale = new PoliticalStateService.Snapshot(
                Map.of("faction", new FactionNode("faction", "Faction", null)),
                Map.of(),
                Map.of(PLAYER.toString(), affiliation),
                Map.of("player", affiliation),
                List.of(new PoliticalStateService.PlayerChoice(
                        PLAYER.toString(), "Player", "Roleplay Name", "faction")),
                null,
                0L);
        Field snapshot = PoliticalStateService.class.getDeclaredField("snapshot");
        snapshot.setAccessible(true);
        snapshot.set(service, stale);

        assertEquals(Relation.NEUTRAL,
                service.resolve(PLAYER, "Player").relation());
        assertEquals("stale", service.resolve(PLAYER, "Player").source());
        assertEquals(Relation.HOSTILE,
                service.resolveForDisplay(PLAYER, "Player").relation());
    }
}
