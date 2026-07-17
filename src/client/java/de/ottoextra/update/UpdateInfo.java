package de.ottoextra.update;
public record UpdateInfo(
        String currentVersion,
        String latestVersion,
        String releaseUrl
) {
}
