package de.ottoextra.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Zugriff auf den ORIGINALEN, server-gesetzten Tablist-Displaynamen
 * ({@code displayName}-Feld) — unser {@link PlayerListEntryMixin} überschreibt
 * {@code getDisplayName()}, daher muss der Titel-Sync das Rohfeld lesen, sonst
 * crawlt er den bereits angepassten Wert.
 */
@Mixin(net.minecraft.client.network.PlayerListEntry.class)
public interface PlayerListEntryAccessor {

    @Accessor("displayName")
    Text ottoextra$rawDisplayName();
}
