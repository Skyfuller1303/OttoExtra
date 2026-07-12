package de.ottoextra;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OttoExtra {

    public static final String MOD_ID = "ottoextra";
    public static final String MOD_NAME = "OttoExtra";

    public static final String OTTONIEN_HOST_MARKER = "ottonien";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private OttoExtra() {
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
