package de.ottoextra.tacticalfactions;

import java.io.InputStream;
import java.util.Properties;

public final class BuildVariant {
    private static final String RESOURCE = "/ottoextra_tacticalfactions.variant.properties";
    private static final Properties VALUES = load();

    private BuildVariant() { }

    public static boolean friendlyFireProtectionAvailable() {
        return Boolean.parseBoolean(VALUES.getProperty("friendlyFireProtection", "true"));
    }

    public static String name() {
        return VALUES.getProperty("buildVariant", "FriendlyFire-Schutz");
    }

    private static Properties load() {
        Properties values = new Properties();
        try (InputStream stream = BuildVariant.class.getResourceAsStream(RESOURCE)) {
            if (stream != null) values.load(stream);
        } catch (Exception ignored) {
        }
        return values;
    }
}
