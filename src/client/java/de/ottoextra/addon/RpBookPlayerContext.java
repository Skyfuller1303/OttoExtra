package de.ottoextra.addon;

import net.minecraft.client.gui.screen.Screen;

import java.util.UUID;

/** Selected player in OttoExtra's RP people book. */
public record RpBookPlayerContext(
        UUID playerUuid,
        String accountName,
        String rpName,
        String title,
        Screen parent
) {
}
