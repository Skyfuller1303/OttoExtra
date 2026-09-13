package de.ottoextra.rpnames;

import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpNamesServicesTest {

    @Test
    void disconnectDiscardsOldMeetSuggestionsAndMarkers() throws Exception {
        var saved = new java.util.LinkedHashMap<java.lang.reflect.Field, Object>();
        for (String name : java.util.List.of("store", "active", "cachedLocalAccount", "cachedLocalProfile",
                "tablistIdentityCache", "tablistIdentityCacheAt")) {
            var field = RpNamesServices.class.getDeclaredField(name);
            field.setAccessible(true);
            saved.put(field, field.get(null));
        }
        var constructor = de.ottoextra.rpnames.store.LocalRpIdentityStore.class.getDeclaredConstructor(boolean.class);
        constructor.setAccessible(true);
        var store = constructor.newInstance(false);
        var storeField = RpNamesServices.class.getDeclaredField("store");
        storeField.setAccessible(true);
        var suggestionsField = RpNamesServices.class.getDeclaredField("suggestions");
        suggestionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var suggestions = (java.util.Map<String, RpNamesServices.MeetSuggestion>) suggestionsField.get(null);
        var pendingField = RpNamesServices.class.getDeclaredField("pendingMeet");
        pendingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var pending = (java.util.Set<String>) pendingField.get(null);
        var previousSuggestions = java.util.Map.copyOf(suggestions);
        var previousPending = java.util.Set.copyOf(pending);
        try {
            storeField.set(null, store);
            RpNamesServices.suggestIdentity("SessionPlayer", "Alter Name", "Graf");
            RpNamesServices.markHeardInRpChat("SessionPlayer");
            assertTrue(RpNamesServices.isPendingMeet("SessionPlayer"));
            RpNamesServices.setActive(false);
            org.junit.jupiter.api.Assertions.assertNull(RpNamesServices.meetSuggestion("SessionPlayer"));
            org.junit.jupiter.api.Assertions.assertFalse(RpNamesServices.isPendingMeet("SessionPlayer"));
        } finally {
            for (var entry : saved.entrySet()) entry.getKey().set(null, entry.getValue());
            suggestions.clear();
            suggestions.putAll(previousSuggestions);
            pending.clear();
            pending.addAll(previousPending);
            store.shutdown();
        }
    }

    @Test
    void knownOwnProfileWinsOverNewUnknownProxyUuidProfile() {
        LocalRpProfile known = new LocalRpProfile();
        known.accountName = "Skyfuller1303";
        known.uuid = "191a9a94-2ee0-4636-a57f-fa575f2d6adc";
        known.rpName = "Matthias";
        known.locked = true;
        LocalRpProfile proxy = new LocalRpProfile();
        proxy.accountName = "Skyfuller1303";
        proxy.uuid = "030f72f2-19be-4fc9-afe2-ccfb015c8b52";

        assertSame(known, RpNamesServices.preferKnownLocalProfile(
                "Skyfuller1303", proxy, java.util.List.of(known, proxy)));
    }

    @Test
    void knownProfileWinsOnlyWhenBothCanonicalAndProxyAreActive() {
        LocalRpProfile known = new LocalRpProfile();
        known.accountName = "countdacson";
        known.uuid = "0cc97e4d-bee7-4f3a-ba34-690607a54a4a";
        known.rpName = "Wilhelm Dacson";
        LocalRpProfile proxy = new LocalRpProfile();
        proxy.accountName = "countdacson";
        proxy.uuid = "cdad3a02-77aa-467b-856e-9ce565f44e09";

        assertSame(known, RpNamesServices.preferKnownActiveAlias(
                proxy.accountName, proxy.uuid, proxy, java.util.List.of(known, proxy),
                java.util.List.of(known.uuid, proxy.uuid)));
        assertSame(proxy, RpNamesServices.preferKnownActiveAlias(
                proxy.accountName, proxy.uuid, proxy, java.util.List.of(known, proxy),
                java.util.List.of(proxy.uuid)));
    }

    @Test
    void leaderToastAcceptsLocallyStoredApiImportedRpName() {
        LocalRpProfile profile = new LocalRpProfile();
        profile.rpName = "  Mönch Matthias  ";
        profile.knowledgeState = KnowledgeState.API_IMPORTED;
        profile.source = RpNameSource.API_IMPORTED;

        assertEquals("Mönch Matthias",
                RpNamesServices.localRpNameForLeader(profile).orElseThrow());
    }

    @Test
    void leaderToastRejectsMissingOrUnknownRpName() {
        assertTrue(RpNamesServices.localRpNameForLeader(null).isEmpty());

        LocalRpProfile profile = new LocalRpProfile();
        profile.rpName = "Unbekannt";
        assertTrue(RpNamesServices.localRpNameForLeader(profile).isEmpty());

        profile.rpName = "  ";
        assertTrue(RpNamesServices.localRpNameForLeader(profile).isEmpty());
    }
}
