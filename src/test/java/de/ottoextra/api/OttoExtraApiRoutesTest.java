package de.ottoextra.api;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class OttoExtraApiRoutesTest {
    @Test
    void usesNewApiDomainByDefault() {
        OttoExtraApiRoutes routes = new OttoExtraApiRoutes(null);
        assertEquals("https://api.ottoextra.dev", routes.baseUrl());
        assertEquals("https://api.ottoextra.dev/v2/bootstrap", routes.v2Bootstrap().toString());
    }
    @Test
    void migratesLegacyBaseAndAbsoluteResourceUrls() {
        OttoExtraApiRoutes routes = new OttoExtraApiRoutes("http://regions.skyfuller.de/");
        assertEquals("https://api.ottoextra.dev", routes.baseUrl());
        assertEquals("https://api.ottoextra.dev/assets/banner.png",
                routes.resolveRelative("https://regions.skyfuller.de/assets/banner.png").toString());
    }
}
