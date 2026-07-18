package de.ottoextra.rpnames.upload;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RpNameUploadServiceTest {

    private static final String ACTOR_UUID = "0ac2296c-7969-44cd-a59e-3155fc2b50b1";
    private static final String TARGET_UUID = "11111111-2222-3333-4444-555555555555";

    @Test
    void ownNameOmitsTargetFieldsAndUnsupportedFields() {
        var data = RpNameUploadService.createUploadDataForActor(
                ACTOR_UUID, "TheLXNCE", "TheLXNCE", ACTOR_UUID, "Lance");
        var json = data.toJson();

        assertEquals(ACTOR_UUID, json.get("uuid").getAsString());
        assertEquals("Lance", json.get("rp_name").getAsString());
        assertFalse(json.has("target_uuid"));
        assertFalse(json.has("target_player_name"));
        assertFalse(json.has("title"));
    }

    @Test
    void otherPlayerUsesTargetUuidWhenAvailable() {
        var data = RpNameUploadService.createUploadDataForActor(
                ACTOR_UUID, "TheLXNCE", "Romegon", TARGET_UUID,
                "Roman von Marienburg");
        var json = data.toJson();

        assertEquals(ACTOR_UUID, json.get("uuid").getAsString());
        assertEquals(TARGET_UUID, json.get("target_uuid").getAsString());
        assertEquals("Roman von Marienburg", json.get("rp_name").getAsString());
        assertFalse(json.has("target_player_name"));
        assertFalse(json.has("title"));
    }

    @Test
    void otherPlayerFallsBackToAccountName() {
        var data = RpNameUploadService.createUploadDataForActor(
                ACTOR_UUID, "TheLXNCE", "Romegon", null,
                "Roman von Marienburg");
        var json = data.toJson();

        assertEquals("Romegon", json.get("target_player_name").getAsString());
        assertFalse(json.has("target_uuid"));
        assertFalse(json.has("title"));
    }

    @Test
    void blankObservedRpNameDoesNotAttemptUpload() {
        var result = RpNameUploadService.uploadObservedIdentity(
                "Romegon", TARGET_UUID, "   ").join();

        assertFalse(result.attempted());
        assertTrue(result.success());
    }

    @Test
    void emptyNameIsKeptForExplicitRemovalPayloads() {
        var data = RpNameUploadService.createUploadDataForActor(
                ACTOR_UUID, "TheLXNCE", "Romegon", TARGET_UUID, "   ");

        assertEquals("", data.toJson().get("rp_name").getAsString());
    }

    @Test
    void nameIsLimitedToOneHundredTwentyCodePoints() {
        String longName = "𐍈".repeat(121);
        var data = RpNameUploadService.createUploadDataForActor(
                ACTOR_UUID, "TheLXNCE", "Romegon", TARGET_UUID, longName);

        String result = data.toJson().get("rp_name").getAsString();
        assertEquals(120, result.codePointCount(0, result.length()));
        assertTrue(result.endsWith("𐍈"));
    }

    @Test
    void requestLogContainsOnlySupportedApiFields() {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", ACTOR_UUID);
        json.addProperty("target_uuid", TARGET_UUID);
        json.addProperty("rp_name", "Roman von Marienburg");

        String log = RpNameUploadService.formatRequestForLog(
                URI.create("https://api.ottoextra.dev/api/index.php?action=community-participant-rp-name"),
                "OttoExtra/0.1.23",
                json);

        assertTrue(log.startsWith("POST https://api.ottoextra.dev/api/index.php?action=community-participant-rp-name\n"));
        assertTrue(log.contains("Content-Type: application/json; charset=UTF-8"));
        assertTrue(log.contains("Accept: application/json"));
        assertTrue(log.contains("User-Agent: OttoExtra/0.1.23"));
        assertTrue(log.endsWith("Body: " + json));
        assertFalse(log.contains("\"title\""));
    }
}
