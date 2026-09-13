package de.ottoextra.rpnames.chat;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatNameRewriterProfileTest {
    private static final String ACCOUNT = "Skyfuller1303";
    private static final String CANONICAL = "191a9a94-2ee0-4636-a57f-fa575f2d6adc";
    private static final String PROXY = "030f72f2-19be-4fc9-afe2-ccfb015c8b52";
    private final Map<Field, Object> saved = new LinkedHashMap<>();
    private LocalRpIdentityStore store;
    private OttoExtraConfig.RpNames config;
    private ChatNameRewriter rewriter;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = LocalRpIdentityStore.class.getDeclaredConstructor(boolean.class);
        constructor.setAccessible(true);
        store = constructor.newInstance(false);
        config = new OttoExtraConfig().rpnames;
        config.enabled = true;
        config.proactiveMeet = true;
        replace("store", store);
        replace("config", config);
        replace("active", true);
        replace("catalog", null);
        replace("tablistIdentityCache", Map.of(ACCOUNT.toLowerCase(java.util.Locale.ROOT),
                List.of(CANONICAL, PROXY)));
        replace("tablistIdentityCacheAt", System.nanoTime() + 60_000_000_000L);
        var known = store.ensureSeen(ACCOUNT, CANONICAL, RpNameSource.MANUAL_EDIT);
        known.rpName = "Matthias";
        store.ensureSeen(ACCOUNT, PROXY, RpNameSource.SEEN_ONLINE);
        rewriter = new ChatNameRewriter(store, new TitleRegistry());
    }

    @AfterEach
    void tearDown() throws Exception {
        for (var entry : saved.entrySet()) entry.getKey().set(null, entry.getValue());
        if (store != null) store.shutdown();
    }

    @Test
    void forcedAccountUsesKnownDisplayProfileBehindUnknownProxy() {
        assertFalse(store.findByName(ACCOUNT).orElseThrow().hasRpName());
        Text message = message(false);
        assertEquals("[Sprechen] Matthias: Hallo", rewriter.rewrite(message, config, ACCOUNT).getString());
        assertFalse(store.findByName(ACCOUNT).orElseThrow().hasRpName());
        assertEquals("[Sprechen] Skyfuller1303: Hallo", message.getString());
    }

    @Test
    void hoverAccountUsesKnownDisplayProfileBehindUnknownProxy() {
        assertEquals("[Sprechen] Matthias: Hallo", rewriter.rewrite(message(true), config).getString());
    }

    @Test
    void reusedForeignAccountDoesNotExposeHistoricalName() throws Exception {
        replace("tablistIdentityCache", Map.of());
        assertEquals("[Sprechen] Unbekannt: Hallo",
                rewriter.rewrite(message(false), config, ACCOUNT).getString());
    }

    @Test
    void importedForeignIdentityStaysHiddenUntilMet() {
        store.find(CANONICAL, ACCOUNT).orElseThrow().source = RpNameSource.API_IMPORTED;
        assertEquals("[Sprechen] Unbekannt: Hallo",
                rewriter.rewrite(message(false), config, ACCOUNT).getString());
    }

    @Test
    void accountOnlyDisplayLookupResolvesActiveProxy() {
        assertEquals("Matthias", RpNamesServices.displayProfile(null, ACCOUNT).orElseThrow().rpName);
    }

    @Test
    void accountTextWithoutForcedSpeakerDoesNotExposeHistoricalIdentity() throws Exception {
        replace("tablistIdentityCache", Map.of());
        assertEquals("[Sprechen] Unbekannt: Hallo", rewriter.rewrite(message(false), config).getString());
    }

    @Test
    void knownActiveAliasDoesNotKeepMeetMarkerOrConsumeMeetClick() throws Exception {
        var field = RpNamesServices.class.getDeclaredField("pendingMeet");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var pending = (java.util.Set<String>) field.get(null);
        var original = java.util.Set.copyOf(pending);
        try {
        RpNamesServices.markHeardInRpChat(ACCOUNT);
        var opened = new java.util.concurrent.atomic.AtomicBoolean();
        replace("chatMeetHandler", (java.util.function.BiConsumer<net.minecraft.client.gui.screen.Screen,
                de.ottoextra.rpnames.model.LocalRpProfile>) (parent, profile) -> opened.set(true));
        assertFalse(RpNamesServices.isPendingMeet(ACCOUNT));
        assertFalse(RpNamesServices.meetFromChat(null, ACCOUNT));
        assertFalse(opened.get());
        } finally {
            pending.clear();
            pending.addAll(original);
        }
    }

    @Test
    void letterAndLeaderNameUseTheActiveDisplayProfile() {
        var resolver = new de.ottoextra.letter.placeholder.OttoExtraRpIdentityResolver();
        store.find(CANONICAL, ACCOUNT).orElseThrow().title = "Graf";
        assertEquals("Matthias", resolver.rpName(ACCOUNT).orElseThrow());
        assertEquals("Graf", resolver.title(ACCOUNT).orElseThrow());
        assertEquals("Matthias", RpNamesServices.knownRpNameForDisplay(ACCOUNT).orElseThrow());
    }

    private Text message(boolean hover) {
        var name = Text.literal(ACCOUNT);
        if (hover) name.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.literal(ACCOUNT))));
        return Text.literal("[Sprechen] ").append(name).append(": Hallo");
    }

    private void replace(String name, Object value) throws Exception {
        Field field = RpNamesServices.class.getDeclaredField(name);
        field.setAccessible(true);
        if (!saved.containsKey(field)) saved.put(field, field.get(null));
        field.set(null, value);
    }
}
