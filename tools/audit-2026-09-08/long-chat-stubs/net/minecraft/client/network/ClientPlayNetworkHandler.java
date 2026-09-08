package net.minecraft.client.network;

import java.util.ArrayList;
import java.util.List;

/** Records calls made by the real LongChatSender source. */
public final class ClientPlayNetworkHandler {
    private final List<String> sentMessages = new ArrayList<>();

    public void sendChatMessage(String message) {
        sentMessages.add(message);
    }

    public List<String> sentMessages() {
        return List.copyOf(sentMessages);
    }
}
