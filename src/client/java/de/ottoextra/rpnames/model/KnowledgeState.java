package de.ottoextra.rpnames.model;

/**
 * Bekanntheitsgrad eines Spielers im lokalen RP-Bekanntschaftssystem
 *.
 */
public enum KnowledgeState {
    /** Gesehen/online erkannt, RP-Name unbekannt ("Unbekannt"). */
    SEEN,
    /** RP-Name erstmals im Chat/Hover erkannt. */
    HEARD_NAME,
    /** Name bestätigt (mehrfach gehört oder vom Nutzer bestätigt). */
    KNOWN,
    /** Aus der Datenbank importiert, ohne lokalen Beleg. */
    API_IMPORTED,
    /** Lokal bearbeitet. */
    MANUAL,
    /** Lokal bearbeitet und gegen automatische Änderungen geschützt. */
    MANUAL_LOCKED;

    /** Darf eine automatische Quelle (Hover/API) diesen Zustand verändern? */
    public boolean allowsAutomaticUpdates() {
        return this != MANUAL && this != MANUAL_LOCKED;
    }
}
