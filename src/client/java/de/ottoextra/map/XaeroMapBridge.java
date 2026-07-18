package de.ottoextra.map;

import de.ottoextra.OttoExtra;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class XaeroMapBridge {

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

    private static volatile boolean failed = false;
    private static volatile boolean resolved = false;
    private static Class<?> guiMapClass;
    private static Field cameraXField;
    private static Field cameraZField;
    private static Field scaleField;
    private static Field screenScaleField;
    private static Method coordScaleMethod;

    private XaeroMapBridge() {
    }

    public static boolean isWorldmapInstalled() {
        return FabricLoader.getInstance().isModLoaded("xaeroworldmap");
    }

    public static boolean isDisabled() {
        return false;
    }

    public static boolean isWorldmapScreen(Screen screen) {
        return screen != null && GUI_MAP.equals(screen.getClass().getName());
    }

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
            coordScaleMethod = null;
        }
        resolved = true;
        OttoExtra.LOGGER.info("[map] Xaero-Bridge aktiv (GuiMap-Felder aufgeloest).");
    }

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
