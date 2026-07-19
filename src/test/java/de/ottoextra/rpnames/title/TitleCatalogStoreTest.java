package de.ottoextra.rpnames.title;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TitleCatalogStoreTest {
    @Test
    void removedOriginalTitleUsesFirstVariantAsReplacementEverywhere() {
        TitleCatalogStore.Entry entry = new TitleCatalogStore.Entry();
        entry.title = "Soldenære";
        entry.variants = List.of("Sölder");
        entry.aliases = List.of("Soldenære");

        assertEquals("Sölder", TitleCatalogStore.displayForm(entry, "Soldenære"));
    }

    @Test
    void originalTitleRemainsCanonicalWhenStillListedAsVariant() {
        TitleCatalogStore.Entry entry = new TitleCatalogStore.Entry();
        entry.title = "Vogt";
        entry.variants = List.of("Vogt", "Vogtin");
        entry.aliases = List.of("Vogt", "Vogtin");

        assertEquals("Vogt", TitleCatalogStore.displayForm(entry, "Vogt"));
        assertEquals("Vogtin", TitleCatalogStore.displayForm(entry, "Vogtin"));
    }
}
