package de.ottoextra.regions.zonebanner;

import java.util.Locale;

/**
 * Runtime-tunable layout values of the {@link BanderoleStyle}. Defaults match the shipped
 * design; changes made through the debug command live in memory only and are lost on restart.
 * Use {@code /ottoextra banderole export} to copy tuned values back into the defaults.
 */
public enum BanderoleTuning {
    PAD_TOP("padTop", 4, 0, 32, "Freie Zeilen zwischen oberer Bandkante und Name"),
    PAD_BOTTOM("padBottom", 2, 0, 32, "Freie Zeilen zwischen Name und unterer Bandkante"),
    PAD_X("padX", 40, 0, 200, "Bandbreite = Namensbreite + padX"),
    MIN_WIDTH("minWidth", 60, 10, 400, "Minimale Bandbreite"),
    TAIL_WIDTH("tailWidth", 18, 0, 80, "Sichtbare Länge der Bandenden"),
    TAIL_DROP("tailDrop", 5, -20, 40, "Versatz der Bandenden nach unten"),
    TAIL_TUCK("tailTuck", 6, 0, 30, "Unter dem Band versteckter Teil der Enden"),
    TAIL_NOTCH("tailNotch", 6, 0, 30, "Tiefe der V-Kerbe"),
    FOLD("fold", 4, 0, 20, "Größe des Faltschattens"),
    ARMS_SIZE("armsSize", 24, 0, 96, "Wappengröße"),
    ARMS_OVERLAP("armsOverlap", 4, -40, 60, "Wie weit das Wappen ins Band ragt"),
    ARMS_DROP("armsDrop", 10, 0, 60, "Fallhöhe des Wappens im Intro"),
    SUBTITLE_GAP("subtitleGap", 6, -20, 60, "Abstand Band zu Untertitel"),
    SIDE_MARGIN("sideMargin", 4, 0, 100, "Mindestabstand zum Bildschirmrand"),
    OFFSET_Y("offsetY", 0, -200, 200, "Zusätzlicher vertikaler Versatz des ganzen Banners"),
    NAME_SCALE("nameScale", 1.35f, 0.5f, 4f, "Namensgröße relativ zur Textskalierung"),
    SUB_SCALE("subScale", 1f, 0.5f, 4f, "Untertitelgröße relativ zur Textskalierung");

    private final String key;
    private final float defaultValue;
    private final float min;
    private final float max;
    private final String description;
    private volatile float value;

    BanderoleTuning(String key, float defaultValue, float min, float max, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.description = description;
        this.value = defaultValue;
    }

    public String key() {
        return key;
    }

    public String description() {
        return description;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public boolean isFloat() {
        return this == NAME_SCALE || this == SUB_SCALE;
    }

    public float value() {
        return value;
    }

    public int intValue() {
        return Math.round(value);
    }

    public boolean isDefault() {
        return value == defaultValue;
    }

    /** Sets the value clamped to the allowed range; returns the stored value. */
    public float set(float v) {
        value = isFloat() ? Math.max(min, Math.min(max, v)) : Math.round(Math.max(min, Math.min(max, v)));
        return value;
    }

    public void reset() {
        value = defaultValue;
    }

    public static void resetAll() {
        for (BanderoleTuning t : values()) t.reset();
    }

    public static BanderoleTuning byKey(String key) {
        if (key == null) return null;
        for (BanderoleTuning t : values()) {
            if (t.key.equalsIgnoreCase(key.trim())) return t;
        }
        return null;
    }

    public static String[] keys() {
        BanderoleTuning[] all = values();
        String[] out = new String[all.length];
        for (int i = 0; i < all.length; i++) out[i] = all[i].key;
        return out;
    }

    /** Value formatted for chat output. */
    public String formatted() {
        return isFloat() ? String.format(Locale.ROOT, "%.2f", value) : Integer.toString(intValue());
    }
}
