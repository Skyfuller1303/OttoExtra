package de.ottoextra.chat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
public final class ChatInputLayout {
    private static final Map<String, IntSupplier> RIGHT_RESERVATIONS =
            new ConcurrentHashMap<>();
    private ChatInputLayout() {
    }
    public static void registerRightReservation(String id, IntSupplier widthSupplier) {
        if (id == null || id.isBlank() || widthSupplier == null) {
            return;
        }
        RIGHT_RESERVATIONS.put(id, widthSupplier);
    }
    public static void removeRightReservation(String id) {
        if (id != null) {
            RIGHT_RESERVATIONS.remove(id);
        }
    }
    public static int rightReservedWidth() {
        int total = 0;
        for (IntSupplier supplier : RIGHT_RESERVATIONS.values()) {
            try {
                total += Math.max(0, supplier.getAsInt());
            } catch (Throwable ignored) {
            }
        }
        return Math.min(512, total);
    }
}
