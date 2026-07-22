package de.ottoextra.rpnames.chat;

import de.ottoextra.logging.DebugLog;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public final class HoverDebug {

    private static final int MAX_OUTPUT_CHARS = 8_192;
    private static final int MAX_TEXT_NODES = 100;
    private static final int MAX_HOVERS = 25;
    private static final int MAX_IDENTITIES = 25;
    private static final String TRUNCATED = "\n[Ausgabe gekürzt]";

    private HoverDebug() {
    }

    public static boolean isEnabled() {
        return DebugLog.isEnabled();
    }

    public static void setEnabled(boolean value) {
        DebugLog.setEnabled(value);
    }

    public static void dump(Text message) {
        if (!DebugLog.isLogging() || message == null) {
            return;
        }
        try {
            StringBuilder output = new StringBuilder();
            WalkState state = new WalkState();
            appendLimited(output, "Nachricht: \"" + limit(message.getString(), 2_048) + "\"\n");
            if (!appendLimited(output, "Struktur: " + toJson(message) + "\n")) {
                state.truncated = true;
            }
            if (!state.truncated) {
                walk(message, output, state);
            }
            if (state.truncated) {
                output.append(TRUNCATED);
            }
            DebugLog.debug("[rpnames-hoverdebug]\n{}", output);
        } catch (Throwable t) {
            DebugLog.debug("[rpnames-hoverdebug] Dump-Fehler: {}", t.toString());
        }
    }

    public static void logParsed(List<HoverIdentityParser.ParsedIdentity> ids) {
        if (!DebugLog.isLogging()) {
            return;
        }
        if (ids == null || ids.isEmpty()) {
            DebugLog.debug("[rpnames-hoverdebug] Parser-Ergebnis: keine Identität erkannt.");
            return;
        }

        StringBuilder output = new StringBuilder("Parser-Ergebnis: ");
        int limit = Math.min(ids.size(), MAX_IDENTITIES);
        for (int i = 0; i < limit; i++) {
            HoverIdentityParser.ParsedIdentity id = ids.get(i);
            if (i > 0) {
                appendLimited(output, " | ");
            }
            if (!appendLimited(output, "account=\"" + id.accountName()
                    + "\" rpName=\"" + id.rpName()
                    + "\" title=\"" + id.title()
                    + "\" group=\"" + id.titleGroup() + "\"")) {
                break;
            }
        }
        if (ids.size() > limit || output.length() >= MAX_OUTPUT_CHARS - TRUNCATED.length()) {
            output.append(TRUNCATED);
        }
        DebugLog.debug("[rpnames-hoverdebug] {}", output);
    }

    private static void walk(Text node, StringBuilder output, WalkState state) {
        if (state.truncated || ++state.nodes > MAX_TEXT_NODES) {
            state.truncated = true;
            return;
        }

        Style style = node.getStyle();
        HoverEvent hover = style != null ? style.getHoverEvent() : null;
        if (hover != null) {
            if (++state.hovers > MAX_HOVERS) {
                state.truncated = true;
                return;
            }
            if (!appendLimited(output, "--- Hover #" + state.hovers
                    + " auf Knotentext \"" + limit(ownText(node), 512) + "\"\n"
                    + "    Event-Typ: " + hover.getClass().getName() + "\n")) {
                state.truncated = true;
                return;
            }
            if (hover instanceof HoverEvent.ShowText shown) {
                dumpSegments(shown.value(), output, state);
                if (!state.truncated && !appendLimited(output,
                        "    JSON: " + toJson(shown.value()) + "\n")) {
                    state.truncated = true;
                }
            } else if (!appendLimited(output,
                    "    (kein ShowText — aktueller Parser ignoriert diesen Hover)\n")) {
                state.truncated = true;
            }
        }
        for (Text sibling : node.getSiblings()) {
            walk(sibling, output, state);
            if (state.truncated) {
                return;
            }
        }
    }

    private static void dumpSegments(Text hover, StringBuilder output, WalkState state) {
        int[] index = {0};
        hover.visit((style, content) -> {
            String segment = "    Segment " + index[0]++
                    + ": \"" + content.replace("\n", "\\n") + '"'
                    + " color=" + style.getColor()
                    + (style.isBold() ? " bold" : "")
                    + (style.isItalic() ? " italic" : "")
                    + (style.isUnderlined() ? " underline" : "")
                    + '\n';
            if (!appendLimited(output, segment)) {
                state.truncated = true;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (index[0] == 0) {
            state.truncated = !appendLimited(output, "    (Hover-Text ohne Segmente/leer)\n");
        }
    }

    private static boolean appendLimited(StringBuilder output, String value) {
        int contentLimit = MAX_OUTPUT_CHARS - TRUNCATED.length();
        int remaining = contentLimit - output.length();
        if (remaining <= 0) {
            return false;
        }
        if (value.length() <= remaining) {
            output.append(value);
            return true;
        }
        output.append(value, 0, remaining);
        return false;
    }

    private static String limit(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "…";
    }

    private static String ownText(Text node) {
        StringBuilder output = new StringBuilder();
        node.getContent().visit(value -> {
            output.append(value);
            return Optional.empty();
        });
        return output.toString();
    }

    private static String toJson(Text text) {
        try {
            var handler = net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler();
            if (handler == null) {
                return "(kein Netzwerk-Handler)";
            }
            var ops = net.minecraft.registry.RegistryOps.of(
                    com.mojang.serialization.JsonOps.INSTANCE, handler.getRegistryManager());
            return net.minecraft.text.TextCodecs.CODEC.encodeStart(ops, text)
                    .result().map(Object::toString)
                    .orElse("(Encode fehlgeschlagen)");
        } catch (Throwable t) {
            return "(JSON-Serialisierung fehlgeschlagen: " + t + ")";
        }
    }

    private static final class WalkState {
        int nodes;
        int hovers;
        boolean truncated;
    }
}
