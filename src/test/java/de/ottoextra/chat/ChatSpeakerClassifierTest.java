package de.ottoextra.chat;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.object.PlayerTextObjectContents;
import org.junit.jupiter.api.Test;

import static de.ottoextra.chat.ChatSpeakerClassifier.SpeakerKind.NPC;
import static de.ottoextra.chat.ChatSpeakerClassifier.SpeakerKind.PLAYER;
import static de.ottoextra.chat.ChatSpeakerClassifier.SpeakerKind.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSpeakerClassifierTest {

    @Test
    void systemRpLineWithoutPlayerEvidenceIsNpc() {
        assertEquals(NPC, classify("[Sprechen] Bote Hinz: Eine Nachricht", false, true));
    }

    @Test
    void signatureAlwaysIdentifiesPlayer() {
        assertEquals(PLAYER, classify("[Sprechen] Bote Hinz: Eine Nachricht", true, true));
    }

    @Test
    void structuredPlayerComponentIdentifiesPlayer() {
        MutableText head = MutableText.of(new ObjectTextContent(
                new PlayerTextObjectContents(ProfileComponent.ofDynamic("Skyfuller1303"), true)));
        Text message = Text.literal("[Sprechen] ").append(head).append(" Unbekannt: Hallo");

        assertEquals(PLAYER, ChatSpeakerClassifier.classify(
                message, false, true, "Skyfuller1303"::equalsIgnoreCase));
    }

    @Test
    void offlinePlayerComponentDoesNotProvePlayerIdentity() {
        MutableText head = MutableText.of(new ObjectTextContent(
                new PlayerTextObjectContents(ProfileComponent.ofDynamic("NpcHead"), true)));
        Text message = Text.literal("[Sprechen] ").append(head).append(" Bote Hinz: Hallo");

        assertEquals(NPC, ChatSpeakerClassifier.classify(
                message, false, true, account -> false));
    }

    @Test
    void exactOnlineHeadAccountIdentifiesPlayer() {
        Text message = Text.literal("[Sprechen] [Skyfuller1303 head] Unbekannt: Hallo");

        assertEquals(PLAYER, ChatSpeakerClassifier.classify(
                message, false, true, "Skyfuller1303"::equalsIgnoreCase));
    }

    @Test
    void fuzzyOrOfflineHeadAccountDoesNotIdentifyPlayer() {
        Text message = Text.literal("[Sprechen] [Skyfuller1303 head] Unbekannt: Hallo");

        assertEquals(NPC, ChatSpeakerClassifier.classify(
                message, false, true, "Skyfuller"::equalsIgnoreCase));
    }

    @Test
    void headTokenInBodyIsNotPlayerEvidence() {
        Text message = Text.literal("[Sprechen] Bote Hinz: Zeigt [Skyfuller1303 head]");

        assertEquals(NPC, ChatSpeakerClassifier.classify(
                message, false, true, account -> true));
    }

    @Test
    void lineWithoutEvidenceOrSystemIndicatorIsUnknown() {
        assertEquals(UNKNOWN, classify("[Sprechen] Bote Hinz: Eine Nachricht", false, false));
    }

    private static ChatSpeakerClassifier.SpeakerKind classify(
            String message, boolean signed, boolean system) {
        return ChatSpeakerClassifier.classify(
                Text.literal(message), signed, system, account -> false);
    }
}
