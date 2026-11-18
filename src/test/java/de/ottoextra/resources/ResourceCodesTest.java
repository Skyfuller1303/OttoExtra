package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCodesTest {
    /** Fixed vector shared with the list service. */
    private static final String WRITE = "ABCDEFGHJKMNPQRSTVWX";
    private static final String READ = "R4092001788584BA284ADC90E";
    private static final String LIST_ID =
            "7cca747513f36dbc81e0f7fe4c0b6c7eeb684b4f903a8d48af712477aab3f75c";

    @Test
    void derivesReadCodeAndListIdFromTheWriteCode() {
        assertEquals(READ, ResourceCodes.readCode(WRITE));
        assertEquals(LIST_ID, ResourceCodes.listId(READ));
        System.out.println("TEST VECTOR write=" + ResourceCodes.display(WRITE) + " read="
                + ResourceCodes.readCode(WRITE) + " listId=" + ResourceCodes.listId(ResourceCodes.readCode(WRITE)));
    }

    @Test
    void derivationIgnoresCaseHyphensAndSpaces() {
        assertEquals(READ, ResourceCodes.readCode("abcde-fghjk mnpqr-stvwx"));
        assertEquals(LIST_ID, ResourceCodes.listId("r4092001788584ba284adc90e"));
        assertEquals(WRITE, ResourceCodes.normalize(" ABCDE-FGHJK-mnpqr-STVWX "));
    }

    @Test
    void readCodeHasTwentyFiveCharactersAndStartsWithR() {
        String read = ResourceCodes.readCode(WRITE);
        assertEquals(25, read.length());
        assertEquals('R', read.charAt(0));
        assertEquals(64, ResourceCodes.listId(read).length());
        assertEquals(ResourceCodes.listId(read).toLowerCase(), ResourceCodes.listId(read));
    }

    @Test
    void generatedWriteCodesUseOnlyTheAllowedAlphabet() {
        Random random = new Random(7);
        for (int index = 0; index < 200; index++) {
            String code = ResourceCodes.generateWriteCode(random);
            assertEquals(20, code.length());
            assertTrue(code.chars().allMatch(c -> ResourceCodes.ALPHABET.indexOf(c) >= 0));
            assertEquals(ResourceCodes.Kind.WRITE, ResourceCodes.kind(code));
        }
        assertEquals(ResourceCodes.Kind.WRITE, ResourceCodes.kind(ResourceCodes.generateWriteCode()));
    }

    @Test
    void displayGroupsByFive() {
        assertEquals("ABCDE-FGHJK-MNPQR-STVWX", ResourceCodes.display(WRITE));
        assertEquals("R4092-00178-8584B-A284A-DC90E", ResourceCodes.display(READ));
    }

    @Test
    void kindIsDecidedByLengthAndCharacters() {
        assertEquals(ResourceCodes.Kind.WRITE, ResourceCodes.kind("ABCDE-FGHJK-MNPQR-STVWX"));
        assertEquals(ResourceCodes.Kind.READ, ResourceCodes.kind(READ));
        assertEquals(ResourceCodes.Kind.READ, ResourceCodes.kind(ResourceCodes.display(READ).toLowerCase()));
        assertEquals(ResourceCodes.Kind.INVALID, ResourceCodes.kind(""));
        assertEquals(ResourceCodes.Kind.INVALID, ResourceCodes.kind(null));
        // 'I', 'L', 'O', 'U', '0', '1' are not in the alphabet.
        assertEquals(ResourceCodes.Kind.INVALID, ResourceCodes.kind("ABCDEFGHIKMNPQRSTVWX"));
        assertEquals(ResourceCodes.Kind.INVALID, ResourceCodes.kind("RZ092001788584BA284ADC90E"));
        assertEquals(ResourceCodes.Kind.INVALID, ResourceCodes.kind("ABCDEFGHJKMNPQRSTVW"));
    }
}
