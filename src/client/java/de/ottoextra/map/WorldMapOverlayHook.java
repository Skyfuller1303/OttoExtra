package de.ottoextra.map;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/** Draws map decorations after the map pass, before Xaero's screen UI. */
public final class WorldMapOverlayHook {
    private static ScreenEvents.BeforeRender renderer;

    private WorldMapOverlayHook() {
    }

    public static void install(ScreenEvents.BeforeRender callback) {
        renderer = callback;
    }

    public static void render(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        if (renderer == null) {
            return;
        }
        // Separate roots keep deferred GUI batching from moving decorations above widgets.
        context.createNewRootLayer();
        try {
            renderer.beforeRender(screen, context, mouseX, mouseY, delta);
        } finally {
            context.createNewRootLayer();
        }
    }
}
