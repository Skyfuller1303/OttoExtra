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

public final class WrittenLetterImport {

    private WrittenLetterImport() {
    }

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

        if (containsIgnoreCase(stack.getName(), needle)) {
            return true;
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                if (line.getString().toLowerCase(Locale.ROOT).contains(needle)) {
                    return true;
                }
            }
        }

        WrittenBookContentComponent book = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        return book != null && book.author() != null
                && book.author().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean containsIgnoreCase(Text text, String needleLower) {
        return text != null
                && text.getString().toLowerCase(Locale.ROOT).contains(needleLower);
    }

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

    public static void editInEditor(OttoExtraConfig config, ItemStack stack) {
        WrittenBookContentComponent book = stack == null
                ? null : stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        LetterDraft draft = new LetterDraft();
        draft.meta.draftId = UUID.randomUUID().toString().substring(0, 8);
        draft.meta.importedFromBook = true;
        draft.meta.pagesOneToOne = false;

        List<String> imported = book == null ? new ArrayList<>() : toSectionPages(book);
        List<String> pages = repaginate(config, imported);

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

        PageSplitter sp = new PageSplitter(tr::getWidth, 108, maxLines,
                config.letter.pageModeEffectiveCharBudget);

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

    public static List<String> toSectionPages(WrittenBookContentComponent book) {
        List<String> out = new ArrayList<>();
        for (Text page : book.getPages(false)) {
            out.add(toSection(page));
        }
        return out;
    }

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
