package de.ottoextra.mixin;

import com.mojang.authlib.GameProfile;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.tablist.TablistNameFormatter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tabliste: lokale RP-Namen/Farben statt Accountname (Muster aus
 * OttoPlus PlayerListEntryMixin). RETURN-Inject, null-sicher; ohne lokales
 * Profil oder bei deaktivierter Option bleibt der Vanilla-Name.
 */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void ottoextra$rpDisplayName(CallbackInfoReturnable<Text> cir) {
        try {
            GameProfile profile = ((PlayerListEntry) (Object) this).getProfile();
            Text replaced = TablistNameFormatter.format(profile, cir.getReturnValue());
            if (replaced != null) {
                cir.setReturnValue(replaced);
            }
        } catch (Throwable ignored) {
            // Tabliste darf nie brechen
        }
    }
}
