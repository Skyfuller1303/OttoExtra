package de.ottoextra.ailments;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * Validates the configurable base URL of the conditions API. Only https is accepted, plain http
 * only for localhost. The device token belongs to ottoextra.dev, so any other remote host is refused
 * as well (as the account link does).
 */
public final class ConditionEndpoint {

    public static final String DEFAULT_BASE = "https://ottoextra.dev";

    private ConditionEndpoint() {
    }

    public static boolean isLocal(String host) {
        return host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")
                || host.equals("[::1]") || host.equals("::1"));
    }

    /** The base URI without trailing slash, or empty if it is not acceptable. */
    public static Optional<URI> base(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            return Optional.empty();
        }
        boolean local = isLocal(host);
        boolean ok = local ? (scheme.equals("http") || scheme.equals("https"))
                : scheme.equals("https") && host.equalsIgnoreCase("ottoextra.dev")
                        && (uri.getPort() == -1 || uri.getPort() == 443);
        if (!ok) return Optional.empty();
        String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
        return Optional.of(URI.create(scheme + "://" + uri.getRawAuthority() + path));
    }

    public static Optional<URI> resolve(String base, String path) {
        return base(base).map(b -> URI.create(b.toString() + path));
    }
}
