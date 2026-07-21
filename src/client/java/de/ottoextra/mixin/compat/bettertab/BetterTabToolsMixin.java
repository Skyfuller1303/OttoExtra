package de.ottoextra.mixin.compat.bettertab;

import de.ottoextra.playerlist.PlayerListSortingModule;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Comparator;

@Pseudo
@Mixin(targets = "tab.bettertab.Tools", remap = false)
public abstract class BetterTabToolsMixin {
    @ModifyArg(
            method = "getPlayerEntries",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;",
                    remap = false
            ),
            index = 0,
            require = 2,
            remap = false
    )
    private static Comparator<PlayerListEntry> ottoextra$sortBetterTabByLehen(
            Comparator<PlayerListEntry> original) {
        return PlayerListSortingModule.wrapComparator(original);
    }
}
