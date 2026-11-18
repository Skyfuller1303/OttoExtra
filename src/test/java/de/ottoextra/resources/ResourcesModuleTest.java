package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcesModuleTest {

    @Test
    void formatsServerCountdownWithoutLocalCooldownState() {
        assertEquals("1 min", ResourcesModule.remaining(1));
        assertEquals("1 h 1 min", ResourcesModule.remaining(3_601));
        assertEquals("–", ResourcesModule.remaining(-1));
    }

    @Test
    void mineCountdownTargetsPartialStageBeforeSeventyPercent() {
        ResourceSnapshot emptyMine = new ResourceSnapshot("mine", "Nordmine", "IRON_MINE",
                "Lehen", "minecraft:overworld", ResourceZone.point(1, 64, 1, 3),
                "REGENERATING", 0, "", "", 7_200, 14_400);
        ResourceSnapshot partialMine = new ResourceSnapshot("mine", "Nordmine", "IRON_MINE",
                "Lehen", "minecraft:overworld", ResourceZone.point(1, 64, 1, 3),
                "REGENERATING", 70, "", "", 7_200, 7_200);
        assertEquals(7_200, ResourcesModule.regenerationCountdown(emptyMine));
        assertEquals("ottoextra.resources.stage.partial",
                ResourcesModule.countdownTargetKey(emptyMine.type(), emptyMine.regenerationPercentage()));
        assertEquals("ottoextra.resources.entered.mine_regenerating",
                ResourcesModule.enteredMessageKey(emptyMine.type(), emptyMine.regenerationPercentage()));
        assertEquals(7_200, ResourcesModule.regenerationCountdown(partialMine));
        assertEquals("ottoextra.resources.stage.full",
                ResourcesModule.countdownTargetKey(partialMine.type(), partialMine.regenerationPercentage()));
        assertEquals("ottoextra.resources.entered.mine_partial",
                ResourcesModule.enteredMessageKey(partialMine.type(), partialMine.regenerationPercentage()));
    }

    @Test
    void recognizesOttonienLapisOreAsSalt() {
        assertTrue(ResourcesModule.relevant("SALT_DEPOSIT",
                "minecraft:lapis_ore", "BLOCK_BREAK"));
        assertTrue(ResourcesModule.relevant("SALT_MINE",
                "minecraft:deepslate_lapis_ore", "BLOCK_BREAK"));
        assertFalse(ResourcesModule.relevant("SALT_DEPOSIT",
                "minecraft:stone", "BLOCK_BREAK"));
    }

    @Test
    void recognizesNormalAndDeepslateOreAcrossAllMineTypes() {
        assertTrue(ResourcesModule.relevant("IRON_MINE", "minecraft:iron_ore", "BLOCK_BREAK"));
        assertTrue(ResourcesModule.relevant("IRON_MINE", "minecraft:deepslate_iron_ore", "BLOCK_BREAK"));
        assertTrue(ResourcesModule.relevant("COAL_MINE", "minecraft:coal_ore", "BLOCK_BREAK"));
        assertTrue(ResourcesModule.relevant("COAL_MINE", "minecraft:deepslate_coal_ore", "BLOCK_BREAK"));
        assertTrue(ResourcesModule.relevant("SALT_MINE", "minecraft:lapis_ore", "BLOCK_BREAK"));
        assertTrue(ResourcesModule.relevant("SALT_MINE", "minecraft:deepslate_lapis_ore", "BLOCK_BREAK"));
    }

    @Test
    void recognizesBlocksAtBothEdgesOfLargeMineZone() {
        ResourceZone mine = ResourceZone.cuboid(
                new net.minecraft.util.math.BlockPos(-120, -40, 75),
                new net.minecraft.util.math.BlockPos(240, 110, 460));
        assertTrue(mine.contains(new net.minecraft.util.math.BlockPos(-120, -40, 75)));
        assertTrue(mine.contains(new net.minecraft.util.math.BlockPos(240, 110, 460)));
        assertFalse(mine.contains(new net.minecraft.util.math.BlockPos(241, 110, 460)));
    }

    @Test
    void recognizesInteractionInsideApiaryRegardlessOfDisplayBlock() {
        assertTrue(ResourcesModule.relevant("APIARY",
                "minecraft:beehive", "BLOCK_INTERACT"));
        assertTrue(ResourcesModule.relevant("APIARY",
                "minecraft:player_head", "BLOCK_INTERACT"));
        assertFalse(ResourcesModule.relevant("APIARY",
                "minecraft:beehive", "BLOCK_BREAK"));
        assertFalse(ResourcesModule.relevant("APIARY",
                "minecraft:player_head", "BLOCK_BREAK"));
    }

    @Test
    void recognizesTuffBrokenInsidePeatBog() {
        assertTrue(ResourcesModule.relevant("PEAT_BOG", "minecraft:tuff", "BLOCK_BREAK"));
        assertFalse(ResourcesModule.relevant("PEAT_BOG", "minecraft:tuff", "BLOCK_INTERACT"));
        assertFalse(ResourcesModule.relevant("IRON_DEPOSIT", "minecraft:tuff", "BLOCK_BREAK"));
    }

    @Test
    void allowsMineExtractionAgainAtPartialStage() {
        assertTrue(ResourcesModule.canStartHarvest("IRON_MINE", "READY", 100));
        assertTrue(ResourcesModule.canStartHarvest("IRON_MINE", "REGENERATING", 70));
        assertTrue(ResourcesModule.canStartHarvest("COAL_MINE", "REGENERATING", 80));
        assertFalse(ResourcesModule.canStartHarvest("SALT_MINE", "REGENERATING", 69));
        assertFalse(ResourcesModule.canStartHarvest("IRON_DEPOSIT", "REGENERATING", 70));
        assertFalse(ResourcesModule.canStartHarvest("IRON_MINE", "DISABLED", 100));
        assertTrue(ResourcesModule.canForceRestart("IRON_MINE", "REGENERATING"));
        assertTrue(ResourcesModule.canForceRestart("COAL_DEPOSIT", "REGENERATING"));
        assertFalse(ResourcesModule.canForceRestart("SALT_MINE", "READY"));
        assertFalse(ResourcesModule.canForceRestart("PEAT_BOG", "REGENERATING"));
    }

    @Test
    void recognizesAndTracksAllMonasteryGardenPlants() {
        Map<String, String> plants = Map.of(
                "minecraft:brown_mushroom", "ottoextra:garden/steinpilzhut",
                "minecraft:red_mushroom", "ottoextra:garden/fliegenpilzhut",
                "minecraft:warped_fungus", "ottoextra:garden/hoehlenpilzhut",
                "minecraft:red_tulip", "ottoextra:garden/weinrautenblueten",
                "minecraft:bubble_coral", "ottoextra:garden/nesselblaetter",
                "minecraft:oxeye_daisy", "ottoextra:garden/baldrianblueten",
                "minecraft:pink_tulip", "ottoextra:garden/ringelblumenblueten");
        plants.forEach((itemId, trackingKey) -> {
            assertTrue(ResourcesModule.relevant(
                    "MONASTERY_GARDEN", itemId, "BLOCK_BREAK"));
            assertFalse(ResourcesModule.relevant(
                    "MONASTERY_GARDEN", itemId, "BLOCK_INTERACT"));
            assertEquals(trackingKey, ResourcesModule.trackingItemKey(
                    "MONASTERY_GARDEN", itemId, ""));
        });
        assertFalse(ResourcesModule.relevant(
                "MONASTERY_GARDEN", "minecraft:wheat", "BLOCK_BREAK"));
    }

    @Test
    void mapsResourceTypesToBundledToastIcons() {
        assertEquals("eisenerz.png", ResourcesModule.resourceIconName("IRON_DEPOSIT"));
        assertEquals("eisenerz.png", ResourcesModule.resourceIconName("IRON_MINE"));
        assertEquals("salzkristall.png", ResourcesModule.resourceIconName("SALT_DEPOSIT"));
        assertEquals("steinkohle.png", ResourcesModule.resourceIconName("COAL_MINE"));
        assertEquals("tonklumpen.png", ResourcesModule.resourceIconName("CLAY_PIT"));
        assertEquals("honig.png", ResourcesModule.resourceIconName("APIARY"));
        assertEquals("rosenkranz.png", ResourcesModule.resourceIconName("MONASTERY_GARDEN"));
        assertEquals("torf.png", ResourcesModule.resourceIconName("PEAT_BOG"));
        assertTrue(ResourcesModule.relevant("CLAY_PIT", "minecraft:clay", "BLOCK_BREAK"));
        assertEquals("ottoextra.resources.type.deposit",
                ResourcesModule.resourceTypeLabelKey("COAL_DEPOSIT"));
        assertEquals("ottoextra.resources.type.mine",
                ResourcesModule.resourceTypeLabelKey("SALT_MINE"));
        assertEquals("ottoextra.resources.type.pit",
                ResourcesModule.resourceTypeLabelKey("CLAY_PIT"));
        assertEquals("ottoextra.resources.material.coal",
                ResourcesModule.resourceMaterialKey("COAL_DEPOSIT"));
        assertEquals("ottoextra.resources.material.clay",
                ResourcesModule.resourceMaterialKey("CLAY_PIT"));
    }

    @Test
    void exposesOnlyActiveResourcesInCurrentMapDimension() {
        ResourceSnapshot visible = snapshot("a", "APIARY", "minecraft:overworld",
                ResourceZone.cuboid(new net.minecraft.util.math.BlockPos(10, 60, 20),
                        new net.minecraft.util.math.BlockPos(14, 70, 28)), "READY");
        ResourceSnapshot disabled = snapshot("c", "PEAT_BOG", "minecraft:overworld",
                ResourceZone.point(3, 64, 4, 3), "DISABLED");
        ResourceSnapshot nether = snapshot("d", "COAL_MINE", "minecraft:the_nether",
                ResourceZone.point(5, 64, 6, 3), "READY");
        assertEquals(List.of(visible), ResourceMapOverlay.visible(
                List.of(visible, disabled, nether), "minecraft:overworld"));
        assertEquals(12.0, ResourceMapOverlay.centerX(visible.zone()));
        assertEquals(24.0, ResourceMapOverlay.centerZ(visible.zone()));
        var view = new de.ottoextra.map.XaeroMapBridge.View(0, 0, 1, 200, 100);
        assertEquals(visible, ResourceMapOverlay.resourceAt(
                List.of(visible), "minecraft:overworld", view, 112, 74));
        assertEquals(null, ResourceMapOverlay.resourceAt(
                List.of(visible), "minecraft:overworld", view, 10, 10));
    }

    @Test
    void scalesWorldMapIconsAndColorsAvailabilityStatus() {
        assertEquals(8, ResourceMapOverlay.iconSize(0.06));
        assertEquals(12, ResourceMapOverlay.iconSize(0.10));
        assertEquals(16, ResourceMapOverlay.iconSize(0.25));

        ResourceZone zone = ResourceZone.point(1, 64, 2, 3);
        ResourceSnapshot ready = new ResourceSnapshot("ready", "Bereit", "IRON_MINE",
                "Lehen", "minecraft:overworld", zone, "READY", 100,
                "", "", -1, 0);
        ResourceSnapshot partial = new ResourceSnapshot("partial", "Teilvoll", "IRON_MINE",
                "Lehen", "minecraft:overworld", zone, "REGENERATING", 70,
                "", "", 3600, 7200);
        ResourceSnapshot mineSoon = new ResourceSnapshot("soon", "Bald", "COAL_MINE",
                "Lehen", "minecraft:overworld", zone, "REGENERATING", 0,
                "", "", 900, 8100);
        ResourceSnapshot gardenSoon = new ResourceSnapshot("garden", "Garten", "MONASTERY_GARDEN",
                "Lehen", "minecraft:overworld", zone, "REGENERATING", 0,
                "", "", 900, 899);
        ResourceSnapshot depositSoon = new ResourceSnapshot("deposit", "Lagerstätte", "IRON_DEPOSIT",
                "Lehen", "minecraft:overworld", zone, "REGENERATING", 70,
                "", "", 900, 899);
        ResourceSnapshot blocked = new ResourceSnapshot("blocked", "Gesperrt", "SALT_MINE",
                "Lehen", "minecraft:overworld", zone, "REGENERATING", 0,
                "", "", 901, 8101);

        assertEquals(0xFF42D66B, ResourceMapOverlay.statusColor(ready));
        assertEquals(0xFFFFC928, ResourceMapOverlay.statusColor(partial));
        assertEquals(0xFFFFC928, ResourceMapOverlay.statusColor(mineSoon));
        assertEquals(0xFFE14949, ResourceMapOverlay.statusColor(gardenSoon));
        assertEquals(0xFFE14949, ResourceMapOverlay.statusColor(depositSoon));
        assertEquals(0xFFE14949, ResourceMapOverlay.statusColor(blocked));
        assertTrue(ResourceMapOverlay.showsMinePickaxe("IRON_MINE"));
        assertFalse(ResourceMapOverlay.showsMinePickaxe("IRON_DEPOSIT"));
        assertFalse(ResourceMapOverlay.showsMinePickaxe("MONASTERY_GARDEN"));
        assertEquals(7, ResourceMapOverlay.minePickaxeSize(8));
        assertEquals(9, ResourceMapOverlay.minePickaxeSize(12));
        assertEquals(11, ResourceMapOverlay.minePickaxeSize(16));
        assertEquals(0xFFE14949, ResourceBookScreen.statusColor(gardenSoon));
        assertEquals(0xFFE4C477, ResourceBookScreen.statusColor(partial));
        assertEquals(0xFF9FC7A8, ResourceBookScreen.statusColor(ready));
        assertEquals("Bereit", ResourceMapOverlay.statusLabel(ready));
        assertEquals("Noch nie", ResourceMapOverlay.lastFarmedLabel(""));
        assertTrue(ResourceMapOverlay.lastFarmedLabel("2026-08-27T12:30:00+00:00")
                .endsWith(" Uhr"));
    }

    @Test
    void countsOnlyPositiveRelevantInventoryGrowth() {
        assertTrue(ResourcesModule.trackedItem("IRON_DEPOSIT", "minecraft:raw_iron"));
        assertTrue(ResourcesModule.trackedItem("COAL_MINE", "minecraft:charcoal"));
        assertTrue(ResourcesModule.trackedItem("PEAT_BOG", "minecraft:charcoal"));
        assertTrue(ResourcesModule.trackedItem("SALT_DEPOSIT", "minecraft:amethyst_shard"));
        assertTrue(ResourcesModule.trackedItem("APIARY", "minecraft:honeycomb"));
        assertTrue(ResourcesModule.trackedItem("CLAY_PIT", "minecraft:clay_ball"));
        assertEquals("ottoextra:garden/steinpilzhut", ResourcesModule.trackingItemKey(
                "MONASTERY_GARDEN", "minecraft:amethyst_shard", "Steinpilzhut"));
        assertEquals("ottoextra:garden/hoehlenpilzhut", ResourcesModule.trackingItemKey(
                "MONASTERY_GARDEN", "minecraft:amethyst_shard", "Höhlenpilzhut"));
        assertEquals("ottoextra:garden/ringelblumenblueten", ResourcesModule.trackingItemKey(
                "MONASTERY_GARDEN", "minecraft:amethyst_shard", "Ringelblumenblüten"));
        assertEquals("ottoextra:garden/fliegenpilzhut", ResourcesModule.trackingItemKey(
                "MONASTERY_GARDEN", "minecraft:amethyst_shard", "Fliegenpilzhut"));
        assertEquals("ottoextra:garden/nesselblaetter", ResourcesModule.trackingItemKey(
                "MONASTERY_GARDEN", "minecraft:amethyst_shard", "Nesselblätter"));
        assertEquals("ottoextra:garden/weinrautenblueten", ResourcesModule.trackingItemKey(
                "MONASTERY_GARDEN", "minecraft:amethyst_shard", "Weinrautenblüten"));
        assertEquals("ottoextra:garden/baldrianblueten", ResourcesModule.trackingItemKey(
                "MONASTERY_GARDEN", "minecraft:amethyst_shard", "Baldrianblüten"));
        assertFalse(ResourcesModule.trackedItem("COAL_MINE", "minecraft:raw_iron"));
        assertFalse(ResourcesModule.trackedItem("COAL_MINE", "minecraft:coal"));
    }

    @Test
    void keepsConfirmationAvailableForOneMinuteAfterLeavingZone() {
        long leftAt = 10_000L;
        assertTrue(ResourcesModule.confirmationAllowed(true, 0L, leftAt));
        assertTrue(ResourcesModule.confirmationAllowed(false, leftAt, leftAt));
        assertTrue(ResourcesModule.confirmationAllowed(false, leftAt, leftAt + 60_000L));
        assertFalse(ResourcesModule.confirmationAllowed(false, leftAt, leftAt + 60_001L));
        assertFalse(ResourcesModule.confirmationAllowed(false, 0L, leftAt));
    }

    private static ResourceSnapshot snapshot(String id, String type, String dimension,
                                             ResourceZone zone, String status) {
        return new ResourceSnapshot(id, id, type, "Lehen", dimension, zone, status,
                100, "", "", -1, 0);
    }
}
