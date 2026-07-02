package de.ottoextra.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Split-Logik für lange Nachrichten inkl. RP-Span-Fortführung (*…* / (…)). */
class LongChatSenderSplitTest {

    @Test
    void keinSplitUnterLimit() {
        assertEquals(List.of("hallo welt"), LongChatSender.split("hallo welt", 50, " >"));
    }

    @Test
    void splitAnWortgrenzeMitMarker() {
        List<String> parts = LongChatSender.split("aaa bbb ccc ddd eee", 10, " >");
        for (int i = 0; i < parts.size() - 1; i++) {
            assertTrue(parts.get(i).endsWith(" >"), "Marker fehlt: " + parts.get(i));
            assertTrue(parts.get(i).length() <= 10, "zu lang: " + parts.get(i));
        }
        assertEquals("aaa bbb ccc ddd eee",
                String.join(" ", parts.stream().map(p -> p.replace(" >", "")).toList()));
    }

    @Test
    void offenerSternSpanWirdGeschlossenUndWiederGeoeffnet() {
        // Span beginnt im ersten Teil, endet im zweiten
        List<String> parts = LongChatSender.split("er sagt *und dann passiert etwas ganz tolles*", 30, " >");
        assertTrue(parts.size() >= 2);
        String first = parts.get(0);
        String second = parts.get(1);
        assertTrue(countChar(first, '*') % 2 == 0, "Stern im ersten Teil nicht geschlossen: " + first);
        assertTrue(second.startsWith("*"), "Stern im zweiten Teil nicht wieder geöffnet: " + second);
        assertTrue(countChar(second, '*') % 2 == 0, "Stern im zweiten Teil nicht geschlossen: " + second);
    }

    @Test
    void offeneKlammerWirdGeschlossenUndWiederGeoeffnet() {
        List<String> parts = LongChatSender.split("bla bla (das hier ist offtopic und ziemlich lang)", 30, " >");
        assertTrue(parts.size() >= 2);
        String first = parts.get(0);
        String second = parts.get(1);
        assertEquals(countChar(first, '('), countChar(first, ')'), "Klammer im ersten Teil offen: " + first);
        assertTrue(second.startsWith("("), "Klammer im zweiten Teil nicht wieder geöffnet: " + second);
        assertEquals(countChar(second, '('), countChar(second, ')'), "Klammer im zweiten Teil offen: " + second);
    }

    @Test
    void geschlosseneSpansBleibenUnveraendert() {
        // Span komplett im ersten Teil -> kein zusätzliches Zeichen
        List<String> parts = LongChatSender.split("*kurz* und dann noch ganz viel weiterer text hier", 30, " >");
        assertTrue(parts.get(0).startsWith("*kurz*"));
        assertEquals(0, countChar(parts.get(parts.size() - 1), '*'));
    }

    @Test
    void spanUeberDreiTeileBleibtDurchgehend() {
        String msg = "*" + "wort ".repeat(20).strip() + "*";
        List<String> parts = LongChatSender.split(msg, 25, " >");
        assertTrue(parts.size() >= 3);
        for (String p : parts) {
            assertTrue(countChar(p, '*') % 2 == 0, "Stern offen in: " + p);
        }
    }

    @Test
    void limitBleibtTrotzSchliesserEingehalten() {
        String msg = "(x " + "wort ".repeat(30).strip() + ")";
        for (String p : LongChatSender.split(msg, 25, " >")) {
            assertTrue(p.length() <= 25, "über Limit (" + p.length() + "): " + p);
        }
    }

    @Test
    void ueberzaehligeSchliessklammerIgnoriert() {
        List<String> parts = LongChatSender.split("komisch) aber ok und noch mehr text dahinter dran", 25, " >");
        // darf nicht crashen und keine Phantom-Klammern erzeugen
        assertTrue(parts.get(parts.size() - 1).indexOf('(') < 0);
    }

    private static int countChar(String s, char c) {
        return (int) s.chars().filter(x -> x == c).count();
    }
}
