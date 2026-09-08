package de.ottoextra.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapClickGestureTest {

    @Test
    void unhandledLeftClickIsAcceptedOnce() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, false);

        assertTrue(gesture.release(0, 20, 30, false));
        assertFalse(gesture.release(0, 20, 30, false));
    }

    @Test
    void releaseWithoutPressIsIgnored() {
        MapClickGesture gesture = new MapClickGesture();

        assertFalse(gesture.release(0, 20, 30, false));
    }

    @Test
    void movementAtFourPixelBoundaryIsAccepted() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, false);

        assertTrue(gesture.release(0, 24, 30, false));
    }

    @Test
    void dragBeyondFourPixelsIsIgnoredAndClearsPress() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, false);

        assertFalse(gesture.release(0, 23, 33, false));
        assertFalse(gesture.release(0, 20, 30, false));
    }

    @Test
    void consumedPressIsIgnoredEvenIfReleaseIsUnhandled() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, true);

        assertFalse(gesture.release(0, 20, 30, false));
    }

    @Test
    void consumedPressClearsPreviousUnhandledPress() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, false);
        gesture.press(0, 20, 30, true);

        assertFalse(gesture.release(0, 20, 30, false));
    }

    @Test
    void consumedReleaseIsIgnoredAndClearsPress() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, false);

        assertFalse(gesture.release(0, 20, 30, true));
        assertFalse(gesture.release(0, 20, 30, false));
    }

    @Test
    void newUnhandledClickWorksAfterConsumedGesture() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, true);
        assertFalse(gesture.release(0, 20, 30, true));
        gesture.press(0, 40, 50, false);

        assertTrue(gesture.release(0, 40, 50, false));
    }

    @Test
    void otherButtonsDoNotStartLeftClickGesture() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(1, 20, 30, false);

        assertFalse(gesture.release(1, 20, 30, false));
        assertFalse(gesture.release(0, 20, 30, false));
    }

    @Test
    void otherButtonsDoNotChangePendingLeftClickGesture() {
        MapClickGesture gesture = new MapClickGesture();

        gesture.press(0, 20, 30, false);
        gesture.press(1, 100, 200, true);

        assertFalse(gesture.release(1, 100, 200, true));
        assertTrue(gesture.release(0, 20, 30, false));
    }
}
