package de.ottoextra.playerlist;

import de.ottoextra.api.OttoExtraApiClient;
import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.regions.RegionDataService;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.object.AtlasTextObjectContents;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TablistWappenResolverTest {
    @TempDir Path tempDir;
    private RegionDataService regions;

    @BeforeEach
    void setUp() throws Exception {
        OttoExtraApiClient api = (OttoExtraApiClient) Proxy.newProxyInstance(
                OttoExtraApiClient.class.getClassLoader(), new Class<?>[]{OttoExtraApiClient.class},
                (proxy, method, args) -> null);
        Path snapshot = tempDir.resolve("regions.json");
        Files.writeString(snapshot, """
                {"factions":[
                  {"uuid":"alpha","name":"Ährstein","region_id":"33",
                   "region_name":"Altenessen","media_override_path":"uploads/aehrstein.png"},
                  {"uuid":"beta","name":"Nordeck","region_id":"65",
                   "region_name":"Karlshof","media_override_path":"uploads/schraeggeteilt_rot_silber.png"}
                ],"regions":[]}
                """);
        var constructor = RegionDataService.class.getDeclaredConstructor(
                OttoExtraApiClient.class, int.class, Path.class);
        constructor.setAccessible(true);
        regions = constructor.newInstance(api, 30, snapshot);
    }

    @AfterEach
    void tearDown() {
        if (regions != null) regions.shutdown();
    }

    @Test
    void resolvesFactionFromNestedWappenSpriteWithColorSuffix() {
        Text name = Text.literal("Graf ").append(wappen("wappen_aehrstein_gruen_golden"))
                .append(" Spieler");

        FactionRecord faction = TablistWappenResolver.resolve(name, regions);

        assertEquals("Ährstein", faction.name());
        assertEquals("Altenessen", faction.region_name());
    }

    @Test
    void resolvesUniqueFactionFromApiBannerFile() {
        FactionRecord faction = TablistWappenResolver.resolve(
                wappen("wappen_schraeggeteilt_rot_silber"), regions);

        assertEquals("Nordeck", faction.name());
    }

    @Test
    void ignoresOtherObjectSprites() {
        assertNull(TablistWappenResolver.resolve(object("rank_knight"), regions));
    }

    private static MutableText wappen(String id) {
        return object(id);
    }

    private static MutableText object(String id) {
        return MutableText.of(new ObjectTextContent(new AtlasTextObjectContents(
                AtlasTextObjectContents.DEFAULT_ATLAS,
                Identifier.of("ottonien", "item/gui/" + id))));
    }
}
