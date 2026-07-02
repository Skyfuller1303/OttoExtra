package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lange Chatnachrichten (OttoPlus-Stil): eine einzeilig getippte Nachricht wird
 * beim Senden an Wortgrenzen in mehrere Teilstücke ≤ {@code chunk} gesplittet
 * und gestaffelt gesendet, sodass sie zusammen wie eine große Nachricht wirken.
 *
 * <p>Nicht-letzte Teilstücke erhalten einen Fortsetzungs-Marker (z. B. {@code " >"});
 * dafür wird beim Schnitt ein Wort zurückgewichen, damit Platz bleibt. Der
 * Versand erfolgt tickbasiert mit konfigurierbarem Versatz, damit die Reihenfolge
 * server­seitig erhalten bleibt.</p>
 */
public final class LongChatSender {

    private static final Deque<String> queue = new ArrayDeque<>();
    private static int cooldown = 0;
    private static int delayTicks = 4;

    private LongChatSender() {
    }

    /** Intervall in Millisekunden -> Ticks (20 TPS, 1 Tick ≈ 50 ms). */
    public static synchronized void configureMs(int delayMs) {
        delayTicks = Math.max(1, Math.round(delayMs / 50.0f));
    }

    /** Teilstücke einreihen (Versand übernimmt {@link #tick}). */
    public static synchronized void enqueue(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        queue.addAll(chunks);
        cooldown = 0; // erstes Stück sofort beim nächsten Tick
    }

    /** Pro Tick max. ein Teilstück senden (gestaffelt). */
    public static synchronized void tick(MinecraftClient client) {
        if (queue.isEmpty()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (client == null || client.getNetworkHandler() == null) {
            queue.clear();
            return;
        }
        String msg = queue.poll();
        try {
            client.getNetworkHandler().sendChatMessage(msg);
        } catch (Throwable ignored) {
            // Versand best effort — Chat darf nie brechen
        }
        cooldown = delayTicks;
    }

    /**
     * Splittet {@code msg} in Stücke ≤ {@code chunk}. Nicht-letzte Stücke enden mit
     * {@code marker}; geschnitten wird am letzten Leerzeichen vor der Grenze (eine
     * Wortgrenze zurück), Fallback harter Schnitt bei sehr langen Wörtern.
     *
     * <p>RP-Spans bleiben über die Teilgrenzen erhalten: Der Server färbt
     * {@code *…*} (Darstellung) und {@code (…)} (Offtopic) nur, wenn die Klammer
     * bzw. das Sternchen in derselben Nachricht wieder geschlossen wird. Endet
     * ein Teilstück mitten in einem offenen Span, wird er vor dem Marker
     * geschlossen und im nächsten Teilstück direkt wieder geöffnet.</p>
     */
    public static List<String> split(String msg, int chunk, String marker) {
        List<String> out = new ArrayList<>();
        String m = marker == null ? "" : marker;
        int size = Math.max(8, chunk);
        int contentMax = Math.max(1, size - m.length());
        String rem = msg;
        // Schutz gegen Endlosschleife
        int guard = 0;
        while (rem.length() > size && guard++ < 1000) {
            // Budget ggf. verkleinern, bis Teil + Span-Schließer ins Limit passen
            int budget = contentMax;
            String part;
            String close;
            String reopen;
            int cut;
            int fit = 0;
            while (true) {
                cut = budget;
                int sp = rem.lastIndexOf(' ', budget);
                if (sp > 0) {
                    cut = sp; // an Wortgrenze schneiden
                }
                part = rem.substring(0, cut).stripTrailing();
                if (part.isEmpty()) {
                    cut = budget; // sehr langes Wort -> harter Schnitt
                    part = rem.substring(0, cut);
                }
                Deque<Character> open = openSpans(part);
                close = closersFor(open);
                reopen = openersFor(open);
                if (part.length() + close.length() <= contentMax || budget <= 1 || fit++ >= 8) {
                    break;
                }
                budget = Math.max(1, contentMax - close.length());
            }
            out.add(part + close + m);
            rem = reopen + rem.substring(cut).stripLeading();
        }
        out.add(rem);
        return out;
    }

    /**
     * Offene RP-Spans in {@code s} als Stack (innerster zuoberst): {@code *} wirkt
     * als Umschalter, {@code (}/{@code )} als Paar; überzählige {@code )} werden
     * ignoriert.
     */
    private static Deque<Character> openSpans(String s) {
        Deque<Character> open = new ArrayDeque<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '*' -> {
                    if (!open.isEmpty() && open.peek() == '*') {
                        open.pop();
                    } else {
                        open.push('*');
                    }
                }
                case '(' -> open.push('(');
                case ')' -> open.removeFirstOccurrence('(');
                default -> {
                }
            }
        }
        return open;
    }

    /** Schließer für offene Spans, innerster zuerst (z. B. {@code "*)"}). */
    private static String closersFor(Deque<Character> open) {
        StringBuilder sb = new StringBuilder(open.size());
        for (char c : open) { // Iteration: top -> bottom (innen -> außen)
            sb.append(c == '(' ? ')' : c);
        }
        return sb.toString();
    }

    /** Wieder-Öffner für offene Spans, äußerster zuerst (z. B. {@code "(*"}). */
    private static String openersFor(Deque<Character> open) {
        StringBuilder sb = new StringBuilder(open.size());
        for (var it = open.descendingIterator(); it.hasNext(); ) { // bottom -> top
            sb.append(it.next());
        }
        return sb.toString();
    }
}
