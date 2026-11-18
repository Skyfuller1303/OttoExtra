package de.ottoextra.map.xaero;

import de.ottoextra.ui.hudlayout.HudLayoutEditorScreen;
import net.minecraft.client.gui.screen.Screen;
import xaero.lib.client.gui.IScreenBase;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

/**
 * The layout editor as a Xaero screen: Xaero keeps rendering its minimap behind {@link IScreenBase}
 * screens (the same trick Xaero's own edit mode and {@code MinimapBannerEditScreen} use), so the real
 * minimap is visible and moves live while it is dragged.
 */
public final class XaeroHudLayoutEditorScreen extends HudLayoutEditorScreen implements IScreenBase {
    public XaeroHudLayoutEditorScreen(Screen parent) {
        super(parent);
    }

    @Override
    public boolean shouldSkipWorldRender() {
        return false;
    }

    @Override
    public Screen getEscape() {
        return parentScreen();
    }

    @Override
    public void onDropdownOpen(DropDownWidget widget) {
    }

    @Override
    public void onDropdownClosed(DropDownWidget widget) {
    }
}
