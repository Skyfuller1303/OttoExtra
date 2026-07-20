package de.ottoextra.api;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class OttoExtraApiRoutes {

    private static final String DEFAULT_BASE_URL = "https://api.ottoextra.dev";
    private static final String LEGACY_BASE_URL = "https://regions.skyfuller.de";

    private final String base;

    public OttoExtraApiRoutes(String baseUrl) {
        String b = (baseUrl == null || baseUrl.isBlank())
                ? DEFAULT_BASE_URL
                : baseUrl.trim();

        if (b.startsWith("http://") && !isLoopbackHttp(b)) {
            b = "https://" + b.substring("http://".length());
        }
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        this.base = migrateLegacyUrl(b);
    }

    private URI action(String action) {
        return URI.create(base + "/api/index.php?action=" + action);
    }

    private URI action(String action, String key, String value) {
        return URI.create(base + "/api/index.php?action=" + action
                + "&" + key + "=" + enc(value));
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    public URI bootstrap() {
        return action("public-bootstrap");
    }

    public URI sync(long since) {
        return action("public-sync", "since", Long.toString(since));
    }

    public URI regionList() {
        return action("public-region-list");
    }

    public URI faction(String uuid) {
        return action("public-faction", "uuid", uuid);
    }

    public URI factionPlayers(String factionUuid) {
        return action("public-faction-players", "uuid", factionUuid);
    }

    public URI player(String uuid) {
        return action("public-player", "uuid", uuid);
    }

    public URI regionByName(String name) {
        return action("public-region", "name", name);
    }

    public URI compactPlayers() {
        return action("public-player-compact");
    }

    public URI playerHeadInfo(String uuid) {
        return action("public-player-head-info", "uuid", uuid);
    }

    public URI playerHead(String uuid) {
        return action("public-player-head", "uuid", uuid);
    }

    public URI communityParticipantRpName() {
        return action("community-participant-rp-name");
    }

    private URI v2(String path) {
        return URI.create(base + "/v2/" + path);
    }

    public URI v2AuthChallenge() {
        return v2("auth/challenge");
    }

    public URI v2AuthVerify() {
        return v2("auth/verify");
    }

    public URI v2Bootstrap() {
        return v2("bootstrap");
    }

    public URI v2Sync(long since) {
        return v2("sync?since=" + since);
    }

    public URI v2RegionList() {
        return v2("region-list");
    }

    public URI v2Faction(String uuid) {
        return v2("faction/" + enc(uuid));
    }

    public URI v2FactionPlayers(String factionUuid) {
        return v2("faction/" + enc(factionUuid) + "/players");
    }

    public URI v2Player(String uuid) {
        return v2("player/" + enc(uuid));
    }

    public URI v2RegionByName(String name) {
        return v2("region/" + enc(name));
    }

    public URI v2CompactPlayers() {
        return v2("player-compact");
    }

    private static boolean isLoopbackHttp(String value) {
        try {
            String host = URI.create(value).getHost();
            return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)
                    || "::1".equals(host);
        } catch (Exception ignored) {
            return false;
        }
    }

    public URI v2ProtectedChatMessages() {
        return v2("chat-translations");
    }

    public URI v2ProtectedChatMessage(String id) {
        return v2("chat-translations/" + enc(id));
    }

    public URI v2ProtectedChatInbox() {
        return v2("chat-translations/inbox");
    }

    public URI resolveRelative(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return URI.create(migrateLegacyUrl(relativePath));
        }
        String p = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return URI.create(base + p);
    }

    public String baseUrl() {
        return base;
    }

    private static String migrateLegacyUrl(String url) {
        if (url.equalsIgnoreCase(LEGACY_BASE_URL)) {
            return DEFAULT_BASE_URL;
        }
        if (url.regionMatches(true, 0, LEGACY_BASE_URL + "/", 0, LEGACY_BASE_URL.length() + 1)) {
            return DEFAULT_BASE_URL + url.substring(LEGACY_BASE_URL.length());
        }
        return url;
    }
}
