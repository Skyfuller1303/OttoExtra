package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
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
        return LongChatSplitter.split(msg, chunk, marker);
    }

    public static List<String> split(String msg, int chunk, String marker,
                                     boolean preserveRpSyntax) {
        return LongChatSplitter.split(msg, chunk, marker, preserveRpSyntax);
    }
}
