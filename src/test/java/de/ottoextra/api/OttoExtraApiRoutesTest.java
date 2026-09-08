package de.ottoextra.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OttoExtraApiRoutesTest {

    @Test
    void usesNewApiDomainByDefault() {
        OttoExtraApiRoutes routes = new OttoExtraApiRoutes(null);

        assertEquals("https://api.ottoextra.dev", routes.baseUrl());
        assertEquals("https://api.ottoextra.dev/v2/bootstrap", routes.v2Bootstrap().toString());
        assertEquals("https://api.ottoextra.dev/v2/chat-translations",
                routes.v2ProtectedChatMessages().toString());
        assertEquals("https://api.ottoextra.dev/v2/chat-translations/abc_123-X",
                routes.v2ProtectedChatMessage("abc_123-X").toString());
        assertEquals("https://api.ottoextra.dev/v2/chat-translations/inbox",
                routes.v2ProtectedChatInbox().toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/horses",
                routes.v2AnimalBreedingHorses().toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/breeder-access",
                routes.v2AnimalBreedingBreederAccess().toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/horse-names",
                routes.v2AnimalBreedingHorseNames().toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/horses/abc-123",
                routes.v2AnimalBreedingHorse("abc-123").toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/horses/abc-123/name",
                routes.v2AnimalBreedingHorseName("abc-123").toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/horses/abc-123/observation",
                routes.v2AnimalBreedingHorseObservation("abc-123").toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/horses/abc-123/editors",
                routes.v2AnimalBreedingHorseEditors("abc-123").toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/farms",
                routes.v2AnimalBreedingFarms().toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/breeding-events",
                routes.v2AnimalBreedingEvents().toString());
        assertEquals("https://api.ottoextra.dev/v2/animal-breeding/breeding-events/event-123/foal",
                routes.v2AnimalBreedingEventFoal("event-123").toString());
    }

    @Test
    void migratesLegacyBaseAndAbsoluteResourceUrls() {
        OttoExtraApiRoutes routes = new OttoExtraApiRoutes("http://regions.skyfuller.de/");

        assertEquals("https://api.ottoextra.dev", routes.baseUrl());
        assertEquals("https://api.ottoextra.dev/assets/banner.png",
                routes.resolveRelative("https://regions.skyfuller.de/assets/banner.png").toString());
    }
}
