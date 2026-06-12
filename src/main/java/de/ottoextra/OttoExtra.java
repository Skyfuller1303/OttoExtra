package de.ottoextra;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zentrale Konstanten und Helfer für OttoExtra.
 *
 * <p>Liegt bewusst im {@code main}-SourceSet, enthält nur gemeinsame, client-
 * neutrale Werte (ID, Logger, Identifier-Helfer). Keine {@code net.minecraft.client.*}
 * Imports hier, damit ein Dedicated-Server diese Klasse problemlos laden könnte.</p>
 */
public final class OttoExtra {

    public static final String MOD_ID = "ottoextra";
    public static final String MOD_NAME = "OttoExtra";

    /** Substring, über den Ottonien-Server (client-seitig) erkannt werden. */
    public static final String OTTONIEN_HOST_MARKER = "ottonien";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private OttoExtra() {
    }

    /** Erzeugt eine namespaced {@link Identifier} im OttoExtra-Namespace. */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
