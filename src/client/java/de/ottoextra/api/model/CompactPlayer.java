package de.ottoextra.api.model;

/** Kompakter Spieler-Eintrag (public-player-compact) für Verzeichnis und RP-Namen. */
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
