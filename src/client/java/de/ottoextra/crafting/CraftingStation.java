package de.ottoextra.crafting;

import de.ottoextra.OttoExtra;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A custom crafting station of the server. To add one, append an entry to {@link #ALL} and put a
 * vertical strip texture (16 px wide, frames of 16x16) under {@code textures/gui/crafting/}.
 * The strike sound plays once per animation loop when {@code hitFrame} is shown; the sound is a supplier
 * so that the registry is only touched when it is actually played.
 */
public record CraftingStation(String title, String texture, int frames, int hitFrame,
                              Supplier<SoundEvent> strikeSound, float strikeVolume,
                              float strikePitch, float strikePitchSpread) {

    public static final List<CraftingStation> ALL = List.of(
            new CraftingStation("Zeugtisch", "zeugtisch", 6, 3,
                    () -> SoundEvents.BLOCK_ANVIL_USE, 0.25f, 1.0f, 0.1f));

    /** Station for a container screen title (plain text), if it is a known one. */
    public static Optional<CraftingStation> forTitle(String title) {
        if (title == null) return Optional.empty();
        String t = title.trim();
        return ALL.stream().filter(s -> s.title.equalsIgnoreCase(t)).findFirst();
    }

    public Identifier textureId() {
        return OttoExtra.id("textures/gui/crafting/" + texture + ".png");
    }
}
