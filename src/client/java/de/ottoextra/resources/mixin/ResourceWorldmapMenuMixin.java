package de.ottoextra.resources.mixin;

import de.ottoextra.resources.xaero.ResourceWorldmapMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

/**
 * Adds resource entries to the world map's own context menu. Only Xaero's own method
 * {@code getRightClickOptions} is hooked; a failed injection is skipped instead of crashing.
 */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class ResourceWorldmapMenuMixin {
    @Shadow private int rightClickX;
    @Shadow private int rightClickZ;

    @Inject(method = "getRightClickOptions", at = @At("RETURN"), remap = false, require = 0)
    private void ottoextraResources$addOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        ResourceWorldmapMenu.add(this, cir.getReturnValue(), rightClickX, rightClickZ);
    }
}
