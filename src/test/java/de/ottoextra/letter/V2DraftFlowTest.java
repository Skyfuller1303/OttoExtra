package de.ottoextra.letter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V2DraftFlowTest {

    @Test
    void migrationRemovesAutomaticPageAndLineBreaksButKeepsParagraphs() {
        assertEquals("Erste Zeile zweite Zeile Zweite Seite\n\nNeuer Absatz",
                V2DraftFlow.migratePages(List.of(
                        "Erste Zeile\nzweite Zeile",
                        "Zweite Seite\n\nNeuer Absatz")));
    }

    @Test
    void repeatedEditsAcrossGeneratedPagesKeepOneCanonicalDocument() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("wort ".repeat(180).trim()));
        V2DraftFlow.ensureDocument(draft);
        var position = V2DraftFlow.reflow(draft, 0, 0, -1,
                "letter", value -> value.length() * 4);
        assertTrue(draft.pages.size() > 1);

        int last = draft.pages.size() - 1;
        String page = draft.pages.get(last);
        position = V2DraftFlow.replacePage(draft, last, page + " Ende",
                page.length() + 5, -1, "letter", value -> value.length() * 4, true);

        assertTrue(draft.meta.flowingDocument.endsWith(" Ende"));
        assertEquals(" Ende", draft.meta.flowingDocument.substring(
                draft.meta.flowingDocument.length() - 5));
        assertTrue(position.page() >= last);
    }

    @Test
    void sourcePagesUseCanonicalDocumentAfterMigration() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("Seite eins", "Seite zwei"));
        V2DraftFlow.ensureDocument(draft);

        assertEquals(List.of("Seite eins Seite zwei"), V2DraftFlow.sourcePages(draft));
    }

    @Test
    void editOnFirstGeneratedPageDoesNotCreatePermanentPageBreak() {
        String original = "eins zwei drei vier fünf sechs sieben acht neun zehn ".repeat(4).trim();
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of(original));
        V2DraftFlow.ensureDocument(draft);
        V2DraftFlow.reflow(draft, 0, 0, -1,
                "letter", value -> value.length() * 9);
        assertTrue(draft.pages.size() > 1);

        String first = draft.pages.getFirst();
        int local = first.indexOf("drei") + 4;
        int global = de.ottoextra.letter.send.LetterV2Planner
                .collapseSoftLineBreaks(first.substring(0, local)).length();
        V2DraftFlow.replacePage(draft, 0,
                first.substring(0, local) + " NEU" + first.substring(local),
                local + 4, -1, "letter", value -> value.length() * 9, true);

        assertEquals(original.substring(0, global) + " NEU" + original.substring(global),
                draft.meta.flowingDocument);
        List<String> once = List.copyOf(draft.pages);
        V2DraftFlow.reflow(draft, 0, 0, -1,
                "letter", value -> value.length() * 9);
        assertEquals(once, draft.pages);
    }

    @Test
    void pendingSpaceSurvivesUntilFollowingCharacterTriggersReflow() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("Hallo"));
        V2DraftFlow.ensureDocument(draft);

        V2DraftFlow.replacePage(draft, 0, "Hallo ", 6, -1,
                "letter", value -> value.length() * 4, false);
        V2DraftFlow.replacePage(draft, 0, "Hallo W", 7, -1,
                "letter", value -> value.length() * 4, true);

        assertEquals("Hallo W", draft.meta.flowingDocument);
        assertEquals(List.of("Hallo W"), draft.pages);
    }

    @Test
    void trailingSpaceSurvivesReflowLikeClosingAndReopeningEditor() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("Hallo "));
        draft.meta.flowingDocument = "Hallo ";
        draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;

        V2DraftFlow.reflow(draft, 0, 6, -1,
                "letter", value -> value.length() * 4);
        assertEquals(List.of("Hallo "), draft.pages);
        V2DraftFlow.replacePage(draft, 0, "Hallo W", 7, -1,
                "letter", value -> value.length() * 4, true);

        assertEquals("Hallo W", draft.meta.flowingDocument);
    }

    @Test
    void repeatedEditsOnDifferentPagesNeverGrowDocumentByPageSeparators() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("Wort ".repeat(220).trim()));
        V2DraftFlow.ensureDocument(draft);
        V2DraftFlow.reflow(draft, 0, 0, -1,
                "letter", value -> value.length() * 5);
        int originalLength = draft.meta.flowingDocument.length();

        for (int i = 0; i < 20; i++) {
            int page = i % draft.pages.size();
            String visible = draft.pages.get(page);
            int local = Math.max(0, visible.length() / 2);
            V2DraftFlow.replacePage(draft, page,
                    visible.substring(0, local) + "X" + visible.substring(local),
                    local + 1, -1, "letter", value -> value.length() * 5, true);
            assertEquals(originalLength + i + 1, draft.meta.flowingDocument.length());
        }

        List<String> stable = List.copyOf(draft.pages);
        V2DraftFlow.reflow(draft, 0, 0, -1,
                "letter", value -> value.length() * 5);
        assertEquals(stable, draft.pages);
    }

    @Test
    void manualTypingPreservesEnterAndAdjacentWords() {
        LetterDraft draft = LetterDraft.empty();
        V2DraftFlow.ensureDocument(draft);
        V2DraftFlow.EditorPosition position = new V2DraftFlow.EditorPosition(0, 0, -1);
        String typed = "Erste Zeile\nZweite Zeile\n\nNeuer Absatz";

        for (int i = 0; i < typed.length(); i++) {
            String character = typed.substring(i, i + 1);
            String page = draft.pages.get(position.page());
            String changed = page.substring(0, position.cursor()) + character
                    + page.substring(position.cursor());
            position = V2DraftFlow.replacePage(draft, position.page(), changed,
                    position.cursor() + 1, -1, "letter", value -> value.length() * 6,
                    !" ".equals(character));
        }

        assertEquals(typed, draft.meta.flowingDocument);
        assertEquals(List.of(typed), draft.pages);
    }

    @Test
    void everyCursorPositionOnLaterPageKeepsExactInsertionPoint() {
        String original = java.util.stream.IntStream.range(0, 90)
                .mapToObj(i -> "wort" + i)
                .collect(java.util.stream.Collectors.joining(" "))
                + "\n\nDank im Voraus.\nMit Hochachtung.";
        LetterDraft laidOut = LetterDraft.empty();
        laidOut.pages = new ArrayList<>(List.of(original));
        V2DraftFlow.ensureDocument(laidOut);
        V2DraftFlow.reflow(laidOut, 0, 0, -1,
                "letter", value -> value.length() * 5);
        int targetPage = java.util.stream.IntStream.range(1, laidOut.pages.size())
                .filter(i -> laidOut.pages.get(i).contains("Dank im Voraus"))
                .findFirst().orElseThrow();
        String visible = laidOut.pages.get(targetPage);
        String alignedOriginal = original.replace('\n', ' ');
        int sourceStart = alignedOriginal.indexOf(visible.replace('\n', ' '));
        assertTrue(sourceStart >= 0);

        for (int offset = 0; offset <= visible.length(); offset++) {
            LetterDraft draft = LetterDraft.empty();
            draft.pages = new ArrayList<>(laidOut.pages);
            draft.meta.flowingDocument = original;
            draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;
            String changed = visible.substring(0, offset) + "X"
                    + visible.substring(offset);

            V2DraftFlow.EditorPosition position = V2DraftFlow.replacePage(
                    draft, targetPage, changed, offset + 1, -1,
                    "letter", value -> value.length() * 5, true);

            assertEquals(original.substring(0, sourceStart + offset) + "X"
                            + original.substring(sourceStart + offset),
                    draft.meta.flowingDocument, "Quellposition " + offset);
            assertEquals('X', draft.pages.get(position.page())
                    .charAt(position.cursor() - 1), "Cursorposition " + offset);
        }
    }

    @Test
    void enterAndFollowingTextStayOnSelectedParagraphOnLaterPage() {
        String original = java.util.stream.IntStream.range(0, 90)
                .mapToObj(i -> "wort" + i)
                .collect(java.util.stream.Collectors.joining(" "))
                + " Schlusszeile";
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of(original));
        V2DraftFlow.ensureDocument(draft);
        V2DraftFlow.reflow(draft, 0, 0, -1,
                "letter", value -> value.length() * 5);
        int targetPage = draft.pages.size() - 1;
        int local = draft.pages.get(targetPage).indexOf("Schlusszeile");
        int global = original.indexOf("Schlusszeile");
        V2DraftFlow.EditorPosition position = new V2DraftFlow.EditorPosition(
                targetPage, local, -1);

        for (String character : List.of("\n", "\n", "N", "e", "u")) {
            String visible = draft.pages.get(position.page());
            String changed = visible.substring(0, position.cursor()) + character
                    + visible.substring(position.cursor());
            position = V2DraftFlow.replacePage(draft, position.page(), changed,
                    position.cursor() + character.length(), -1,
                    "letter", value -> value.length() * 5, true);
            assertEquals(character.charAt(character.length() - 1),
                    draft.pages.get(position.page()).charAt(position.cursor() - 1),
                    "Cursor nach " + character.replace("\n", "ENTER")
                            + " bei Seite " + position.page() + " Offset "
                            + position.cursor() + " in "
                            + draft.pages.get(position.page()).replace("\n", "<NL>"));
        }

        assertEquals(de.ottoextra.letter.send.LetterV2Planner.normalizeDocument(
                        original.substring(0, global) + "\n\nNeu"
                                + original.substring(global)),
                draft.meta.flowingDocument);
        assertEquals("Neu", draft.pages.get(position.page()).substring(
                position.cursor() - 3, position.cursor()));
    }

    @Test
    void manualPageCanBeAddedEditedAndRemoved() {
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of("Erste Seite"));
        V2DraftFlow.ensureDocument(draft);

        V2DraftFlow.EditorPosition position = V2DraftFlow.insertPageAfter(draft, 0,
                "letter", value -> value.length() * 6);
        assertEquals(1, position.page());
        assertEquals(List.of("Erste Seite", ""), draft.pages);

        position = V2DraftFlow.replacePage(draft, position.page(), "Zweite Seite",
                12, -1, "letter", value -> value.length() * 6, true);
        assertEquals(List.of("Erste Seite", "Zweite Seite"), V2DraftFlow.sourcePages(draft));

        position = V2DraftFlow.deletePage(draft, position.page(),
                "letter", value -> value.length() * 6);
        assertEquals(0, position.page());
        assertEquals(List.of("Erste Seite"), draft.pages);
        assertEquals(List.of("Erste Seite"), V2DraftFlow.sourcePages(draft));
    }

    @Test
    void paragraphAfterHardWrappedWordsKeepsTheCursorWithTheNewText() {
        String original = "Selbstverständlichkeit ".repeat(25) + "Schlusszeile";
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of(original));
        V2DraftFlow.ensureDocument(draft);
        V2DraftFlow.reflow(draft, 0, 0, -1, "letter", value -> value.length() * 6);
        int targetPage = draft.pages.size() - 1;
        int local = draft.pages.get(targetPage).indexOf("Schlusszeile");
        assertTrue(local >= 0);
        int global = original.indexOf("Schlusszeile");
        var position = new V2DraftFlow.EditorPosition(targetPage, local, -1);
        for (String character : List.of("\n", "\n", "N", "e", "u")) {
            String page = draft.pages.get(position.page());
            position = V2DraftFlow.replacePage(draft, position.page(),
                    page.substring(0, position.cursor()) + character + page.substring(position.cursor()),
                    position.cursor() + 1, -1, "letter", value -> value.length() * 6, true);
        }
        assertEquals(de.ottoextra.letter.send.LetterV2Planner.normalizeDocument(
                original.substring(0, global) + "\n\nNeu" + original.substring(global)),
                draft.meta.flowingDocument);
        assertEquals("Neu", draft.pages.get(position.page()).substring(position.cursor() - 3, position.cursor()));
    }

    @Test
    void editingEveryPositionAfterHardWrapDoesNotMoveOrDuplicateParagraphs() {
        String original = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        LetterDraft laidOut = LetterDraft.empty();
        laidOut.pages = new ArrayList<>(List.of(original));
        V2DraftFlow.ensureDocument(laidOut);
        V2DraftFlow.reflow(laidOut, 0, 0, -1, "letter", value -> value.length() * 6);
        String page = laidOut.pages.getFirst();
        assertTrue(page.contains("\n"));
        for (int offset = 0; offset <= page.length(); offset++) {
            var draft = LetterDraft.empty();
            draft.pages = new ArrayList<>(laidOut.pages);
            draft.meta.flowingDocument = original;
            draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;
            int global = page.substring(0, offset).replace("\n", "").length();
            var position = V2DraftFlow.replacePage(draft, 0,
                    page.substring(0, offset) + "\nX" + page.substring(offset),
                    offset + 2, -1, "letter", value -> value.length() * 6, true);
            assertEquals(original.substring(0, global) + "\nX" + original.substring(global),
                    draft.meta.flowingDocument, "Insertion at " + offset);
            assertEquals('X', draft.pages.get(position.page()).charAt(position.cursor() - 1), "Cursor at " + offset);
        }
    }

    @Test
    void emptySecondPageWithOnlyLineBreaksCanBeRemoved() {
        LetterDraft draft = LetterDraft.empty();
        draft.meta.flowingDocument = "\n\f\n\n\n\n \n";
        draft.meta.flowingDocumentVersion = V2DraftFlow.DOCUMENT_VERSION;
        draft.pages = new ArrayList<>(List.of("", ""));

        V2DraftFlow.EditorPosition position = V2DraftFlow.deletePage(draft, 1,
                "letter", value -> value.length() * 6);

        assertEquals(0, position.page());
        // Removing the second page must preserve the first page's explicit blank line.
        assertEquals(List.of("\n"), draft.pages);
        assertEquals(List.of("\n"), V2DraftFlow.sourcePages(draft));
    }

    @Test
    void lateFormattingResetChangesOnlyTheExactCursorPosition() {
        StringBuilder source = new StringBuilder("§6");
        for (int i = 0; i < 180; i++) {
            if (i > 0) source.append(' ');
            source.append("wort").append(i);
        }
        String original = source.toString();
        LetterDraft draft = LetterDraft.empty();
        draft.pages = new ArrayList<>(List.of(original));
        V2DraftFlow.ensureDocument(draft);
        V2DraftFlow.reflow(draft, 0, 0, -1,
                "letter", value -> value.length() * 5);
        assertTrue(draft.pages.size() > 2);

        int targetPage = 2;
        String visible = draft.pages.get(targetPage);
        String token = java.util.regex.Pattern.compile("wort\\d+")
                .matcher(visible).results().skip(2).findFirst()
                .orElseThrow().group();
        int localCursor = visible.indexOf(token);
        int globalCursor = original.indexOf(token);

        V2DraftFlow.replacePage(draft, targetPage,
                visible.substring(0, localCursor) + "§r" + visible.substring(localCursor),
                localCursor + 2, -1, "letter", value -> value.length() * 5, true);

        assertEquals(original.substring(0, globalCursor) + "§r"
                        + original.substring(globalCursor),
                draft.meta.flowingDocument);
    }
}
