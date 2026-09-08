package de.ottoextra.addon;

import net.minecraft.text.Text;

/**
 * The current nametag result passed through the addon chain. Addons should
 * return a copied record with the fields they want to change.
 */
public record NametagPresentation(
        boolean visible,
        boolean forceVisible,
        Text title,
        Text name,
        Text account,
        boolean custom
) {
    public static NametagPresentation vanilla(boolean visible, Text vanillaName) {
        return new NametagPresentation(visible, false, null, vanillaName, null, false);
    }

    public NametagPresentation withVisible(boolean value, boolean force) {
        return new NametagPresentation(value, force, title, name, account, custom || force);
    }

    public NametagPresentation withLines(Text newTitle, Text newName, Text newAccount) {
        return new NametagPresentation(visible, forceVisible, newTitle, newName, newAccount, true);
    }
}
