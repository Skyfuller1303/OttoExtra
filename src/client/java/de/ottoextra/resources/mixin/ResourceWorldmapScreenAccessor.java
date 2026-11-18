package de.ottoextra.resources.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;

/** Optional, read-only access: Xaero exposes no public test for an open context menu. */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public interface ResourceWorldmapScreenAccessor {
    @Accessor("rightClickMenu")
    GuiRightClickMenu ottoextraResources$getRightClickMenu();
}
