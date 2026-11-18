package de.ottoextra.resources;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Counts server actionbar packets in a mining context, never render frames or mining clicks. */
final class ResourceActionbarTracker {
    static final long INTERACTION_WINDOW_MILLIS = 5_000;
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-orx]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\p{Z}]+");
    private Target target;
    private long lastInteraction;

    record Target(String id, String name, String type, String dimension, String region) {
        Target(String id, String name, String type, String dimension) {
            this(id, name, type, dimension, "");
        }
    }
    record Observation(Target target, String message) { }

    void interact(Target target, long now) {
        this.target = target;
        lastInteraction = now;
    }

    Observation receive(String rawMessage, String dimension, long now) {
        String message = normalize(rawMessage);
        // Consecutive attempts can produce exactly the same text. Deduplicating that text
        // collapses success/failure streaks into alternating results and biases the rate to 50%.
        // receive is called only for incoming server packets, not while the HUD renders them.
        if (message.isEmpty() || context(dimension, now) == null) return null;
        return new Observation(target, message);
    }

    Target context(String dimension, long now) {
        return target == null || !target.dimension().equals(dimension) || now < lastInteraction
                || now - lastInteraction > INTERACTION_WINDOW_MILLIS ? null : target;
    }

    void reset() {
        target = null;
        lastInteraction = 0;
    }

    static String normalize(String message) {
        if (message == null) return "";
        String text = Normalizer.normalize(message, Normalizer.Form.NFC);
        text = FORMATTING.matcher(text).replaceAll("");
        text = text.replace("\u200B", "").replace("\uFEFF", "");
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }
}
