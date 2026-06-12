package de.ottoextra.map;

import de.ottoextra.OttoExtra;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Einzige Berührungsfläche zu Xaero (World Map). Alles via Reflection, lazy,
 * fehlertolerant: jeder Fehler deaktiviert die Bridge einmalig (Warn-Log),
 * niemals Crash. Ausserhalb dieser Klasse existiert kein Xaero-Symbol.
 *
 * <p>Verifiziert gegen xaeroworldmap-1.40.16 (Modpack-JAR):
 * {@code xaero.map.gui.GuiMap} mit privaten Feldern {@code cameraX}, {@code cameraZ},
 * {@code scale}, {@code screenScale} und optional
 * {@code getCurrentMapCoordinateScale()}. Projektion (aus OttoMap übernommen):
 * {@code effScale = scale * coordScale / screenScale};
 * {@code sx = (wx - cameraX) * effScale + width/2}.</p>
 */
public final class XaeroMapBridge {

    /** Sichtparameter der geöffneten Worldmap für einen Frame. */
    public record View(double cameraX, double cameraZ, double effScale, int width, int height) {
        public double worldMinX() {
            return cameraX - width / effScale / 2.0;
        }

        public double worldMaxX() {
            return cameraX + width / effScale / 2.0;
        }

        public double worldMinZ() {
            return cameraZ - height / effScale / 2.0;
        }

        public double worldMaxZ() {
            return cameraZ + height / effScale / 2.0;
        }

        public float screenX(double worldX) {
            return (float) ((worldX - cameraX) * effScale + width / 2.0);
        }

        public float screenY(double worldZ) {
            return (float) ((worldZ - cameraZ) * effScale + height / 2.0);
        }
    }

    private static final String GUI_MAP = "xaero.map.gui.GuiMap";

    private static volatile boolean disabled = false;
    private static volatile boolean resolved = false;
    private static Class<?> guiMapClass;
    private static Field cameraXField;
    private static Field cameraZField;
    private static Field scaleField;
    private static Field screenScaleField;
    private static Method coordScaleMethod; // optional

    private XaeroMapBridge() {
    }

    public static boolean isWorldmapInstalled() {
        return FabricLoader.getInstance().isModLoaded("xaeroworldmap");
    }

    public static boolean isDisabled() {
        return disabled;
    }

    /** Ist dieser Screen die Xaero-Worldmap? (Klassennamen-Check, kein Klassenladen.) */
    public static boolean isWorldmapScreen(Screen screen) {
        return screen != null && GUI_MAP.equals(screen.getClass().getName());
    }

    /**
     * Liest die Sichtparameter der geöffneten Worldmap.
     * {@code null} bei deaktivierter Bridge oder Lesefehler.
     */
    public static View view(Screen screen) {
        if (disabled || screen == null) {
            return null;
        }
        try {
            if (!resolved) {
                resolve(screen);
            }
            double cameraX = cameraXField.getDouble(screen);
            double cameraZ = cameraZField.getDouble(screen);
            double scale = scaleField.getDouble(screen);
            double screenScale = screenScaleField.getDouble(screen);
            double coordScale = 1.0;
            if (coordScaleMethod != null) {
                try {
                    Object r = coordScaleMethod.invoke(screen);
                    if (r instanceof Number n) {
                        coordScale = n.doubleValue();
                    }
                } catch (Throwable ignored) {
                    // optionaler Multiplikator
                }
            }
            double effScale = scale * coordScale / screenScale;
            if (!(effScale > 0) || Double.isNaN(effScale)) {
                return null;
            }
            return new View(cameraX, cameraZ, effScale, screen.width, screen.height);
        } catch (Throwable t) {
            disable("Sichtparameter lesen fehlgeschlagen: " + t);
            return null;
        }
    }

    /**
     * Zentriert die Worldmap-Kamera auf eine Weltposition (Lehen-Fokus).
     * Fehler deaktivieren die Bridge nicht — Fokus ist ein Komfort-Feature.
     */
    public static void setCamera(Screen screen, double worldX, double worldZ) {
        if (disabled || !resolved || screen == null) {
            return;
        }
        try {
            cameraXField.setDouble(screen, worldX);
            cameraZField.setDouble(screen, worldZ);
        } catch (Throwable t) {
            OttoExtra.LOGGER.debug("[map] Kamera-Fokus fehlgeschlagen: {}", t.toString());
        }
    }

    /** Lazy Reflection-Auflösung — NIE im Mod-Init aufrufen (GLFW-Lehre). */
    private static synchronized void resolve(Screen screen) throws ReflectiveOperationException {
        if (resolved) {
            return;
        }
        guiMapClass = screen.getClass();
        cameraXField = guiMapClass.getDeclaredField("cameraX");
        cameraXField.setAccessible(true);
        cameraZField = guiMapClass.getDeclaredField("cameraZ");
        cameraZField.setAccessible(true);
        scaleField = guiMapClass.getDeclaredField("scale");
        scaleField.setAccessible(true);
        screenScaleField = guiMapClass.getDeclaredField("screenScale");
        screenScaleField.setAccessible(true);
        try {
            coordScaleMethod = guiMapClass.getDeclaredMethod("getCurrentMapCoordinateScale");
            coordScaleMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            coordScaleMethod = null; // optional
        }
        resolved = true;
        OttoExtra.LOGGER.info("[map] Xaero-Bridge aktiv (GuiMap-Felder aufgeloest).");
    }

    private static void disable(String reason) {
        if (!disabled) {
            disabled = true;
            OttoExtra.LOGGER.warn("[map] Xaero-Bridge deaktiviert: {} — Overlay bleibt aus.", reason);
        }
    }
}
