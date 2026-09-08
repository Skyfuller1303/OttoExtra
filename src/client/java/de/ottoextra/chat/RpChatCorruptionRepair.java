package de.ottoextra.chat;

import net.minecraft.text.Text;

/** Keeps received text intact until corruption has an unambiguous protocol signal. */
final class RpChatCorruptionRepair {
    private RpChatCorruptionRepair() {
    }

    static Text repair(Text message) {
        // Repeated suffixes are also valid prose, punctuation or numbers.
        // Text alone cannot establish whether the server duplicated them.
        return message;
    }
}
