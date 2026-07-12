package de.ottoextra.api.model;

import java.util.List;

public record PlayerRecord(
        String entity_key,
        String uuid,
        String name,
        String title,
        String rank,
        String state,
        String faction,
        String faction_name,
        int money,
        int warns,
        int remaining_warns,
        List<String> permissions,
        String joined_at,
        String updated_at
) {
}
