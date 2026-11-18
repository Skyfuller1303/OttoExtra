package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PoliticalStateServiceRefreshTest {
    private static final PlayerAffiliation PLAYER = new PlayerAffiliation("p", "Player", "old");

    private static PoliticalStateService.Snapshot snapshot(Map<String, FactionNode> factions, long updatedAt) {
        return new PoliticalStateService.Snapshot(factions, Map.of(),
                Map.of("p", PLAYER), Map.of("player", PLAYER),
                List.of(new PoliticalStateService.PlayerChoice("p", "Player", "Player", "old")),
                "own", updatedAt);
    }

    @Test
    void failedPlayerListStillUpdatesFactionsButKeepsPlayersAndAge() {
        PoliticalStateService.Snapshot previous = snapshot(Map.of(), 1234L);
        PoliticalStateService.Snapshot factionsOnly = new PoliticalStateService.Snapshot(
                Map.of("new", new FactionNode("new", "New", null)), Map.of(),
                Map.of(), Map.of(), List.of(), null, 9999L);

        PoliticalStateService.Snapshot merged = PoliticalStateService.withFactions(previous, factionsOnly);

        assertEquals(Map.of("new", new FactionNode("new", "New", null)), merged.factions());
        assertEquals(previous.byUuid(), merged.byUuid());
        assertEquals(previous.players(), merged.players());
        assertEquals("own", merged.ownFactionId());
        assertEquals(1234L, merged.updatedAt());
    }

    @Test
    void emptyFactionResultKeepsPreviousSnapshot() {
        PoliticalStateService.Snapshot previous = snapshot(Map.of("old", new FactionNode("old", "Old", null)), 1L);
        PoliticalStateService.Snapshot empty = PoliticalStateService.Snapshot.empty();

        assertSame(previous, PoliticalStateService.withFactions(previous, empty));
    }

    @Test
    void factionRelationFallsBackToNameWhenUuidChanged() throws Exception {
        TacticalConfig config = new TacticalConfig();
        // Relation was stored for an old UUID plus the name fallback.
        config.factionRelations.put("old-uuid", Relation.ALLIED);
        config.factionNameRelations.put("kelterfels", Relation.ALLIED);
        PoliticalStateService service = new PoliticalStateService(config);
        Field field = PoliticalStateService.class.getDeclaredField("snapshot");
        field.setAccessible(true);
        field.set(service, new PoliticalStateService.Snapshot(
                Map.of("new-uuid", new FactionNode("new-uuid", "Kelterfels", null)), Map.of(),
                Map.of(), Map.of(), List.of(), null, System.currentTimeMillis()));

        assertEquals(Relation.ALLIED, service.resolveFaction("new-uuid").relation());
    }
}
