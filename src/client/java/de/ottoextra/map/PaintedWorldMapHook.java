package de.ottoextra.map;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.BooleanSupplier;

/**
 * Brücke für den GuiMap-Mixin: rendert die gemalte Karte (PaintedMap) auf der
 * Xaero-Weltkarte ZWISCHEN den Terrain-Kacheln und der Element-/Waypoint-Ebene.
 *
 * <p>Vorher lief das Composite im {@code afterRender} der Screen — also NACH den
 * Waypoints, wodurch die Kartentextur über nicht aufgedeckten Bereichen die
 * (z. B. schwarzen) Waypoints überdeckte. Der Mixin ruft hier kurz vor
 * {@code MapElementRenderHandler.render} auf, sodass die Waypoints danach
 * obenauf liegen.</p>
 */
public final class PaintedWorldMapHook {

    private static volatile OttoExtraConfig.Map cfg;
    private static volatile BooleanSupplier visible;

    private PaintedWorldMapHook() {
    }

    public static void install(OttoExtraConfig.Map config, BooleanSupplier visibleCheck) {
        cfg = config;
        visible = visibleCheck;
    }

    /** Vom GuiMap-Mixin aufgerufen — direkt vor der Waypoint-/Element-Ebene. */
    public static void renderUnderElements(Screen screen) {
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
            PaintedMapRenderer.render(view, screen.width, screen.height);
        } catch (Throwable t) {
            OttoExtra.LOGGER.warn("[map] PaintedMap-Hook-Fehler: {}", t.toString());
        }
    }
}
