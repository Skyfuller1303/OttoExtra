package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.paste.PageSplitter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    /** Beschriebenes Buch (written_book) in Haupt-/Nebenhand, oder {@code null}. */
    public static ItemStack heldWrittenBookStack() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }
        for (Hand hand : Hand.values()) {
            ItemStack stack = client.player.getStackInHand(hand);
            if (stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null) {
                return stack;
            }
        }
        return null;
    }

    /**
     * Stammt der Brief vom lokalen Spieler? Der Server kennzeichnet eigene Briefe
     * mit dem Accountnamen (z. B. „Brief von Skyfuller1303") — geprüft werden
     * Item-Anzeigename, Lore-Zeilen und der Buchautor. Der Accountname wird über
     * das GameProfile (an die Spieler-UUID gebunden) ermittelt.
     */
    public static boolean isOwn(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (stack == null || client.player == null) {
            return false;
        }
        String me = localAccountName();
        if (me == null || me.isBlank()) {
            return false;
        }
        String needle = me.toLowerCase(Locale.ROOT);
        // 1) Item-Anzeigename (custom_name oder Default, z. B. "Brief von <Name>")
        if (containsIgnoreCase(stack.getName(), needle)) {
            return true;
        }
        // 2) Lore-Zeilen
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                if (line.getString().toLowerCase(Locale.ROOT).contains(needle)) {
                    return true;
                }
            }
        }
        // 3) Autor des beschriebenen Buchs
        WrittenBookContentComponent book = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        return book != null && book.author() != null
                && book.author().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean containsIgnoreCase(Text text, String needleLower) {
        return text != null
                && text.getString().toLowerCase(Locale.ROOT).contains(needleLower);
    }

    /** Accountname des lokalen Spielers über das GameProfile (UUID-gebunden),
     *  mit Session-Username als Fallback. */
    private static String localAccountName() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getGameProfile() != null) {
            String name = client.player.getGameProfile().name();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return client.getSession() != null ? client.getSession().getUsername() : null;
    }

    /**
     * Buchinhalt in einen neuen Entwurf übernehmen und den Editor öffnen. Der
     * vorhandene Text wird als „bereits geschrieben" gesperrt (read-only, reduzierte
     * Deckkraft) — nur neu angehängter Text wird beim Schreiben per /letter gesendet.
     */
    public static void editInEditor(OttoExtraConfig config, ItemStack stack) {
        WrittenBookContentComponent book = stack == null
                ? null : stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        LetterDraft draft = new LetterDraft();
        draft.meta.draftId = UUID.randomUUID().toString().substring(0, 8);
        draft.meta.importedFromBook = true;
        draft.meta.pagesOneToOne = false;

        // Importierten Text zu vollen Editor-Seiten (12 Zeilen) umbrechen, statt
        // die Buchseiten 1:1 zu übernehmen — so wird der Platz pro Seite genutzt.
        List<String> imported = book == null ? new ArrayList<>() : toSectionPages(book);
        List<String> pages = repaginate(config, imported);

        // Eine Leerzeile zwischen importiertem Text und neuem Editor-Bereich; der
        // alte Text (inkl. Trenn-Leerzeile) wird gesperrt, der Cursor steht direkt
        // dahinter — bei Platz auf derselben Seite, sonst auf der nächsten.
        int last = pages.size() - 1;
        String content = pages.get(last);
        String withBlank = content.isEmpty() ? "" : content + "\n";
        pages.set(last, withBlank);

        draft.pages = pages;
        draft.meta.lockedPages = last;
        draft.meta.lockedOffset = withBlank.length();
        LetterDraftCache.save(draft);
        MinecraftClient.getInstance().setScreen(new LetterEditorScreen(null, config));
    }

    /** Importierten Text zu vollen Editor-Seiten (12 Zeilen / Zeichen-Budget)
     *  umbrechen — gleiche Limits wie der Editor. */
    private static List<String> repaginate(OttoExtraConfig config, List<String> imported) {
        var tr = MinecraftClient.getInstance().textRenderer;
        if (tr == null || imported.isEmpty()) {
            List<String> fallback = new ArrayList<>(imported);
            if (fallback.isEmpty()) {
                fallback.add("");
            }
            return fallback;
        }
        int maxLines = "PAGE".equalsIgnoreCase(config.letter.sendMode)
                ? config.letter.pageModeMaxLinesPerPage : config.letter.maxLinesPerPage;
        // 108 = Buch-Zeilenbreite in Pixeln (WYSIWYG), wie im Editor.
        PageSplitter sp = new PageSplitter(tr::getWidth, 108, maxLines,
                config.letter.pageModeEffectiveCharBudget);
        // Server-Briefzeilen haben Trailing-Spaces; beim Wortumbruch erzeugt ein
        // solcher Rest-Space am Zeilenende eine Phantom-Leerzeile. Daher jede Zeile
        // rechts trimmen (Figure-Space   = bewusste Leerzeile bleibt erhalten)
        // und Trailing-Leerzeilen entfernen.
        String joined = String.join("\n", imported);
        String[] rawLines = joined.split("\n", -1);
        StringBuilder norm = new StringBuilder();
        for (int i = 0; i < rawLines.length; i++) {
            if (i > 0) {
                norm.append('\n');
            }
            norm.append(rawLines[i].stripTrailing());
        }
        String cleaned = norm.toString().replaceAll("\\n+$", "");
        List<String> pages = new ArrayList<>(sp.split(cleaned));
        if (pages.isEmpty()) {
            pages.add("");
        }
        return pages;
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
