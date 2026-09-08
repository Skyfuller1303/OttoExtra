package de.ottoextra.letter.send;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CourierNewProtocolTest {
    @Test void rejoinsBukkitArgumentsWithTrailingSpace() {
        assertEquals("A B ", CourierNewProtocol.rejoinArguments("  A   B  "));
    }
    @Test void decodesPluginPlaceholdersAndColors() {
        assertEquals("§aGrün\nC:\\Briefe ", CourierNewProtocol.format("&aGrün\\nC:\\Briefe "));
        assertEquals("§7[§6Courier§7]", CourierNewProtocol.format("%prefix%"));
    }
    @Test void appendBeginsOnNewLogicalLine() {
        assertEquals(List.of("A\nX "), CourierNewProtocol.apply(List.of("A"), "X"));
    }
    @Test void emptyLastPageIsReused() {
        assertEquals(List.of("X "), CourierNewProtocol.apply(List.of(""), "X"));
    }
    @Test void emptyLogicalLinesAreDiscardedByServer() {
        assertEquals(List.of("A\nB "), CourierNewProtocol.apply(List.of(), "A\\n\\nB"));
    }
    @Test void twelfthShortLineFitsButThirteenthStartsAnotherPage() {
        String eleven = String.join("\n", java.util.Collections.nCopies(11, "x"));
        List<String> twelve = CourierNewProtocol.apply(List.of(eleven), "y");
        assertEquals(List.of(eleven + "\ny "), twelve);
        assertEquals(List.of(eleven + "\ny ", "z "), CourierNewProtocol.apply(twelve, "z"));
    }
    @Test void formattingCountsTowardServerVisualEstimate() {
        assertEquals(2, CourierNewProtocol.countRenderedLines("§6" + "x".repeat(17)));
        assertTrue(CourierNewProtocol.wouldOverflow("x".repeat(230), "abc"));
    }
    @Test void serverLongWordBugIsModeledRatherThanHidden() {
        // Original algorithm adds an empty page, then commits an oversized word anyway.
        assertEquals(List.of("", "x".repeat(260)),
                CourierNewProtocol.apply(List.of(), "x".repeat(260)));
    }
    @Test void suppliedPageListIsNotMutated() {
        List<String> initial = List.of("Alt");
        CourierNewProtocol.apply(initial, "Neu");
        assertEquals(List.of("Alt"), initial);
    }
}
