package de.ottoextra.regions;

/** Pure decisions about whether a cached banner still matches the API's current banner path. */
final class BannerSourcePolicy {
    private BannerSourcePolicy() {}

    /** Trims surrounding whitespace; null becomes empty. */
    static String normalize(String path) {
        return path == null ? "" : path.strip();
    }

    /**
     * True when the source of the cached texture is known, the API now names a banner path,
     * and the two differ. An unknown source keeps the regular timed refresh.
     */
    static boolean needsImmediateRefresh(String knownSource, String currentPath) {
        String known = normalize(knownSource);
        String current = normalize(currentPath);
        return !known.isEmpty() && !current.isEmpty() && !known.equals(current);
    }
}
