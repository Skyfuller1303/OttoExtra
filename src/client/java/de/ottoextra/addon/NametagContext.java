package de.ottoextra.addon;

import net.minecraft.text.Text;

import java.util.UUID;

/** Stable, read-only information made available to nametag addons. */
public record NametagContext(
        UUID playerUuid,
        String accountName,
        Text vanillaName,
        boolean onlinePlayer,
        boolean baseModuleEnabled,
        boolean baseVisible
) {
}
