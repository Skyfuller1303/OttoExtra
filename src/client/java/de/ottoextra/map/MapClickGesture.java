package de.ottoextra.map;

final class MapClickGesture {

    private boolean pressed;
    private double pressX;
    private double pressY;

    void press(int button, double x, double y, boolean handled) {
        if (button != 0) {
            return;
        }
        pressed = !handled;
        pressX = x;
        pressY = y;
    }

    boolean release(int button, double x, double y, boolean handled) {
        if (button != 0) {
            return false;
        }
        boolean wasPressed = pressed;
        pressed = false;
        if (!wasPressed || handled) {
            return false;
        }
        double dx = x - pressX;
        double dy = y - pressY;
        return dx * dx + dy * dy <= 16.0;
    }
}
