package de.ottoextra.api.model;

/** Autorisierte Zuordnung einer öffentlichen Übersetzung zum geschützten Original. */
public record ProtectedChatInboxEntry(
        String id,
        String translation,
        String original,
        String senderUuid,
        String createdAt,
        String expiresAt) {
}
