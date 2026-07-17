package de.ottoextra.mixin;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PlayerLikeEntity.class)
public interface PlayerCustomizationAccessor {
    @Accessor("PLAYER_MODE_CUSTOMIZATION_ID")
    static TrackedData<Byte> ottoextra$customization() {
        throw new AssertionError();
    }
}
