package de.ottoextra.resourcepack;

/**
 * Versions-Manifest des Server-Resourcepacks.
 *
 * <p>Feldnamen entsprechen dem JSON. Tolerant: zusätzliche Felder werden ignoriert,
 * optionale Felder dürfen fehlen (null).</p>
 *
 * <pre>{@code
 * { "version": "2026.06.11-3",
 *   "url": "https://host/ottoextra_2026.06.11-3.zip",
 *   "sha256": "a1b2...",
 *   "sizeBytes": 18432311,
 *   "packFormat": 0,
 *   "notes": "Neue Lehen-Banner" }
 * }</pre>
 */
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
