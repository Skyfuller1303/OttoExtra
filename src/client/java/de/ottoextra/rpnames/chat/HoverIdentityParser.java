package de.ottoextra.rpnames.chat;

import de.ottoextra.rpnames.title.TitleRegistry;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extrahiert (Accountname, RP-Name, Titel) aus Chat-Hover-Komponenten —
 * Heuristik aus dem Bestand (TextRpNameResolver):
 * sichtbarer Knotentext = Accountname; erste Hover-Zeile liefert Titel+Name;
 * bekannter Titel als Präfix gewinnt, sonst Farbsegment-Wechsel, sonst Split
 * am ersten Leerzeichen. Der gesamte Parser ist exceptionsicher.
 */
public final class HoverIdentityParser {

    /** Gefundene Identität aus einem Hover. */
    public record ParsedIdentity(String accountName, String rpName, String title, String titleGroup) {
    }

    private final TitleRegistry titles;

    public HoverIdentityParser(TitleRegistry titles) {
        this.titles = titles;
    }

    /** Alle Identitäten aus einer Chat-Nachricht (rekursiv über Siblings). */
    public List<ParsedIdentity> parseMessage(Text message) {
        List<ParsedIdentity> out = new ArrayList<>(2);
        try {
            walk(message, out);
        } catch (Throwable ignored) {
            // kaputter/fremder Hover darf nie werfen (Skill-Test)
        }
        return out;
    }

    private void walk(Text node, List<ParsedIdentity> out) {
        Style style = node.getStyle();
        if (style != null && style.getHoverEvent() instanceof HoverEvent.ShowText shown) {
            String account = plainOwnText(node).trim();
            if (!account.isEmpty() && account.length() <= 32) {
                parseHover(account, shown.value()).ifPresent(out::add);
            }
        }
        for (Text sibling : node.getSiblings()) {
            walk(sibling, out);
        }
    }

    /** Nur der eigene Inhalt des Knotens (ohne Siblings). */
    private static String plainOwnText(Text node) {
        StringBuilder sb = new StringBuilder();
        node.getContent().visit(s -> {
            sb.append(s);
            return Optional.empty();
        });
        return sb.toString();
    }

    Optional<ParsedIdentity> parseHover(String accountName, Text hover) {
        List<Segment> segments = firstLineSegments(hover);
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        String firstLine = join(segments).trim();
        if (firstLine.isEmpty()) {
            return Optional.empty();
        }
        String title = null;
        String group = null;
        String rpName;

        Optional<TitleRegistry.ResolvedTitle> known = titles.findPrefix(firstLine);
        if (known.isPresent()) {
            title = known.get().title();
            group = known.get().groupKey();
            rpName = stripPrefixWords(firstLine, title);
        } else if (segments.size() >= 2 && differentColors(segments.get(0), segments.get(1))) {
            // Farbsegment-Heuristik: erstes Segment = Titel, Rest = Name
            String candidate = segments.get(0).text.trim();
            String rest = join(segments.subList(1, segments.size())).trim();
            if (!candidate.isEmpty() && !rest.isEmpty() && candidate.split(" ").length <= 2) {
                title = candidate;
                rpName = rest;
            } else {
                rpName = firstLine;
            }
        } else {
            rpName = firstLine;
        }

        rpName = rpName == null ? "" : rpName.trim();
        if (rpName.isEmpty() || rpName.equalsIgnoreCase(accountName)) {
            return Optional.empty();
        }
        return Optional.of(new ParsedIdentity(accountName, rpName, title, group));
    }

    private static String stripPrefixWords(String line, String title) {
        // Titel kann umlaut-variant geschrieben sein — wortweise überspringen
        int words = title.split(" ").length;
        String[] parts = line.split(" ");
        if (parts.length <= words) {
            return "";
        }
        return String.join(" ", java.util.Arrays.copyOfRange(parts, words, parts.length));
    }

    // ---- Segmentierung der ersten Hover-Zeile nach Farben ----------------------

    private record Segment(String text, TextColor color) {
    }

    private static boolean differentColors(Segment a, Segment b) {
        if (a.color == null && b.color == null) {
            return false;
        }
        return a.color == null || !a.color.equals(b.color);
    }

    private static String join(List<Segment> segments) {
        StringBuilder sb = new StringBuilder();
        for (Segment s : segments) {
            sb.append(s.text);
        }
        return sb.toString();
    }

    private static List<Segment> firstLineSegments(Text hover) {
        List<Segment> out = new ArrayList<>(4);
        boolean[] stop = {false};
        hover.visit((style, content) -> {
            if (stop[0]) {
                return Optional.of(true);
            }
            int nl = content.indexOf('\n');
            String part = nl >= 0 ? content.substring(0, nl) : content;
            if (!part.isEmpty()) {
                TextColor color = style != null ? style.getColor() : null;
                if (!out.isEmpty() && !differentColors(
                        out.get(out.size() - 1), new Segment(part, color))) {
                    Segment last = out.remove(out.size() - 1);
                    out.add(new Segment(last.text + part, last.color));
                } else {
                    out.add(new Segment(part, color));
                }
            }
            if (nl >= 0) {
                stop[0] = true;
                return Optional.of(true);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
