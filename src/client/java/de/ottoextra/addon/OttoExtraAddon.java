package de.ottoextra.addon;

import de.ottoextra.config.settings.SettingsRegistry;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.List;

public interface OttoExtraAddon {

    void registerSettings(SettingsRegistry registry);

    default void registerDefaultSettings(SettingsRegistry registry) {
    }

    default Text processChatMessage(Text message) {
        return message;
    }

    /**
     * Kontextreicher Chat-Hook. Der Default delegiert auf den bisherigen Hook,
     * damit bereits kompilierte beziehungsweise aeltere Addons kompatibel
     * bleiben.
     */
    default Text processChatMessage(ChatMessageContext context) {
        return processChatMessage(context.displayed());
    }

    /**
     * Gibt an, ob eingehende Chatzeilen bis zum Abschluss der asynchronen
     * Verarbeitung zurueckgehalten werden sollen.
     */
    default boolean delaysIncomingChat(ChatMessageContext context) {
        return false;
    }

    /**
     * Asynchroner Chat-Hook. Addons ohne Netzwerkarbeit bleiben durch den
     * Default vollstaendig kompatibel zum bisherigen synchronen Hook.
     */
    default CompletableFuture<Text> processChatMessageAsync(ChatMessageContext context) {
        return CompletableFuture.completedFuture(processChatMessage(context));
    }

    /** Dynamische Texte aus OttoExtra-Oberflaechen (z. B. Regionsdaten). */
    default Text processDisplayText(String scope, Text text) {
        return text;
    }

    /** Prepare or override a real online player's nametag. */
    default NametagPresentation processNametag(NametagContext context,
                                                NametagPresentation presentation) {
        return presentation;
    }

    /** Movable HUD boxes contributed to the shared HUD layout editor ({@code /ottoextra hud}). */
    default List<de.ottoextra.ui.hudlayout.HudLayoutElement> hudLayoutElements() {
        return List.of();
    }

    /** Extra labelled actions shown in the selected RP-book player profile. */
    default List<RpBookPlayerField> rpBookPlayerFields(RpBookPlayerContext context) {
        return List.of();
    }
}
