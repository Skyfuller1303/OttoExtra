package de.ottoextra.api;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Baut die Routen der multiplexten Regions-API ({@code api/index.php?action=...}).
 *
 * <p>Einziger Ort, an dem rohe URLs entstehen. Feature-Module sehen
 * nur Services, nie diese Klasse.</p>
 */
public final class OttoExtraApiRoutes {

    private final String base; // normalisiert, endet ohne Slash

    public OttoExtraApiRoutes(String baseUrl) {
        String b = (baseUrl == null || baseUrl.isBlank())
                ? "https://regions.skyfuller.de/"
                : baseUrl.trim();
        // https ist Pflicht — http hart reparieren
        if (b.startsWith("http://")) {
            b = "https://" + b.substring("http://".length());
        }
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        this.base = b;
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

    // ---- v2 (authentifizierte API) ---------

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

    /** Löst einen relativen Pfad (z. B. banner_path) gegen die Basis-URL auf. */
    public URI resolveRelative(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return URI.create(relativePath);
        }
        String p = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return URI.create(base + p);
    }

    public String baseUrl() {
        return base;
    }
}
