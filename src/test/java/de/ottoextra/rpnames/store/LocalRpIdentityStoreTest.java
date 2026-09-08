package de.ottoextra.rpnames.store;

import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalRpIdentityStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void apiImportUpdatesStateOnLockedProfileWithoutChangingProtectedIdentityOrColors() {
        LocalRpIdentityStore store = new LocalRpIdentityStore(false);
        LocalRpProfile profile = new LocalRpProfile();
        profile.uuid = "00000000-0000-0000-0000-000000000001";
        profile.accountName = "LockedPlayer";
        profile.rpName = "Lokaler Name";
        profile.title = "Lokaler Titel";
        profile.apiState = "Gemeiner";
        profile.colors.chatTitleColor = "#111111";
        profile.colors.tabTitleColor = "#222222";
        profile.colors.nametagTitleColor = "#333333";
        profile.locked = true;
        profile.knowledgeState = KnowledgeState.MANUAL_LOCKED;
        store.insert(profile);

        assertTrue(store.importApi(profile.accountName, profile.uuid,
                "API Name", "API Titel", "  Adlige  "));

        assertEquals("Adlige", profile.apiState);
        assertEquals("Lokaler Name", profile.rpName);
        assertEquals("Lokaler Titel", profile.title);
        assertEquals("#111111", profile.colors.chatTitleColor);
        assertEquals("#222222", profile.colors.tabTitleColor);
        assertEquals("#333333", profile.colors.nametagTitleColor);
    }

    @Test
    void corruptFileIsPreservedAndRestoredFromRollingPreviousVersion() throws Exception {
        Path dataFile = temporaryDirectory.resolve("known-players.json");
        Path backups = temporaryDirectory.resolve("backups");
        Files.writeString(dataFile, "{abgebrochen", StandardCharsets.UTF_8);
        Files.writeString(temporaryDirectory.resolve("known-players.previous.json"),
                fileWithPlayer("Geretteter", "Lokaler Name"), StandardCharsets.UTF_8);

        LocalRpIdentityStore store = new LocalRpIdentityStore(dataFile, backups);
        store.load();

        assertEquals(LocalRpIdentityStore.RepairStatus.RESTORED,
                store.lastRepairResult().status());
        assertEquals(1, store.size());
        assertEquals("Lokaler Name", store.findByName("Geretteter").orElseThrow().rpName);
        assertTrue(Files.exists(dataFile));
        try (var files = Files.list(temporaryDirectory)) {
            assertEquals(1, files.filter(p -> p.getFileName().toString()
                    .startsWith("known-players.broken-")).count());
        }
        store.shutdown();
    }

    @Test
    void corruptFileWithoutBackupCannotBecomeAnEmptyReplacementOnShutdown() throws Exception {
        Path dataFile = temporaryDirectory.resolve("known-players.json");
        Files.writeString(dataFile, "{abgebrochen", StandardCharsets.UTF_8);

        LocalRpIdentityStore store = new LocalRpIdentityStore(
                dataFile, temporaryDirectory.resolve("backups"));
        store.load();
        store.shutdown();

        assertEquals(LocalRpIdentityStore.RepairStatus.NO_VALID_BACKUP,
                store.lastRepairResult().status());
        assertFalse(Files.exists(dataFile));
        try (var files = Files.list(temporaryDirectory)) {
            assertEquals(1, files.filter(p -> p.getFileName().toString()
                    .startsWith("known-players.broken-")).count());
        }
    }

    @Test
    void repairSkipsBrokenBackupAndUsesNewestValidBackup() throws Exception {
        Path dataFile = temporaryDirectory.resolve("known-players.json");
        Path backups = temporaryDirectory.resolve("backups");
        Path valid = backups.resolve("2026-08-20-17-16-39-869")
                .resolve("known-players.json");
        Path newerButBroken = backups.resolve("2026-08-22-12-00-00-000")
                .resolve("known-players.json");
        Files.createDirectories(valid.getParent());
        Files.createDirectories(newerButBroken.getParent());
        Files.writeString(dataFile, "{abgebrochen", StandardCharsets.UTF_8);
        Files.writeString(valid, fileWithPlayer("AusBackup", "Sicherer Name"),
                StandardCharsets.UTF_8);
        Files.writeString(newerButBroken, "{auch kaputt", StandardCharsets.UTF_8);

        LocalRpIdentityStore store = new LocalRpIdentityStore(dataFile, backups);
        store.load();

        assertEquals(LocalRpIdentityStore.RepairStatus.RESTORED,
                store.lastRepairResult().status());
        assertEquals(valid, store.lastRepairResult().source());
        assertEquals("Sicherer Name", store.findByName("AusBackup").orElseThrow().rpName);
        store.shutdown();
    }

    @Test
    void successfulSaveKeepsLastValidFileAsRollingPreviousVersion() throws Exception {
        Path dataFile = temporaryDirectory.resolve("known-players.json");
        Files.writeString(dataFile, fileWithPlayer("Bewahrt", "Alter Name"),
                StandardCharsets.UTF_8);
        LocalRpIdentityStore store = new LocalRpIdentityStore(
                dataFile, temporaryDirectory.resolve("backups"));
        store.load();

        store.updateManual("Bewahrt", p -> p.rpName = "Neuer Name", false);

        String previous = Files.readString(
                temporaryDirectory.resolve("known-players.previous.json"));
        assertTrue(previous.contains("Alter Name"));
        assertTrue(Files.readString(dataFile).contains("Neuer Name"));
        store.shutdown();
    }

    @Test
    void restoreReplacesLivePlayersAndSurvivesPendingAndNextSave() throws Exception {
        Path dataFile = temporaryDirectory.resolve("known-players.json");
        Files.writeString(dataFile, fileWithPlayer("Bewahrt", "Alter Name"));
        LocalRpIdentityStore store = new LocalRpIdentityStore(
                dataFile, temporaryDirectory.resolve("backups"));
        try {
            store.load();
            LocalRpProfile pending = new LocalRpProfile();
            pending.accountName = "NochNichtGespeichert";
            pending.rpName = "Aus laufender Sitzung";
            store.insert(pending);

            assertTrue(store.restoreBackup(() -> {
                try {
                    Files.writeString(dataFile, fileWithPlayer("AusBackup", "Gesicherter Name"));
                    return true;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));

            assertEquals(1, store.size());
            assertTrue(store.findByName("NochNichtGespeichert").isEmpty());
            assertEquals("Gesicherter Name", store.findByName("AusBackup").orElseThrow().rpName);
            store.saveSoon();
            store.saveNow();
            assertTrue(Files.readString(dataFile).contains("Gesicherter Name"));
            assertFalse(Files.readString(dataFile).contains("Aus laufender Sitzung"));
        } finally {
            store.shutdown();
        }
        assertTrue(Files.readString(dataFile).contains("Gesicherter Name"));
    }

    @Test
    void partiallyFailedRestoreAlsoReloadsReplacedPlayersBeforeNextSave() throws Exception {
        Path dataFile = temporaryDirectory.resolve("known-players.json");
        Files.writeString(dataFile, fileWithPlayer("Vorher", "Alter Name"));
        LocalRpIdentityStore store = new LocalRpIdentityStore(
                dataFile, temporaryDirectory.resolve("backups"));
        try {
            store.load();

            assertFalse(store.restoreBackup(() -> {
                try {
                    Files.writeString(dataFile, fileWithPlayer("AusBackup", "Gesicherter Name"));
                    return false;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));

            store.saveSoon();
            store.saveNow();
            assertTrue(store.findByName("Vorher").isEmpty());
            assertEquals("Gesicherter Name", store.findByName("AusBackup").orElseThrow().rpName);
            assertTrue(Files.readString(dataFile).contains("Gesicherter Name"));
        } finally {
            store.shutdown();
        }
    }

    @Test
    void rejectedRestorePreservesUnsavedPlayers() throws Exception {
        Path dataFile = temporaryDirectory.resolve("known-players.json");
        Files.writeString(dataFile, fileWithPlayer("Vorher", "Alter Name"));
        LocalRpIdentityStore store = new LocalRpIdentityStore(
                dataFile, temporaryDirectory.resolve("backups"));
        try {
            store.load();
            LocalRpProfile pending = new LocalRpProfile();
            pending.accountName = "Neu";
            pending.rpName = "Noch nicht gespeichert";
            store.insert(pending);

            assertFalse(store.restoreBackup(() -> false));

            assertEquals("Noch nicht gespeichert", store.findByName("Neu").orElseThrow().rpName);
            assertTrue(Files.readString(dataFile).contains("Noch nicht gespeichert"));
        } finally {
            store.shutdown();
        }
    }

    private static String fileWithPlayer(String account, String rpName) {
        return """
                {
                  "schemaVersion": 1,
                  "players": [
                    {
                      "accountName": "%s",
                      "rpName": "%s",
                      "knowledgeState": "MANUAL",
                      "source": "MANUAL_EDIT"
                    }
                  ]
                }
                """.formatted(account, rpName);
    }
}
