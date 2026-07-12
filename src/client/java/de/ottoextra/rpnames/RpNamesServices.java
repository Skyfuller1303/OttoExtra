package de.ottoextra.rpnames;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.chat.ChatNameRewriter;
import de.ottoextra.rpnames.chat.HoverIdentityParser;
import de.ottoextra.rpnames.chat.OttoChatChannel;
import de.ottoextra.rpnames.model.KnowledgeState;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.text.Text;

public final class RpNamesServices {

    private static volatile LocalRpIdentityStore store;
    private static volatile TitleRegistry titles;
    private static volatile de.ottoextra.rpnames.title.TitleCatalogStore catalog;
    private static volatile HoverIdentityParser hoverParser;
    private static volatile ChatNameRewriter rewriter;
    private static volatile OttoExtraConfig.RpNames config;
    private static volatile boolean active = false;

    private RpNamesServices() {
    }

    static void init(OttoExtraConfig.RpNames cfg) {
        ensureInitialized(cfg);
    }

    public static synchronized void ensureInitialized(OttoExtraConfig.RpNames cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("RP-Namen-Konfiguration darf nicht null sein");
        }
        config = cfg;
        if (store != null && titles != null && catalog != null
                && hoverParser != null && rewriter != null) {
            return;
        }

        LocalRpIdentityStore newStore = new LocalRpIdentityStore();
        TitleRegistry newTitles = new TitleRegistry();
        de.ottoextra.rpnames.title.TitleCatalogStore newCatalog =
                new de.ottoextra.rpnames.title.TitleCatalogStore();
        newStore.load();
        newTitles.load();
        newCatalog.load();

        HoverIdentityParser newHoverParser = new HoverIdentityParser(newTitles);
        ChatNameRewriter newRewriter = new ChatNameRewriter(newStore, newTitles);

