package de.ottoextra.rpnames.chat;

import de.ottoextra.OttoExtra;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public final class HoverDebug {

    private static volatile boolean enabled;

    private HoverDebug() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void dump(Text message) {
        if (!enabled || message == null) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            int[] count = {0};
            walk(message, sb, count);
            if (count[0] > 0) {
                OttoExtra.LOGGER.info(
                        "[rpnames-hoverdebug] Nachricht \"{}\" — {} Hover:\n{}",
                        message.getString(), count[0], sb);
            }
        } catch (Throwable t) {
            OttoExtra.LOGGER.info("[rpnames-hoverdebug] Dump-Fehler: {}", t.toString());
        }
    }

    public static void logParsed(List<HoverIdentityParser.ParsedIdentity> ids) {
        if (!enabled) {
            return;
        }
        if (ids == null || ids.isEmpty()) {
            OttoExtra.LOGGER.info(
                    "[rpnames-hoverdebug] Parser-Ergebnis: KEINE Identität erkannt"
                            + " — Kennenlernen bekommt keinen Prefill.");
            return;
        }
        for (HoverIdentityParser.ParsedIdentity id : ids) {
            OttoExtra.LOGGER.info(
                    "[rpnames-hoverdebug] Parser-Ergebnis: account=\"{}\" rpName=\"{}\""
                            + " title=\"{}\" group=\"{}\"",
                    id.accountName(), id.rpName(), id.title(), id.titleGroup());
        }
    }

    private static void walk(Text node, StringBuilder sb, int[] count) {
        Style style = node.getStyle();
        HoverEvent hover = style != null ? style.getHoverEvent() : null;
        if (hover != null) {
            count[0]++;
            sb.append("--- Hover #").append(count[0])
                    .append(" auf Knotentext \"").append(ownText(node)).append("\"\n");
            sb.append("    Event-Typ: ").append(hover.getClass().getName()).append('\n');
            if (hover instanceof HoverEvent.ShowText shown) {
                dumpSegments(shown.value(), sb);
                sb.append("    JSON: ").append(toJson(shown.value())).append('\n');
            } else {
                sb.append("    (kein ShowText — aktueller Parser ignoriert diesen Hover)\n");
            }
        }
        for (Text sibling : node.getSiblings()) {
            walk(sibling, sb, count);
        }
    }

    private static void dumpSegments(Text hover, StringBuilder sb) {
        int[] i = {0};
        hover.visit((style, content) -> {
            sb.append("    Segment ").append(i[0]++)
                    .append(": \"").append(content.replace("\n", "\\n")).append('"')
                    .append(" color=").append(style.getColor())
                    .append(style.isBold() ? " bold" : "")
                    .append(style.isItalic() ? " italic" : "")
                    .append(style.isUnderlined() ? " underline" : "")
                    .append('\n');
            return Optional.empty();
        }, Style.EMPTY);
        if (i[0] == 0) {
            sb.append("    (Hover-Text ohne Segmente/leer)\n");
        }
    }

    private static String ownText(Text node) {
        StringBuilder sb = new StringBuilder();
        node.getContent().visit(s -> {
            sb.append(s);
            return Optional.empty();
        });
        return sb.toString();
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
}
