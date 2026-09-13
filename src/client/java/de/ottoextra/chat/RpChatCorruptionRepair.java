package de.ottoextra.chat;

import de.ottoextra.config.OttoExtraConfig;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.List;

/** Repairs the nested-OOC duplicate only when Minecraft supplies the signed original. */
final class RpChatCorruptionRepair {
    private RpChatCorruptionRepair() {
    }

    static Text repair(Text message, @Nullable MessageSignatureData signature,
                       @Nullable MessageIndicator indicator) {
        if (message == null || signature == null) return message;
        try {
            OttoExtraConfig.Chat config = OttoExtraConfig.active().chat;
            if (config == null || !config.enabled || !config.rpFormattingEnabled) return message;
            String signedOriginal = originalText(indicator);
            if (signedOriginal == null) return message;

            String plain = message.getString();
            int bodyStart = RpChatFormatter.findBodyStart(plain);
            if (bodyStart < 0 || !RpChatFormatter.isRpChannelLine(plain, bodyStart)) return message;
            String body = plain.substring(bodyStart);
            int keep = repairedLength(body, signedOriginal);
            if (keep < 0) return message;

            // Cut a copy of the component tree, preserving heads, crests, hover and click events.
            Text repaired = prefix(message, bodyStart + keep);
            return repaired.getString().equals(plain.substring(0, bodyStart + keep)) ? repaired : message;
        } catch (RuntimeException ignored) {
            // Unknown component/indicator layouts must never cause text loss or block chat.
            return message;
        }
    }

    /** Structure produced by Minecraft 1.21.11's MessageIndicator.modified(signedContent). */
    private static @Nullable String originalText(@Nullable MessageIndicator indicator) {
        if (indicator == null || indicator.icon() != MessageIndicator.Icon.CHAT_MODIFIED
                || !"Modified".equals(indicator.loggedName()) || indicator.text() == null) return null;
        Text tooltip = indicator.text();
        List<Text> parts = tooltip.getSiblings();
        if (!(tooltip.getContent() instanceof PlainTextContent root) || !root.string().isEmpty()
                || parts.size() != 3
                || !(parts.getFirst().getContent() instanceof TranslatableTextContent label)
                || !"chat.tag.modified".equals(label.getKey()) || label.getArgs().length != 0
                || !parts.getFirst().getSiblings().isEmpty()
                || !(parts.get(1).getContent() instanceof PlainTextContent separator)
                || !"\n".equals(separator.string()) || !parts.get(1).getSiblings().isEmpty()
                || !(parts.getLast().getContent() instanceof PlainTextContent original)
                || !parts.getLast().getSiblings().isEmpty()) return null;
        return original.string();
    }

    private static int repairedLength(String body, String signedOriginal) {
        int end = oocEnd(body);
        if (end < 0 || end == body.length()) return -1;
        String kept = body.substring(0, end);
        String duplicate = body.substring(end);
        int duplicateStart = kept.length() - duplicate.length();
        if (duplicateStart <= 0 || !duplicate.startsWith("*") || !kept.endsWith(duplicate)) return -1;

        BitSet delimiters = RpChatSyntax.pairedEmoteDelimiters(kept);
        RpChatSyntax.State before = RpChatSyntax.scan(kept, duplicateStart, delimiters);
        if (!delimiters.get(duplicateStart) || before.emote() || before.oocDepth() == 0) return -1;

        // The server may display (( OOC )) as ( OOC ). Apart from that outer wrapper,
        // every character must agree with the original; never infer a correction from repetition alone.
        String originalCore = oocCore(signedOriginal);
        return originalCore != null && originalCore.equals(oocCore(kept)) ? end : -1;
    }

    private static @Nullable String oocCore(String text) {
        String core = text.strip();
        if (oocEnd(core) != core.length()) return null;
        do {
            core = core.substring(1, core.length() - 1).strip();
        } while (!core.isEmpty() && oocEnd(core) == core.length());
        return core;
    }

    /** Exclusive end of the first complete outer OOC block, respecting nested emotes. */
    private static int oocEnd(String text) {
        if (!text.startsWith("(")) return -1;
        BitSet delimiters = RpChatSyntax.pairedEmoteDelimiters(text);
        RpChatSyntax.State state = RpChatSyntax.State.normal();
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c == '§' && index + 1 < text.length()) {
                index++;
                continue;
            }
            state = RpChatSyntax.step(state, c, delimiters.get(index)).after();
            if (state.stack().isEmpty()) return index + 1;
        }
        return -1;
    }

    private static Text prefix(Text node, int length) {
        if (length >= node.getString().length()) return node.copy();
        if (length <= 0) return Text.empty();
        MutableText own = MutableText.of(node.getContent()).setStyle(node.getStyle());
        int ownLength = own.getString().length();
        if (length < ownLength) {
            if (node.getContent() instanceof PlainTextContent literal) {
                return Text.literal(literal.string().substring(0, length)).setStyle(node.getStyle());
            }
            if (node.getContent() instanceof TranslatableTextContent translated
                    && "%s".equals(translated.getKey()) && translated.getArgs().length == 1
                    && translated.getArgs()[0] instanceof Text argument) {
                return MutableText.of(new TranslatableTextContent(translated.getKey(),
                        translated.getFallback(), new Object[]{prefix(argument, length)}))
                        .setStyle(node.getStyle());
            }
            throw new IllegalArgumentException("Unsupported partial chat component");
        }
        int remaining = length - ownLength;
        for (Text sibling : node.getSiblings()) {
            if (remaining <= 0) break;
            own.append(prefix(sibling, remaining));
            remaining -= sibling.getString().length();
        }
        return own;
    }
}
