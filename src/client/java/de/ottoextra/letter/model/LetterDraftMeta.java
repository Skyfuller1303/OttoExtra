package de.ottoextra.letter.model;

/**
 * Metadaten eines Entwurfs: Modus + Import-Herkunft.
 * GSON-direkt, daher mutable.
 */
public final class LetterDraftMeta {
    public String draftId = "";
    public LetterOutputMode mode;
    public boolean importedFromBook = false;
    public boolean pagesOneToOne = false;
    public long updatedAtMs = 0L;
}
