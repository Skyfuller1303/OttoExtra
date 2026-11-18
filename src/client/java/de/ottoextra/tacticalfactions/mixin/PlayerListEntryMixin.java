package de.ottoextra.tacticalfactions.mixin;

import com.mojang.authlib.GameProfile;
import de.ottoextra.tacticalfactions.TablistRelations;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerListEntry.class, priority = 900)
public abstract class PlayerListEntryMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void tacticalFactions$colorRelationName(CallbackInfoReturnable<Text> cir) {
        GameProfile profile = ((PlayerListEntry) (Object) this).getProfile();
        Text colored = TablistRelations.colorName(profile, cir.getReturnValue());
        if (colored != cir.getReturnValue()) cir.setReturnValue(colored);
    }
}
