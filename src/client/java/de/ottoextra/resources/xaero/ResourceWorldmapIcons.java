package de.ottoextra.resources.xaero;

import de.ottoextra.resources.OttoExtraResources;
import de.ottoextra.resources.ResourceMapOverlay;
import de.ottoextra.resources.ResourceMapStatus;
import de.ottoextra.resources.ResourceSnapshot;
import de.ottoextra.resources.mixin.ResourceWorldmapScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.WorldMap;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Optional World Map integration, loaded only when xaeroworldmap is present. */
public final class ResourceWorldmapIcons extends ElementRenderer<ResourceSnapshot,
        ResourceWorldmapIcons.Context, ResourceWorldmapIcons> {
    private static final int SIZE = 16;
    private final Function<ResourceSnapshot, Identifier> icons;

    private ResourceWorldmapIcons(Supplier<List<ResourceSnapshot>> resources,
                                 Function<ResourceSnapshot, Identifier> icons,
                                 Consumer<ResourceSnapshot> navigate) {
        super(new Context(), new Provider(resources), new Reader(navigate));
        this.icons = icons;
    }

    public static boolean register(Supplier<List<ResourceSnapshot>> resources,
                                   Function<ResourceSnapshot, Identifier> icons,
                                   Consumer<ResourceSnapshot> navigate) {
        try {
            if (WorldMap.mapElementRenderHandler == null) return false;
            WorldMap.mapElementRenderHandler.add(new ResourceWorldmapIcons(resources, icons, navigate));
            ResourceMapOverlay.useNativeWorldmap();
        } catch (LinkageError | RuntimeException error) {
            OttoExtraResources.LOGGER.warn("Xaero-Worldmap-Ressourcenmenü nicht verfügbar; Overlay bleibt aktiv: {}", error.toString());
        }
        return true;
    }

    @Override public int getOrder() { return 110; }
    @Override public boolean shouldBeDimScaled() { return false; }
    @Override public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
        return !shadow && ResourceMapOverlay.iconsVisible() && location == ElementRenderLocation.WORLD_MAP;
    }
    @Override public void preRender(ElementRenderInfo info, VertexConsumerProvider.Immediate vanilla,
                                    MultiTextureRenderTypeRendererProvider multiTexture, boolean hovered) {
        context.dimension = info.mapDimension == null ? "" : info.mapDimension.getValue().toString();
        // Xaero's element coordinates are framebuffer pixels, unlike Screen overlays.
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        context.iconScale = (float) (guiScale * ResourceMapOverlay.iconSize(info.scale / guiScale) / SIZE);
    }
    @Override public void postRender(ElementRenderInfo info, VertexConsumerProvider.Immediate vanilla,
                                     MultiTextureRenderTypeRendererProvider multiTexture, boolean hovered) { }
    @Override public void renderElementShadow(ResourceSnapshot resource, boolean hovered, float scale,
            double partialX, double partialZ, ElementRenderInfo info, MapElementGraphics graphics,
            VertexConsumerProvider.Immediate vanilla, MultiTextureRenderTypeRendererProvider multiTexture) { }

    @Override public boolean renderElement(ResourceSnapshot resource, boolean hovered, double depth,
            float scale, double partialX, double partialZ, ElementRenderInfo info, MapElementGraphics graphics,
            VertexConsumerProvider.Immediate vanilla, MultiTextureRenderTypeRendererProvider multiTexture) {
        Identifier icon = icons.apply(resource);
        if (icon == null) return false;
        graphics.pose().push();
        try {
            graphics.pose().translate(partialX, partialZ, depth);
            graphics.pose().scale(context.iconScale, context.iconScale, 1);
            if (hovered) graphics.fill(-10, -10, 10, 10, 0x997B6733);
            // World Map blit uses bottom-to-top V coordinates (unlike Minimap blit).
            graphics.blit(icon, -SIZE / 2, -SIZE / 2, 0, 16, SIZE, SIZE, 16, 0, 16, RenderPipelines.GUI_TEXTURED);
            if (!resource.markerOnly()) {
                String status = context.status(resource);
                int color = ResourceMapStatus.indicatorColor(status);
                graphics.fill(3, -8, 8, -3, 0xEE080D16);
                graphics.fill(4, -7, 7, -4, color);
                if (ResourceMapOverlay.showsMinePickaxe(resource.type())) {
                    graphics.blit(ResourceMapOverlay.minePickaxeTexture(), -2, -2, 0, 16,
                            10, 10, 16, 0, 16, RenderPipelines.GUI_TEXTURED);
                }
            }
            return true;
        } finally {
            graphics.pose().pop();
        }
    }

    static final class Context {
        String dimension = "";
        float iconScale = 1;
        private long statusSecond = -1;
        private final Map<ResourceSnapshot, String> statuses = new HashMap<>();

        String status(ResourceSnapshot resource) {
            long second = System.currentTimeMillis() / 1_000;
            if (statusSecond != second) {
                statusSecond = second;
                statuses.clear();
            }
            return statuses.computeIfAbsent(resource, ResourceMapStatus::label);
        }
    }

    private static final class Provider extends ElementRenderProvider<ResourceSnapshot, Context> {
        private final Supplier<List<ResourceSnapshot>> resources;
        private Iterator<ResourceSnapshot> iterator = List.<ResourceSnapshot>of().iterator();
        Provider(Supplier<List<ResourceSnapshot>> resources) { this.resources = resources; }
        @Override public void begin(ElementRenderLocation location, Context context) {
            iterator = resources.get().stream().filter(resource -> !"DISABLED".equals(resource.status()) && resource.dimension().equals(context.dimension)).iterator();
        }
        @Override public boolean hasNext(ElementRenderLocation location, Context context) { return iterator.hasNext(); }
        @Override public ResourceSnapshot getNext(ElementRenderLocation location, Context context) { return iterator.next(); }
        @Override public void end(ElementRenderLocation location, Context context) { }
    }

    private static final class Reader extends ElementReader<ResourceSnapshot, Context, ResourceWorldmapIcons> {
        private final Consumer<ResourceSnapshot> navigate;
        Reader(Consumer<ResourceSnapshot> navigate) { this.navigate = navigate; }
        @Override public boolean isHidden(ResourceSnapshot resource, Context context) {
            return !ResourceMapOverlay.iconsVisible() || "DISABLED".equals(resource.status())
                    || !resource.dimension().equals(context.dimension);
        }
        @Override public double getRenderX(ResourceSnapshot resource, Context context, float partial) {
            return ResourceMapOverlay.centerX(resource.zone());
        }
        @Override public double getRenderZ(ResourceSnapshot resource, Context context, float partial) {
            return ResourceMapOverlay.centerZ(resource.zone());
        }
        @Override public int getInteractionBoxLeft(ResourceSnapshot r, Context c, float p) { return -SIZE / 2; }
        @Override public int getInteractionBoxRight(ResourceSnapshot r, Context c, float p) { return SIZE / 2; }
        @Override public int getInteractionBoxTop(ResourceSnapshot r, Context c, float p) { return -SIZE / 2; }
        @Override public int getInteractionBoxBottom(ResourceSnapshot r, Context c, float p) { return SIZE / 2; }
        @Override public int getRenderBoxLeft(ResourceSnapshot r, Context c, float p) { return -SIZE / 2; }
        @Override public int getRenderBoxRight(ResourceSnapshot r, Context c, float p) { return SIZE / 2; }
        @Override public int getRenderBoxTop(ResourceSnapshot r, Context c, float p) { return -SIZE / 2; }
        @Override public int getRenderBoxBottom(ResourceSnapshot r, Context c, float p) { return SIZE / 2; }
        @Override public int getLeftSideLength(ResourceSnapshot r, MinecraftClient client) { return SIZE / 2; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return false; }
        @Override public float getBoxScale(ElementRenderLocation location, ResourceSnapshot resource, Context context) {
            return context.iconScale;
        }
        @Override public boolean isInteractable(ElementRenderLocation location, ResourceSnapshot resource) { return true; }
        @Override public boolean isRightClickValid(ResourceSnapshot resource) {
            return ResourceMapOverlay.iconsVisible() && !"DISABLED".equals(resource.status());
        }
        @Override public String getMenuName(ResourceSnapshot resource) {
            return resource.name() + " · " + ResourceMapStatus.label(resource);
        }
        @Override public String getFilterName(ResourceSnapshot resource) { return resource.name(); }
        @Override public int getMenuTextFillLeftPadding(ResourceSnapshot resource) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(ResourceSnapshot resource) { return 0xCC080D16; }
        @Override public Tooltip getTooltip(ResourceSnapshot resource, Context context, boolean menu) {
            if (MinecraftClient.getInstance().currentScreen instanceof ResourceWorldmapScreenAccessor screen
                    && screen.ottoextraResources$getRightClickMenu() != null
                    && !screen.ottoextraResources$getRightClickMenu().isClosed()) return null;
            return new Tooltip(Text.literal(resource.name()).formatted(Formatting.GOLD)
                    // Xaero's TextSplitter recognizes a newline only as a separate space-delimited token.
                    .append(Text.literal(" \n " + context.status(resource)).formatted(Formatting.WHITE))
                    .append(Text.literal(" \n ").append(Text.translatable("ottoextra.resources.map.menu_hint")).formatted(Formatting.GRAY)));
        }
        @Override public ArrayList<RightClickOption> getRightClickOptions(ResourceSnapshot resource, IRightClickableElement target) {
            var options = new ArrayList<RightClickOption>();
            options.add(new RightClickOption(resource.name(), 0, target) {
                @Override public void onAction(Screen screen) { }
            }.setActive(false));
            options.add(new RightClickOption(ResourceMapStatus.label(resource), 1, target) {
                @Override public void onAction(Screen screen) { }
            }.setActive(false));
            options.add(new RightClickOption("ottoextra.resources.map.navigate", 2, target) {
                @Override public void onAction(Screen screen) { navigate.accept(resource); }
            });
            return options;
        }
    }
}
