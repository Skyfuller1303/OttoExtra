package de.ottoextra.rpnames.store;

import com.google.gson.JsonParser;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.model.RpNameSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Diagnostic probe against existing compiled product classes, without stubs. */
public final class PersistenceProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected isolated output directory");
        }
        Path runDir = Files.createTempDirectory(Path.of(args[0]), "run-");
        configRestoreIncludesChatLog();
        freshStoreWithoutRepairPersists(runDir.resolve("fresh-control"));
        repairOnFreshStoreAllowsPersistence(runDir.resolve("fresh-repair"));
        healthyRepairKeepsPendingChanges(runDir.resolve("healthy-repair"));
        System.out.println("RESULT: 3 persistence regressions and 2 controls passed.");
        System.out.println("FIXTURES: " + runDir);
        System.out.println("SCOPE: existing compiled production classes and local temporary files; no Minecraft UI or network.");
    }

    private static void configRestoreIncludesChatLog() {
        var config = new OttoExtraConfig();
        String initial = config.snapshotJson();
        config.tweaks.chatLog = false;
        config.tweaks.combatModeHud = false;
        config.restoreFrom(initial);
        require(config.tweaks.chatLog, "Snapshot's chatLog=true must be restored");
        require(config.tweaks.combatModeHud, "Control: combatModeHud is restored");
        config.tweaks.chatLog = false;
        String disabled = config.snapshotJson();
        config.tweaks.chatLog = true;
        config.restoreFrom(disabled);
        require(!config.tweaks.chatLog, "Snapshot's chatLog=false must be restored");
        System.out.println("PASS config_restore: chatLog restored in both directions.");
        System.out.println("CONTROL config_restore: combatModeHud returns to true from the same snapshot.");
    }

    private static void freshStoreWithoutRepairPersists(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path dataFile = directory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(dataFile, directory.resolve("backups"));
        try {
            store.load();
            require(store.lastRepairResult().status() == LocalRpIdentityStore.RepairStatus.NO_DATA,
                    "Control starts without data");
            store.ensureSeen("ControlAccount", null, RpNameSource.SEEN_ONLINE);
            store.saveNow();
            require(Files.exists(dataFile) && playerCount(dataFile) == 1, "Fresh store saves without repair");
        } finally {
            store.shutdown();
        }
        System.out.println("CONTROL fresh_store: NO_DATA, ensureSeen + saveNow => file exists with 1 player.");
    }

    private static void repairOnFreshStoreAllowsPersistence(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path dataFile = directory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(dataFile, directory.resolve("backups"));
        try {
            store.load();
            require(store.lastRepairResult().status() == LocalRpIdentityStore.RepairStatus.NO_DATA,
                    "Fresh repair starts without data");
            var repair = store.repairData();
            require(repair.status() == LocalRpIdentityStore.RepairStatus.NO_DATA,
                    "Missing initial file is a writable empty store");
            store.ensureSeen("UnsavedAccount", null, RpNameSource.SEEN_ONLINE);
            store.saveNow();
            require(store.size() == 1 && Files.exists(dataFile), "New profile persisted after repair");
        } finally {
            store.shutdown();
        }
        require(playerCount(dataFile) == 1, "Profile survives shutdown after fresh repair");
        System.out.println("PASS fresh_repair: NO_DATA; file=1 after saveNow and shutdown.");
    }

    private static void healthyRepairKeepsPendingChanges(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path dataFile = directory.resolve("known-players.json");
        var store = new LocalRpIdentityStore(dataFile, directory.resolve("backups"));
        long elapsedMs;
        try {
            store.ensureSeen("SavedAccount", null, RpNameSource.SEEN_ONLINE);
            store.saveNow();
            require(playerCount(dataFile) == 1, "Fixture contains one saved profile");
            synchronized (store) {
                long start = System.nanoTime();
                store.ensureSeen("PendingAccount", null, RpNameSource.SEEN_ONLINE);
                require(store.size() == 2, "Pending profile exists before repair");
                var repair = store.repairData();
                elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                require(repair.status() == LocalRpIdentityStore.RepairStatus.HEALTHY,
                        "Healthy file takes the normal repair branch");
                require(store.size() == 2 && store.findByName("PendingAccount").isPresent(),
                        "Repair must retain pending memory changes");
            }
            store.saveNow();
        } finally {
            store.shutdown();
        }
        require(playerCount(dataFile) == 2, "Both profiles survive explicit flush and shutdown");
        System.out.println("PASS healthy_repair: memory=2, file=2 after saveNow and shutdown; elapsedMs=" + elapsedMs + ".");
    }

    private static int playerCount(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                .getAsJsonArray("players").size();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
