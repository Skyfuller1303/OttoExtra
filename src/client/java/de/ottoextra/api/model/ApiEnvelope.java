package de.ottoextra.api.model;

import com.google.gson.JsonElement;

import java.util.List;

public record ApiEnvelope(
        boolean ok,
        String server_time,
        long sync_cursor,
        List<FactionRecord> factions,
        List<RegionRecord> regions,
        List<PlayerRecord> players,
        List<PlayerRecord> participants,
        JsonElement players_by_faction,
        JsonElement participants_by_faction,
        MetaRecord meta,
        FactionRecord faction,
        RegionRecord region,
        PlayerRecord profile
) {
}
