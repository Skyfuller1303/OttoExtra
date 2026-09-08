package de.ottoextra.mixin.compat.xaeroplus;

import de.ottoextra.map.PaintedWorldMapHook;
import de.ottoextra.map.xaero.XaeroPlusPersonalDrawings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.graphics.XaeroBufferProvider;

/** Optional XaeroPlus integration; no XaeroPlus or World Map classes on our linkage path. */
@Pseudo
@Mixin(targets = "xaeroplus.feature.render.DrawManager", remap = false)
public class XaeroPlusDrawManagerMixin {
    // Verified against XaeroPlus 2.31.5 and 2.34.5 (Minecraft 1.21.11).
    @Inject(method = "drawWorldMapFeatures", at = @At("HEAD"), require = 0)
    private void ottoextra$paintBeforeFeatures(int cameraX, int cameraZ, MatrixStack matrices,
                                              double fboScale, @Coerce Object bufferProvider,
                                              CallbackInfo ci) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null) {
            PaintedWorldMapHook.renderBeforeXaeroPlus(screen, cameraX, cameraZ, matrices);
        }
        // Keep Xaero's FairPlay state and original gate intact; only local annotations opt in.
        XaeroPlusPersonalDrawings.renderPersonalDrawings(this, cameraX, cameraZ,
                matrices, fboScale, bufferProvider, true);
    }

    // @Desc retains exact argument matching even during Mixin's permissive dev remapping.
    // String selectors can fall back to name-only matching and select the other API here.
    // Class literals live only in annotation metadata; handlers still avoid optional linkage.
    @Inject(target = @Desc(value = "drawMinimapFeatures", args = {int.class, int.class,
            int.class, int.class, int.class, int.class, double.class, MatrixStack.class,
            XaeroBufferProvider.class}, min = 0), at = @At("HEAD"), require = 0)
    private void ottoextra$drawPersonalMinimapFeatures(int chunkX, int chunkZ, int tileX, int tileZ,
                                                     int insideX, int insideZ, double zoom, MatrixStack matrices,
                                                     @Coerce Object bufferProvider, CallbackInfo ci) {
        XaeroPlusPersonalDrawings.renderPersonalDrawings(this,
                chunkX * 64 + tileX * 16 + insideX, chunkZ * 64 + tileZ * 16 + insideZ,
                matrices, zoom, bufferProvider, false);
    }

    // XaeroPlus 2.31.5 has no zoom argument and uses scale 1 for its minimap DrawContext.
    @Inject(target = @Desc(value = "drawMinimapFeatures", args = {int.class, int.class,
            int.class, int.class, int.class, int.class, MatrixStack.class,
            XaeroBufferProvider.class}, min = 0), at = @At("HEAD"), require = 0)
    private void ottoextra$drawPersonalMinimapFeaturesLegacy(int chunkX, int chunkZ, int tileX, int tileZ,
                                                           int insideX, int insideZ, MatrixStack matrices,
                                                           @Coerce Object bufferProvider, CallbackInfo ci) {
        ottoextra$drawPersonalMinimapFeatures(chunkX, chunkZ, tileX, tileZ, insideX, insideZ,
                1, matrices, bufferProvider, ci);
    }
}
