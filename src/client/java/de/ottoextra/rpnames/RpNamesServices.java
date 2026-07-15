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

    /*
     * Die Ottonien-Chatzeile enthält vor dem sichtbaren RP-Namen einen
     * barrierefreien Kopf-Text wie "[ChaosElder1 head]". Dieser Text ist
     * die zuverlässigste Zuordnung zwischen Chatsprecher und Minecraft-Konto,
     * wenn der Server keine auswertbaren Hover-Daten mitsendet.
     */
    private static final java.util.regex.Pattern CHAT_HEAD_ACCOUNT =
            java.util.regex.Pattern.compile(
                    "\\[([A-Za-z0-9_]{3,16})\\s+(?:head|kopf)\\]",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );

    private static final java.util.regex.Pattern CHAT_COMPONENT_TOKEN =
            java.util.regex.Pattern.compile("\\[[^\\]\\r\\n]+\\]\\s*");

    private record VisibleChatSpeaker(
            String accountName,
            String uuid,
            String rpName
    ) {
    }

    private RpNamesServices() {
    }

    static void init(OttoExtraConfig.RpNames cfg) {
        ensureInitialized(cfg);
    }

    public static synchronized void ensureInitialized(OttoExtraConfig.RpNames cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException(
                    "RP-Namen-Konfiguration darf nicht null sein"
            );
        }

        config = cfg;

        if (store != null
                && titles != null
                && catalog != null
                && hoverParser != null
                && rewriter != null) {
            return;
        }

        LocalRpIdentityStore newStore = new LocalRpIdentityStore();
        TitleRegistry newTitles = new TitleRegistry();

        de.ottoextra.rpnames.title.TitleCatalogStore newCatalog =
                new de.ottoextra.rpnames.title.TitleCatalogStore();

        newStore.load();
        newTitles.load();
        newCatalog.load();

        HoverIdentityParser newHoverParser =
                new HoverIdentityParser(newTitles);

        ChatNameRewriter newRewriter =
                new ChatNameRewriter(newStore, newTitles);

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
        LocalRpIdentityStore currentStore = store;

        if (currentStore != null) {
            currentStore.shutdown();
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
        var currentCatalog = catalog;
        return currentCatalog == null
                ? raw
                : currentCatalog.displayForm(raw);
    }

    private static String catalogDefaultNameColor() {
        var currentCatalog = catalog;

        return currentCatalog != null
                ? currentCatalog.defaultNameColor()
                : "#c7a87f";
    }

    private static String titleNameColor(String title) {
        var currentCatalog = catalog;

        return currentCatalog != null
                ? currentCatalog.titleNameColor(title).orElse(null)
                : null;
    }

    public static boolean titleOverridesColor(String title) {
        var currentCatalog = catalog;

        return currentCatalog != null
                && currentCatalog.overridesColor(title);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    public static String rpNameColor(
            String personOverride,
            String title
    ) {
        String global = config != null
                ? config.globalRpNameColor
                : null;

        if (titleOverridesColor(title)) {
            return firstNonBlank(
                    personOverride,
                    titleNameColor(title),
                    global,
                    catalogDefaultNameColor()
            );
        }

        return firstNonBlank(
                personOverride,
                titleNameColor(title),
                global,
                catalogDefaultNameColor()
        );
    }

    public static String playerNameColor(
            String personOverride,
            String title
    ) {
        String global = config != null
                ? config.globalPlayerNameColor
                : null;

        if (titleOverridesColor(title)) {
            return firstNonBlank(
                    personOverride,
                    titleNameColor(title),
                    global,
                    catalogDefaultNameColor()
            );
        }

        return firstNonBlank(
                personOverride,
                titleNameColor(title),
                global,
                catalogDefaultNameColor()
        );
    }

    public static boolean isActive() {
        return active
                && store != null
                && config != null
                && config.enabled;
    }

    public static boolean isKnownForDisplay(LocalRpProfile profile) {
        if (profile == null || !profile.hasRpName()) {
            return false;
        }

        if (isLocalPlayer(profile)) {
            return true;
        }

        if (!proactiveMeetEnabled()) {
            return true;
        }

        return profile.knowledgeState != KnowledgeState.API_IMPORTED
                && profile.source != RpNameSource.API_IMPORTED;
    }

    private static boolean isLocalPlayer(LocalRpProfile profile) {
        if (profile == null) {
            return false;
        }

        try {
            net.minecraft.client.MinecraftClient client =
                    net.minecraft.client.MinecraftClient.getInstance();

            if (client == null) {
                return false;
            }

            if (client.player != null) {
                if (profile.uuid != null
                        && !profile.uuid.isBlank()
                        && profile.uuid.equalsIgnoreCase(
                        client.player.getUuidAsString()
                )) {
                    return true;
                }

                if (client.player.getGameProfile() != null
                        && profile.accountName != null
                        && profile.accountName.equalsIgnoreCase(
                        client.player.getGameProfile().name()
                )) {
                    return true;
                }
            }

            return client.getSession() != null
                    && profile.accountName != null
                    && profile.accountName.equalsIgnoreCase(
                    client.getSession().getUsername()
            );
        } catch (Throwable ignored) {
            return false;
        }
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
            pendingMeet.add(
                    account.toLowerCase(java.util.Locale.ROOT)
            );
        }
    }

    public static boolean isPendingMeet(String account) {
        if (account == null
                || store == null
                || !pendingMeet.contains(
                account.toLowerCase(java.util.Locale.ROOT)
        )) {
            return false;
        }

        LocalRpProfile profile =
                store.findByName(account).orElse(null);

        return profile == null || !isKnownForDisplay(profile);
    }

    public record MeetSuggestion(
            String rpName,
            String title
    ) {
    }

    private static final java.util.Map<String, MeetSuggestion> suggestions =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void suggestIdentity(
            String account,
            String rpName,
            String title
    ) {
        if (account == null
                || account.isBlank()
                || store == null) {
            return;
        }

        boolean hasName =
                rpName != null && !rpName.isBlank();

        boolean hasTitle =
                title != null && !title.isBlank();

        if (!hasName && !hasTitle) {
            return;
        }

        suggestions.put(
                account.toLowerCase(java.util.Locale.ROOT),
                new MeetSuggestion(
                        hasName ? rpName.trim() : "",
                        hasTitle ? title.trim() : ""
                )
        );
    }

    public static MeetSuggestion meetSuggestion(String account) {
        if (account == null) {
            return null;
        }

        return suggestions.get(
                account.toLowerCase(java.util.Locale.ROOT)
        );
    }

    private static int chatSeparatorColon(
            String plain,
            int from
    ) {
        if (plain == null) {
            return -1;
        }

        int nestedBrackets = 0;
        int fallback = -1;

        for (int i = Math.max(0, from); i < plain.length(); i++) {
            char character = plain.charAt(i);

            if (character == '[') {
                nestedBrackets++;
                continue;
            }

            if (character == ']' && nestedBrackets > 0) {
                nestedBrackets--;
                continue;
            }

            if (character == ':' && nestedBrackets == 0) {
                if (fallback < 0) {
                    fallback = i;
                }

                if (i + 1 >= plain.length()
                        || Character.isWhitespace(
                        plain.charAt(i + 1)
                )) {
                    return i;
                }
            }
        }

        return fallback;
    }

    private static String speakerZone(String plain) {
        if (plain == null) {
            return null;
        }

        int bracket = plain.indexOf(']');
        int colon = bracket < 0
                ? -1
                : chatSeparatorColon(plain, bracket + 1);

        if (bracket < 0 || colon <= bracket) {
            return null;
        }

        String zone = plain.substring(bracket + 1, colon).trim();
        return zone.isBlank() ? null : zone;
    }

    private static String accountFromHeadToken(String zone) {
        if (zone == null || zone.isBlank()) {
            return null;
        }

        java.util.regex.Matcher matcher = CHAT_HEAD_ACCOUNT.matcher(zone);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String visibleSpeakerText(String zone) {
        if (zone == null || zone.isBlank()) {
            return null;
        }

        String visible = CHAT_COMPONENT_TOKEN.matcher(zone)
                .replaceAll("")
                .replaceAll("\\s{2,}", " ")
                .trim();

        return visible.isBlank() ? null : visible;
    }

    public static String speakerAccount(String plain) {
        if (store == null || plain == null) {
            return null;
        }

        String zone = speakerZone(plain);
        if (zone == null) {
            return null;
        }

        String headAccount = accountFromHeadToken(zone);
        if (headAccount != null) {
            return headAccount;
        }

        String[] words = zone.split("\\s+");
        for (int i = words.length - 1; i >= 0; i--) {
            String word = words[i]
                    .replaceAll("^[^A-Za-z0-9_]+|[^A-Za-z0-9_]+$", "");

            if (!word.isBlank()
                    && store.findByName(word).isPresent()) {
                return word;
            }
        }

        return null;
    }

    public static String serverTitleFor(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }

        var client =
                net.minecraft.client.MinecraftClient.getInstance();

        if (client.getNetworkHandler() == null) {
            return null;
        }

        for (net.minecraft.client.network.PlayerListEntry entry
                : client.getNetworkHandler().getPlayerList()) {

            var profile = entry.getProfile();

            if (profile == null
                    || profile.name() == null
                    || !account.equalsIgnoreCase(profile.name())) {
                continue;
            }

            Text rawDisplayName =
                    ((de.ottoextra.mixin.PlayerListEntryAccessor)
                            (Object) entry)
                            .ottoextra$rawDisplayName();

            if (rawDisplayName == null) {
                return null;
            }

            return de.ottoextra.rpnames.tablist.TablistNameFormatter
                    .extractServerTitle(
                            rawDisplayName,
                            profile.name()
                    );
        }

        return null;
    }

    public static LocalRpProfile findProfileByAnyName(String name) {
        if (store == null
                || name == null
                || name.isBlank()) {
            return null;
        }

        String trimmed = name.trim();

        var byAccount = store.findByName(trimmed);

        if (byAccount.isPresent()) {
            return byAccount.get();
        }

        String lower =
                trimmed.toLowerCase(java.util.Locale.ROOT);

        LocalRpProfile best = null;
        int bestLength = 0;

        for (LocalRpProfile profile : store.all()) {
            int length = matchLen(lower, profile.rpName);

            if (length == 0) {
                length = matchLen(
                        lower,
                        profile.accountName
                );
            }

            if (length > bestLength) {
                bestLength = length;
                best = profile;
            }
        }

        return best;
    }

    private static int matchLen(
            String lower,
            String candidate
    ) {
        if (candidate == null || candidate.isBlank()) {
            return 0;
        }

        String normalizedCandidate =
                candidate.trim()
                        .toLowerCase(java.util.Locale.ROOT);

        return lower.equals(normalizedCandidate)
                || lower.endsWith(" " + normalizedCandidate)
                ? normalizedCandidate.length()
                : 0;
    }

    private static String unknownPlaceholder() {
        OttoExtraConfig.RpNames currentConfig = config;

        if (currentConfig == null
                || currentConfig.unknownPlaceholder == null
                || currentConfig.unknownPlaceholder.isBlank()) {
            return "Unbekannt";
        }

        return currentConfig.unknownPlaceholder;
    }

    public static String unknownDisplay(String accountName) {
        OttoExtraConfig.RpNames currentConfig = config;

        if (currentConfig == null) {
            return accountName;
        }

        if (!proactiveMeetEnabled()
                && currentConfig.unknownShowAccount
                && accountName != null
                && !accountName.isBlank()) {
            return accountName;
        }

        return unknownPlaceholder();
    }

    public static String unknownChatDisplay(String accountName) {
        return proactiveMeetEnabled()
                ? unknownPlaceholder()
                : unknownDisplay(accountName);
    }

    public static String unknownNametagDisplay(String accountName) {
        return proactiveMeetEnabled()
                ? unknownPlaceholder()
                : unknownDisplay(accountName);
    }

    public static boolean unknownShowsAccount() {
        return config != null
                && config.unknownShowAccount
                && !proactiveMeetEnabled();
    }

    public static boolean unknownAccountLineEnabled() {
        return config != null
                && config.unknownShowAccount
                && proactiveMeetEnabled();
    }

    private static HoverIdentityParser.ParsedIdentity speakerIdentity(
            java.util.List<HoverIdentityParser.ParsedIdentity> identities,
            String plain
    ) {
        if (identities == null || identities.isEmpty()) {
            return null;
        }

        if (identities.size() == 1) {
            return identities.get(0);
        }

        int bracket =
                plain == null ? -1 : plain.indexOf(']');

        int colon =
                bracket < 0
                        ? -1
                        : chatSeparatorColon(plain, bracket + 1);

        String speakerZone =
                bracket >= 0 && colon > bracket
                        ? plain.substring(bracket + 1, colon)
                        .toLowerCase(java.util.Locale.ROOT)
                        : "";

        for (HoverIdentityParser.ParsedIdentity identity
                : identities) {

            String rpName =
                    identity.rpName() == null
                            ? ""
                            : identity.rpName()
                            .trim()
                            .toLowerCase(java.util.Locale.ROOT);

            String account =
                    identity.accountName() == null
                            ? ""
                            : identity.accountName()
                            .trim()
                            .toLowerCase(java.util.Locale.ROOT);

            if ((!rpName.isEmpty()
                    && speakerZone.contains(rpName))
                    || (!account.isEmpty()
                    && speakerZone.contains(account))) {
                return identity;
            }
        }

        return identities.get(0);
    }

    private static boolean isApiUploadChannel(
            OttoChatChannel channel
    ) {
        return channel != null
                && !channel.isOoc()
                && channel != OttoChatChannel.SYSTEM
                && channel != OttoChatChannel.OTHER;
    }

    private static String onlineUuidForAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }

        try {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client == null || client.getNetworkHandler() == null) {
                return null;
            }

            for (net.minecraft.client.network.PlayerListEntry entry
                    : client.getNetworkHandler().getPlayerList()) {
                var profile = entry.getProfile();
                if (profile != null
                        && profile.name() != null
                        && account.equalsIgnoreCase(profile.name())) {
                    return profile.id().toString();
                }
            }
        } catch (Throwable ignored) {
            // Der lokale Store bleibt als Fallback verfügbar.
        }

        return null;
    }

    private static boolean endsWithName(String visible, String name) {
        if (visible == null || name == null || name.isBlank()) {
            return false;
        }

        String visibleNormalized = visible.trim()
                .toLowerCase(java.util.Locale.ROOT);
        String nameNormalized = name.trim()
                .toLowerCase(java.util.Locale.ROOT);

        return visibleNormalized.equals(nameNormalized)
                || visibleNormalized.endsWith(" " + nameNormalized);
    }

    private static String stripTitlePrefix(String visible, String title) {
        if (visible == null || visible.isBlank()
                || title == null || title.isBlank()) {
            return null;
        }

        String[] visibleWords = visible.trim().split("\\s+");
        String[] titleWords = title.trim().split("\\s+");

        if (visibleWords.length <= titleWords.length) {
            return null;
        }

        String visiblePrefix = String.join(" ",
                java.util.Arrays.copyOfRange(visibleWords, 0, titleWords.length));

        if (!de.ottoextra.rpnames.title.TitleRegistry.normalize(visiblePrefix)
                .equals(de.ottoextra.rpnames.title.TitleRegistry.normalize(title))) {
            return null;
        }

        String remainder = String.join(" ",
                java.util.Arrays.copyOfRange(
                        visibleWords, titleWords.length, visibleWords.length));

        return remainder.isBlank() ? null : remainder.trim();
    }

    private static String rpNameFromVisibleSpeaker(
            String visible,
            String account,
            LocalRpProfile profile
    ) {
        if (visible == null || visible.isBlank()) {
            return null;
        }

        /*
         * Ein bereits bekannter/API-geladener Name ist die sicherste Quelle.
         * Die sichtbare Chatzeile muss genau mit diesem Namen enden.
         */
        if (profile != null
                && profile.hasRpName()
                && endsWithName(visible, profile.rpName)) {
            return profile.rpName.trim();
        }

        java.util.LinkedHashSet<String> titleCandidates =
                new java.util.LinkedHashSet<>();

        if (profile != null && profile.hasTitle()) {
            titleCandidates.add(profile.title.trim());
        }

        String serverTitle = serverTitleFor(account);
        if (serverTitle != null && !serverTitle.isBlank()) {
            titleCandidates.add(serverTitle.trim());
        }

        if (titles != null) {
            titles.findPrefix(visible)
                    .map(de.ottoextra.rpnames.title.TitleRegistry.ResolvedTitle::title)
                    .filter(title -> title != null && !title.isBlank())
                    .ifPresent(titleCandidates::add);
        }

        for (String title : titleCandidates) {
            String stripped = stripTitlePrefix(visible, title);
            if (stripped != null
                    && !stripped.equalsIgnoreCase(account)) {
                return stripped;
            }
        }

        /*
         * Kein Titel vorhanden: Dann ist der sichtbare Sprechertext bereits
         * der RP-Name. Ein reiner Minecraft-Accountname wird nicht hochgeladen.
         */
        if (account != null && visible.equalsIgnoreCase(account)) {
            return null;
        }

        return visible.trim();
    }

    private static VisibleChatSpeaker speakerFromVisibleChat(String plain) {
        if (plain == null || store == null) {
            return null;
        }

        String zone = speakerZone(plain);
        if (zone == null) {
            return null;
        }

        String visible = visibleSpeakerText(zone);
        if (visible == null) {
            return null;
        }

        String account = accountFromHeadToken(zone);
        LocalRpProfile profile = account == null
                ? findProfileByAnyName(visible)
                : store.findByName(account).orElse(null);

        if (account == null && profile != null) {
            account = profile.accountName;
        }

        if (account == null || account.isBlank()) {
            return null;
        }

        String rpName = rpNameFromVisibleSpeaker(visible, account, profile);
        if (rpName == null || rpName.isBlank()) {
            return null;
        }

        String uuid = onlineUuidForAccount(account);
        if ((uuid == null || uuid.isBlank()) && profile != null) {
            uuid = profile.uuid;
        }

        return new VisibleChatSpeaker(account, uuid, rpName);
    }

    /**
     * Liefert den sichtbaren Titel mit einem sicheren Fallback auf den gespeicherten
     * Rohwert. Nach einem Personen-Reset darf eine angepasste Katalogvariante den
     * wiederhergestellten Servertitel nicht erneut ersetzen.
     */
    public static String displayTitle(LocalRpProfile profile) {
        if (profile == null || profile.title == null || profile.title.isBlank()) {
            return "";
        }
        if (profile.rawTitleDisplay) {
            return profile.title;
        }
        String mapped = canonicalTitle(profile.title);
        return mapped == null || mapped.isBlank() ? profile.title : mapped;
    }

    /**
     * Schreibt eine bereits empfangene Nachricht ausschließlich für die Anzeige
     * neu. Anders als {@link #processChatMessage(Text)} werden dabei weder
     * Profile gelernt noch API-Uploads ausgelöst.
     */
    public static Text rewriteChatDisplay(Text message) {
        if (!isActive() || message == null) {
            return message;
        }

        try {
            String plain = message.getString();
            OttoChatChannel channel = OttoChatChannel.fromMessage(plain);
            java.util.List<HoverIdentityParser.ParsedIdentity> identities =
                    hoverParser.parseMessage(message, channel.isRpSpeak());
            HoverIdentityParser.ParsedIdentity speaker = speakerIdentity(identities, plain);

            String forcedAccount = speaker != null ? speaker.accountName() : speakerAccount(plain);
            if (forcedAccount == null) {
                VisibleChatSpeaker visible = speakerFromVisibleChat(plain);
                forcedAccount = visible != null ? visible.accountName() : null;
            }

            return rewriteChatDisplay(message, channel, forcedAccount);
        } catch (Throwable ignored) {
            return message;
        }
    }

    private static Text rewriteChatDisplay(Text message, OttoChatChannel channel,
                                           String forcedAccount) {
        if (!channel.shouldReplace(config)) {
            return channel.isOoc()
                    ? rewriter.rewriteTitleOnly(message, config)
                    : message;
        }
        return rewriter.rewrite(message, config, forcedAccount);
    }

    public static Text processChatMessage(Text message) {
        de.ottoextra.rpnames.chat.HoverDebug.dump(message);

        if (!isActive() || message == null) {
            return message;
        }

        try {
            String plain = message.getString();

            OttoChatChannel channel =
                    OttoChatChannel.fromMessage(plain);

            boolean learn =
                    channel.shouldLearn()
                            && !proactiveMeetEnabled();

            boolean meet =
                    proactiveMeetEnabled();

            java.util.List<HoverIdentityParser.ParsedIdentity> identities =
                    hoverParser.parseMessage(
                            message,
                            channel.isRpSpeak()
                    );

            de.ottoextra.rpnames.chat.HoverDebug
                    .logParsed(identities);

            boolean markedMeetSpeaker = false;

            for (HoverIdentityParser.ParsedIdentity identity
                    : identities) {

                store.updateImportedIdentityIfChanged(
                        identity.accountName(),
                        onlineUuidForAccount(identity.accountName()),
                        identity.rpName(),
                        identity.title()
                );

                if (learn) {
                    store.learnIdentity(
                            identity.accountName(),
                            identity.rpName(),
                            identity.title(),
                            identity.titleGroup(),
                            RpNameSource.LEARNED_FROM_HOVER
                    );
                }

                if (meet) {
                    store.ensureSeen(
                            identity.accountName(),
                            null,
                            RpNameSource.SEEN_ONLINE
                    );

                    suggestIdentity(
                            identity.accountName(),
                            identity.rpName(),
                            identity.title()
                    );

                    if (channel.isRpSpeak()) {
                        markHeardInRpChat(
                                identity.accountName()
                        );

                        markedMeetSpeaker = true;
                    }
                }

                store.updateTitleIfChanged(
                        identity.accountName(),
                        null,
                        identity.title()
                );
            }

            if (meet
                    && channel.isRpSpeak()
                    && !markedMeetSpeaker) {
                markHeardInRpChat(
                        speakerAccount(plain)
                );
            }

            HoverIdentityParser.ParsedIdentity speakerIdentity =
                    speakerIdentity(identities, plain);

            String forcedSpeakerAccount =
                    speakerIdentity != null
                            ? speakerIdentity.accountName()
                            : null;

            /*
             * RP-Namen aus allen echten Spieler-Chats außer OOC und
             * Offtopic an die API übertragen. Systemmeldungen und
             * unbekannte Chatformate sind ausgeschlossen.
             */
            if (isApiUploadChannel(channel)) {

                if (speakerIdentity != null
                        && speakerIdentity.rpName() != null
                        && !speakerIdentity.rpName().isBlank()) {

                    String targetUuid = onlineUuidForAccount(
                            speakerIdentity.accountName());

                    if (targetUuid == null) {
                        targetUuid = store.findByName(
                                        speakerIdentity.accountName())
                                .map(profile -> profile.uuid)
                                .orElse(null);
                    }

                    de.ottoextra.rpnames.upload.RpNameUploadService
                            .uploadObservedIdentity(
                                    speakerIdentity.accountName(),
                                    targetUuid,
                                    speakerIdentity.rpName()
                            );

                } else {

                    /*
                     * Fallback für Servernachrichten ohne verwertbaren Hover:
                     * Der Account wird aus "[MinecraftName head]" und der
                     * RP-Name aus dem sichtbaren Sprecherbereich gewonnen.
                     * Das funktioniert für den eigenen und für fremde Spieler.
                     */
                    VisibleChatSpeaker visibleSpeaker =
                            speakerFromVisibleChat(plain);

                    if (visibleSpeaker != null) {
                        forcedSpeakerAccount = visibleSpeaker.accountName();

                        store.updateImportedIdentityIfChanged(
                                visibleSpeaker.accountName(),
                                visibleSpeaker.uuid(),
                                visibleSpeaker.rpName(),
                                serverTitleFor(visibleSpeaker.accountName())
                        );

                        de.ottoextra.OttoExtra.LOGGER.info(
                                "[rpnames] Chatsprecher für API erkannt: "
                                        + "account={}, uuid={}, rpName={}",
                                visibleSpeaker.accountName(),
                                visibleSpeaker.uuid(),
                                visibleSpeaker.rpName()
                        );

                        de.ottoextra.rpnames.upload.RpNameUploadService
                                .uploadObservedIdentity(
                                        visibleSpeaker.accountName(),
                                        visibleSpeaker.uuid(),
                                        visibleSpeaker.rpName()
                                );
                    }
                }
            }

            return rewriteChatDisplay(message, channel, forcedSpeakerAccount);

        } catch (Throwable ignored) {
            return message;
        }
    }
}
