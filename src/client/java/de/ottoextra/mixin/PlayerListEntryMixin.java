package de.ottoextra.mixin;

import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.tablist.TablistNameFormatter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void ottoextra$rpDisplayName(CallbackInfoReturnable<Text> cir) {
        try {
            PlayerListEntry entry = (PlayerListEntry) (Object) this;
            Text original = cir.getReturnValue();
            Team team = original == null && RpNamesServices.isActive()
                    ? entry.getScoreboardTeam() : null;
            Text replaced = TablistNameFormatter.format(entry.getProfile(), original, team);
            if (replaced != null) {
                cir.setReturnValue(replaced);
            }
        } catch (Throwable ignored) {

        }
    }
}
