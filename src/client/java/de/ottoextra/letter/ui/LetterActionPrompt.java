package de.ottoextra.letter.ui;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.LetterDraft;
import de.ottoextra.letter.LetterServices;
import de.ottoextra.letter.model.LetterOutputMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
public final class LetterActionPrompt {
    private LetterActionPrompt() {
    }
    public static void show(OttoExtraConfig config, LetterDraft draft) {
        LetterServices.setPending(draft);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        MutableText message = Text.translatable("ottoextra.letter.prompt.intro")
                .append(button("ottoextra.letter.prompt.send", "/ottoextra letter send",
                        0x55FF55, "ottoextra.letter.prompt.send.hover"))
                .append(Text.literal(" "))
                .append(button("ottoextra.letter.prompt.announce", "/ottoextra letter announce",
                        0xFFAA00, "ottoextra.letter.prompt.announce.hover"))
                .append(Text.literal(" "))
                .append(button("ottoextra.letter.prompt.close", "/ottoextra letter close",
                        0x888888, "ottoextra.letter.prompt.close.hover"));
        client.player.sendMessage(message, false);
    }
    public static void onSend(OttoExtraConfig config) {
        LetterDraft draft = LetterServices.pendingDraft();
        if (draft == null) {
            error();
            return;
        }
        draft.meta.mode = LetterOutputMode.BRIEF;
        MinecraftClient.getInstance().setScreen(new RecipientScreen(null, config, draft));
    }
    public static void onAnnounce(OttoExtraConfig config) {
        LetterDraft draft = LetterServices.pendingDraft();
        if (draft == null) {
            error();
            return;
        }
        draft.meta.mode = LetterOutputMode.VERKUENDUNG;
        MinecraftClient.getInstance().setScreen(
                new AnnouncementPreflightScreen(null, config, draft));
    }
    public static void onClose() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.translatable("ottoextra.letter.prompt.closed"), false);
        }
    }
    private static MutableText button(String labelKey, String command, int color, String hoverKey) {
        return Text.translatable(labelKey).styled(style -> style
                .withColor(color)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Text.translatable(hoverKey))));
    }
    private static void error() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.translatable("ottoextra.letter.prompt.none"), false);
        }
    }
}
