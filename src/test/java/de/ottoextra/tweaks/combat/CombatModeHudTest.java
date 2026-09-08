package de.ottoextra.tweaks.combat;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CombatModeHudTest {
    @Test
    void mirrorsTheOffhandPositionAndLeavesRoomForRightSideHudElements() {
        for (int width : new int[]{320, 640, 854, 1280}) {
            int vanillaLeftIcon = width / 2 - 117;
            assertEquals(width / 2 * 2 - vanillaLeftIcon - 16, CombatModeHud.iconX(width, false));
            assertEquals(29, CombatModeHud.iconX(width, true) - CombatModeHud.iconX(width, false));
        }
    }

    @Test
    void allModeAndUnknownFamilyIconsAreBundledAtNativeSize() throws Exception {
        for (CombatMode mode : CombatMode.values()) checkIcon(mode.icon());
        for (WeaponFamily family : WeaponFamily.values()) checkIcon(family.icon());
    }

    private static void checkIcon(String name) throws Exception {
        Path file = Path.of("src/main/resources/assets/ottoextra/textures/gui/combat", name + ".png");
        var image = ImageIO.read(file.toFile());
        assertNotNull(image, name);
        assertEquals(16, image.getWidth(), name);
        assertEquals(16, image.getHeight(), name);
    }
}
