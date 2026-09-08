package de.ottoextra.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class LongChatSender {

    private static final Deque<String> queue = new ArrayDeque<>();
    private static int cooldown = 0;
    private static int delayTicks = 4;
    private static ClientPlayNetworkHandler connection;

    private LongChatSender() {
    }

    public static synchronized void configureMs(int delayMs) {
        delayTicks = Math.max(1, Math.round(delayMs / 50.0f));
    }

    public static void enqueue(List<String> chunks) {
        MinecraftClient client = MinecraftClient.getInstance();
        enqueue(client == null ? null : client.getNetworkHandler(), chunks);
    }

    /** Capture the originating connection before the first queued tick. */
    public static synchronized boolean enqueue(ClientPlayNetworkHandler handler, List<String> chunks) {
        if (handler == null || chunks == null || chunks.isEmpty()) {
            return false;
        }
        if (connection != handler) {
            cancel();
            connection = handler;
        }
        queue.addAll(chunks);
        // Further input must not bypass the pause after the previous chunk.
        return true;
    }

    public static synchronized void cancel() {
        queue.clear();
        connection = null;
        cooldown = 0;
    }

    public static synchronized void tick(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
        if (handler == null || handler != connection) {
            cancel();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (queue.isEmpty()) {
            return;
        }
        String msg = queue.poll();
        try {
            handler.sendChatMessage(msg);
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
