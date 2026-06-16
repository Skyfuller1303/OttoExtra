package de.ottoextra.letter.model;

/**
 * Metadaten eines Entwurfs: Modus + Import-Herkunft.
 * GSON-direkt, daher mutable.
 */
public final class LetterDraftMeta {
    public String draftId = "";
    /** Anzeigename für gespeicherte Entwürfe (leer = unbenannt). */
    public String name = "";
    public LetterOutputMode mode;
    public boolean importedFromBook = false;
    public boolean pagesOneToOne = false;
    public long updatedAtMs = 0L;
    /** Anzahl vollständig gesperrter, bereits geschriebener Seiten (read-only beim
     *  Bearbeiten eines beschriebenen Briefs). Auf der Seite mit Index
     *  {@code lockedPages} wird zusätzlich {@link #lockedOffset} vorne gesperrt, damit
     *  neuer Text INLINE weitergeschrieben (nicht auf neue Seite gelegt) wird.
     *  Gesperrtes wird NICHT erneut per /letter gesendet und faded angezeigt. */
    public int lockedPages = 0;
    /** Gesperrte führende Zeichen auf der Fortsetzungs-Seite ({@code lockedPages}). */
    public int lockedOffset = 0;
}
