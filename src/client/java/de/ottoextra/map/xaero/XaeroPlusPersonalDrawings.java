package de.ottoextra.map.xaero;

import de.ottoextra.OttoExtra;
import de.ottoextra.logging.FailureLogGate;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/** World map and minimap fallback for user-authored lines/text, preserving Xaero's FairPlay state. */
public final class XaeroPlusPersonalDrawings {

    private static final FailureLogGate FAILURE_LOG = new FailureLogGate();
    private static final ClassValue<Resolution> APIS = new ClassValue<>() {
        @Override
        protected Resolution computeValue(Class<?> managerType) {
            try {
                ClassLoader loader = managerType.getClassLoader();
                return new Resolution(Api.resolve(managerType,
                        Class.forName("xaero.common.HudMod", false, loader),
                        Class.forName("xaeroplus.feature.render.DrawContext", false, loader),
                        Class.forName("xaeroplus.feature.render.DrawFeature", false, loader),
                        Class.forName("xaero.lib.client.graphics.XaeroBufferProvider", false, loader)), null);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                return new Resolution(null, error);
            }
        }
    };
    private static boolean activationLogged;

    private XaeroPlusPersonalDrawings() {
    }

    public static void renderPersonalDrawings(Object drawManager, int cameraX, int cameraZ,
                                              MatrixStack matrices, double scale, Object bufferProvider,
                                              boolean worldMap) {
        if (drawManager == null || matrices == null || bufferProvider == null) {
            return;
        }
        Resolution resolution = APIS.get(drawManager.getClass());
        if (resolution.api == null) {
            reportFailure(resolution.error);
            return;
        }
        try {
            int rendered = resolution.api.render(drawManager, cameraX, cameraZ, matrices, scale, bufferProvider, worldMap);
            FAILURE_LOG.onSuccess();
            if (rendered > 0 && !activationLogged) {
                activationLogged = true;
                OttoExtra.LOGGER.info("[map] XaeroPlus: Render-Ausnahme fuer eigene Linien/Texte auf Weltkarte und Minimap aktiv; "
                        + "Server-Flag und andere Sperren bleiben unveraendert.");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            reportFailure(error);
        }
    }

    private static void reportFailure(Throwable error) {
        if (FAILURE_LOG.onFailure()) {
            OttoExtra.LOGGER.warn("[map] XaeroPlus-Zeichnungen: optionale FairPlay-Ausnahme nicht verfuegbar: {}",
                    error.toString());
        }
    }

    static boolean isPersonalDrawing(String id) {
        return "Drawing-lines-saved".equals(id)
                || "Drawing-lines-in-progress".equals(id)
                || "Drawing-text".equals(id);
    }

    private record Resolution(Api api, Throwable error) {
    }

    // Only reflection metadata is cached; the active HUD, registry and features are read each frame.
    static final class Api {
        private final Field hudInstance;
        private final Method isFairPlay;
        private final Method registry;
        private final Method forEach;
        private final Method featureId;
        private final Method renderFeature;
        private final Constructor<?> drawContext;

        private Api(Field hudInstance, Method isFairPlay, Method registry, Method forEach,
                    Method featureId, Method renderFeature, Constructor<?> drawContext) {
            this.hudInstance = hudInstance;
            this.isFairPlay = isFairPlay;
            this.registry = registry;
            this.forEach = forEach;
            this.featureId = featureId;
            this.renderFeature = renderFeature;
            this.drawContext = drawContext;
        }

        static Api resolve(Class<?> managerType, Class<?> hudType, Class<?> contextType,
                           Class<?> featureType, Class<?> providerType) throws ReflectiveOperationException {
            Method registry = managerType.getMethod("registry");
            Constructor<?> context;
            try {
                context = contextType.getConstructor(MatrixStack.class, providerType, double.class,
                        boolean.class, Matrix4f.class, int.class, int.class);
            } catch (NoSuchMethodException oldVersion) {
                // XaeroPlus 2.31.5 predates the camera-relative line renderer used by 2.34.5.
                context = contextType.getConstructor(MatrixStack.class, providerType, double.class, boolean.class);
            }
            return new Api(hudType.getField("INSTANCE"), hudType.getMethod("isFairPlay"), registry,
                    registry.getReturnType().getMethod("forEach", Consumer.class),
                    featureType.getMethod("id"), featureType.getMethod("render", contextType), context);
        }

        int render(Object manager, int cameraX, int cameraZ, MatrixStack matrices,
                   double scale, Object bufferProvider, boolean worldMap) throws ReflectiveOperationException {
            Object hud = hudInstance.get(null);
            if (hud == null || !Boolean.TRUE.equals(isFairPlay.invoke(hud))) {
                return 0;
            }
            Object currentRegistry = registry.invoke(manager);
            if (currentRegistry == null) {
                return 0;
            }
            Object context;
            int depth = worldMap ? 1 : 0;
            if (drawContext.getParameterCount() == 7) {
                Matrix4f untranslated = new Matrix4f(matrices.peek().getPositionMatrix()).translate(0, 0, depth);
                context = drawContext.newInstance(matrices, bufferProvider, scale, worldMap,
                        untranslated, cameraX, cameraZ);
            } else {
                context = drawContext.newInstance(matrices, bufferProvider, scale, worldMap);
            }
            int[] rendered = {0};
            Consumer<Object> renderPersonal = feature -> {
                if (feature == null) {
                    return;
                }
                try {
                    if (isPersonalDrawing((String) featureId.invoke(feature))) {
                        renderFeature.invoke(feature, context);
                        rendered[0]++;
                    }
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("XaeroPlus personal drawing render failed", error);
                }
            };
            matrices.push();
            try {
                matrices.translate(-cameraX, -cameraZ, depth);
                forEach.invoke(currentRegistry, renderPersonal);
            } finally {
                matrices.pop();
            }
            return rendered[0];
        }
    }
}
