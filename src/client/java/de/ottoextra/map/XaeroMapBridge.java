package de.ottoextra.map;

import de.ottoextra.OttoExtra;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Einzige Berührungsfläche zu Xaero (World Map). Alles via Reflection, lazy,
 * fehlertolerant: Lesefehler schalten das Overlay nur für den aktuellen Frame
 * ab und heilen sich selbst, sobald Xaero wieder einen lesbaren Zustand hat —
 * niemals Crash, niemals dauerhaftes Aus bis Client-Neustart. Ausserhalb dieser
 * Klasse existiert kein Xaero-Symbol.
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

    /**
     * Transienter Fehlerzustand (nicht dauerhaft). Nur zum Drosseln der Logs:
     * 1× Warn beim Wechsel gesund→Fehler, 1× Info bei der Erholung. Das Overlay
     * wird in jedem Frame neu versucht.
     */
    private static volatile boolean failed = false;
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

    /**
     * Früher ein dauerhafter Kill-Switch; heute heilt sich die Bridge selbst,
     * daher nie dauerhaft „disabled". Für API-Kompatibilität beibehalten.
     */
    public static boolean isDisabled() {
        return false;
    }

    /** Ist dieser Screen die Xaero-Worldmap? (Klassennamen-Check, kein Klassenladen.) */
    public static boolean isWorldmapScreen(Screen screen) {
        return screen != null && GUI_MAP.equals(screen.getClass().getName());
    }

    /**
     * Liest die Sichtparameter der geöffneten Worldmap.
     * {@code null} bei Lesefehler — der nächste Frame versucht es erneut.
     */
    public static View view(Screen screen) {
        if (screen == null) {
            return null;
        }
        try {
            if (!resolved || guiMapClass != screen.getClass()) {
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
            if (failed) {
                failed = false;
                OttoExtra.LOGGER.info("[map] Xaero-Bridge wieder aktiv.");
            }
            return new View(cameraX, cameraZ, effScale, screen.width, screen.height);
        } catch (Throwable t) {
            invalidate(t);
            return null;
        }
    }

    /**
     * Zentriert die Worldmap-Kamera auf eine Weltposition (Lehen-Fokus).
     * Fehler invalidieren den Cache (Selbstheilung), kein Crash.
     */
    public static void setCamera(Screen screen, double worldX, double worldZ) {
        if (!resolved || screen == null || guiMapClass != screen.getClass()) {
            return;
        }
        try {
            cameraXField.setDouble(screen, worldX);
            cameraZField.setDouble(screen, worldZ);
        } catch (Throwable t) {
            invalidate(t);
        }
    }

    /**
     * Lazy Reflection-Auflösung — NIE im Mod-Init aufrufen (GLFW-Lehre).
     * Löst auch neu auf, wenn sich die GuiMap-Klasse geändert hat (Reconnect,
     * Xaero-Reload), statt veraltete Field-Handles weiterzubenutzen.
     */
    private static synchronized void resolve(Screen screen) throws ReflectiveOperationException {
        Class<?> cls = screen.getClass();
        if (resolved && cls == guiMapClass) {
            return;
        }
        guiMapClass = cls;
        cameraXField = cls.getDeclaredField("cameraX");
        cameraXField.setAccessible(true);
        cameraZField = cls.getDeclaredField("cameraZ");
        cameraZField.setAccessible(true);
        scaleField = cls.getDeclaredField("scale");
        scaleField.setAccessible(true);
        screenScaleField = cls.getDeclaredField("screenScale");
        screenScaleField.setAccessible(true);
        try {
            coordScaleMethod = cls.getDeclaredMethod("getCurrentMapCoordinateScale");
            coordScaleMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            coordScaleMethod = null; // optional
        }
        resolved = true;
        OttoExtra.LOGGER.info("[map] Xaero-Bridge aktiv (GuiMap-Felder aufgeloest).");
    }

    /**
     * Cache verwerfen, damit der nächste Frame frisch auflöst. Loggt nur den
     * Übergang gesund→Fehler einmalig (kein Spam pro Frame).
     */
    private static void invalidate(Throwable t) {
        resolved = false;
        guiMapClass = null;
        cameraXField = null;
        cameraZField = null;
        scaleField = null;
        screenScaleField = null;
        coordScaleMethod = null;
        if (!failed) {
            failed = true;
            OttoExtra.LOGGER.warn("[map] Xaero-Bridge Lesefehler — Overlay pausiert, "
                    + "Selbstheilung beim nächsten Frame: {}", t.toString());
        }
    }
}
