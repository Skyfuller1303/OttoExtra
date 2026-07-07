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
 * Extrahiert (Accountname, RP-Name, Titel) aus Chat-Hover-Komponenten.
 *
 * <p>Das Server-Format ist je Kanal invertiert (Stand 2026-07):</p>
 * <ul>
 *   <li><b>RP-Kanäle</b> (Sprechen/Reden/…): sichtbarer Knotentext = RP-Name,
 *       erste Hover-Zeile = {@code Titel Spielername} — Account ist das letzte
 *       Wort der Hover-Zeile (MC-Namen haben keine Leerzeichen).</li>
 *   <li><b>OOC-Kanäle</b>: sichtbarer Knotentext = Accountname, erste
 *       Hover-Zeile = {@code Titel RP-Name} (klassische Heuristik aus
 *       TextRpNameResolver: bekannter Titel als Präfix gewinnt, sonst
 *       Farbsegment-Wechsel, sonst alles = Name).</li>
 * </ul>
 * Der gesamte Parser ist exceptionsicher.
 */
public final class HoverIdentityParser {

    /** Gefundene Identität aus einem Hover. */
    public record ParsedIdentity(String accountName, String rpName, String title, String titleGroup) {
    }

    /** Gültiger Minecraft-Accountname (keine Leerzeichen, 3–16 Wortzeichen). */
    private static final java.util.regex.Pattern MC_NAME =
            java.util.regex.Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final TitleRegistry titles;

    public HoverIdentityParser(TitleRegistry titles) {
        this.titles = titles;
    }

    /**
     * Alle Identitäten aus einer Chat-Nachricht (rekursiv über Siblings).
     *
     * @param hoverHasAccount true für RP-Kanäle (Hover = Titel + Accountname,
     *                        sichtbar = RP-Name); false für OOC/klassisch
     *                        (Hover = Titel + RP-Name, sichtbar = Account).
     */
    public List<ParsedIdentity> parseMessage(Text message, boolean hoverHasAccount) {
        List<ParsedIdentity> out = new ArrayList<>(2);
        try {
            walk(message, out, hoverHasAccount);
        } catch (Throwable ignored) {
            // kaputter/fremder Hover darf nie werfen (Skill-Test)
        }
        return out;
    }

    /** Klassischer Modus (Hover = Titel + RP-Name). */
    public List<ParsedIdentity> parseMessage(Text message) {
        return parseMessage(message, false);
    }

    private void walk(Text node, List<ParsedIdentity> out, boolean hoverHasAccount) {
        Style style = node.getStyle();
        if (style != null && style.getHoverEvent() instanceof HoverEvent.ShowText shown) {
            String visible = plainOwnText(node).trim();
            if (!visible.isEmpty() && visible.length() <= 48) {
                Optional<ParsedIdentity> id = hoverHasAccount
                        ? parseHoverWithAccount(visible, shown.value())
                        : parseHover(visible, shown.value());
                id.ifPresent(out::add);
            }
        }
        for (Text sibling : node.getSiblings()) {
            walk(sibling, out, hoverHasAccount);
        }
    }

    /**
     * RP-Kanal-Modus: erste Hover-Zeile = {@code Titel Spielername}, der
     * sichtbare Knotentext ist der RP-Name (ggf. mit Titel-Präfix).
     */
    Optional<ParsedIdentity> parseHoverWithAccount(String visibleName, Text hover) {
        List<Segment> segments = firstLineSegments(hover);
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        String firstLine = join(segments).trim();
        if (firstLine.isEmpty()) {
            return Optional.empty();
        }
        // Account = letztes Wort (MC-Namen sind leerzeichenfrei), Titel = Rest
        int lastSpace = firstLine.lastIndexOf(' ');
        String account = lastSpace >= 0 ? firstLine.substring(lastSpace + 1).trim() : firstLine;
        String rawTitle = lastSpace >= 0 ? firstLine.substring(0, lastSpace).trim() : "";
        if (!MC_NAME.matcher(account).matches()) {
            return Optional.empty();
        }
        String title = null;
        String group = null;
        if (!rawTitle.isEmpty()) {
            Optional<TitleRegistry.ResolvedTitle> known = titles.findPrefix(rawTitle);
            if (known.isPresent()) {
                title = known.get().title();
                group = known.get().groupKey();
            } else {
                title = rawTitle;
            }
        }
        // Sichtbaren RP-Namen von einem evtl. Titel-Präfix befreien
        String rpName = visibleName;
        Optional<TitleRegistry.ResolvedTitle> visTitle = titles.findPrefix(rpName);
        if (visTitle.isPresent()) {
            rpName = stripPrefixWords(rpName, visTitle.get().title());
        } else if (title != null && startsWithWord(rpName, title)) {
            rpName = stripPrefixWords(rpName, title);
        }
        rpName = rpName.trim();
        if (rpName.isEmpty() || rpName.equalsIgnoreCase(account)) {
            return Optional.empty();
        }
        return Optional.of(new ParsedIdentity(account, rpName, title, group));
    }

    private static boolean startsWithWord(String text, String prefix) {
        return text.length() > prefix.length()
                && text.regionMatches(true, 0, prefix, 0, prefix.length())
                && text.charAt(prefix.length()) == ' ';
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
