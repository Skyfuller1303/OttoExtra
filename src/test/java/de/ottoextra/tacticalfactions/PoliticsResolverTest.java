package de.ottoextra.tacticalfactions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoliticsResolverTest {
    private static final Map<String, FactionNode> TREE = Map.of(
            "empire", new FactionNode("empire", "Reich", null),
            "duchy", new FactionNode("duchy", "Herzogtum", "empire"),
            "barony", new FactionNode("barony", "Baronie", "duchy"),
            "foreign", new FactionNode("foreign", "Ausland", null));

    @Test void playerOverrideWinsOverOwnFaction() {
        PoliticsResolver resolver = new PoliticsResolver(TREE, Map.of(),
                Map.of("p", Relation.HOSTILE), "empire");
        assertEquals(Relation.HOSTILE, resolver.resolve(player("p", "barony")).relation());
    }

    @Test void ownFactionAndRecursiveVassalsAreAllied() {
        PoliticsResolver resolver = new PoliticsResolver(TREE, Map.of(), Map.of(), "empire");
        assertEquals(Relation.ALLIED, resolver.resolve(player("p", "empire")).relation());
        assertEquals(Relation.ALLIED, resolver.resolve(player("p", "barony")).relation());
    }

    @Test void directFactionBeatsParentAndExplicitNeutralStopsInheritance() {
        PoliticsResolver resolver = new PoliticsResolver(TREE,
                Map.of("empire", Relation.HOSTILE, "duchy", Relation.NEUTRAL), Map.of(), null);
        Resolution result = resolver.resolve(player("p", "barony"));
        assertEquals(Relation.NEUTRAL, result.relation());
        assertEquals("inherited:Herzogtum", result.source());
    }

    @Test void nearestRecursiveParentWins() {
        PoliticsResolver resolver = new PoliticsResolver(TREE,
                Map.of("empire", Relation.ALLIED, "duchy", Relation.HOSTILE), Map.of(), null);
        assertEquals(Relation.HOSTILE, resolver.resolve(player("p", "barony")).relation());
    }

    @Test void unknownPlayerIsNeutral() {
        PoliticsResolver resolver = new PoliticsResolver(TREE, Map.of(), Map.of(), "empire");
        assertEquals(Relation.NEUTRAL, resolver.resolve(null).relation());
        assertEquals(Relation.NEUTRAL, resolver.resolve(player("p", "missing")).relation());
    }

    @Test void inheritPlayerValueFallsThrough() {
        PoliticsResolver resolver = new PoliticsResolver(TREE, Map.of("foreign", Relation.HOSTILE),
                Map.of("p", Relation.INHERIT), null);
        assertEquals(Relation.HOSTILE, resolver.resolve(player("p", "foreign")).relation());
    }

    @Test void factionCardWithoutPlayerUuidResolvesSafely() {
        PoliticsResolver resolver = new PoliticsResolver(TREE,
                Map.of("foreign", Relation.HOSTILE), Map.of(), null);
        Resolution result = resolver.resolve(new PlayerAffiliation(null, null, "foreign"));
        assertEquals(Relation.HOSTILE, result.relation());
        assertEquals("faction", result.source());
    }

    @Test void customMembershipWinsOverApiFaction() {
        Map<String, FactionNode> merged = new java.util.HashMap<>(TREE);
        merged.put("custom:guard", new FactionNode("custom:guard", "Garde", "foreign"));
        PoliticsResolver resolver = new PoliticsResolver(merged,
                Map.of("custom:guard", Relation.ALLIED, "empire", Relation.HOSTILE), Map.of(), null);
        PlayerAffiliation player = new PlayerAffiliation("p", "Player", "empire", "custom:guard");
        assertEquals(Relation.ALLIED, resolver.resolve(player).relation());
    }

    @Test void ownCustomSideIncludesItsRecursiveFollowers() {
        Map<String, FactionNode> merged = new java.util.HashMap<>(TREE);
        merged.put("custom:house", new FactionNode("custom:house", "Haus", null));
        merged.put("custom:guard", new FactionNode("custom:guard", "Garde", "custom:house"));
        PoliticsResolver resolver = new PoliticsResolver(merged, Map.of(), Map.of(), "custom:house");
        assertEquals(Relation.ALLIED, resolver.resolve(
                new PlayerAffiliation("p", "Player", null, "custom:guard")).relation());
    }

    private static PlayerAffiliation player(String id, String faction) {
        return new PlayerAffiliation(id, "Player", faction);
    }
}
