package de.ottoextra.regions.zonebanner;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Registry of the procedural zone banner styles. "classic" means the old toast. */
public final class ZoneBannerStyles {

    public static final String CLASSIC = "classic";

    private static final Map<String, ZoneBannerStyle> STYLES = new LinkedHashMap<>();

    static {
        register(new ZonentitelStyle());
        register(new BanderoleStyle());
        register(new PlaketteStyle());
        register(new MinimalStyle());
    }

    private ZoneBannerStyles() {
    }

    private static void register(ZoneBannerStyle style) {
        STYLES.put(style.id(), style);
    }

    /** Style for a config value, or null for "classic"/unknown values. */
    public static ZoneBannerStyle byId(String id) {
        if (id == null) return null;
        return STYLES.get(id.trim().toLowerCase(Locale.ROOT));
    }

    /** Config values in cycle order, starting with "classic". */
    public static String[] ids() {
        String[] out = new String[STYLES.size() + 1];
        out[0] = CLASSIC;
        int i = 1;
        for (String id : STYLES.keySet()) out[i++] = id;
        return out;
    }
}
