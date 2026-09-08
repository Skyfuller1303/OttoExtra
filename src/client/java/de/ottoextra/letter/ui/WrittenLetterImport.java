package de.ottoextra.letter.ui;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterDraftCache;
import de.ottoextra.letter.LetterSendMode;
import de.ottoextra.letter.LetterWriteItemGuard;
import de.ottoextra.letter.paste.PageSplitter;
import de.ottoextra.letter.send.LetterV2Planner;
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
        ItemStack stack = client.player.getMainHandStack();
        return stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) == null ? null : stack;
    }

    public static boolean isOwn(ItemStack stack) {
        // Courier permits editing any unsent letter in the main hand, irrespective of author.
        return LetterWriteItemGuard.isWritableLetter(stack);
    }

    public static boolean hasPages(ItemStack stack, List<String> expectedPages) {
        WrittenBookContentComponent book = stack == null
                ? null : stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        return book != null && expectedPages != null
                && toSectionPages(book).equals(expectedPages);
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
        if (!LetterWriteItemGuard.isWritableLetter(stack)) {
            return;
        }
        WrittenBookContentComponent book = stack == null
                ? null : stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        LetterDraft draft = new LetterDraft();
        draft.meta.draftId = UUID.randomUUID().toString().substring(0, 8);
        draft.meta.importedFromBook = true;
        draft.meta.pagesOneToOne = false;

        List<String> imported = book == null ? new ArrayList<>() : toSectionPages(book);
        if (imported.isEmpty()) {
            imported.add("");
        }
        draft.meta.courierInitialPages = new ArrayList<>(imported);
        List<String> pages = LetterSendMode.from(config.letter.sendMode) == LetterSendMode.V2
                ? new ArrayList<>(imported) : repaginate(config, imported);
        draft.meta.pagesOneToOne = LetterSendMode.from(config.letter.sendMode)
                == LetterSendMode.V2;

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
        int maxLines = switch (LetterSendMode.from(config.letter.sendMode)) {
            case LEGACY -> config.letter.maxLinesPerPage;
            case PAGE -> config.letter.pageModeMaxLinesPerPage;
            case V2 -> LetterV2Planner.BOOK_MAX_LINES;
        };

        if (LetterSendMode.from(config.letter.sendMode) == LetterSendMode.V2) {
            return new ArrayList<>(LetterV2Planner.planFlowing(config.letter.letterCommand,
                    List.of(cleanImported(imported)), tr::getWidth).predictedPages());
        }

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

    private static String cleanImported(List<String> imported) {
        String[] rawLines = String.join("\n", imported).split("\n", -1);
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < rawLines.length; i++) {
            if (i > 0) {
                normalized.append('\n');
            }
            normalized.append(rawLines[i].stripTrailing());
        }
        return normalized.toString().replaceAll("\\n+$", "");
    }

    public static List<String> toSectionPages(WrittenBookContentComponent book) {
        List<String> out = new ArrayList<>();
        for (Text page : book.getPages(false)) {
            out.add(toSection(page));
        }
        return out;
    }

    public static String toSection(Text page) {
        StringBuilder sb = new StringBuilder();
        String[] previousCodes = {""};
        page.visit((style, asString) -> {
            if (asString.isEmpty()) {
                return Optional.empty();
            }
            String codes = codesFor(style);
            if (!codes.equals(previousCodes[0])) {
                boolean startsWithColor = codes.length() >= 2
                        && "0123456789abcdef".indexOf(codes.charAt(1)) >= 0;
                if (!previousCodes[0].isEmpty() && !startsWithColor) {
                    sb.append("§r");
                }
                sb.append(codes);
            }
            previousCodes[0] = codes;
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
