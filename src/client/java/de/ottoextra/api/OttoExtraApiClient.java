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

/**
 * Einzige API-Schicht von OttoExtra (ein Client statt vieler).
 *
 * <p>Alle Methoden sind asynchron und blockieren nie den Render-/Tick-Thread.
 * Fehler werden über {@link ApiProblem.ApiException} im Future-Fehlerpfad
 * transportiert. Implementierungen halten <b>keine</b> Secrets/API-Keys.</p>
 */
public interface OttoExtraApiClient {

    CompletableFuture<ApiEnvelope> bootstrap();

    CompletableFuture<ApiEnvelope> sync(long cursor);

    CompletableFuture<List<RegionRecord>> regionList();

    CompletableFuture<RegionRecord> regionByName(String name);

    CompletableFuture<FactionRecord> faction(UUID uuid);

    CompletableFuture<List<PlayerRecord>> factionPlayers(UUID factionUuid);

    CompletableFuture<PlayerRecord> player(UUID uuid);

    CompletableFuture<List<CompactPlayer>> compactPlayers();

    /** Lädt eine Binärdatei (Banner/Head). {@code uri} stammt aus den Routen/Records. */
    CompletableFuture<byte[]> downloadBinary(URI uri);

    /** Routen-Helfer für Module, die relative Pfade auflösen müssen. */
    OttoExtraApiRoutes routes();

    /** Gibt Threadpools/Verbindungen frei. */
    void close();
}
