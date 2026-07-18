package de.ottoextra.resourcepack;

public record PackManifest(
        String version,
        String url,
        String sha256,
        Long sizeBytes,
        Integer packFormat,
        Integer minPackFormat,
        String notes
) {
    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    public boolean hasSha() {
        return sha256 != null && !sha256.isBlank();
    }
}
