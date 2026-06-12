package de.ottoextra.letter;

import de.ottoextra.letter.model.LetterDraftMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Brief-/Verkündungsentwurf: Seitentexte + Metadaten (GSON-direkt). */
public final class LetterDraft {

    public LetterDraftMeta meta = new LetterDraftMeta();
    public List<String> pages = new ArrayList<>();

    public static LetterDraft empty() {
        LetterDraft draft = new LetterDraft();
        draft.meta.draftId = UUID.randomUUID().toString().substring(0, 8);
        draft.pages.add("");
        return draft;
    }

    public void repair() {
        if (meta == null) {
            meta = new LetterDraftMeta();
        }
        if (meta.draftId == null || meta.draftId.isBlank()) {
            meta.draftId = UUID.randomUUID().toString().substring(0, 8);
        }
        if (pages == null) {
            pages = new ArrayList<>();
        }
        if (pages.isEmpty()) {
            pages.add("");
        }
    }
}
