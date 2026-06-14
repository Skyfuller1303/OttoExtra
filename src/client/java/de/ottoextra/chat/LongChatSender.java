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

    public static synchronized void configure(int delay) {
        delayTicks = Math.max(1, delay);
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
            int cut = contentMax;
            int sp = rem.lastIndexOf(' ', contentMax);
            if (sp > 0) {
                cut = sp; // an Wortgrenze schneiden
            }
            String part = rem.substring(0, cut).stripTrailing();
            if (part.isEmpty()) {
                cut = contentMax; // sehr langes Wort -> harter Schnitt
                part = rem.substring(0, cut);
            }
            out.add(part + m);
            rem = rem.substring(cut).stripLeading();
        }
        out.add(rem);
        return out;
    }
}
