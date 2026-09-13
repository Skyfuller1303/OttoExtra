package de.ottoextra.rpnames;

import de.ottoextra.OttoExtra;
import de.ottoextra.chat.ChatSpeakerClassifier;
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
    private static volatile String cachedLocalAccount;
    private static volatile LocalRpProfile cachedLocalProfile;
    private static volatile long tablistIdentityCacheAt;
    private static volatile java.util.Map<String, java.util.List<String>>
            tablistIdentityCache = java.util.Map.of();
    private static java.util.function.BiConsumer<net.minecraft.client.gui.screen.Screen, LocalRpProfile> chatMeetHandler;

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
        cachedLocalAccount = null;
        cachedLocalProfile = null;
        tablistIdentityCacheAt = 0L;
        tablistIdentityCache = java.util.Map.of();
        titles = newTitles;
        catalog = newCatalog;
        hoverParser = newHoverParser;
        rewriter = newRewriter;
    }

    static void setActive(boolean value) {
        active = value;
        if (!value) {
            pendingMeet.clear();
            suggestions.clear();
            cachedLocalAccount = null;
            cachedLocalProfile = null;
            tablistIdentityCacheAt = 0L;
            tablistIdentityCache = java.util.Map.of();
        }
    }

    static void shutdown() {
        LocalRpIdentityStore currentStore = store;

        if (currentStore != null) {
            currentStore.shutdown();
        }

        setActive(false);
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

    public static synchronized boolean restoreBackup(java.util.function.BooleanSupplier restoreFiles) {
        var currentTitles = titles;
        var currentCatalog = catalog;
        synchronized (currentTitles != null ? currentTitles : RpNamesServices.class) {
            synchronized (currentCatalog != null ? currentCatalog : RpNamesServices.class) {
                java.util.function.BooleanSupplier restoreAndReload = () -> {
                    return restoreAndReload(restoreFiles,
                            () -> { if (currentTitles != null) currentTitles.load(); },
                            () -> { if (currentCatalog != null) currentCatalog.reloadAfterRestore(); });
                };
                try {
                    return store == null ? restoreAndReload.getAsBoolean()
                            : store.restoreBackup(restoreAndReload);
                } catch (Exception e) {
                    OttoExtra.LOGGER.error("[Backup] RP-Daten konnten nicht neu geladen werden: {}",
                            e.toString());
                    return false;
                }
            }
        }
    }

    static boolean restoreAndReload(java.util.function.BooleanSupplier restoreFiles,
                                    Runnable... reloads) {
        boolean restored = false;
        try {
            restored = restoreFiles.getAsBoolean();
        } catch (Exception e) {
            OttoExtra.LOGGER.error("[Backup] Wiederherstellung fehlgeschlagen: {}", e.toString());
        }
        for (Runnable reload : reloads) {
            try {
                reload.run();
            } catch (Exception e) {
                restored = false;
                OttoExtra.LOGGER.error("[Backup] RP-Daten konnten nicht neu geladen werden: {}",
                        e.toString());
            }
        }
        return restored;
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

    public static java.util.Optional<String> knownRpNameForDisplay(String accountName) {
        LocalRpIdentityStore currentStore = store;
        if (!isActive() || currentStore == null
                || accountName == null || accountName.isBlank()) {
            return java.util.Optional.empty();
        }

        LocalRpProfile profile = displayProfile(null, accountName).orElse(null);
        return localRpNameForLeader(profile);
    }

    /**
     * Anzeigeprofil mit Schutz gegen Proxy-/Offline-UUIDs des eigenen Spielers.
     * Der echte Account kann serverseitig eine zweite UUID bekommen; dann darf
     * ein neuer "Unbekannt"-Datensatz das vorhandene eigene RP-Profil nicht verdecken.
     */
    public static java.util.Optional<LocalRpProfile> displayProfile(
            String uuid, String accountName) {
        LocalRpIdentityStore currentStore = store;
        if (currentStore == null) {
            return java.util.Optional.empty();
        }
        LocalRpProfile resolved = currentStore.find(uuid, accountName).orElse(null);
        if (resolved != null && resolved.hasRpName()) {
            return java.util.Optional.ofNullable(resolved);
        }

        LocalRpProfile activeAlias = preferKnownActiveAlias(
                accountName, uuid == null || uuid.isBlank()
                        ? (resolved == null ? null : resolved.uuid) : uuid, resolved, currentStore.all(),
                activeTablistUuids(accountName));
        if (activeAlias != null && activeAlias.hasRpName()) {
            return java.util.Optional.of(activeAlias);
        }
        if (!isLocalAccount(accountName)) {
            return java.util.Optional.ofNullable(resolved);
        }

        LocalRpProfile cached = cachedLocalProfile;
        if (cached != null && cached.hasRpName() && cachedLocalAccount != null
                && cachedLocalAccount.equalsIgnoreCase(accountName)
                && accountName.equalsIgnoreCase(cached.accountName)
                && currentStore.find(cached.uuid, cached.accountName).orElse(null) == cached) {
            return java.util.Optional.of(cached);
        }
        LocalRpProfile preferred = preferKnownLocalProfile(
                accountName, resolved, currentStore.all());
        if (preferred != null && preferred.hasRpName()) {
            cachedLocalAccount = accountName;
            cachedLocalProfile = preferred;
        }
        return java.util.Optional.ofNullable(preferred);
    }

    /** Erkennt gleichzeitig sichtbare Liege-/Pose-Proxies desselben Accounts. */
    static LocalRpProfile preferKnownActiveAlias(
            String accountName, String requestedUuid, LocalRpProfile resolved,
            java.util.Collection<LocalRpProfile> profiles,
            java.util.Collection<String> activeUuids) {
        if (resolved != null && resolved.hasRpName()) {
            return resolved;
        }
        if (accountName == null || accountName.isBlank()
                || requestedUuid == null || requestedUuid.isBlank()
                || profiles == null || activeUuids == null
                || activeUuids.stream().noneMatch(requestedUuid::equalsIgnoreCase)) {
            return resolved;
        }

        LocalRpProfile best = resolved;
        for (LocalRpProfile candidate : profiles) {
            if (candidate == null || candidate.accountName == null
                    || !accountName.equalsIgnoreCase(candidate.accountName)
                    || candidate.uuid == null || candidate.uuid.isBlank()
                    || requestedUuid.equalsIgnoreCase(candidate.uuid)
                    || !candidate.hasRpName()
                    || activeUuids.stream().noneMatch(candidate.uuid::equalsIgnoreCase)) {
                continue;
            }
            if (best == null || !best.hasRpName()
                    || localProfileStrength(candidate) > localProfileStrength(best)
                    || localProfileStrength(candidate) == localProfileStrength(best)
                    && candidate.lastUpdatedAt > best.lastUpdatedAt) {
                best = candidate;
            }
        }
        return best;
    }

    static java.util.Optional<LocalRpProfile> concurrentTablistAlias(
            String uuid, String accountName) {
        LocalRpIdentityStore currentStore = store;
        if (currentStore == null) {
            return java.util.Optional.empty();
        }
        LocalRpProfile resolved = currentStore.find(uuid, accountName).orElse(null);
        LocalRpProfile preferred = preferKnownActiveAlias(
                accountName, uuid, resolved, currentStore.all(),
                activeTablistUuids(accountName));
        return preferred != null && preferred != resolved && preferred.hasRpName()
                ? java.util.Optional.of(preferred) : java.util.Optional.empty();
    }

    private static java.util.List<String> activeTablistUuids(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            return java.util.List.of();
        }
        long now = System.nanoTime();
        if (tablistIdentityCacheAt == 0L
                || now - tablistIdentityCacheAt > 250_000_000L) {
            refreshTablistIdentityCache(now);
        }
        return tablistIdentityCache.getOrDefault(
                accountName.toLowerCase(java.util.Locale.ROOT), java.util.List.of());
    }

    private static synchronized void refreshTablistIdentityCache(long now) {
        if (tablistIdentityCacheAt != 0L
                && now - tablistIdentityCacheAt <= 250_000_000L) {
            return;
        }
        net.minecraft.client.MinecraftClient client =
                net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            tablistIdentityCache = java.util.Map.of();
            tablistIdentityCacheAt = now;
            return;
        }
        java.util.Map<String, java.util.List<String>> identities =
                new java.util.HashMap<>();
        for (var entry : client.getNetworkHandler().getPlayerList()) {
            var profile = entry.getProfile();
            if (profile == null || profile.name() == null || profile.name().isBlank()
                    || profile.id() == null) {
                continue;
            }
            identities.computeIfAbsent(
                            profile.name().toLowerCase(java.util.Locale.ROOT),
                            ignored -> new java.util.ArrayList<>())
                    .add(profile.id().toString());
        }
        identities.replaceAll((ignored, uuids) -> uuids.stream().distinct().toList());
        tablistIdentityCache = java.util.Map.copyOf(identities);
        tablistIdentityCacheAt = now;
    }

    static LocalRpProfile preferKnownLocalProfile(
            String accountName, LocalRpProfile resolved,
            java.util.Collection<LocalRpProfile> profiles) {
        LocalRpProfile best = resolved;
        if (accountName == null || accountName.isBlank() || profiles == null) {
            return best;
        }
        for (LocalRpProfile candidate : profiles) {
            if (candidate == null || candidate.accountName == null
                    || !accountName.equalsIgnoreCase(candidate.accountName)
                    || !candidate.hasRpName()) {
                continue;
            }
            if (best == null || !best.hasRpName()
                    || localProfileStrength(candidate) > localProfileStrength(best)
                    || localProfileStrength(candidate) == localProfileStrength(best)
                    && candidate.lastUpdatedAt > best.lastUpdatedAt) {
                best = candidate;
            }
        }
        return best;
    }

    private static int localProfileStrength(LocalRpProfile profile) {
        if (profile == null || !profile.hasRpName()) return 0;
        if (profile.locked || profile.knowledgeState == KnowledgeState.MANUAL_LOCKED) return 3;
        return profile.source == RpNameSource.API_IMPORTED ? 1 : 2;
    }

    private static boolean isLocalAccount(String accountName) {
        if (accountName == null || accountName.isBlank()) return false;
        try {
            net.minecraft.client.MinecraftClient client =
                    net.minecraft.client.MinecraftClient.getInstance();
            if (client == null) return false;
            if (client.player != null && client.player.getGameProfile() != null
                    && accountName.equalsIgnoreCase(client.player.getGameProfile().name())) {
                return true;
            }
            return client.getSession() != null
                    && accountName.equalsIgnoreCase(client.getSession().getUsername());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Alle lokal/API-bekannten Identitaetsbestandteile fuer Uebersetzungsfilter. */
    public static java.util.Set<String> translationProtectedNames() {
        LocalRpIdentityStore currentStore = store;
        if (currentStore == null) return java.util.Set.of();
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        for (LocalRpProfile profile : currentStore.all()) {
            addTranslationName(names, profile.accountName);
            if (profile.hasRpName()) addTranslationName(names, profile.rpName);
        }
        return java.util.Set.copyOf(names);
    }

    private static void addTranslationName(java.util.Set<String> names, String value) {
        if (value == null) return;
        String clean = value.trim();
        if (clean.length() >= 2 && !LocalRpProfile.UNKNOWN_NAME.equalsIgnoreCase(clean)) {
            names.add(clean);
        }
    }

    /**
     * Lehnsherr-Einblendungen dürfen vorhandene lokale RP-Namen auch dann nutzen,
     * wenn sie aus dem API-Abgleich stammen. Die Kennenlern-Sichtbarkeit anderer
     * Anzeigen bleibt davon unberührt.
     */
    static java.util.Optional<String> localRpNameForLeader(LocalRpProfile profile) {
        if (profile == null || !profile.hasRpName()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(profile.rpName.trim());
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

    static void setChatMeetHandler(java.util.function.BiConsumer<net.minecraft.client.gui.screen.Screen, LocalRpProfile> handler) {
        chatMeetHandler = handler;
    }

    public static boolean meetFromChat(net.minecraft.client.gui.screen.Screen parent, String account) {
        var handler = chatMeetHandler;
        if (!proactiveMeetEnabled() || handler == null || store == null) return false;
        LocalRpProfile profile = displayProfile(null, account).orElse(null);
        if (profile == null || isKnownForDisplay(profile)) return false;
        handler.accept(parent, profile);
        return true;
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
        return isPendingMeet(account, null);
    }

    public static boolean isPendingMeet(String account, String uuid) {
        if (account == null
                || store == null
                || !pendingMeet.contains(
                account.toLowerCase(java.util.Locale.ROOT)
        )) {
            return false;
        }

        LocalRpProfile profile =
                displayProfile(uuid, account).orElse(null);

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
                LocalRpProfile.isKnownRpName(rpName);

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

            String forcedAccount = ChatSpeakerClassifier.speakerComponentAccount(message);
            if (forcedAccount == null) {
                forcedAccount = speaker != null ? speaker.accountName() : speakerAccount(plain);
            }
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

            String componentSpeakerAccount =
                    ChatSpeakerClassifier.speakerComponentAccount(message);

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
                        componentSpeakerAccount != null
                                ? componentSpeakerAccount
                                : speakerAccount(plain)
                );
            }

            HoverIdentityParser.ParsedIdentity speakerIdentity =
                    speakerIdentity(identities, plain);

            String forcedSpeakerAccount = componentSpeakerAccount;
            if (forcedSpeakerAccount == null && speakerIdentity != null) {
                forcedSpeakerAccount = speakerIdentity.accountName();
            }

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
                        if (forcedSpeakerAccount == null) {
                            forcedSpeakerAccount = visibleSpeaker.accountName();
                        }

                        store.updateImportedIdentityIfChanged(
                                visibleSpeaker.accountName(),
                                visibleSpeaker.uuid(),
                                visibleSpeaker.rpName(),
                                serverTitleFor(visibleSpeaker.accountName())
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
