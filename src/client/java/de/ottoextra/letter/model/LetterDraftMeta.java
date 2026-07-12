package de.ottoextra.letter.model;

public final class LetterDraftMeta {
    public String draftId = "";

    public String name = "";
    public LetterOutputMode mode;
    public boolean importedFromBook = false;
    public boolean pagesOneToOne = false;
    public long updatedAtMs = 0L;

    public int lockedPages = 0;

    public int lockedOffset = 0;
}