        store = newStore;
        titles = newTitles;
        catalog = newCatalog;
        hoverParser = newHoverParser;
        rewriter = newRewriter;
    }

    static void setActive(boolean value) {
        active = value;
    }

    static void shutdown() {
        LocalRpIdentityStore s = store;
        if (s != null) {
            s.shutdown();
        }
        active = false;
    }

    public static LocalRpIdentityStore store() {
        return store;
    }

    public static TitleRegistry titles() {
        return titles;
    }

    public static de.ottoextra.rpnames.title.TitleCatalogStore catalog() {
        return catalog;
    }

    public static OttoExtraConfig.RpNames config() {
        return config;
    }

    public static String canonicalTitle(String raw) {
        var c = catalog;
        return c == null ? raw : c.displayForm(raw);
    }

    private static String catalogDefaultNameColor() {
        var cat = catalog;
        return cat != null ? cat.defaultNameColor() : "#c7a87f";
    }

    private static String titleNameColor(String title) {
        var cat = catalog;
        return cat != null ? cat.titleNameColor(title).orElse(null) : null;
    }

    public static boolean titleOverridesColor(String title) {
        var cat = catalog;
        return cat != null && cat.overridesColor(title);
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    public static String rpNameColor(String personOverride, String title) {
        String global = config != null ? config.globalRpNameColor : null;
        if (titleOverridesColor(title)) {
            return firstNonBlank(titleNameColor(title), personOverride, global,
                    catalogDefaultNameColor());
        }
        return firstNonBlank(personOverride, titleNameColor(title), global,
                catalogDefaultNameColor());
    }

    public static String playerNameColor(String personOverride, String title) {
        String global = config != null ? config.globalPlayerNameColor : null;
        if (titleOverridesColor(title)) {
            return firstNonBlank(titleNameColor(title), personOverride, global,
                    catalogDefaultNameColor());
        }
        return firstNonBlank(personOverride, titleNameColor(title), global,
                catalogDefaultNameColor());
    }

    public static boolean isActive() {
        return active && store != null && config != null && config.enabled;
    }

    public static boolean isKnownForDisplay(LocalRpProfile profile) {
        if (profile == null || !profile.hasRpName()) {
            return false;
        }
        if (!proactiveMeetEnabled()) {
            return true;
        }
        return profile.knowledgeState != KnowledgeState.API_IMPORTED
                && profile.source != RpNameSource.API_IMPORTED;
    }

    public static boolean openBookOnClickEnabled() {
        return isActive() && config.openBookOnClick;
    }

    public static boolean proactiveMeetEnabled() {
        return isActive() && config.proactiveMeet;
    }

    private static final java.util.Set<String> pendingMeet =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void markHeardInRpChat(String account) {
        if (account != null && !account.isBlank()) {
            pendingMeet.add(account.toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static boolean isPendingMeet(String account) {
        if (account == null || store == null
                || !pendingMeet.contains(account.toLowerCase(java.util.Locale.ROOT))) {
            return false;
        }
        de.ottoextra.rpnames.model.LocalRpProfile p = store.findByName(account).orElse(null);
        return p == null || !isKnownForDisplay(p);
    }

    public record MeetSuggestion(String rpName, String title) {
    }

    private static final java.util.Map<String, MeetSuggestion> suggestions =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void suggestIdentity(String account, String rpName, String title) {
        if (account == null || account.isBlank() || store == null) {
            return;
        }
        boolean hasName = rpName != null && !rpName.isBlank();
        boolean hasTitle = title != null && !title.isBlank();
        if (!hasName && !hasTitle) {
            return;
        }
        suggestions.put(account.toLowerCase(java.util.Locale.ROOT),
                new MeetSuggestion(hasName ? rpName.trim() : "", hasTitle ? title.trim() : ""));
    }

    public static MeetSuggestion meetSuggestion(String account) {
        return account == null ? null
                : suggestions.get(account.toLowerCase(java.util.Locale.ROOT));
    }

    private static int chatSeparatorColon(String plain, int from) {
        if (plain == null) {
            return -1;
        }
        int nestedBrackets = 0;
        int fallback = -1;
        for (int i = Math.max(0, from); i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '[') {
                nestedBrackets++;
            } else if (c == ']' && nestedBrackets > 0) {
                nestedBrackets--;
            } else if (c == ':' && nestedBrackets == 0) {
                if (fallback < 0) {
                    fallback = i;
                }
                if (i + 1 >= plain.length() || Character.isWhitespace(plain.charAt(i + 1))) {
                    return i;
                }
            }
        }
        return fallback;
    }

    public static String speakerAccount(String plain) {
        if (store == null || plain == null) {
            return null;
        }
        int br = plain.indexOf(']');
        int colon = chatSeparatorColon(plain, br + 1);
        if (br < 0 || colon < 0) {
            return null;
        }
        String[] words = plain.substring(br + 1, colon).trim().split("\\s+");
        for (int i = words.length - 1; i >= 0; i--) {
            if (!words[i].isBlank() && store.findByName(words[i]).isPresent()) {
                return words[i];
            }
        }
        return null;
    }

    public static String serverTitleFor(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) {
            return null;
        }
        for (net.minecraft.client.network.PlayerListEntry entry
                : mc.getNetworkHandler().getPlayerList()) {
            var p = entry.getProfile();
            if (p == null || p.name() == null || !account.equalsIgnoreCase(p.name())) {
                continue;
            }
            net.minecraft.text.Text raw =
                    ((de.ottoextra.mixin.PlayerListEntryAccessor) (Object) entry)
                            .ottoextra$rawDisplayName();
            if (raw == null) {
                return null;
            }
            return de.ottoextra.rpnames.tablist.TablistNameFormatter
                    .extractServerTitle(raw, p.name());
        }
        return null;
    }

    public static de.ottoextra.rpnames.model.LocalRpProfile findProfileByAnyName(String name) {
        if (store == null || name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        var byAccount = store.findByName(trimmed);
        if (byAccount.isPresent()) {
            return byAccount.get();
        }

        String low = trimmed.toLowerCase(java.util.Locale.ROOT);
        de.ottoextra.rpnames.model.LocalRpProfile best = null;
        int bestLen = 0;
        for (de.ottoextra.rpnames.model.LocalRpProfile p : store.all()) {
            int len = matchLen(low, p.rpName);
            if (len == 0) {
                len = matchLen(low, p.accountName);
            }
            if (len > bestLen) {
                bestLen = len;
                best = p;
            }
        }
        return best;
    }

    private static int matchLen(String low, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return 0;
        }
        String c = candidate.trim().toLowerCase(java.util.Locale.ROOT);
        return low.equals(c) || low.endsWith(" " + c) ? c.length() : 0;
    }

    private static String unknownPlaceholder() {
        OttoExtraConfig.RpNames cfg = config;
        return cfg == null || cfg.unknownPlaceholder == null || cfg.unknownPlaceholder.isBlank()
                ? "Unbekannt" : cfg.unknownPlaceholder;
    }

    public static String unknownDisplay(String accountName) {
        OttoExtraConfig.RpNames cfg = config;
        if (cfg == null) {
            return accountName;
        }
        if (!proactiveMeetEnabled() && cfg.unknownShowAccount
                && accountName != null && !accountName.isBlank()) {
            return accountName;
        }
        return unknownPlaceholder();
    }

    public static String unknownChatDisplay(String accountName) {
        return proactiveMeetEnabled() ? unknownPlaceholder() : unknownDisplay(accountName);
    }

    public static String unknownNametagDisplay(String accountName) {
        return proactiveMeetEnabled() ? unknownPlaceholder() : unknownDisplay(accountName);
    }

    public static boolean unknownShowsAccount() {
        return config != null && config.unknownShowAccount && !proactiveMeetEnabled();
    }

    public static boolean unknownAccountLineEnabled() {
        return config != null && config.unknownShowAccount && proactiveMeetEnabled();
    }

    private static HoverIdentityParser.ParsedIdentity speakerIdentity(
            java.util.List<HoverIdentityParser.ParsedIdentity> identities, String plain) {
        if (identities == null || identities.isEmpty()) {
            return null;
        }
        if (identities.size() == 1) {
            return identities.get(0);
        }
        int bracket = plain == null ? -1 : plain.indexOf(']');
        int colon = bracket < 0 ? -1 : chatSeparatorColon(plain, bracket + 1);
        String zone = bracket >= 0 && colon > bracket
                ? plain.substring(bracket + 1, colon).toLowerCase(java.util.Locale.ROOT)
                : "";
        for (HoverIdentityParser.ParsedIdentity id : identities) {
            String rp = id.rpName() == null ? "" : id.rpName().trim().toLowerCase(java.util.Locale.ROOT);
            String account = id.accountName() == null ? "" : id.accountName().trim().toLowerCase(java.util.Locale.ROOT);
            if ((!rp.isEmpty() && zone.contains(rp)) || (!account.isEmpty() && zone.contains(account))) {
                return id;
            }
        }
        return identities.get(0);
    }

    public static Text processChatMessage(Text message) {

        de.ottoextra.rpnames.chat.HoverDebug.dump(message);
        if (!isActive() || message == null) {
            return message;
        }
        try {
            String plain = message.getString();
            OttoChatChannel channel = OttoChatChannel.fromMessage(plain);

            boolean learn = channel.shouldLearn() && !proactiveMeetEnabled();
            boolean meet = proactiveMeetEnabled();

            java.util.List<HoverIdentityParser.ParsedIdentity> ids =
                    hoverParser.parseMessage(message, channel.isRpSpeak());
            de.ottoextra.rpnames.chat.HoverDebug.logParsed(ids);
            boolean markedMeetSpeaker = false;
            for (HoverIdentityParser.ParsedIdentity id : ids) {

                if (learn) {
                    store.learnIdentity(id.accountName(), id.rpName(), id.title(),
                            id.titleGroup(), RpNameSource.LEARNED_FROM_HOVER);
                }

                if (meet) {
                    store.ensureSeen(id.accountName(), null, RpNameSource.SEEN_ONLINE);
                    suggestIdentity(id.accountName(), id.rpName(), id.title());
                    if (channel.isRpSpeak()) {
                        markHeardInRpChat(id.accountName());
                        markedMeetSpeaker = true;
                    }
                }
                store.updateTitleIfChanged(id.accountName(), null, id.title());
            }

            if (meet && channel.isRpSpeak() && !markedMeetSpeaker) {
                markHeardInRpChat(speakerAccount(plain));
            }
            HoverIdentityParser.ParsedIdentity speakerIdentity = speakerIdentity(ids, plain);
            String forcedSpeakerAccount = speakerIdentity != null
                    ? speakerIdentity.accountName() : null;
            Text result;
            if (!channel.shouldReplace(config)) {

                result = channel.isOoc() ? rewriter.rewriteTitleOnly(message, config) : message;
            } else {

                result = rewriter.rewrite(message, config, forcedSpeakerAccount);
            }
            return result;
        } catch (Throwable t) {
            return message;
        }
    }

}
