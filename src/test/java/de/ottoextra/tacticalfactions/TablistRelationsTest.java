package de.ottoextra.tacticalfactions;

import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TablistRelationsTest {
    @Test
    void relationOrderAlwaysPlacesNeutralsLast() {
        assertEquals(0, TablistRelations.relationRank(Relation.ALLIED, false));
        assertEquals(1, TablistRelations.relationRank(Relation.HOSTILE, false));
        assertEquals(2, TablistRelations.relationRank(Relation.NEUTRAL, false));

        assertEquals(0, TablistRelations.relationRank(Relation.HOSTILE, true));
        assertEquals(1, TablistRelations.relationRank(Relation.ALLIED, true));
        assertEquals(2, TablistRelations.relationRank(Relation.NEUTRAL, true));
        assertEquals(2, TablistRelations.relationRank(Relation.INHERIT, true));
    }

    @Test
    void colorsOnlyVisibleNameAndPreservesTitleColor() {
        Text original = Text.literal("Herzog ").formatted(Formatting.GOLD)
                .append(Text.literal("Alrik").formatted(Formatting.GREEN));
        TextColor relationColor = TextColor.fromRgb(0xFF5555);

        Text result = TablistRelations.colorVisibleName(
                original, List.of("Alrik", "Account"), relationColor);

        assertEquals("Herzog Alrik", result.getString());
        assertEquals(TextColor.fromFormatting(Formatting.GOLD), colorOf(result, "Herzog "));
        assertEquals(relationColor, colorOf(result, "Alrik"));
    }

    private static TextColor colorOf(Text node, String value) {
        if (node.getContent() instanceof PlainTextContent plain && plain.string().equals(value)) {
            return node.getStyle().getColor();
        }
        for (Text sibling : node.getSiblings()) {
            TextColor color = colorOf(sibling, value);
            if (color != null) return color;
        }
        return null;
    }
}
