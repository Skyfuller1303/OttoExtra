package de.ottoextra.regions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Bounds decoded banner memory, evicting unused entries instead of refusing every new banner. */
final class BannerTextureBudget {
    static final int MAX_TEXTURES = 256;
    static final long MAX_BYTES = 32L * 1024 * 1024;

    private final int maxTextures;
    private final long maxBytes;
    private final LinkedHashMap<String, Long> sizes = new LinkedHashMap<>(16, 0.75f, true);
    private long bytes;

    BannerTextureBudget() { this(MAX_TEXTURES, MAX_BYTES); }

    BannerTextureBudget(int maxTextures, long maxBytes) {
        if (maxTextures <= 0 || maxBytes <= 0) throw new IllegalArgumentException("Positive banner budget required");
        this.maxTextures = maxTextures;
        this.maxBytes = maxBytes;
    }

    synchronized void touch(String key) { sizes.get(key); }

    /** Returns keys whose owned native/GPU textures the caller must release on the render thread. */
    synchronized List<String> retain(String key, long size) {
        if (size < 0 || size > maxBytes) throw new IllegalArgumentException("Banner exceeds memory budget");
        Long previous = sizes.put(key, size);
        bytes += size - (previous == null ? 0 : previous);
        List<String> evicted = new ArrayList<>();
        var iterator = sizes.entrySet().iterator();
        while (sizes.size() > maxTextures || bytes > maxBytes) {
            var oldest = iterator.next();
            bytes -= oldest.getValue();
            evicted.add(oldest.getKey());
            iterator.remove();
        }
        return evicted;
    }
}
