package de.ottoextra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ottoextra.letter.LetterDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OttoExtraBackupServiceTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    @TempDir
    Path temporaryDirectory;

    @Test
    void backupWithValidGrantCannotUndoWithdrawalOnDiskOrInMemory() throws Exception {
        Path root = temporaryDirectory.resolve("ottoextra");
        Path file = root.resolve("ottoextra.json");
        OttoExtraConfig config = new OttoExtraConfig();
        var purpose = de.ottoextra.privacy.ConsentPolicy.Purpose.EXTERNAL_IMAGES;
        String scope = de.ottoextra.privacy.ConsentPolicy.scope(config, purpose);
        de.ottoextra.privacy.ConsentPolicy.decide(config, purpose, true, scope);
        config.save(file);
        Path backup = OttoExtraBackupService.createBackup(root);
        de.ottoextra.privacy.ConsentPolicy.decide(config, purpose, false, scope);
        config.save(file);
        assertTrue(OttoExtraBackupService.restoreBackup(backup, root, config));
        assertFalse(de.ottoextra.privacy.ConsentPolicy.allowed(config, purpose));
        assertFalse(de.ottoextra.privacy.ConsentPolicy.allowed(OttoExtraConfig.load(file), purpose));
        assertTrue(GSON.fromJson(Files.readString(backup.resolve("ottoextra/ottoextra.json")),
                OttoExtraConfig.class).consent.externalImageDownloads);
    }

    @Test
    void restoredConfigSurvivesNextSaveAndRetainsSharedModuleReferences() throws Exception {
        Path configRoot = temporaryDirectory.resolve("ottoextra");
        Path configFile = configRoot.resolve("ottoextra.json");
        OttoExtraConfig config = new OttoExtraConfig();
        config.chat.enabled = false;
        config.consent.externalImageDownloads = false;
        config.save(configFile);
        Path backup = OttoExtraBackupService.createBackup(configRoot);
        String originalBackup = Files.readString(backup.resolve("ottoextra/ottoextra.json"));
        var chat = config.chat;
        var consent = config.consent;
        config.chat.enabled = true;
        config.consent.externalImageDownloads = true;
        config.save(configFile);

        assertTrue(OttoExtraBackupService.restoreBackup(backup, configRoot, config));
        assertSame(chat, config.chat);
        assertSame(consent, config.consent);
        assertFalse(chat.enabled);
        assertFalse(consent.externalImageDownloads);
        config.save(configFile);

        OttoExtraConfig reloaded = GSON.fromJson(Files.readString(configFile), OttoExtraConfig.class);
        assertFalse(reloaded.chat.enabled);
        assertFalse(reloaded.consent.externalImageDownloads);
        assertEquals(originalBackup, Files.readString(backup.resolve("ottoextra/ottoextra.json")));
    }

    @Test
    void savedDraftJsonIsIncludedInManifestAndRestoredByteForByte() throws Exception {
        Path configRoot = temporaryDirectory.resolve("ottoextra");
        LetterDraft draft = LetterDraft.empty();
        draft.meta.name = "Brief an die Gräfin";
        draft.pages.clear();
        draft.pages.add("Geehrte Gräfin,\nNachricht mit äöü und <Siegel>.");
        draft.pages.add("Zweite Seite.");
        Path draftFile = OttoExtraPaths.draftsDir(configRoot).resolve(draft.meta.draftId + ".json");
        Files.createDirectories(draftFile.getParent());
        String savedJson = GSON.toJson(draft);
        Files.writeString(draftFile, savedJson, StandardCharsets.UTF_8);

        Path backup = OttoExtraBackupService.createBackup(configRoot);

        String relative = "ottoextra/drafts/" + draft.meta.draftId + ".json";
        assertEquals(savedJson, Files.readString(backup.resolve(relative)));
        var manifest = GSON.fromJson(Files.readString(backup.resolve("backup-manifest.json")),
                OttoExtraBackupService.Manifest.class);
        assertTrue(manifest.files.stream().anyMatch(file -> relative.equals(file.source)
                && relative.equals(file.target) && file.sha256.length() == 64));
        Files.delete(draftFile);

        assertTrue(OttoExtraBackupService.restoreBackup(backup, configRoot, new OttoExtraConfig()));
        assertEquals(savedJson, Files.readString(draftFile));
        LetterDraft restored = GSON.fromJson(Files.readString(draftFile), LetterDraft.class);
        assertEquals(draft.meta.name, restored.meta.name);
        assertEquals(draft.pages, restored.pages);
    }

    @Test
    void backupDirectoryCollisionPreservesExistingFiles() throws Exception {
        Path backups = temporaryDirectory.resolve("backups");
        String stamp = "2026-09-06_12-00-00";
        Path first = OttoExtraBackupService.createBackupDirectory(backups, stamp);
        Files.writeString(first.resolve("keep.json"), "old backup");

        Path second = OttoExtraBackupService.createBackupDirectory(backups, stamp);

        assertNotEquals(first, second);
        assertEquals(stamp + "-1", second.getFileName().toString());
        assertEquals("old backup", Files.readString(first.resolve("keep.json")));
    }

    @Test
    void laterCopyFailureKeepsRestoredConfigAndNextSaveConsistent() throws Exception {
        Path configRoot = temporaryDirectory.resolve("ottoextra");
        Path configFile = configRoot.resolve("ottoextra.json");
        OttoExtraConfig config = new OttoExtraConfig();
        config.chat.enabled = false;
        config.save(configFile);
        Path backup = OttoExtraBackupService.createBackup(configRoot);
        Path blockedSource = backup.resolve("blocked/data.json");
        Files.createDirectories(blockedSource.getParent());
        Files.writeString(blockedSource, "{}", StandardCharsets.UTF_8);
        Files.writeString(temporaryDirectory.resolve("blocked"), "not a directory");
        config.chat.enabled = true;
        config.save(configFile);

        assertFalse(OttoExtraBackupService.restoreBackup(backup, configRoot, config));

        assertFalse(config.chat.enabled);
        config.save(configFile);
        OttoExtraConfig reloaded = GSON.fromJson(Files.readString(configFile), OttoExtraConfig.class);
        assertFalse(reloaded.chat.enabled);
    }

    @Test
    void missingOrInvalidConfigBackupLeavesLiveConfigAndDiskUntouched() throws Exception {
        Path configRoot = temporaryDirectory.resolve("ottoextra");
        Path configFile = configRoot.resolve("ottoextra.json");
        OttoExtraConfig config = new OttoExtraConfig();
        config.chat.enabled = false;
        config.save(configFile);
        String original = Files.readString(configFile);

        assertFalse(OttoExtraBackupService.restoreBackup(
                temporaryDirectory.resolve("missing"), configRoot, config));
        Path brokenBackup = temporaryDirectory.resolve("broken");
        Path brokenConfig = brokenBackup.resolve("ottoextra/ottoextra.json");
        Files.createDirectories(brokenConfig.getParent());
        for (String invalid : new String[]{"{abgebrochen", "null", ""}) {
            Files.writeString(brokenConfig, invalid);
            assertFalse(OttoExtraBackupService.restoreBackup(brokenBackup, configRoot, config));
            assertFalse(config.chat.enabled);
            assertEquals(original, Files.readString(configFile));
        }
    }
}
