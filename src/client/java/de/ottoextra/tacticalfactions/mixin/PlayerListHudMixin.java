package de.ottoextra.tacticalfactions.mixin;

import de.ottoextra.tacticalfactions.TablistRelations;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = PlayerListHud.class, priority = 900)
public abstract class PlayerListHudMixin {
    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void tacticalFactions$groupRelations(
            CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        List<PlayerListEntry> grouped = TablistRelations.groupEntries(cir.getReturnValue());
        if (grouped != cir.getReturnValue()) cir.setReturnValue(grouped);
    }
}
