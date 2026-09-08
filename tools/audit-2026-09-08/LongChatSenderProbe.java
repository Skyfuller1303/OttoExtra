import de.ottoextra.chat.LongChatSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import java.util.List;

/** Regression against the production sender; handlers only record output. */
public final class LongChatSenderProbe {
    public static void main(String[] args) {
        for (int gap : new int[]{0, 1, 5, 31}) connectionChange(gap);
        beforeFirstTick();
        explicitDisconnect();
        freshInputOnNewConnection();
        enqueuePreservesPause(false);
        enqueuePreservesPause(true);
        require(!LongChatSender.enqueue(null, List.of("offline")), "Offline input rejected");
        System.out.println("RESULT: 10 sender regression scenarios passed.");
        System.out.println("SCOPE: actual production sender source; stub connection boundary; no Minecraft reconnect test.");
    }

    private static void connectionChange(int gap) {
        reset();
        var a = new ClientPlayNetworkHandler();
        var b = new ClientPlayNetworkHandler();
        LongChatSender.enqueue(a, List.of("first", "second"));
        LongChatSender.tick(new MinecraftClient(a));
        ticks(new MinecraftClient(null), gap);
        ticks(new MinecraftClient(b), 35);
        require(a.sentMessages().equals(List.of("first")), "Only first sent on A");
        require(b.sentMessages().isEmpty(), "A text must never reach B");
        System.out.println("PASS connection_change: disconnectedTicks=" + gap + "; B=[]");
    }

    private static void beforeFirstTick() {
        reset();
        var a = new ClientPlayNetworkHandler();
        var b = new ClientPlayNetworkHandler();
        LongChatSender.enqueue(a, List.of("old"));
        ticks(new MinecraftClient(b), 35);
        require(b.sentMessages().isEmpty(), "Bind connection before first tick");
    }

    private static void explicitDisconnect() {
        reset();
        var a = new ClientPlayNetworkHandler();
        LongChatSender.enqueue(a, List.of("cancelled"));
        LongChatSender.cancel();
        ticks(new MinecraftClient(a), 35);
        require(a.sentMessages().isEmpty(), "Disconnect clears even a reused handler");
    }

    private static void freshInputOnNewConnection() {
        reset();
        var a = new ClientPlayNetworkHandler();
        var b = new ClientPlayNetworkHandler();
        LongChatSender.enqueue(a, List.of("old"));
        LongChatSender.enqueue(b, List.of("new"));
        ticks(new MinecraftClient(b), 35);
        require(a.sentMessages().isEmpty(), "Old unsent queue cancelled");
        require(b.sentMessages().equals(List.of("new")), "New input remains usable");
    }

    private static void enqueuePreservesPause(boolean emptyQueue) {
        reset();
        var a = new ClientPlayNetworkHandler();
        var client = new MinecraftClient(a);
        LongChatSender.enqueue(a, emptyQueue ? List.of("first") : List.of("first", "second"));
        LongChatSender.tick(client);
        ticks(client, 10);
        LongChatSender.enqueue(a, emptyQueue ? List.of("second") : List.of("third"));
        ticks(client, 20);
        require(a.sentMessages().equals(List.of("first")), "Enqueue must preserve remaining pause");
        LongChatSender.tick(client);
        require(a.sentMessages().equals(List.of("first", "second")), "Second follows full pause");
        if (!emptyQueue) {
            ticks(client, 31);
            require(a.sentMessages().equals(List.of("first", "second", "third")), "Order preserved");
        }
    }

    private static void reset() {
        LongChatSender.cancel();
        LongChatSender.configureMs(1500);
    }

    private static void ticks(MinecraftClient client, int count) {
        for (int i = 0; i < count; i++) LongChatSender.tick(client);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
