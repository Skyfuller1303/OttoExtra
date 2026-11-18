package de.ottoextra.resources.xaero;

import de.ottoextra.resources.ResourceSnapshot;
import de.ottoextra.resources.ResourceMapOverlay;
import de.ottoextra.resources.ResourceZone;
import de.ottoextra.resources.ResourcesModule;
import de.ottoextra.resources.ResourceMapStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import xaero.common.HudMod;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ResourceMinimapIcons extends MinimapElementRenderer<ResourceSnapshot, Void> {
    private static final int SIZE = 20;
    private final Function<ResourceSnapshot, Identifier> iconResolver;
    private final BooleanSupplier showStatusIndicator;

    private ResourceMinimapIcons(Supplier<List<ResourceSnapshot>> resources,
                                 Function<ResourceSnapshot, Identifier> iconResolver,
                                 BooleanSupplier showStatusIndicator) {
        super(new Reader(), new Provider(resources), null);
        this.iconResolver = iconResolver;
        this.showStatusIndicator = showStatusIndicator;
    }

    public static boolean register(Supplier<List<ResourceSnapshot>> resources,
                                   Function<ResourceSnapshot, Identifier> iconResolver,
                                   BooleanSupplier showStatusIndicator) {
        try {
            HudMod hud = HudMod.INSTANCE;
            if (hud == null || hud.getMinimap() == null) return false;
            var over = hud.getMinimap().getOverMapRendererHandler();
            if (over == null) return false;
            over.add(new ResourceMinimapIcons(resources, iconResolver, showStatusIndicator));
            return true;
        } catch (Throwable error) {
            de.ottoextra.resources.OttoExtraResources.LOGGER.warn(
                    "Xaero-Ressourcenicons konnten nicht registriert werden: {}", error.toString());
            return true;
        }
    }

    @Override
    public boolean shouldRender(MinimapElementRenderLocation location) {
        return location == MinimapElementRenderLocation.OVER_MINIMAP;
    }

    @Override
    public boolean renderElement(ResourceSnapshot resource, boolean highlighted,
                                 boolean outOfBounds, double optionalDepth, float optionalScale,
                                 double partialX, double partialY, MinimapElementRenderInfo info,
                                 MinimapElementGraphics graphics,
                                 VertexConsumerProvider.Immediate vanillaBufferSource) {
        if (outOfBounds) return false;
        Identifier icon = iconResolver.apply(resource);
        if (icon == null) return false;
        graphics.blit(icon, -SIZE / 2, -SIZE / 2, 0, 0,
                SIZE, SIZE, 16, 16, 16, RenderPipelines.GUI_TEXTURED);
        if (!resource.markerOnly() && showStatusIndicator.getAsBoolean()) {
            int x = SIZE / 2 - 5;
            int y = -SIZE / 2;
            graphics.fill(x, y, x + 5, y + 5, 0xEE080D16);
            graphics.fill(x + 1, y + 1, x + 4, y + 4,
                    ResourceMapStatus.indicatorColor(ResourceMapStatus.label(resource)));
        }
        if (!resource.markerOnly() && ResourceMapOverlay.showsMinePickaxe(resource.type())) {
            int pickaxeSize = 12;
            int pickaxePosition = SIZE / 2 - pickaxeSize;
            graphics.blit(ResourceMapOverlay.minePickaxeTexture(), pickaxePosition, pickaxePosition,
                    0, 0, pickaxeSize, pickaxeSize, 16, 16, 16,
                    RenderPipelines.GUI_TEXTURED);
        }
        return true;
    }

    @Override public void preRender(MinimapElementRenderInfo info,
            VertexConsumerProvider.Immediate vanilla,
            MultiTextureRenderTypeRendererProvider multiTexture) { }
    @Override public void postRender(MinimapElementRenderInfo info,
            VertexConsumerProvider.Immediate vanilla,
            MultiTextureRenderTypeRendererProvider multiTexture) { }
    @Override public int getOrder() { return 100; }

    private static double centerX(ResourceZone zone) {
        return zone.type() == ResourceZone.Type.POINT
                ? zone.x() : ((double)zone.minX() + zone.maxX()) / 2.0;
    }

    private static double centerY(ResourceZone zone) {
        return zone.type() == ResourceZone.Type.POINT
                ? zone.y() : ((double)zone.minY() + zone.maxY()) / 2.0;
    }

    private static double centerZ(ResourceZone zone) {
        return zone.type() == ResourceZone.Type.POINT
                ? zone.z() : ((double)zone.minZ() + zone.maxZ()) / 2.0;
    }

    private static final class Reader extends MinimapElementReader<ResourceSnapshot, Void> {
        @Override public boolean isHidden(ResourceSnapshot resource, Void context) { return false; }
        @Override public double getRenderX(ResourceSnapshot resource, Void context, float partial) {
            return centerX(resource.zone());
        }
        @Override public double getRenderY(ResourceSnapshot resource, Void context, float partial) {
            return centerY(resource.zone());
        }
        @Override public double getRenderZ(ResourceSnapshot resource, Void context, float partial) {
            return centerZ(resource.zone());
        }
        @Override public int getInteractionBoxLeft(ResourceSnapshot r, Void c, float p) { return -SIZE / 2; }
        @Override public int getInteractionBoxRight(ResourceSnapshot r, Void c, float p) { return SIZE / 2; }
        @Override public int getInteractionBoxTop(ResourceSnapshot r, Void c, float p) { return -SIZE / 2; }
        @Override public int getInteractionBoxBottom(ResourceSnapshot r, Void c, float p) { return SIZE / 2; }
        @Override public int getRenderBoxLeft(ResourceSnapshot r, Void c, float p) { return -SIZE / 2; }
        @Override public int getRenderBoxRight(ResourceSnapshot r, Void c, float p) { return SIZE / 2; }
        @Override public int getRenderBoxTop(ResourceSnapshot r, Void c, float p) { return -SIZE / 2; }
        @Override public int getRenderBoxBottom(ResourceSnapshot r, Void c, float p) { return SIZE / 2; }
        @Override public int getLeftSideLength(ResourceSnapshot r, MinecraftClient mc) { return SIZE / 2; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return false; }
        @Override public String getMenuName(ResourceSnapshot r) { return r.name(); }
        @Override public String getFilterName(ResourceSnapshot r) { return "OttoExtra Resources"; }
        @Override public int getMenuTextFillLeftPadding(ResourceSnapshot r) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(ResourceSnapshot r) { return 0xCC080D16; }
    }

    private static final class Provider extends MinimapElementRenderProvider<ResourceSnapshot, Void> {
        private final Supplier<List<ResourceSnapshot>> resources;
        private Iterator<ResourceSnapshot> iterator = List.<ResourceSnapshot>of().iterator();

        private Provider(Supplier<List<ResourceSnapshot>> resources) {
            this.resources = resources;
        }

        @Override
        public void begin(MinimapElementRenderLocation location, Void context) {
            MinecraftClient client = MinecraftClient.getInstance();
            String dimension = client.world == null ? ""
                    : client.world.getRegistryKey().getValue().toString();
            iterator = !ResourceMapOverlay.iconsVisible()
                    ? List.<ResourceSnapshot>of().iterator()
                    : resources.get().stream().filter(resource -> !"DISABLED".equals(resource.status())
                    && resource.dimension().equals(dimension)).iterator();
        }

        @Override public boolean hasNext(MinimapElementRenderLocation location, Void context) {
            return iterator.hasNext();
        }
        @Override public ResourceSnapshot getNext(MinimapElementRenderLocation location, Void context) {
            return iterator.next();
        }
        @Override public void end(MinimapElementRenderLocation location, Void context) { }
    }
}
