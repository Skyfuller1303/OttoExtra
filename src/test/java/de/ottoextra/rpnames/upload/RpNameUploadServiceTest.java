package de.ottoextra.rpnames.upload;

import org.junit.jupiter.api.Test;

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
}
