package de.ottoextra.mixin;

import de.ottoextra.playerlist.PlayerListSortingModule;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Comparator;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @ModifyArg(
            method = "collectPlayerEntries",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"
            ),
            index = 0
    )
    private Comparator<PlayerListEntry> ottoextra$sortByLehen(
            Comparator<PlayerListEntry> vanilla) {
        return PlayerListSortingModule.wrapComparator(vanilla);
    }
}
