package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LongChatSender {

    private static final Deque<String> queue = new ArrayDeque<>();
    private static int cooldown = 0;
    private static int delayTicks = 4;

    private LongChatSender() {
    }

    public static synchronized void configureMs(int delayMs) {
        delayTicks = Math.max(1, Math.round(delayMs / 50.0f));
    }

    public static synchronized void enqueue(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        queue.addAll(chunks);
        cooldown = 0;
    }

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

        }
        cooldown = delayTicks;
    }

    public static List<String> split(String msg, int chunk, String marker) {
        List<String> out = new ArrayList<>();
        String m = marker == null ? "" : marker;
        int size = Math.max(8, chunk);
        int contentMax = Math.max(1, size - m.length());
        String rem = msg;

        int guard = 0;
        while (rem.length() > size && guard++ < 1000) {

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
                    cut = sp;
                }
                part = rem.substring(0, cut).stripTrailing();
                if (part.isEmpty()) {
                    cut = budget;
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

    private static String closersFor(Deque<Character> open) {
        StringBuilder sb = new StringBuilder(open.size());
        for (char c : open) {
            sb.append(c == '(' ? ')' : c);
        }
        return sb.toString();
    }

    private static String openersFor(Deque<Character> open) {
        StringBuilder sb = new StringBuilder(open.size());
        for (var it = open.descendingIterator(); it.hasNext(); ) {
            sb.append(it.next());
        }
        return sb.toString();
    }
}
