package de.ottoextra.api.model;
public record CompactPlayer(
        String entityKey,
        String uuid,
        String name,
        String title,
        String rank,
        String state,
        String faction,
        String factionName
) {
}
