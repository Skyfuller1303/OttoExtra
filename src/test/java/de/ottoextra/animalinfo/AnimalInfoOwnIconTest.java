package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnimalInfoOwnIconTest {

    @Test
    void allOwnIconsExistAsNineByNine() throws Exception {
        for (String name : new String[]{"mast", "melkbar", "scherbar", "zucht", "wartet", "jung",
                "weiblich", "maennlich"}) {
            try (InputStream in = getClass().getResourceAsStream(
                    "/assets/ottoextra/textures/gui/animalinfo/" + name + ".png")) {
                assertNotNull(in, name);
                var image = ImageIO.read(in);
                assertEquals(9, image.getWidth(), name);
                assertEquals(9, image.getHeight(), name);
            }
        }
    }

    @Test
    void statusIconsFollowTheStatus() {
        assertEquals("zucht", AnimalToastModel.statusIcon(AnimalStatus.Kind.READY));
        assertEquals("zucht", AnimalToastModel.statusIcon(AnimalStatus.Kind.IN_LOVE));
        assertEquals("wartet", AnimalToastModel.statusIcon(AnimalStatus.Kind.WAITING));
        assertEquals("jung", AnimalToastModel.statusIcon(AnimalStatus.Kind.YOUNG));
        assertEquals("mast", AnimalToastModel.statusIcon(AnimalStatus.Kind.NEED_MAST));
        assertNull(AnimalToastModel.statusIcon(AnimalStatus.Kind.UNKNOWN));
    }
}
