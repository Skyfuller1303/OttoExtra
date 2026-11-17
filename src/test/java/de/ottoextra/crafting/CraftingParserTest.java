package de.ottoextra.crafting;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingParserTest {

    private static Text bar(int gold, int gray) {
        Text root = Text.literal("");
        if (gold > 0) root.getSiblings().add(Text.literal("■".repeat(gold)).formatted(Formatting.GOLD));
        if (gray > 0) root.getSiblings().add(Text.literal("■".repeat(gray)).formatted(Formatting.DARK_GRAY));
        return root;
    }

    @Test
    void readsGoldAndGraySiblings() {
        var p = CraftingParser.progress(bar(1, 9)).orElseThrow();
        assertEquals(1, p.done());
        assertEquals(10, p.total());
        assertEquals(0.1f, p.fraction(), 1e-6);
        assertEquals(0.9f, CraftingParser.progress(bar(9, 1)).orElseThrow().fraction(), 1e-6);
        assertEquals(1.0f, CraftingParser.progress(bar(10, 0)).orElseThrow().fraction(), 1e-6);
    }

    @Test
    void isRobustToSplitSiblingsAndInheritedStyle() {
        Text root = Text.literal("");
        root.getSiblings().add(Text.literal("■").formatted(Formatting.GOLD));
        root.getSiblings().add(Text.literal("■■").formatted(Formatting.GOLD));
        Text gray = Text.literal("").formatted(Formatting.DARK_GRAY);
        gray.getSiblings().add(Text.literal("■■"));
        gray.getSiblings().add(Text.literal("■■■■■"));
        root.getSiblings().add(gray);
        var p = CraftingParser.progress(root).orElseThrow();
        assertEquals(3, p.done());
        assertEquals(10, p.total());
    }

    @Test
    void rejectsNonMatchingTexts() {
        assertTrue(CraftingParser.progress(null).isEmpty());
        assertTrue(CraftingParser.progress(Text.literal("")).isEmpty());
        assertTrue(CraftingParser.progress(Text.literal("§7Die Herstellung ist erfolgreich!")).isEmpty());
        assertTrue(CraftingParser.progress(Text.literal("■■").formatted(Formatting.RED)).isEmpty());
        assertTrue(CraftingParser.progress(Text.literal("■■")).isEmpty());
        Text mixed = Text.literal("").append(Text.literal("■ x").formatted(Formatting.GOLD));
        assertTrue(CraftingParser.progress(mixed).isEmpty());
    }

    @Test
    void parsesItemNameFromStartLine() {
        assertEquals("Leine", CraftingParser.itemName("[Info] Du beginnst die Herstellung von Leine...").orElseThrow());
        assertEquals("Eiserne Axt", CraftingParser.itemName("Du beginnst die Herstellung von Eiserne Axt...").orElseThrow());
        assertEquals("Leine", CraftingParser.itemName("§7[Info] Du beginnst die Herstellung von Leine...").orElseThrow());
        assertTrue(CraftingParser.itemName("Du beginnst die Herstellung von ...").isEmpty());
        assertTrue(CraftingParser.itemName("[Info] Etwas anderes").isEmpty());
        assertTrue(CraftingParser.itemName(null).isEmpty());
    }

    @Test
    void detectsCompletion() {
        assertTrue(CraftingParser.isCompletion("§7Die Herstellung ist erfolgreich!"));
        assertFalse(CraftingParser.isCompletion("■■"));
        assertFalse(CraftingParser.isCompletion(null));
    }

    @Test
    void detectsFailureKeywordsAndSuccessWins() {
        assertTrue(CraftingParser.isFailure("§c[Info] Die Herstellung ist fehlgeschlagen!"));
        assertTrue(CraftingParser.isFailure("Herstellung ABGEBROCHEN"));
        assertTrue(CraftingParser.isFailure("Die Herstellung wurde nicht abgeschlossen"));
        assertTrue(CraftingParser.isFailure("Du hast kein Material für die Herstellung"));
        assertFalse(CraftingParser.isFailure("§7Die Herstellung ist erfolgreich!"));
        assertFalse(CraftingParser.isFailure("Die Herstellung ist nicht erfolgreich? Doch: Die Herstellung ist erfolgreich"));
        assertFalse(CraftingParser.isFailure("[Info] Du beginnst die Herstellung von Keine Ahnung..."));
        assertFalse(CraftingParser.isFailure("Das ist nicht relevant"));
        assertFalse(CraftingParser.isFailure(null));
    }

    @Test
    void timeoutBecomesFailureThenSuccessCanStillWin() {
        CraftingState s = new CraftingState();
        s.onProgress(0.4f, null, 0);
        assertFalse(s.tick(CraftingState.TIMEOUT_MS));
        assertTrue(s.tick(CraftingState.TIMEOUT_MS + 1));
        assertFalse(s.tick(CraftingState.TIMEOUT_MS + 2)); // only once
        assertEquals(CraftingState.Result.FAILURE, s.result());
        assertTrue(s.visible(CraftingState.TIMEOUT_MS + 10));
        assertFalse(s.visible(CraftingState.TIMEOUT_MS + 1 + CraftingState.HOLD_MS));
        // Late completion overrides the assumed failure.
        s.onProgress(0.4f, null, 10_000);
        s.tick(10_000 + CraftingState.TIMEOUT_MS + 1);
        assertTrue(s.onComplete(10_000 + CraftingState.TIMEOUT_MS + 100));
        assertEquals(CraftingState.Result.SUCCESS, s.result());
        assertEquals(1.0f, s.advance(20_000), 1e-6);
    }

    @Test
    void explicitFailureKeepsFillAndOnlyAppliesWhileCrafting() {
        CraftingState s = new CraftingState();
        assertFalse(s.onFailure(0)); // nothing crafting
        s.onProgress(0.5f, null, 0);
        float before = s.advance(2000);
        assertTrue(s.onFailure(2000));
        assertEquals(before, s.advance(2500), 1e-6);
        assertEquals(CraftingState.Result.FAILURE, s.result());
        assertFalse(s.onFailure(2100));
        assertFalse(s.visible(2000 + CraftingState.HOLD_MS));
    }

    @Test
    void mapsStationTitles() {
        assertEquals("zeugtisch", CraftingStation.forTitle("Zeugtisch").orElseThrow().texture());
        assertEquals(6, CraftingStation.forTitle(" Zeugtisch ").orElseThrow().frames());
        assertEquals(3, CraftingStation.forTitle("Zeugtisch").orElseThrow().hitFrame());
        assertTrue(CraftingStation.forTitle("Truhe").isEmpty());
        assertTrue(CraftingStation.forTitle(null).isEmpty());
    }

    @Test
    void stateInterpolatesHidesAndResets() {
        CraftingState s = new CraftingState();
        s.onProgress(0.1f, null, 0);
        assertTrue(s.visible(0));
        float a = s.advance(500);
        assertTrue(a > 0.0f && a <= 0.1f);
        s.onProgress(0.2f, null, 1000);
        float b = s.advance(1500);
        assertTrue(b >= a && b <= 0.2f);
        assertEquals(b, s.advance(1500), 1e-6); // never backwards
        assertFalse(s.visible(1000 + CraftingState.TIMEOUT_MS + 1));
        s.onProgress(0.5f, null, 5000);
        s.onProgress(0.1f, null, 6000); // lower value = reset
        assertTrue(s.advance(6000) <= 0.1f); // restarted below the old 0.5, not continued from it
        s.onComplete(6100);
        assertEquals(CraftingState.Result.SUCCESS, s.result());
        assertEquals(1.0f, s.advance(6200), 1e-6);
        assertFalse(s.visible(6100 + CraftingState.HOLD_MS));
    }
}
