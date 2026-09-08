package de.ottoextra.letter.model;

import java.util.ArrayList;
import java.util.List;

public final class LetterDraftMeta {
    public String draftId = "";

    public String name = "";
    public LetterOutputMode mode;
    public boolean importedFromBook = false;
    public boolean pagesOneToOne = false;
    public long updatedAtMs = 0L;

    public int lockedPages = 0;

    public int lockedOffset = 0;

    /** Vom gehaltenen Buch gelesene CourierNew-Seiten als Ausgangszustand. */
    public List<String> courierInitialPages = new ArrayList<>();

    /** Kanonischer Fliesstext fuer neue V2-Entwuerfe; pages bleibt reine Darstellung. */
    public String flowingDocument;
    public int flowingDocumentVersion = 0;
}
