package de.ottoextra.ailments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure logic for the server menu ("Menü", a generic_9x6 container opened with the hotbar book):
 * recognising the menu and reading the condition items from its slots.
 */
public final class MenuReader {

    public static final String MENU_HANDLER = "minecraft:generic_9x6";
    public static final String CONDITION_ITEM = "minecraft:amethyst_shard";
    public static final String MENU_ITEM = "minecraft:enchanted_book";
    /** Resource pack glyph title of the menu; only a hint, the buttons decide. */
    public static final String MENU_TITLE_GLYPH = "ꈃ쇳";
    /** Container slots that hold conditions (the two bottom rows of the chest). */
    public static final int FIRST_CONDITION_SLOT = 36;
    public static final int LAST_CONDITION_SLOT = 53;
    public static final int MAX_CONDITIONS = LAST_CONDITION_SLOT - FIRST_CONDITION_SLOT + 1;

    private static final Set<String> BUTTON_NAMES = Set.of(
            "meine fertigkeiten", "meine quests", "mein gefolge", "mein charakter", "kalender",
            "hilfe & support", "ottonien unterstützen");

    private MenuReader() {
    }

    /** One slot as far as this reader cares; {@code container} is false for player inventory slots. */
    public record SlotView(int index, boolean container, String itemId, String name, List<String> lore) {
    }

    /**
     * @param handlerId   registry id of the screen handler type
     * @param title       plain screen title
     * @param slots       all slots of the handler
     * @param expectedNow true shortly after the chat line "Du öffnest dein Menü."
     */
    public static boolean isMenu(String handlerId, String title, List<SlotView> slots, boolean expectedNow) {
        if (!MENU_HANDLER.equals(handlerId) || slots == null) return false;
        int buttons = 0;
        for (SlotView slot : slots) {
            if (slot.container() && slot.index() < FIRST_CONDITION_SLOT
                    && BUTTON_NAMES.contains(AilmentNames.plain(slot.name()).toLowerCase(java.util.Locale.ROOT))) {
                buttons++;
            }
        }
        boolean hint = expectedNow || (title != null && title.contains(MENU_TITLE_GLYPH));
        return buttons >= (hint ? 1 : 2);
    }

    /** The chat line the server sends when the menu opens. */
    public static boolean isOpenMessage(String plainChatLine) {
        return plainChatLine != null && AilmentNames.plain(plainChatLine).contains("Du öffnest dein Menü");
    }

    /** True for the hotbar book that opens the menu. */
    public static boolean isMenuItem(String itemId, String name, List<String> lore) {
        if (!MENU_ITEM.equals(itemId)) return false;
        if ("Menü".equals(AilmentNames.plain(name))) return true;
        if (lore != null) {
            for (String line : lore) {
                if (AilmentNames.plain(line).contains("Öffne dein Menü")) return true;
            }
        }
        return false;
    }

    /** All amethyst shards in container slots 36 to 53, in slot order. */
    public static List<Condition> readConditions(List<SlotView> slots) {
        List<Condition> out = new ArrayList<>();
        if (slots == null) return out;
        List<SlotView> sorted = new ArrayList<>(slots);
        sorted.sort(java.util.Comparator.comparingInt(SlotView::index));
        for (SlotView slot : sorted) {
            if (!slot.container() || slot.index() < FIRST_CONDITION_SLOT || slot.index() > LAST_CONDITION_SLOT
                    || !CONDITION_ITEM.equals(slot.itemId())) {
                continue;
            }
            Condition.fromItem(slot.name(), slot.lore()).ifPresent(out::add);
            if (out.size() >= MAX_CONDITIONS) break;
        }
        return out;
    }
}
