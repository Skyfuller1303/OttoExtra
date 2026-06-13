package de.ottoextra.letter;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.letter.model.AnnouncementSendProgress;
import de.ottoextra.letter.model.LetterSendProgress;
import de.ottoextra.letter.ui.LetterEditorScreen;
import de.ottoextra.letter.ui.RecoveryPromptScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Modul: Briefsystem (Quelle: OttoLetter, v2-Spec ottoletter-integration-md-v2).
 *
 * <p>Editor mit Seiten/Paste/Buchimport/Platzhaltern, Modusdialog
 * Brief vs. Verkündung, robuster Versand mit Recovery
 * ({@code .cache/letters/}). Hotkey öffnet den Editor (Standard unbelegt);
 * nach Join wird unfertiger Versand erkannt (Fortsetzen/Neu/Verwerfen).</p>
 */
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

        // Rechtsklick mit dem Trigger-Item (Custom-Name aus der Config,
        // z. B. "Pergament und Feder") öffnet den Editor — nur clientseitig
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register(
                (player, world, hand) -> {
                    if (!world.isClient() || config.letter.triggerItemName == null
                            || config.letter.triggerItemName.isBlank()) {
                        return net.minecraft.util.ActionResult.PASS;
                    }
                    var stack = player.getStackInHand(hand);
                    // tolerant matchen: 1.21.5+ serialisiert custom_name teils
                    // verschachtelt (Quotes/Escapes) — Quotes strippen + contains
                    String shown = stack.isEmpty() ? "" : stack.getName().getString()
                            .replace("\"", "").replace("'", "").trim();
                    String trigger = config.letter.triggerItemName.trim();
                    if (!shown.isEmpty() && (shown.equalsIgnoreCase(trigger)
                            || shown.toLowerCase(java.util.Locale.ROOT)
                                    .contains(trigger.toLowerCase(java.util.Locale.ROOT)))) {
                        net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                                net.minecraft.client.MinecraftClient.getInstance().setScreen(
                                        new LetterEditorScreen(null, config)));
                        return net.minecraft.util.ActionResult.SUCCESS;
                    }
                    return net.minecraft.util.ActionResult.PASS;
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (editorKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new LetterEditorScreen(null, config));
                }
            }
            // Versand-Status in der Actionbar mit animierten Punkten anzeigen
            if (LetterServices.isSending() && client.player != null && client.world != null) {
                int dots = (int) (client.world.getTime() / 5 % 4);
                String key = LetterServices.isSendingAnnouncement()
                        ? "ottoextra.letter.actionbar.announcement"
                        : "ottoextra.letter.actionbar.letter";
                net.minecraft.text.Text msg = net.minecraft.text.Text.translatable(key)
                        .copy().append(".".repeat(dots));
                client.player.sendMessage(msg, true);
            }

            // Recovery einmalig nach Join prüfen (Spieler + Welt vorhanden)
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

        OttoExtra.LOGGER.info("[letter] initialisiert (Editor + Brief/Verkündung + Recovery).");
    }

    @Override
    public void onDisconnect(OttoExtraContext context) {
        recoveryChecked = false;
        // Versand wurde von der Queue bei Verbindungsverlust gestoppt;
        // Actionbar-Status nicht über den Reconnect hinweg hängen lassen
        LetterServices.clearSendingState();
    }
}
