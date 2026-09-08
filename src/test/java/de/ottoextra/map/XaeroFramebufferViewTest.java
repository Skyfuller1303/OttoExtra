package de.ottoextra.map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class XaeroFramebufferViewTest {

    @Test
    void recoversCameraFromAnchoredInvertedPose() {
        Matrix4f matrix = new Matrix4f().translate(400, 300, 0)
                .scale(2, -2, 1).translate(-0.25f, 0.75f, 0);

        XaeroMapBridge.View view = XaeroFramebufferView.fromMatrix(matrix, -120, 37, 800, 600);

        assertNotNull(view);
        assertEquals(-119.75, view.cameraX());
        assertEquals(36.25, view.cameraZ());
        assertEquals(2, view.effScale());
        assertEquals(800, view.width());
        assertEquals(600, view.height());
    }

    @Test
    void mirroredViewMatchesPoseAcrossAnchorsZoomsAndViewportSizes() {
        double[][] anchors = {{0, 0}, {-1842.75, 921.125}, {32768.5, -16384.25}};
        int[][] viewports = {{800, 600}, {1279, 719}, {320, 240}};
        float[] zooms = {0.0625f, 0.7f, 1, 4.5f, 32};
        float[][] offsets = {{0, 0}, {-31.25f, 67.5f}, {201.125f, -82.75f}};
        for (double[] anchor : anchors) {
            for (int[] viewport : viewports) {
                for (float zoom : zooms) {
                    Matrix4f matrix = new Matrix4f()
                            .translate(viewport[0] / 2f, viewport[1] / 2f, 0)
                            .scale(zoom, -zoom, 1).translate(-0.375f, 0.625f, 0);
                    XaeroMapBridge.View view = XaeroFramebufferView.fromMatrix(
                            matrix, anchor[0], anchor[1], viewport[0], viewport[1]);

                    assertNotNull(view);
                    for (float[] offset : offsets) {
                        Vector3f expected = matrix.transformPosition(offset[0], offset[1], 0,
                                new Vector3f());
                        assertEquals(expected.x, view.screenX(anchor[0] + offset[0]), 0.001);
                        assertEquals(expected.y, viewport[1] - view.screenY(anchor[1] + offset[1]),
                                0.001);
                    }
                }
            }
        }
    }

    @Test
    void allowsSmallFloatingPointScaleDifference() {
        Matrix4f matrix = new Matrix4f().scale(2, -2.000002f, 1);

        assertNotNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, 800, 600));
    }

    @Test
    void rejectsMissingPoseInvalidDimensionsAndNonfiniteAnchors() {
        Matrix4f matrix = new Matrix4f().scale(2, -2, 1);

        assertNull(XaeroFramebufferView.fromMatrix(null, 0, 0, 800, 600));
        assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, 0, 600));
        assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, 800, 0));
        assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, -1, 600));
        assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, 800, -1));
        assertNull(XaeroFramebufferView.fromMatrix(matrix, Double.NaN, 0, 800, 600));
        assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, Double.POSITIVE_INFINITY, 800, 600));
        assertNull(XaeroFramebufferView.fromMatrix(matrix, Double.NEGATIVE_INFINITY, 0, 800, 600));
    }

    @Test
    void rejectsNonfinitePoseComponents() {
        for (Matrix4f matrix : List.of(
                new Matrix4f().scale(2, -2, 1).m00(Float.NaN),
                new Matrix4f().scale(2, -2, 1).m11(Float.NEGATIVE_INFINITY),
                new Matrix4f().scale(2, -2, 1).m30(Float.POSITIVE_INFINITY),
                new Matrix4f().scale(2, -2, 1).m31(Float.NaN),
                new Matrix4f().scale(2, -2, 1).m32(Float.NaN))) {
            assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, 800, 600));
        }
    }

    @Test
    void rejectsWrongOrientationSingularOrUnequalScales() {
        for (Matrix4f matrix : List.of(
                new Matrix4f(),
                new Matrix4f().scale(-2, -2, 1),
                new Matrix4f().scale(0, -2, 1),
                new Matrix4f().scale(2, 0, 1),
                new Matrix4f().scale(2, -2, 0),
                new Matrix4f().scale(2, -3, 1))) {
            assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, 800, 600));
        }
    }

    @Test
    void rejectsRotationShearAndPerspective() {
        for (Matrix4f matrix : List.of(
                new Matrix4f().scale(2, -2, 1).rotateZ(0.1f),
                new Matrix4f().scale(2, -2, 1).m10(0.01f),
                new Matrix4f().scale(2, -2, 1).m01(0.01f),
                new Matrix4f().scale(2, -2, 1).m20(0.01f),
                new Matrix4f().scale(2, -2, 1).m03(0.01f),
                new Matrix4f().scale(2, -2, 1).m13(0.01f),
                new Matrix4f().scale(2, -2, 1).m23(0.01f),
                new Matrix4f().scale(2, -2, 1).m33(2))) {
            assertNull(XaeroFramebufferView.fromMatrix(matrix, 0, 0, 800, 600));
        }
    }
}
