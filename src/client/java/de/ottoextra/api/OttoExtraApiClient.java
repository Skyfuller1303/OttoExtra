package de.ottoextra.api;

import de.ottoextra.api.model.ApiEnvelope;
import de.ottoextra.api.model.CompactPlayer;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.api.model.PlayerRecord;
import de.ottoextra.api.model.RegionRecord;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface OttoExtraApiClient {

    CompletableFuture<ApiEnvelope> bootstrap();

    CompletableFuture<ApiEnvelope> sync(long cursor);

    CompletableFuture<List<RegionRecord>> regionList();

    CompletableFuture<RegionRecord> regionByName(String name);

    CompletableFuture<FactionRecord> faction(UUID uuid);

    CompletableFuture<List<PlayerRecord>> factionPlayers(UUID factionUuid);

    CompletableFuture<PlayerRecord> player(UUID uuid);

    CompletableFuture<List<CompactPlayer>> compactPlayers();

    CompletableFuture<byte[]> downloadBinary(URI uri);

    OttoExtraApiRoutes routes();

    void close();
}
