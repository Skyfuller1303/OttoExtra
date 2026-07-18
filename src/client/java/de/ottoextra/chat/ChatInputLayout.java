package de.ottoextra.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

/**
 * Kleine Layout-Schnittstelle fuer Addons, die rechts neben der
 * Chat-Eingabe eigene Bedienelemente zeichnen.
 */
public final class ChatInputLayout {

    private static final Map<String, IntSupplier> RIGHT_RESERVATIONS =
            new ConcurrentHashMap<>();

    private ChatInputLayout() {
    }

    /**
     * Registriert oder ersetzt den rechts reservierten Platz eines Addons.
     * Der Supplier darf dynamisch 0 liefern, wenn das Element gerade unsichtbar ist.
     */
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
                // Ein fehlerhaftes Addon darf das Chatfeld nicht beschaedigen.
            }
        }
        return Math.min(512, total);
    }
}
