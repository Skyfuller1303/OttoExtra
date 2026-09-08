package de.ottoextra.welcome;

import com.mojang.authlib.GameProfile;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.nametags.NameTagMode;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.tablist.TablistNameFormatter;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleplayPresetTest {
    private static final GameProfile PLAYER = new GameProfile(new UUID(0, 123), "TestPlayer");
    private final Map<Field, Object> savedServices = new LinkedHashMap<>();
    private OttoExtraConfig config;
    private LocalRpIdentityStore store;

    @BeforeEach
    void setUp() throws Exception {
        config = new OttoExtraConfig();
        var constructor = LocalRpIdentityStore.class.getDeclaredConstructor(boolean.class);
        constructor.setAccessible(true);
        store = constructor.newInstance(false);
        replaceService("store", store);
        replaceService("config", config.rpnames);
        replaceService("catalog", null);
        replaceService("titles", null);
        replaceService("active", true);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (var entry : savedServices.entrySet()) entry.getKey().set(null, entry.getValue());
        if (store != null) store.shutdown();
    }

    @Test
    void comfortKeepsAccountNamesInTabEvenWhenRpNameExists() {
        var profile = store.ensureSeen(PLAYER.name(), PLAYER.id().toString(), RpNameSource.API_IMPORTED);
        profile.rpName = "Alrik";
        RoleplayPreset.COMFORT.apply(config);

        assertEquals("TestPlayer", TablistNameFormatter.visibleSortName(PLAYER));
        assertEquals("TestPlayer", RpNamesServices.unknownChatDisplay(PLAYER.name()));
        assertEquals(NameTagMode.NORMAL, config.nametags.mode);
        assertTrue(config.nametags.showPlayerName);
        assertFalse(config.nametags.showRpName);
    }

    @Test
    void balancedUsesAvailableNamesAndFallsBackToAccounts() {
        RoleplayPreset.BALANCED.apply(config);
        assertEquals("TestPlayer", TablistNameFormatter.visibleSortName(PLAYER));
        var profile = store.ensureSeen(PLAYER.name(), PLAYER.id().toString(), RpNameSource.API_IMPORTED);
        profile.rpName = "Alrik";
        profile.knowledgeState = KnowledgeState.API_IMPORTED;

        assertEquals("Alrik", TablistNameFormatter.visibleSortName(PLAYER));
        assertEquals(NameTagMode.REALISTIC, config.nametags.mode);
        assertTrue(config.nametags.showRpName);
        assertFalse(config.nametags.showPlayerName);
    }

    @Test
    void immersiveHidesImportedIdentityUntilMetAndStripsTabTitles() {
        var profile = store.ensureSeen(PLAYER.name(), PLAYER.id().toString(), RpNameSource.API_IMPORTED);
        profile.rpName = "Alrik";
        profile.knowledgeState = KnowledgeState.API_IMPORTED;
        RoleplayPreset.IMMERSIVE.apply(config);

        assertEquals("Unbekannt", TablistNameFormatter.visibleSortName(PLAYER));
        assertEquals("Unbekannt", RpNamesServices.unknownChatDisplay(PLAYER.name()));
        assertEquals("Unbekannt", TablistNameFormatter.format(PLAYER, Text.literal("Ritter TestPlayer")).getString());
        assertEquals(NameTagMode.HIDE_ALL, config.nametags.mode);

        profile.source = RpNameSource.MANUAL_EDIT;
        profile.knowledgeState = KnowledgeState.KNOWN;
        assertEquals("Alrik", TablistNameFormatter.visibleSortName(PLAYER));
    }

    @Test
    void presetsPreserveConsentDataAndSharedConfigReferences() {
        var names = config.rpnames;
        var tags = config.nametags;
        config.consent.externalImageDownloads = true;
        config.consent.onboardingVersion = 2;
        config.rpnames.unknownPlaceholder = "???";
        config.rpnames.globalRpNameColor = "#123456";
        config.rpnames.syncFromPublicApi = false;
        config.letter.enabled = false;
        var before = com.google.gson.JsonParser.parseString(config.snapshotJson()).getAsJsonObject();
        before.remove("rpnames");
        before.remove("nametags");

        for (RoleplayPreset preset : RoleplayPreset.values()) {
            preset.apply(config);
            assertSame(names, config.rpnames);
            assertSame(tags, config.nametags);
            assertEquals("???", names.unknownPlaceholder);
            assertEquals("#123456", names.globalRpNameColor);
            assertFalse(names.syncFromPublicApi);
            var after = com.google.gson.JsonParser.parseString(config.snapshotJson()).getAsJsonObject();
            after.remove("rpnames");
            after.remove("nametags");
            assertEquals(before, after);
        }
    }

    @Test
    void switchingPresetsAndReloadingKeepsTheirDisplayBehavior() {
        RoleplayPreset.IMMERSIVE.apply(config);
        RoleplayPreset.BALANCED.apply(config);
        config.restoreFrom(config.snapshotJson());

        assertEquals("TestPlayer", TablistNameFormatter.visibleSortName(PLAYER));
        assertEquals(NameTagMode.REALISTIC, config.nametags.mode);
        assertTrue(config.rpnames.tablistShowTitle);
        assertFalse(config.rpnames.proactiveMeet);
    }

    private void replaceService(String name, Object value) throws Exception {
        Field field = RpNamesServices.class.getDeclaredField(name);
        field.setAccessible(true);
        savedServices.put(field, field.get(null));
        field.set(null, value);
    }
}
