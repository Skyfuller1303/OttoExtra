package de.ottoextra.chat;

import de.ottoextra.rpnames.chat.OttoChatChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OttoChatChannelTest {

    @Test
    void recognizesRpChannelsBehindLegacyFormattingCodes() {
        assertEquals(OttoChatChannel.SPRECHEN,
                OttoChatChannel.fromMessage("§8[Sprechen] §7Stallmeister Ermenoldus: Hallo"));
        assertEquals(OttoChatChannel.REDEN,
                OttoChatChannel.fromMessage("§7§l[Reden] Krämerin Ada: Hallo"));
        assertEquals(OttoChatChannel.BRUELLEN,
                OttoChatChannel.fromMessage("§c[Bruellen] Wächter: Halt!"));
        assertEquals(OttoChatChannel.FLUESTERN,
                OttoChatChannel.fromMessage("§8[Fluestern] Magd: Leise"));
    }

    @Test
    void nonChannelTextRemainsOtherAfterFormattingRemoval() {
        assertEquals(OttoChatChannel.OTHER,
                OttoChatChannel.fromMessage("§7Stallmeister Ermenoldus: Hallo"));
        assertEquals(OttoChatChannel.OTHER,
                OttoChatChannel.fromMessage("§z[Sprechen] Ungültiger Code"));
    }
}
