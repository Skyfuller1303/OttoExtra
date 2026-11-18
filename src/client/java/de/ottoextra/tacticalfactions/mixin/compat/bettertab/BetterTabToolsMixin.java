package de.ottoextra.tacticalfactions.mixin.compat.bettertab;

import de.ottoextra.tacticalfactions.TablistRelations;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "tab.bettertab.Tools", priority = 900, remap = false)
public abstract class BetterTabToolsMixin {
    @Inject(
            method = "getPlayerEntries",
            at = @At(value = "RETURN", remap = false),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void tacticalFactions$groupBetterTabRelations(
            CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        List<PlayerListEntry> grouped = TablistRelations.groupEntries(cir.getReturnValue());
        if (grouped != cir.getReturnValue()) cir.setReturnValue(grouped);
    }
}
