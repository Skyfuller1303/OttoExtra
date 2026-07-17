package de.ottoextra.mixin;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(net.minecraft.client.network.PlayerListEntry.class)
public interface PlayerListEntryAccessor {
    @Accessor("displayName")
    Text ottoextra$rawDisplayName();
}
