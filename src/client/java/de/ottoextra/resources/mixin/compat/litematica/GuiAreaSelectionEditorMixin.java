package de.ottoextra.resources.mixin.compat.litematica;

import de.ottoextra.resources.OttoExtraResourcesClient;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.gui.GuiAreaSelectionEditorNormal", remap = false)
public abstract class GuiAreaSelectionEditorMixin {
    @Shadow @Final protected AreaSelection selection;

    @Inject(method = "initGui", at = @At("TAIL"), remap = false)
    private void ottoExtraResources$addResourceButton(CallbackInfo ci) {
        if (selection == null || !OttoExtraResourcesClient.isFeatureAvailable()) return;

        GuiBase gui = (GuiBase) (Object) this;
        String label = Text.translatable("ottoextra.resources.litematica.button").getString();
        String hint = Text.translatable("ottoextra.resources.litematica.button.hint").getString();
        int buttonWidth = Math.max(86, gui.getStringWidth(label) + 20);
        ButtonGeneric button = new ButtonGeneric(gui.getScreenWidth() - buttonWidth - 12,
                24, buttonWidth, 20, label, hint);
        gui.addButton(button, (clicked, mouseButton) -> ottoExtraResources$openResourceScreen());
    }

    private void ottoExtraResources$openResourceScreen() {
        Box box = selection.getSelectedSubRegionBox();
        BlockPos first = box == null ? null : box.getPos1();
        BlockPos second = box == null ? null : box.getPos2();
        MinecraftClient client = MinecraftClient.getInstance();

        if (first == null || second == null) {
            if (client.player != null) {
                client.player.sendMessage(Text.translatable(
                        "ottoextra.resources.litematica.selection_missing").formatted(Formatting.RED), false);
            }
            return;
        }

        OttoExtraResourcesClient.openCreateScreenFromSelection(
                (Screen) (Object) this, first, second);
    }
}
