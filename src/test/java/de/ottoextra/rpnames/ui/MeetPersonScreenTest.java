package de.ottoextra.rpnames.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MeetPersonScreenTest {
    private static final String ACCOUNT = "UnknownPlayer";
    private static final String CANONICAL = "00000000-0000-4000-8000-000000000001";
    private static final String PROXY = "00000000-0000-4000-8000-000000000002";
    private final Map<Field, Object> saved = new LinkedHashMap<>();
    private LocalRpIdentityStore store;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = LocalRpIdentityStore.class.getDeclaredConstructor(boolean.class);
        constructor.setAccessible(true);
        store = constructor.newInstance(false);
        var config = new OttoExtraConfig().rpnames;
        config.enabled = true;
        config.proactiveMeet = true;
        replace("store", store);
        replace("config", config);
        replace("active", true);
        replace("catalog", null);
        replace("tablistIdentityCache", Map.of("unknownplayer", List.of(CANONICAL, PROXY)));
        replace("tablistIdentityCacheAt", System.nanoTime() + 60_000_000_000L);
        var known = store.ensureSeen(ACCOUNT, CANONICAL, RpNameSource.API_IMPORTED);
        known.rpName = "Wilhelm";
        known.title = "Graf";
        store.ensureSeen(ACCOUNT, PROXY, RpNameSource.SEEN_ONLINE);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (var entry : saved.entrySet()) entry.getKey().set(null, entry.getValue());
        if (store != null) store.shutdown();
    }

    @Test
    void activeProxyUsesImportedNameAndTitleWithoutMarkingPersonKnown() throws Exception {
        var prefill = MeetPersonScreen.localPrefill(ACCOUNT, PROXY);

        assertEquals("Wilhelm", prefill.name());
        assertEquals("Graf", prefill.title());
        assertFalse(RpNamesServices.isKnownForDisplay(store.find(CANONICAL, ACCOUNT).orElseThrow()));
        assertFalse(store.findByName(ACCOUNT).orElseThrow().hasRpName());
    }

    @Test
    void explicitUuidWinsOverDifferentCurrentNameOwner() throws Exception {
        replace("tablistIdentityCache", Map.of());
        var prefill = MeetPersonScreen.localPrefill(ACCOUNT, CANONICAL);

        assertEquals("Wilhelm", prefill.name());
        assertEquals("Graf", prefill.title());
    }

    @Test
    void inactiveHistoricalIdentityDoesNotFillUnknownCurrentPerson() throws Exception {
        replace("tablistIdentityCache", Map.of());
        var prefill = MeetPersonScreen.localPrefill(ACCOUNT, PROXY);

        assertEquals("", prefill.name());
        assertEquals("", prefill.title());
    }

    @Test
    void explicitApiPrefillRemainsAuthoritative() throws Exception {
        var prefill = MeetPersonScreen.apiPrefill("Aktuell", "Ritter");

        assertEquals("Aktuell", prefill.name());
        assertEquals("Ritter", prefill.title());
        prefill = MeetPersonScreen.apiPrefill(null, null);

        assertEquals("", prefill.name());
        assertEquals("", prefill.title());
    }

    @Test
    void meetingActiveProxyUpdatesCanonicalProfileAndPreservesLock() {
        var canonical = store.find(CANONICAL, ACCOUNT).orElseThrow();
        canonical.locked = true;
        MeetPersonScreen.saveIdentity(ACCOUNT, PROXY, "Wilhelm", "Herzog");
        assertEquals("Herzog", canonical.title);
        assertTrue(canonical.locked);
        assertTrue(RpNamesServices.isKnownForDisplay(RpNamesServices.displayProfile(PROXY, ACCOUNT).orElseThrow()));
        assertFalse(store.find(PROXY, ACCOUNT).orElseThrow().hasRpName());
    }

    @Test
    void meetingExplicitUuidDoesNotWriteToSameNamedCurrentOwner() throws Exception {
        replace("tablistIdentityCache", Map.of());
        MeetPersonScreen.saveIdentity(ACCOUNT, CANONICAL, "Manuell", "Ritter");
        assertEquals("Manuell", store.find(CANONICAL, ACCOUNT).orElseThrow().rpName);
        assertFalse(store.find(PROXY, ACCOUNT).orElseThrow().hasRpName());
        assertEquals(PROXY, store.findByName(ACCOUNT).orElseThrow().uuid);
    }

    @Test
    void openingBookPreservesUuidWithoutChangingNameOwner() throws Exception {
        replace("tablistIdentityCache", Map.of());
        assertSame(store.find(CANONICAL, ACCOUNT).orElseThrow(),
                RpNamesPeopleBookScreen.resolveOpenProfile(ACCOUNT, CANONICAL));
        assertEquals(PROXY, store.findByName(ACCOUNT).orElseThrow().uuid);
    }

    @Test
    void explicitAccountUuidCannotBeMatchedToAnotherPersonsRpName() {
        var canonical = store.find(CANONICAL, ACCOUNT).orElseThrow();
        canonical.rpName = "NewPlayer";
        var opened = RpNamesPeopleBookScreen.resolveOpenProfile("NewPlayer", "new-player-uuid");
        assertEquals("NewPlayer", opened.accountName);
        assertEquals("new-player-uuid", opened.uuid);
        assertFalse(opened.hasRpName());
        assertEquals(ACCOUNT, canonical.accountName);
    }

    private void replace(String name, Object value) throws Exception {
        Field field = RpNamesServices.class.getDeclaredField(name);
        field.setAccessible(true);
        if (!saved.containsKey(field)) saved.put(field, field.get(null));
        field.set(null, value);
    }
}
