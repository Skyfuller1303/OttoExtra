package de.ottoextra.mixin;

import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Zugriff auf die Skin-Customization-TrackedData (2nd-Layer-Sichtbarkeit), um
 * für die 3D-Vorschau im Personenbuch alle Modellteile (Hut/Jacke/Ärmel/Hose)
 * einzuschalten.
 */
@Mixin(PlayerLikeEntity.class)
public interface PlayerCustomizationAccessor {

    @Accessor("PLAYER_MODE_CUSTOMIZATION_ID")
    static TrackedData<Byte> ottoextra$customization() {
        throw new AssertionError();
    }
}
