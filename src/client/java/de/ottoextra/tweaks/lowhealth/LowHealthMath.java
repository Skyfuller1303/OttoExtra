package de.ottoextra.tweaks.lowhealth;

/** Kleine Mathe-Helfer für den Low-Health-Effekt. */
public final class LowHealthMath {

    private LowHealthMath() {
    }

    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Lineare Interpolation von a→b über t (t wird auf 0..1 geklemmt). */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0.0f, 1.0f);
    }

    /** Weiche S-Kurve (smoothstep) für natürlicheres Anschwellen. */
    public static float smoothstep(float t) {
        t = clamp(t, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
