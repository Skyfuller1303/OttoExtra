package de.ottoextra.config;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
public final class OttoExtraPaths {
    private OttoExtraPaths() {
    }
    public static Path root() {
        return FabricLoader.getInstance().getConfigDir().resolve("ottoextra");
    }
    public static Path configFile() {
        return root().resolve("ottoextra.json");
    }
    public static Path welcomeState() {
        return root().resolve("welcome.json");
    }
    public static Path cacheDir() {
        return root().resolve("cache");
    }
    public static Path bannersCache() {
        return cacheDir().resolve("banners");
    }
    public static Path headsCache() {
        return cacheDir().resolve("heads");
    }
    public static Path apiCache() {
        return cacheDir().resolve("api");
    }
    public static Path draftsDir() {
        return root().resolve("drafts");
    }
    public static Path importDir() {
        return root().resolve("import");
    }
    public static Path rpnamesDir() {
        return root().resolve("rpnames");
    }
    public static Path rpnamesKnownPlayers() {
        return rpnamesDir().resolve("known-players.json");
    }
    public static Path rpnamesTitleGroups() {
        return rpnamesDir().resolve("title-groups.json");
    }
    public static Path rpnamesImportState() {
        return rpnamesDir().resolve("import-state.json");
    }
    public static Path rpnamesBackups() {
        return rpnamesDir().resolve("backups");
    }
    public static Path resourcepackDir() {
        return root().resolve("resourcepack");
    }
    public static Path resourcepackState() {
        return resourcepackDir().resolve("state.json");
    }
    public static Path resourcepackTmp() {
        return resourcepackDir().resolve("download.tmp");
    }
    public static Path resourcePacksFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
    }
    public static String serverPackFileName() {
        return "Ottonien.zip";
    }
    public static Path serverPackFile() {
        return resourcePacksFolder().resolve(serverPackFileName());
    }
}
