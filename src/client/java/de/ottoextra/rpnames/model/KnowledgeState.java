package de.ottoextra.rpnames.model;
public enum KnowledgeState {
    SEEN,
    HEARD_NAME,
    KNOWN,
    API_IMPORTED,
    MANUAL,
    MANUAL_LOCKED;
    public boolean allowsAutomaticUpdates() {
        return this != MANUAL && this != MANUAL_LOCKED;
    }
}
