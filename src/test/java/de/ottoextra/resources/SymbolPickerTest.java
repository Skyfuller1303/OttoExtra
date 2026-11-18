package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolPickerTest {
    @Test
    void twelveShapesInTwoRowsAndTheArmsTileBelowTheFirstSwatch() {
        assertEquals("marker", SymbolPicker.shapeAt(0, 0, 1, 1));
        assertEquals("beutel", SymbolPicker.shapeAt(0, 0, 5 * 20 + 1, 20 + 1));
        int ax = SymbolPicker.armsX(0) + 1;
        int ay = SymbolPicker.armsY(0) + 1;
        assertTrue(SymbolPicker.armsAt(0, 0, ax, ay));
        assertNull(SymbolPicker.shapeAt(0, 0, ax, ay));
        assertNull(SymbolPicker.colorAt(0, 0, ax, ay));
        assertFalse(SymbolPicker.armsAt(0, 0, 1, 1));
        assertTrue(SymbolPicker.armsY(0) + SymbolPicker.BOX <= SymbolPicker.HEIGHT);
        assertTrue(SymbolPicker.armsX(0) + SymbolPicker.BOX <= SymbolPicker.width());
    }
}
