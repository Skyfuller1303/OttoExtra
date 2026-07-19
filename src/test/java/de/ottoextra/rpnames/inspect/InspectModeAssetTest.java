package de.ottoextra.rpnames.inspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class InspectModeAssetTest {

    @Test
    void lensTextureIsPackagedAsModResource() {
        assertNotNull(getClass().getClassLoader().getResource(
                "assets/ottoextra/textures/gui/inspect_lens.png"));
    }

    @Test
    void lensPostEffectAndShaderArePackaged() {
        ClassLoader loader = getClass().getClassLoader();
        assertNotNull(loader.getResource("assets/ottoextra/post_effect/inspect_lens.json"));
        assertNotNull(loader.getResource("assets/ottoextra/shaders/post/inspect_lens.fsh"));
    }
}
