package de.ottoextra.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Actual position and size of a handled screen's background, for the Werkstatt tab and panel. */
@Mixin(HandledScreen.class)
public interface WerkstattHandledScreenAccessor {
    @Accessor("x")
    int ottoextra$werkstattX();

    @Accessor("y")
    int ottoextra$werkstattY();

    @Accessor("backgroundWidth")
    int ottoextra$werkstattWidth();

    @Accessor("backgroundHeight")
    int ottoextra$werkstattHeight();
}
