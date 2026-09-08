package de.ottoextra.map;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.logging.FailureLogGate;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.BooleanSupplier;

public final class PaintedWorldMapHook {

    private static final FailureLogGate FAILURE_LOG = new FailureLogGate();
    private static final WorldMapRenderPass RENDER_PASS = new WorldMapRenderPass();

    private static volatile OttoExtraConfig.Map cfg;
    private static volatile BooleanSupplier visible;

    private PaintedWorldMapHook() {
    }

    public static void install(OttoExtraConfig.Map config, BooleanSupplier visibleCheck) {
        cfg = config;
        visible = visibleCheck;
    }

    public static void beginFrame(Screen screen) {
        RENDER_PASS.begin(screen);
    }

    public static void renderUnderElements(Screen screen) {
        if (RENDER_PASS.shouldRenderLate(screen)) {
            render(screen, 0, 0, null);
        }
    }

    public static void renderBeforeXaeroPlus(Screen screen, int cameraX, int cameraZ, MatrixStack matrices) {
        if (XaeroMapBridge.isWorldmapScreen(screen) && RENDER_PASS.claimEarly(screen)) {
            // Claim even a failed/skipped paint: a late retry would cover XaeroPlus again.
            render(screen, cameraX, cameraZ, matrices);
        }
    }

    private static void render(Screen screen, int cameraX, int cameraZ, MatrixStack matrices) {
        OttoExtraConfig.Map c = cfg;
        BooleanSupplier v = visible;
        try {
            if (c == null || v == null || !v.getAsBoolean() || XaeroMapBridge.isDisabled()) {
                return;
            }
            if (!c.paintedMap || PaintedMapRenderer.isDisabled()) {
                return;
            }
            XaeroMapBridge.View view = XaeroMapBridge.view(screen);
            if (view == null) {
                return;
            }
            PaintedMapRenderer.setUserOffset(c.paintedMapOffsetX, c.paintedMapOffsetZ);
            if (matrices == null) {
                PaintedMapRenderer.render(view, screen.width, screen.height);
            } else {
                XaeroMapBridge.View framebufferView = XaeroFramebufferView.fromMatrix(
                        matrices.peek().getPositionMatrix(), cameraX, cameraZ, screen.width, screen.height);
                if (framebufferView == null) {
                    throw new IllegalStateException("Unsupported XaeroPlus framebuffer transform");
                }
                PaintedMapRenderer.render(framebufferView, screen.width, screen.height, view.effScale(), true);
            }
            if (FAILURE_LOG.onSuccess()) {
                DebugLog.debug("[map] PaintedMap-Hook wieder aktiv.");
            }
        } catch (Throwable t) {
            if (FAILURE_LOG.onFailure()) {
                OttoExtra.LOGGER.warn("[map] PaintedMap-Hook-Fehler: {}", t.toString());
            }
        }
    }
}
