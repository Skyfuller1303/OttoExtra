package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Beschriebene Briefe (written_book) wieder bearbeiten.
 *
 * <p>Liest das in der Hand gehaltene beschriebene Buch, prüft ob der Spieler
 * selbst Autor ist, konvertiert die (gestylten) Buchseiten zurück in §-codierte
 * Editor-Seiten (Farbe + Formatierung bleiben erhalten — anders als beim
 * Klartext-Import) und öffnet den {@link LetterEditorScreen} mit dem Inhalt.</p>
 *
 * <p>Der „Bearbeiten"-Button wird in {@code LetterModule} an die Vanilla-Lese-GUI
 * ({@code BookScreen}) gehängt, aber nur für eigene Briefe.</p>
 */
public final class WrittenLetterImport {

    private WrittenLetterImport() {
    }

    /** Beschriebenes Buch in Haupt-/Nebenhand, oder {@code null}. */
    public static WrittenBookContentComponent heldWrittenBook() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }
        for (Hand hand : Hand.values()) {
            ItemStack stack = client.player.getStackInHand(hand);
            WrittenBookContentComponent written =
                    stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
            if (written != null) {
                return written;
            }
        }
        return null;
    }

    /** Wurde das Buch vom lokalen Spieler verfasst? */
    public static boolean isOwn(WrittenBookContentComponent book) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (book == null || client.player == null) {
            return false;
        }
        String me = client.player.getGameProfile().name();
        return me != null && me.equalsIgnoreCase(book.author());
    }

    /** Buchinhalt in einen neuen Entwurf übernehmen und den Editor öffnen. */
    public static void editInEditor(OttoExtraConfig config, WrittenBookContentComponent book) {
        LetterDraft draft = new LetterDraft();
        draft.meta.draftId = UUID.randomUUID().toString().substring(0, 8);
        draft.meta.importedFromBook = true;
        draft.meta.pagesOneToOne = true;
        draft.pages = toSectionPages(book);
        if (draft.pages.isEmpty()) {
            draft.pages.add("");
        }
        LetterDraftCache.save(draft);
        MinecraftClient.getInstance().setScreen(new LetterEditorScreen(null, config));
    }

    /** Alle Buchseiten als §-codierte Strings (Formatierung rekonstruiert). */
    public static List<String> toSectionPages(WrittenBookContentComponent book) {
        List<String> out = new ArrayList<>();
        for (Text page : book.getPages(false)) {
            out.add(toSection(page));
        }
        return out;
    }

    /**
     * Gestyltes Text-Component einer Buchseite in einen §-String zurückwandeln.
     * Jeder Style-Lauf bekommt seine Codes vorangestellt; ein formatierter Lauf,
     * der von einem unformatierten gefolgt wird, wird mit §r zurückgesetzt.
     * Liegen die Codes bereits als Literale (§/&) im Text, bleiben sie erhalten.
     */
    private static String toSection(Text page) {
        StringBuilder sb = new StringBuilder();
        boolean[] prevFormatted = {false};
        page.visit((style, asString) -> {
            String codes = codesFor(style);
            if (codes.isEmpty()) {
                if (prevFormatted[0]) {
                    sb.append("§r");
                }
            } else {
                sb.append(codes);
            }
            prevFormatted[0] = !codes.isEmpty();
            sb.append(asString);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static String codesFor(Style s) {
        StringBuilder c = new StringBuilder();
        TextColor col = s.getColor();
        if (col != null) {
            Formatting f = colorToFormatting(col);
            if (f != null) {
                c.append('§').append(f.getCode());
            }
        }
        if (s.isBold()) {
            c.append("§l");
        }
        if (s.isItalic()) {
            c.append("§o");
        }
        if (s.isUnderlined()) {
            c.append("§n");
        }
        if (s.isStrikethrough()) {
            c.append("§m");
        }
        if (s.isObfuscated()) {
            c.append("§k");
        }
        return c.toString();
    }

    /** Nächste Vanilla-Formatierungsfarbe zu einem TextColor, oder {@code null}. */
    private static Formatting colorToFormatting(TextColor col) {
        int rgb = col.getRgb();
        for (Formatting f : Formatting.values()) {
            Integer value = f.getColorValue();
            if (f.isColor() && value != null && value == rgb) {
                return f;
            }
        }
        return null;
    }
}
