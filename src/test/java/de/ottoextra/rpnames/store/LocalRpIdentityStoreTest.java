package de.ottoextra.rpnames.store;

import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalRpIdentityStoreTest {

    @Test
    void removesOnlyUnknownSyntheticTablistAliasAndRestoresKnownNameOwner() {
        LocalRpIdentityStore store = new LocalRpIdentityStore(false);
        LocalRpProfile known = new LocalRpProfile();
        known.accountName = "countdacson";
        known.uuid = "0cc97e4d-bee7-4f3a-ba34-690607a54a4a";
        known.rpName = "Wilhelm Dacson";
        known.source = RpNameSource.MANUAL_EDIT;
        store.insert(known);
        LocalRpProfile proxy = store.ensureSeen(
                "countdacson", "cdad3a02-77aa-467b-856e-9ce565f44e09",
                RpNameSource.SEEN_TABLIST);

        assertTrue(store.removeSyntheticTablistAlias(
                proxy.accountName, proxy.uuid, known));
        assertSame(known, store.findByName("countdacson").orElseThrow());
        assertTrue(store.find(proxy.uuid, proxy.accountName).isEmpty());
    }

    @Test
    void serverGuiImportCreatesKnownProfile() {
        LocalRpIdentityStore store = new LocalRpIdentityStore(false);
        assertTrue(store.importServerGui("Acc", "u-1", "Johann", "Großschmied", "Gemein"));
        LocalRpProfile p = store.find("u-1", "Acc").orElseThrow();
        assertEquals("Johann", p.rpName);
        assertEquals("Großschmied", p.title);
        assertEquals("Gemein", p.apiState);
        assertEquals(KnowledgeState.KNOWN, p.knowledgeState);
        assertEquals(RpNameSource.SERVER_GUI, p.source);
        assertFalse(store.importServerGui("Acc", "u-1", "Johann", "Großschmied", "Gemein"));
    }

    @Test
    void serverGuiImportOverwritesServerValuesButNotManualOnes() {
        LocalRpIdentityStore store = new LocalRpIdentityStore(false);
        store.importServerGui("Acc", "u-1", "Johann", "Schmied", null);
        store.importServerGui("Acc", "u-1", "Johann N.", "Großschmied", null);
        assertEquals("Johann N.", store.find("u-1", "Acc").orElseThrow().rpName);
        assertEquals("Großschmied", store.find("u-1", "Acc").orElseThrow().title);

        LocalRpProfile edited = new LocalRpProfile();
        edited.accountName = "Other";
        edited.uuid = "u-2";
        edited.rpName = "Mein Name";
        edited.source = RpNameSource.MANUAL_EDIT;
        edited.knowledgeState = KnowledgeState.MANUAL;
        store.insert(edited);
        store.importServerGui("Other", "u-2", "Server Name", "Server Titel", "Adel");
        assertEquals("Mein Name", edited.rpName);
        assertEquals("Server Titel", edited.title);
        assertEquals("Adel", edited.apiState);
        assertEquals(KnowledgeState.MANUAL, edited.knowledgeState);
        store.importServerGui("Other", "u-2", "Server Name", "Anderer Titel", "Klerus");
        assertEquals("Server Titel", edited.title);
        assertEquals("Adel", edited.apiState);
    }

    @Test
    void manualEditSurvivesLaterServerGuiImports() {
        LocalRpIdentityStore store = new LocalRpIdentityStore(false);
        store.importServerGui("Acc", "u-1", "Johann", "Schmied", "Gemein");
        store.updateManual("Acc", "u-1", p -> {
            p.rpName = "Hans";
            p.title = "Meister";
        }, false);
        assertFalse(store.importServerGui("Acc", "u-1", "Johann", "Schmied", "Gemein"));
        LocalRpProfile p = store.find("u-1", "Acc").orElseThrow();
        assertEquals("Hans", p.rpName);
        assertEquals("Meister", p.title);
    }

    @TempDir
    Path temporaryDirectory;

    @Test
    void repairBeforeFirstSaveKeepsNewProfilesWritable() throws Exception {
        Path file = temporaryDirectory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(file, temporaryDirectory.resolve("backups"));
        try {
            store.load();
            assertEquals(LocalRpIdentityStore.RepairStatus.NO_DATA, store.repairData().status());
            store.ensureSeen("FirstPlayer", null, de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
        } finally {
            store.shutdown();
        }
        var reloaded = new LocalRpIdentityStore(file, temporaryDirectory.resolve("backups"));
        try {
            reloaded.load();
            assertTrue(reloaded.findByName("FirstPlayer").isPresent());
        } finally {
            reloaded.shutdown();
        }
    }

    @Test
    void healthyRepairKeepsChangesWaitingForTheDebouncedSave() throws Exception {
        Path file = temporaryDirectory.resolve("known-players.json");
        Files.writeString(file, fileWithPlayer("Saved", "Saved Name"));
        var store = new LocalRpIdentityStore(file, temporaryDirectory.resolve("backups"));
        try {
            store.load();
            synchronized (store) {
                store.ensureSeen("Pending", null, de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
                assertEquals(LocalRpIdentityStore.RepairStatus.HEALTHY, store.repairData().status());
                assertTrue(store.findByName("Pending").isPresent());
            }
        } finally {
            store.shutdown();
        }
        var reloaded = new LocalRpIdentityStore(file, temporaryDirectory.resolve("backups"));
        try {
            reloaded.load();
            assertEquals(2, reloaded.size());
            assertTrue(reloaded.findByName("Pending").isPresent());
        } finally {
            reloaded.shutdown();
        }
    }

    @Test
    void repeatedRepairAfterCorruptionCannotUnlockSavingWithoutBackup() throws Exception {
        Path file = temporaryDirectory.resolve("known-players.json");
        Files.writeString(file, "{broken");
        var store = new LocalRpIdentityStore(file, temporaryDirectory.resolve("backups"));
        try {
            store.load();
            assertEquals(LocalRpIdentityStore.RepairStatus.NO_VALID_BACKUP, store.repairData().status());
            store.ensureSeen("Pending", null, de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
            assertEquals(LocalRpIdentityStore.RepairStatus.NO_VALID_BACKUP, store.repairData().status());
        } finally {
            store.shutdown();
        }
        assertFalse(Files.exists(file));
    }

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
    void manualProfileAcceptsServerAndApiTitlesWithoutOverwritingPersonalNameOrColors() {
        var store = new LocalRpIdentityStore(false);
        try {
            var profile = store.updateManual("Player", p -> {
                p.rpName = "Mein Bekannter";
                p.title = "Knappe";
                p.colors.chatNameColor = "#123456";
            }, false);
            assertFalse(profile.locked);
            assertFalse(profile.titleLocked);
            assertEquals(KnowledgeState.MANUAL, profile.knowledgeState);
            assertTrue(store.updateTitleIfChanged("Player", null, "Ritter"));
            assertTrue(store.learnIdentity("Player", "Servername", "Freiherr", "Adel",
                    de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE));
            assertEquals("Freiherr", profile.title);
            assertTrue(store.importApi("Player", null, "API-Name", "Graf", "Adlige"));
            assertEquals("Graf", profile.title);
            assertEquals("Mein Bekannter", profile.rpName);
            assertEquals("#123456", profile.colors.chatNameColor);
            assertEquals("Adlige", profile.apiState);
        } finally { store.shutdown(); }
    }

    @Test
    void explicitTitleLockBlocksAllAutomaticTitleSourcesUntilItIsCleared() {
        var store = new LocalRpIdentityStore(false);
        try {
            var profile = store.updateManual("Player", p -> {
                p.rpName = "Bekannter";
                p.title = "Mein Titel";
                p.titleLocked = true;
            }, false);
            assertFalse(store.updateTitleIfChanged("Player", null, "Servertitel"));
            store.learnIdentity("Player", "Servername", "Beobachteter Titel", "Adel",
                    de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
            store.importApi("Player", null, "API-Name", "API-Titel", "Adlige");
            assertEquals("Mein Titel", profile.title);
            assertFalse(profile.locked);
            store.updateManual("Player", p -> p.titleLocked = false, false);
            assertTrue(store.updateTitleIfChanged("Player", null, "Neuer Titel"));
            assertEquals("Neuer Titel", profile.title);
        } finally { store.shutdown(); }
    }

    @Test
    void explicitlyLockedEmptyTitleIsNotFilledAutomatically() {
        var store = new LocalRpIdentityStore(false);
        try {
            var profile = store.updateManual("Player", p -> p.titleLocked = true, false);
            store.learnIdentity("Player", "Name", "Beobachteter Titel", "Adel",
                    de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
            store.importApi("Player", null, "Name", "API-Titel", "Adlige");
            assertFalse(store.updateTitleIfChanged("Player", null, "Servertitel"));
            assertEquals("", profile.title);
        } finally { store.shutdown(); }
    }

    @Test
    void fullProfileLockCanBeExplicitlyRemovedAndStaysRemovedAfterReload() {
        Path file = temporaryDirectory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(file, temporaryDirectory.resolve("backups"));
        try {
            var profile = store.updateManual("Player", p -> { p.rpName = "Name"; p.title = "Alt"; }, true);
            assertTrue(profile.locked);
            assertFalse(store.updateTitleIfChanged("Player", null, "Blockiert"));
            store.updateManual("Player", p -> { }, false);
            assertFalse(profile.locked);
            assertEquals(KnowledgeState.MANUAL, profile.knowledgeState);
        } finally { store.shutdown(); }
        var reloaded = new LocalRpIdentityStore(file, temporaryDirectory.resolve("backups"));
        try {
            reloaded.load();
            assertFalse(reloaded.findByName("Player").orElseThrow().locked);
            assertTrue(reloaded.updateTitleIfChanged("Player", null, "Aktuell"));
        } finally { reloaded.shutdown(); }
    }

    @Test
    void titleLockStillAllowsApiManagedNameAndMetadataUpdates() {
        var store = new LocalRpIdentityStore(false);
        try {
            var profile = store.ensureSeen("Player", null, de.ottoextra.rpnames.model.RpNameSource.SEEN_ONLINE);
            store.importApi("Player", null, "Alter Name", "Fester Titel", "Gemeiner");
            profile.titleLocked = true;
            store.importApi("Player", null, "Neuer Name", "Neuer Titel", "Adlige");
            assertEquals("Neuer Name", profile.rpName);
            assertEquals("Adlige", profile.apiState);
            assertEquals("Fester Titel", profile.title);
            assertFalse(profile.locked);
        } finally { store.shutdown(); }
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
