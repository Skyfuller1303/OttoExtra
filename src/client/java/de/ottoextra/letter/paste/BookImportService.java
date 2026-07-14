package de.ottoextra.letter.paste;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public final class BookImportService {

    private BookImportService() {
    }

    public static List<String> readHeldBook() {
        List<String> pages = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return pages;
        }
        for (Hand hand : Hand.values()) {
            ItemStack stack = client.player.getStackInHand(hand);
            WritableBookContentComponent writable =
                    stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
            if (writable != null) {
                writable.stream(false).forEach(p -> pages.add(TextNormalizer.normalize(p)));
                return pages;
            }
            WrittenBookContentComponent written =
                    stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
            if (written != null) {
                for (Text page : written.getPages(false)) {
                    pages.add(TextNormalizer.normalize(page.getString()));
                }
                return pages;
            }
        }
        return pages;
    }
}
