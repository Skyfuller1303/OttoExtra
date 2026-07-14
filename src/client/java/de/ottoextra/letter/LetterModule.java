package de.ottoextra.letter;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.model.AnnouncementSendProgress;
import de.ottoextra.letter.model.LetterSendProgress;
import de.ottoextra.letter.ui.LetterActionPrompt;
import de.ottoextra.letter.ui.LetterEditorScreen;
import de.ottoextra.letter.ui.RecoveryPromptScreen;
import de.ottoextra.letter.ui.WrittenLetterImport;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class LetterModule implements OttoExtraModule {

    private KeyBinding editorKey;
    private boolean recoveryChecked = false;

    @Override
    public String id() {
        return "letter";
    }

    @Override
    public boolean enabled(OttoExtraConfig config) {
        return config.letter.enabled;
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        OttoExtraConfig config = context.config();

        editorKey = new KeyBinding("key.ottoextra.letter_editor",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(editorKey);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("letter")
                                .then(ClientCommandManager.literal("send").executes(ctx -> {
                                    LetterActionPrompt.onSend(config);
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("announce").executes(ctx -> {
                                    LetterActionPrompt.onAnnounce(config);
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("close").executes(ctx -> {
                                    LetterActionPrompt.onClose();
                                    return 1;
                                })))));

        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> {
                    if (!(screen instanceof net.minecraft.client.gui.screen.ingame.BookScreen)) {
                        return;
                    }
                    var stack = WrittenLetterImport.heldWrittenBookStack();
                    if (stack == null || !WrittenLetterImport.isOwn(stack)) {
                        return;
                    }
                    var buttons = net.fabricmc.fabric.api.client.screen.v1.Screens
                            .getButtons(screen);

                    int bottom = 0;
                    for (var w : buttons) {
                        if (w instanceof net.minecraft.client.gui.widget.ClickableWidget cw) {
                            bottom = Math.max(bottom, cw.getY() + cw.getHeight());
                        }
                    }
                    int y = Math.min(bottom + 4, scaledHeight - 24);
                    buttons.add(net.minecraft.client.gui.widget.ButtonWidget.builder(
                                    net.minecraft.text.Text.translatable(
                                            "ottoextra.letter.book.edit"),
                                    b -> WrittenLetterImport.editInEditor(config, stack))
                            .dimensions(scaledWidth / 2 - 100, y, 200, 20).build());
                });

        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register(
                (player, world, hand) -> {
                    if (!world.isClient() || config.letter.triggerItemName == null
                            || config.letter.triggerItemName.isBlank()) {
                        return net.minecraft.util.ActionResult.PASS;
                    }
                    var stack = player.getStackInHand(hand);

                    String shown = stack.isEmpty() ? "" : stack.getName().getString()
                            .replace("\"", "").replace("'", "").trim();
                    String trigger = config.letter.triggerItemName.trim();
                    if (!shown.isEmpty() && (shown.equalsIgnoreCase(trigger)
                            || shown.toLowerCase(java.util.Locale.ROOT)
                                    .contains(trigger.toLowerCase(java.util.Locale.ROOT)))) {
                        net.minecraft.client.MinecraftClient.getInstance().execute(
                                () -> openComposeEditor(config));
                        return net.minecraft.util.ActionResult.SUCCESS;
                    }
                    return net.minecraft.util.ActionResult.PASS;
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (editorKey.wasPressed()) {
                if (client.currentScreen == null) {
                    openComposeEditor(config);
                }
            }

            if (LetterServices.isSending() && client.player != null && client.world != null) {
                int dots = (int) (client.world.getTime() / 5 % 4);
                String key = LetterServices.isSendingAnnouncement()
                        ? "ottoextra.letter.actionbar.announcement"
                        : "ottoextra.letter.actionbar.letter";
                net.minecraft.text.Text msg = net.minecraft.text.Text.translatable(key)
                        .copy().append(".".repeat(dots));
                client.player.sendMessage(msg, true);
            }

            if (!recoveryChecked && client.player != null && client.currentScreen == null) {
                recoveryChecked = true;
                LetterSendProgress letter = LetterServices.letterStore().load();
                AnnouncementSendProgress announcement =
                        LetterServices.announcementStore().load();
                boolean letterOpen = letter != null
                        && letter.sentCommands < letter.pendingCommands.size();
                boolean announcementOpen = announcement != null
                        && announcement.sentCommands < announcement.pendingCommands.size();
                if (letterOpen || announcementOpen) {
                    client.setScreen(new RecoveryPromptScreen(config,
                            letterOpen ? letter : null,
                            announcementOpen ? announcement : null));
                }
            }
        });

        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.ALLOW_GAME
                .register((message, overlay) -> !isHiddenLetterHint(message.getString()));

        OttoExtra.LOGGER.info("[letter] initialisiert (Editor + Brief/Verkündung + Recovery).");
    }

    private static void openComposeEditor(OttoExtraConfig config) {
        LetterDraft cached = LetterDraftCache.load();
        if (cached.meta != null
                && (cached.meta.lockedPages > 0 || cached.meta.lockedOffset > 0)) {
            LetterDraftCache.clear();
        }
        net.minecraft.client.MinecraftClient.getInstance().setScreen(
                new LetterEditorScreen(null, config));
    }

    private static final String[] HIDDEN_LETTER_HINTS = {
            "Beschreibe den Brief mit /letter",
            "Du hast den Brief bearbeitet",
    };

    private static boolean isHiddenLetterHint(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String needle : HIDDEN_LETTER_HINTS) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        recoveryChecked = false;

        LetterServices.clearSendingState();
    }
}
