package de.ottoextra.map;

import org.joml.Matrix4f;

final class XaeroFramebufferView {

    private static final double SCALE_TOLERANCE = 1.0e-5;

    private XaeroFramebufferView() {
    }

    /**
     * Converts Xaero's anchored, Y-inverted framebuffer pose in GUI units to a
     * positive-scale view. The renderer must mirror Y around the viewport center.
     */
    static XaeroMapBridge.View fromMatrix(Matrix4f matrix, double anchorX, double anchorZ,
                                         int width, int height) {
        if (matrix == null || !matrix.isFinite() || !Double.isFinite(anchorX)
                || !Double.isFinite(anchorZ) || width <= 0 || height <= 0) {
            return null;
        }
        double scaleX = matrix.m00();
        double scaleY = matrix.m11();
        if (!(scaleX > 0) || !(scaleY < 0)
                || Math.abs(scaleX + scaleY) > Math.max(scaleX, -scaleY) * SCALE_TOLERANCE
                || matrix.m01() != 0 || matrix.m02() != 0 || matrix.m03() != 0
                || matrix.m10() != 0 || matrix.m12() != 0 || matrix.m13() != 0
                || matrix.m20() != 0 || matrix.m21() != 0 || matrix.m23() != 0
                || matrix.m22() == 0 || matrix.m33() != 1) {
            return null;
        }
        double cameraX = anchorX + (width / 2.0 - matrix.m30()) / scaleX;
        double cameraZ = anchorZ + (height / 2.0 - matrix.m31()) / scaleY;
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraZ)) {
            return null;
        }
        return new XaeroMapBridge.View(cameraX, cameraZ, scaleX, width, height);
    }
}
