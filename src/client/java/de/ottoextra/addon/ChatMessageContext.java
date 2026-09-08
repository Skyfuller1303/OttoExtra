package de.ottoextra.addon;

import de.ottoextra.chat.ChatSpeakerClassifier;
import net.minecraft.text.Text;

/**
 * Kontext einer von OttoExtra vorbereiteten Chatzeile fuer optionale Addons.
 * Das unveraenderte Original wird bewusst getrennt von der bereits
 * formatierten Darstellung mitgegeben.
 */
public record ChatMessageContext(
        Text original,
        Text displayed,
        ChatSpeakerClassifier.SpeakerKind speakerKind,
        boolean incoming) {
}
