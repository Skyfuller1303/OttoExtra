package de.ottoextra.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.ottoextra.api.auth.ApiAuthService;
import de.ottoextra.api.auth.ResponseVerifier;
import de.ottoextra.api.auth.SessionSnapshot;
import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.api.model.AnimalBreedingHorseLocation;
import de.ottoextra.config.OttoExtraConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalBreedingApiTest {

    private static final String HORSE_UUID =
            "5f4c96e3-8ef0-4fa0-a90c-efe5831b9a11";
    private static final UUID MARE_UUID = UUID.fromString(HORSE_UUID);
    private static final UUID STALLION_UUID = UUID.fromString(
            "32436c53-3054-4b04-8948-67e462810c71");
    private static final UUID FOAL_UUID = UUID.fromString(
            "4e48c721-e2ab-4169-b4ba-3aa2d15b3508");
    private static final UUID EVENT_UUID = UUID.fromString(
            "292d2177-78ea-4230-9af0-391209fca55c");

    private HttpServer server;
    private ExecutorService executor;
    private KeyPair signingKey;
    private HttpOttoExtraApiClient client;
    private final AtomicReference<String> putBody = new AtomicReference<>();
    private final AtomicReference<String> observationPutBody = new AtomicReference<>();
    private final AtomicReference<String> namePutBody = new AtomicReference<>();
    private final AtomicReference<String> eventBody = new AtomicReference<>();
    private final AtomicReference<String> foalBody = new AtomicReference<>();
    private final AtomicReference<String> finderMethod = new AtomicReference<>();
    private final AtomicReference<String> finderBody = new AtomicReference<>();
    private final AtomicReference<String> finderPath = new AtomicReference<>();
    private final AtomicReference<String> horseMethod = new AtomicReference<>();
    private final AtomicReference<String> editorMethod = new AtomicReference<>();
    private final AtomicReference<String> editorBody = new AtomicReference<>();
    private final AtomicReference<String> breederAccessMethod = new AtomicReference<>();
    private final AtomicReference<String> breederAccessBody = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        signingKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        executor = Executors.newFixedThreadPool(4);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/animal-breeding/horses", exchange -> {
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, "{\"horses\":[" + horseJson() + "]}");
                return;
            }
            respond(exchange, 405, "{}");
        });
        server.createContext("/v2/animal-breeding/horses/" + HORSE_UUID,
                exchange -> {
                    assertEquals("Bearer player-token",
                            exchange.getRequestHeaders().getFirst("Authorization"));
                    horseMethod.set(exchange.getRequestMethod());
                    if ("DELETE".equals(exchange.getRequestMethod())) {
                        respond(exchange, 200, "{\"ok\":true,\"deleted\":true}");
                        return;
                    }
                    assertEquals("PUT", exchange.getRequestMethod());
                    putBody.set(new String(exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));
                    respond(exchange, 200, "{\"ok\":true,\"horse\":"
                            + horseJson() + "}");
                });
        server.createContext("/v2/animal-breeding/horses/" + HORSE_UUID
                + "/name", exchange -> {
            assertEquals("PUT", exchange.getRequestMethod());
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            namePutBody.set(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"ok\":true,\"horse\":"
                    + horseJson() + "}");
        });
        server.createContext("/v2/animal-breeding/horses/" + HORSE_UUID
                + "/observation", exchange -> {
            assertEquals("PUT", exchange.getRequestMethod());
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            observationPutBody.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"ok\":true,\"status\":\"updated\",\"horse\":"
                    + horseJson() + "}");
        });
        server.createContext("/v2/animal-breeding/horse-names", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"horseNames\":[{\"entityUuid\":\""
                    + HORSE_UUID + "\",\"registryName\":\"Abendrot\",\"revision\":7}]}");
        });
        server.createContext("/v2/animal-breeding/breeder-access", exchange -> {
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            breederAccessMethod.set(exchange.getRequestMethod());
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200,
                        "{\"role\":\"editor\",\"breederAutofill\":true}");
                return;
            }
            breederAccessBody.set(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"ok\":true}");
        });
        server.createContext("/v2/animal-breeding/horses/" + HORSE_UUID
                + "/editors", exchange -> {
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            editorMethod.set(exchange.getRequestMethod());
            editorBody.set(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"ok\":true,\"editors\":[]}");
        });
        server.createContext("/v2/animal-breeding/finder", exchange -> {
            assertEquals("Bearer player-token",
                    exchange.getRequestHeaders().getFirst("Authorization"));
            finderMethod.set(exchange.getRequestMethod());
            finderPath.set(exchange.getRequestURI().getPath());
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, "{\"finder\":{\"entityUuid\":\""
                        + HORSE_UUID + "\"},\"finders\":[{\"entityUuid\":\""
                        + HORSE_UUID + "\"},{\"entityUuid\":\"" + STALLION_UUID + "\"}]}");
                return;
            }
            if ("DELETE".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, "{\"ok\":true,\"status\":\"cleared\"}");
                return;
            }
            if ("PUT".equals(exchange.getRequestMethod())) {
                finderBody.set(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                respond(exchange, 200, "{\"ok\":true,\"finder\":{\"entityUuid\":\""
                        + HORSE_UUID + "\"}}");
                return;
            }
            respond(exchange, 405, "{}");
        });
        server.createContext("/v2/animal-breeding/breeding-events",
                exchange -> {
                    assertEquals("POST", exchange.getRequestMethod());
                    assertEquals("Bearer player-token",
                            exchange.getRequestHeaders().getFirst("Authorization"));
                    eventBody.set(new String(exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));
                    respond(exchange, 201, "{\"ok\":true,\"event\":{\"id\":\""
                            + EVENT_UUID + "\"}}");
                });
        server.createContext("/v2/animal-breeding/breeding-events/"
                        + EVENT_UUID + "/foal", exchange -> {
                    assertEquals("PUT", exchange.getRequestMethod());
                    assertEquals("Bearer player-token",
                            exchange.getRequestHeaders().getFirst("Authorization"));
                    foalBody.set(new String(exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));
                    respond(exchange, 200, "{\"ok\":true,\"status\":\"linked\"}");
                });
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        ResponseVerifier verifier = new ResponseVerifier(Map.of("k1",
                Base64.getEncoder().encodeToString(signingKey.getPublic().getEncoded())),
                () -> true);
        HttpClient http = HttpClient.newBuilder().executor(executor).build();
        SessionSnapshot session = new SessionSnapshot("Skyfuller1303",
                UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
                "mc-token");
        ApiAuthService auth = new ApiAuthService(http, Duration.ofSeconds(5),
                URI.create(base + "/v2/auth/challenge"),
                URI.create(base + "/v2/auth/verify"), executor,
                () -> session, (uuid, token, serverId) -> { }, verifier,
                Clock.systemUTC());
        // The test injects a pre-issued token through tiny auth endpoints.
        server.createContext("/v2/auth/challenge", exchange ->
                respond(exchange, 200, "{\"serverId\":\"challenge\"}"));
        server.createContext("/v2/auth/verify", exchange ->
                respond(exchange, 200, "{\"token\":\"player-token\","
                        + "\"expiresAt\":\"2099-01-01T00:00:00Z\"}"));
        server.start();
        OttoExtraConfig config = new OttoExtraConfig();
        config.api.baseUrl = base;
        client = new HttpOttoExtraApiClient(config, executor, http, verifier, auth);
    }

    @AfterEach
    void stop() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
    }

    @Test
    void downloadsAndIdempotentlyPutsHorseByEntityUuid() {
        AnimalBreedingHorse horse = client.animalBreedingHorses().join().getFirst();
        assertEquals(HORSE_UUID, horse.entityUuid());
        assertEquals("Abendrot", horse.registryName());
        assertEquals("CHESTNUT", horse.coatColor());
        assertEquals("WHITE_DOTS", horse.marking());
        assertEquals("Skyfuller1303", horse.ownerName());
        assertTrue(horse.canManage());

        AnimalBreedingHorse stored = client.putAnimalBreedingHorse(horse,
                new AnimalBreedingHorseLocation("minecraft:overworld", 1739, 70, 4811)).join();
        assertEquals(7, stored.revision());
        assertEquals("PUT", horseMethod.get());
        assertTrue(putBody.get().contains("\"entityUuid\":\"" + HORSE_UUID));
        assertTrue(putBody.get().contains("\"coatColor\":\"CHESTNUT\""));
        assertTrue(putBody.get().contains("\"marking\":\"WHITE_DOTS\""));
        assertTrue(putBody.get().contains("\"updatedAtEpochMillis\":2000"));
        assertTrue(putBody.get().contains("\"dimension\":\"minecraft:overworld\""));
        assertTrue(putBody.get().contains("\"x\":1739"));
    }

    @Test
    void deletesHorseByEntityUuid() {
        client.deleteAnimalBreedingHorse(MARE_UUID).join();

        assertEquals("DELETE", horseMethod.get());
    }

    @Test
    void putsObservedStatsThroughDedicatedRoute() {
        AnimalBreedingHorse horse = client.animalBreedingHorses().join().getFirst();

        AnimalBreedingHorse stored = client
                .putAnimalBreedingHorseObservation(horse).join();

        assertEquals(HORSE_UUID, stored.entityUuid());
        assertTrue(observationPutBody.get().contains(
                "\"entityUuid\":\"" + HORSE_UUID + "\""));
        assertTrue(observationPutBody.get().contains("\"mast\":\"72/100\""));
        assertTrue(observationPutBody.get().contains(
                "\"observedAtEpochMillis\":1000"));
    }

    @Test
    void grantsAndRevokesOneHorseByMinecraftName() {
        client.setAnimalBreedingHorseEditor(MARE_UUID, "Pferdepfleger", true).join();
        assertEquals("PUT", editorMethod.get());
        assertTrue(editorBody.get().contains("\"playerName\":\"Pferdepfleger\""));
        client.setAnimalBreedingHorseEditor(MARE_UUID, "Pferdepfleger", false).join();
        assertEquals("DELETE", editorMethod.get());
    }

    @Test
    void readsAndAdministersGlobalBreederAccess() {
        assertEquals("editor", client.animalBreedingRole().join().orElseThrow());
        assertEquals("GET", breederAccessMethod.get());

        client.setAnimalBreedingBreederAccess("Pferdezuechter", true).join();
        assertEquals("PUT", breederAccessMethod.get());
        assertTrue(breederAccessBody.get().contains(
                "\"playerName\":\"Pferdezuechter\""));

        client.setAnimalBreedingBreederAccess("Pferdezuechter", false).join();
        assertEquals("DELETE", breederAccessMethod.get());
    }

    @Test
    void writesBasicNameThroughDedicatedNonDestructiveRoute() {
        AnimalBreedingHorse horse = client.animalBreedingHorses().join().getFirst();
        AnimalBreedingHorse stored = client.putAnimalBreedingHorseName(horse,
                new AnimalBreedingHorseLocation("minecraft:overworld", 1, 70, -2)).join();

        assertEquals("Abendrot", stored.registryName());
        assertTrue(namePutBody.get().contains("\"registryName\":\"Abendrot\""));
        assertTrue(namePutBody.get().contains("\"dimension\":\"minecraft:overworld\""));
    }

    @Test
    void downloadsMinimalHorseNamesWithoutFarmStats() {
        var name = client.animalBreedingHorseNames().join().getFirst();
        assertEquals(MARE_UUID, name.uuid());
        assertEquals("Abendrot", name.registryName());
        assertEquals(7, name.revision());
    }

    @Test
    void readsWritesAndClearsPersonalFinderTarget() {
        assertEquals(MARE_UUID,
                client.animalBreedingFinderTarget().join().orElseThrow());
        assertEquals("GET", finderMethod.get());
        client.setAnimalBreedingFinderTarget(MARE_UUID).join();
        assertEquals("PUT", finderMethod.get());
        assertTrue(finderBody.get().contains("\"entityUuid\":\"" + MARE_UUID + "\""));
        client.clearAnimalBreedingFinderTarget().join();
        assertEquals("DELETE", finderMethod.get());
    }

    @Test
    void readsAddsAndRemovesMultipleFinderTargets() {
        assertEquals(List.of(MARE_UUID, STALLION_UUID),
                client.animalBreedingFinderTargets().join());

        client.addAnimalBreedingFinderTarget(STALLION_UUID).join();
        assertEquals("PUT", finderMethod.get());
        assertEquals("/v2/animal-breeding/finder/" + STALLION_UUID, finderPath.get());

        client.removeAnimalBreedingFinderTarget(MARE_UUID).join();
        assertEquals("DELETE", finderMethod.get());
        assertEquals("/v2/animal-breeding/finder/" + MARE_UUID, finderPath.get());
    }

    @Test
    void createsBreedingEventAndLinksFoal() {
        UUID event = client.createAnimalBreedingEvent(MARE_UUID, STALLION_UUID,
                "Apfelhof", 1_788_260_400_000L).join();
        client.linkAnimalBreedingFoal(event, FOAL_UUID).join();

        assertEquals(EVENT_UUID, event);
        assertTrue(eventBody.get().contains("\"mareUuid\":\"" + MARE_UUID + "\""));
        assertTrue(eventBody.get().contains("\"stallionUuid\":\"" + STALLION_UUID + "\""));
        assertTrue(eventBody.get().contains("\"farmName\":\"Apfelhof\""));
        assertTrue(eventBody.get().contains("\"pairedAtEpochMillis\":1788260400000"));
        assertTrue(foalBody.get().contains("\"foalUuid\":\"" + FOAL_UUID + "\""));
    }

    private String horseJson() {
        return "{\"entityUuid\":\"" + HORSE_UUID + "\","
                + "\"registryName\":\"Abendrot\",\"origin\":\"Falkenwacht\","
                + "\"currentFarm\":\"Apfelhof\",\"animalType\":\"Reitpferd\","
                + "\"gender\":\"Stute\",\"coatColor\":\"CHESTNUT\","
                + "\"marking\":\"WHITE_DOTS\",\"life\":\"24/30\","
                + "\"jumpHeight\":\"1,42 Blöcke\",\"speed\":\"14,2 Blöcke/s\","
                + "\"mast\":\"72/100\",\"breedingReady\":\"in 4h\","
                + "\"observedAtEpochMillis\":1000,\"updatedAtEpochMillis\":2000,"
                + "\"revision\":7,\"ownerUuid\":\"069a79f4-44e9-4726-a5be-fca90e38aaf5\","
                + "\"ownerName\":\"Skyfuller1303\",\"canEdit\":true,\"canManage\":true,"
                + "\"updatedByUuid\":\"069a79f4-44e9-4726-a5be-fca90e38aaf5\","
                + "\"serverUpdatedAt\":\"2026-09-01 10:00:00\"}";
    }

    private void respond(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(signingKey.getPrivate());
            signer.update(bytes);
            exchange.getResponseHeaders().set(ResponseVerifier.KEY_ID_HEADER, "k1");
            exchange.getResponseHeaders().set(ResponseVerifier.SIGNATURE_HEADER,
                    Base64.getEncoder().encodeToString(signer.sign()));
        } catch (Exception error) {
            throw new IOException(error);
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
