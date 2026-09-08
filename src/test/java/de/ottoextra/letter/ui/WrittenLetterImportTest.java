package de.ottoextra.letter.ui;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WrittenLetterImportTest {
    @Test void italicDoesNotInheritBold() {
        Text page = Text.empty().append(Text.literal("A").formatted(Formatting.BOLD))
                .append(Text.literal("B").formatted(Formatting.ITALIC));
        assertEquals("§lA§r§oB", WrittenLetterImport.toSection(page));
    }
    @Test void uncoloredBoldDoesNotInheritRed() {
        Text page = Text.empty().append(Text.literal("A").formatted(Formatting.RED))
                .append(Text.literal("B").formatted(Formatting.BOLD));
        assertEquals("§cA§r§lB", WrittenLetterImport.toSection(page));
    }
    @Test void identicalStylesAreNotRepeated() {
        Text page = Text.empty().append(Text.literal("A").formatted(Formatting.BOLD))
                .append(Text.literal("B").formatted(Formatting.BOLD));
        assertEquals("§lAB", WrittenLetterImport.toSection(page));
    }
    @Test void plainTextResetsStyleAndRawCourierCodesStayIntact() {
        Text page = Text.empty().append(Text.literal("A").formatted(Formatting.BOLD))
                .append(Text.literal("B"));
        assertEquals("§lA§rB", WrittenLetterImport.toSection(page));
        assertEquals("§6Brief\n§rText", WrittenLetterImport.toSection(Text.literal("§6Brief\n§rText")));
    }
}
