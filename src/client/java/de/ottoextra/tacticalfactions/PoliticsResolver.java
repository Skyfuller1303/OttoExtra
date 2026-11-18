package de.ottoextra.tacticalfactions;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Pure deterministic status resolver; API failures naturally resolve neutral. */
public final class PoliticsResolver {
    private final Map<String, FactionNode> factions;
    private final Map<String, Relation> factionRelations;
    private final Map<String, Relation> playerRelations;
    private final String ownFactionId;

    public PoliticsResolver(Map<String, FactionNode> factions,
                            Map<String, Relation> factionRelations,
                            Map<String, Relation> playerRelations,
                            String ownFactionId) {
        this.factions = factions == null ? Map.of() : Map.copyOf(factions);
        this.factionRelations = factionRelations == null ? Map.of() : Map.copyOf(factionRelations);
        this.playerRelations = playerRelations == null ? Map.of() : Map.copyOf(playerRelations);
        this.ownFactionId = ownFactionId;
    }

    public Resolution resolve(PlayerAffiliation player) {
        if (player == null) return neutral("unknown");
        Relation manual = player.uuid() == null
                ? Relation.INHERIT : relation(playerRelations.get(player.uuid()));
        if (manual != Relation.INHERIT) return new Resolution(manual, "player");
        if (player.factionId() == null || !factions.containsKey(player.factionId())) return neutral("unknown");
        if (isOwnSide(player.factionId())) return new Resolution(Relation.ALLIED, "own");

        String current = player.factionId();
        Set<String> seen = new HashSet<>();
        boolean direct = true;
        while (current != null && seen.add(current)) {
            Relation configured = relation(factionRelations.get(current));
            if (configured != Relation.INHERIT) {
                FactionNode source = factions.get(current);
                return new Resolution(configured, direct ? "faction"
                        : "inherited:" + (source != null ? source.name() : current));
            }
            FactionNode node = factions.get(current);
            current = node != null ? node.parentId() : null;
            direct = false;
        }
        return neutral("default");
    }

    private boolean isOwnSide(String factionId) {
        if (ownFactionId == null) return false;
        String current = factionId;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            if (ownFactionId.equals(current)) return true;
            FactionNode node = factions.get(current);
            current = node != null ? node.parentId() : null;
        }
        return false;
    }

    private static Relation relation(Relation relation) {
        return relation == null ? Relation.INHERIT : relation;
    }

    private static Resolution neutral(String source) {
        return new Resolution(Relation.NEUTRAL, source);
    }
}
