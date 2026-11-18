package de.ottoextra.regions.zonebanner;

import net.minecraft.util.Identifier;

/**
 * Everything a zone banner style needs for one frame.
 *
 * @param name       display name of the entered fief (faction name, else region name); never null
 * @param subtitle   small second line, e.g. "Dorf · Niederpölt"; may be empty
 * @param banner     coat of arms texture (16x16 source, any square size), or null if not loaded yet
 * @param screenW    scaled GUI width
 * @param screenH    scaled GUI height
 * @param topMargin  configured distance from the top edge in GUI pixels
 * @param textScale  user text scale multiplier (1.0 = default)
 * @param sticky     true while previewing: hold the fully shown state, no fade-out
 */
public record ZoneBannerFrame(String name, String subtitle, Identifier banner,
                              int screenW, int screenH, int topMargin,
                              float textScale, boolean sticky) {
}
