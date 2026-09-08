package de.ottoextra.map.xaero;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class XaeroPlusPersonalDrawingsTest {
    private final Provider provider = new Provider();
    private final MatrixStack matrices = new MatrixStack();
    private final Manager manager = new Manager();
    private final List<String> rendered = new ArrayList<>();

    @BeforeEach
    void resetHud() {
        Hud.INSTANCE = new Hud();
        Hud.INSTANCE.fairPlay = true;
    }

    @Test
    void allowlistIsExactAndExcludesAllOtherFeatures() {
        for (String id : List.of("Drawing-lines-saved", "Drawing-lines-in-progress", "Drawing-text")) {
            assertTrue(XaeroPlusPersonalDrawings.isPersonalDrawing(id), id);
        }
        for (String id : List.of("", "Drawing-highlights", "Drawing-lines", "drawing-text", "Drawing-text-extra",
                " Drawing-text", "Drawing-lines-saved ", "Radar", "NewChunks", "PortalSkip", "FutureFeature")) {
            assertFalse(XaeroPlusPersonalDrawings.isPersonalDrawing(id), id);
        }
        assertFalse(XaeroPlusPersonalDrawings.isPersonalDrawing(null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nonFairPlayLeavesOriginalRendererAlone(boolean worldMap) throws Exception {
        Hud.INSTANCE.fairPlay = false;
        manager.current.features.add(feature("Drawing-text"));
        assertEquals(0, api().render(manager, 10, 20, matrices, 2, provider, worldMap));
        assertEquals(0, manager.registryReads);
        assertTrue(rendered.isEmpty());
        assertFalse(Hud.INSTANCE.fairPlay);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fairPlayRendersOnlyPersonalFeaturesInRegistryOrderAndKeepsFlag(boolean worldMap) throws Exception {
        manager.current.features.addAll(List.of(feature("Drawing-text"), feature("Radar"),
                feature("Drawing-lines-in-progress"), feature("Drawing-highlights"),
                feature("Drawing-lines-saved"), feature("NewChunks")));
        assertEquals(3, api().render(manager, 10, 20, matrices, 2, provider, worldMap));
        assertEquals(List.of("Drawing-text", "Drawing-lines-in-progress", "Drawing-lines-saved"), rendered);
        assertTrue(Hud.INSTANCE.fairPlay);
    }

    @Test
    void contextUsesOriginalUntranslatedMatrixAndRestoresStack() throws Exception {
        matrices.translate(100, 200, 0);
        matrices.scale(2, -2, 1);
        Matrix4f original = new Matrix4f(matrices.peek().getPositionMatrix());
        manager.current.features.add(new Feature() {
            public String id() { return "Drawing-text"; }
            public void render(Context context) {
                assertSame(matrices, context.matrices);
                assertSame(provider, context.provider);
                assertEquals(1.5, context.scale);
                assertTrue(context.worldMap);
                assertEquals(10, context.cameraX);
                assertEquals(-20, context.cameraZ);
                assertEquals(new Matrix4f(original).translate(0, 0, 1), context.untranslated);
                assertEquals(new Matrix4f(original).translate(-10, 20, 1), matrices.peek().getPositionMatrix());
            }
        });
        assertEquals(1, api().render(manager, 10, -20, matrices, 1.5, provider, true));
        assertEquals(original, matrices.peek().getPositionMatrix());
        assertTrue(matrices.isEmpty());
    }

    @Test
    void minimapKeepsRotatedViewZoomAndCameraWithoutWorldMapDepthOffset() throws Exception {
        matrices.translate(128, 128, 5);
        matrices.scale(1.5f, 1.5f, 1);
        matrices.peek().getPositionMatrix().rotateZ(0.8f);
        Matrix4f original = new Matrix4f(matrices.peek().getPositionMatrix());
        manager.current.features.add(new Feature() {
            public String id() { return "Drawing-lines-saved"; }
            public void render(Context context) {
                assertSame(matrices, context.matrices);
                assertSame(provider, context.provider);
                assertEquals(1.5, context.scale);
                assertFalse(context.worldMap);
                assertEquals(-4097, context.cameraX);
                assertEquals(2050, context.cameraZ);
                assertEquals(original, context.untranslated);
                assertEquals(new Matrix4f(original).translate(4097, -2050, 0),
                        matrices.peek().getPositionMatrix());
            }
        });
        assertEquals(1, api().render(manager, -4097, 2050, matrices, 1.5, provider, false));
        assertEquals(original, matrices.peek().getPositionMatrix());
        assertTrue(matrices.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void featureFailureRestoresStackAndNeverEnablesUnlistedFeatures(boolean worldMap) throws Exception {
        Matrix4f original = new Matrix4f(matrices.peek().getPositionMatrix());
        manager.current.features.add(new Feature() {
            public String id() { return "Drawing-lines-saved"; }
            public void render(Context context) { throw new IllegalStateException("test failure"); }
        });
        manager.current.features.add(feature("Radar"));
        assertThrows(InvocationTargetException.class, () -> api().render(manager, 10, 20, matrices, 2, provider, worldMap));
        assertEquals(original, matrices.peek().getPositionMatrix());
        assertTrue(matrices.isEmpty());
        assertTrue(rendered.isEmpty());
        assertTrue(Hud.INSTANCE.fairPlay);
    }

    @Test
    void transientHudAndRegistryAreNotCached() throws Exception {
        XaeroPlusPersonalDrawings.Api api = api();
        Hud.INSTANCE = null;
        assertEquals(0, api.render(manager, 0, 0, matrices, 1, provider, true));
        Hud.INSTANCE = new Hud();
        Hud.INSTANCE.fairPlay = true;
        manager.current = null;
        assertEquals(0, api.render(manager, 0, 0, matrices, 1, provider, true));
        manager.current = new Registry();
        manager.current.features.add(feature("Drawing-lines-saved"));
        assertEquals(1, api.render(manager, 0, 0, matrices, 1, provider, true));
        manager.current = new Registry();
        manager.current.features.add(feature("Drawing-text"));
        assertEquals(1, api.render(manager, 0, 0, matrices, 1, provider, true));
        assertEquals(List.of("Drawing-lines-saved", "Drawing-text"), rendered);
    }

    @Test
    void emptyRegistryAndNullFeaturesAreHarmless() throws Exception {
        assertEquals(0, api().render(manager, 0, 0, matrices, 1, provider, true));
        manager.current.features.add(null);
        assertEquals(0, api().render(manager, 0, 0, matrices, 1, provider, true));
        assertTrue(matrices.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void olderFourArgumentContextRemainsSupported(boolean worldMap) throws Exception {
        XaeroPlusPersonalDrawings.Api api = XaeroPlusPersonalDrawings.Api.resolve(
                LegacyManager.class, Hud.class, LegacyContext.class, LegacyFeature.class, Provider.class);
        LegacyManager legacy = new LegacyManager();
        legacy.current.feature = new LegacyFeature() {
            public String id() { return "Drawing-lines-in-progress"; }
            public void render(LegacyContext context) {
                assertSame(matrices, context.matrices);
                assertSame(provider, context.provider);
                assertEquals(worldMap ? 2 : 1, context.scale);
                assertEquals(worldMap, context.worldMap);
                assertEquals(new Matrix4f().translate(-10, -20, worldMap ? 1 : 0), matrices.peek().getPositionMatrix());
            }
        };
        assertEquals(1, api.render(legacy, 10, 20, matrices, worldMap ? 2 : 1, provider, worldMap));
        assertTrue(matrices.isEmpty());
        assertTrue(Hud.INSTANCE.fairPlay);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingOptionalApiFailsClosedAndNullInputsAreHarmless(boolean worldMap) {
        manager.current.features.add(feature("Drawing-text"));
        assertDoesNotThrow(() -> XaeroPlusPersonalDrawings.renderPersonalDrawings(manager, 0, 0, matrices, 1, provider, worldMap));
        assertDoesNotThrow(() -> XaeroPlusPersonalDrawings.renderPersonalDrawings(manager, 0, 0, matrices, 1, provider, worldMap));
        assertDoesNotThrow(() -> XaeroPlusPersonalDrawings.renderPersonalDrawings(null, 0, 0, matrices, 1, provider, worldMap));
        assertDoesNotThrow(() -> XaeroPlusPersonalDrawings.renderPersonalDrawings(manager, 0, 0, null, 1, provider, worldMap));
        assertDoesNotThrow(() -> XaeroPlusPersonalDrawings.renderPersonalDrawings(manager, 0, 0, matrices, 1, null, worldMap));
        assertTrue(rendered.isEmpty());
        assertTrue(Hud.INSTANCE.fairPlay);
    }

    @Test
    void unsupportedContextSignatureCannotFallThroughToRendering() {
        assertThrows(NoSuchMethodException.class, () -> XaeroPlusPersonalDrawings.Api.resolve(
                Manager.class, Hud.class, Object.class, Feature.class, Provider.class));
        assertTrue(rendered.isEmpty());
    }

    private XaeroPlusPersonalDrawings.Api api() throws ReflectiveOperationException {
        return XaeroPlusPersonalDrawings.Api.resolve(Manager.class, Hud.class, Context.class, Feature.class, Provider.class);
    }

    private Feature feature(String id) {
        return new Feature() {
            public String id() { return id; }
            public void render(Context context) { rendered.add(id); }
        };
    }

    public static final class Hud {
        public static Hud INSTANCE;
        boolean fairPlay;
        public boolean isFairPlay() { return fairPlay; }
    }

    public static final class Provider {
    }

    public record Context(MatrixStack matrices, Provider provider, double scale, boolean worldMap,
                          Matrix4f untranslated, int cameraX, int cameraZ) {
    }

    public interface Feature {
        String id();
        void render(Context context);
    }

    public static final class Registry {
        final List<Feature> features = new ArrayList<>();
        public void forEach(Consumer<Feature> consumer) { features.forEach(consumer); }
    }

    public static final class Manager {
        Registry current = new Registry();
        int registryReads;
        public Registry registry() { registryReads++; return current; }
    }

    public record LegacyContext(MatrixStack matrices, Provider provider, double scale, boolean worldMap) {
    }

    public interface LegacyFeature {
        String id();
        void render(LegacyContext context);
    }

    public static final class LegacyRegistry {
        LegacyFeature feature;
        public void forEach(Consumer<LegacyFeature> consumer) { consumer.accept(feature); }
    }

    public static final class LegacyManager {
        final LegacyRegistry current = new LegacyRegistry();
        public LegacyRegistry registry() { return current; }
    }
}
