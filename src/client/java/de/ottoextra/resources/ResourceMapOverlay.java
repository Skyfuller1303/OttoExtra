package de.ottoextra.resources;

import de.ottoextra.map.XaeroMapBridge;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ResourceMapOverlay {
    private static final int MAX_ICON_SIZE = 16;
    private static final int STATUS_GREEN = 0xFF42D66B;
    private static final int STATUS_YELLOW = 0xFFFFC928;
    private static final int STATUS_RED = 0xFFE14949;
    private static final int STATUS_BORDER = 0xEE080D16;
    private static final Identifier MINE_PICKAXE = Identifier.of(
            "minecraft", "textures/item/iron_pickaxe.png");
    private static final DateTimeFormatter FARMED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static volatile boolean iconsVisible = true;
    private static volatile boolean nativeWorldmap;

    private ResourceMapOverlay() {
    }

    static void register(Supplier<List<ResourceSnapshot>> resources,
                         Function<ResourceSnapshot, Identifier> iconResolver,
                         Consumer<ResourceSnapshot> navigationHandler,
                         Consumer<Screen> filterHandler) {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!XaeroMapBridge.isWorldmapScreen(screen)) return;
            net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(
                    new ResourceBook.Button(6, Math.max(6, screen.height - 26), 118, 20, buttonText(),
                            ResourceBook.Tone.SLATE, button -> {
                                toggle();
                                button.setMessage(buttonText());
                            }));
            net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(
                    new ResourceBook.Button(128, Math.max(6, screen.height - 26), 72, 20, Text.literal("Filter…"),
                            ResourceBook.Tone.SLATE, button -> filterHandler.accept(screen)));
            double[] press = {Double.NaN, Double.NaN};
            ScreenMouseEvents.allowMouseClick(screen).register((current, click) -> {
                if (click.button() == 0) {
                    press[0] = click.x();
                    press[1] = click.y();
                }
                return true;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((current, click) -> {
                if (click.button() != 0 || Double.isNaN(press[0])) return true;
                double dx = click.x() - press[0];
                double dy = click.y() - press[1];
                press[0] = Double.NaN;
                // Native Xaero handles selection and its context-menu actions itself.
                if (nativeWorldmap) return true;
                if (dx * dx + dy * dy > 16.0 || !iconsVisible || client.world == null) return true;
                XaeroMapBridge.View view = XaeroMapBridge.view(current);
                if (view == null) return true;
                String dimension = client.world.getRegistryKey().getValue().toString();
                ResourceSnapshot target = resourceAt(resources.get(), dimension, view,
                        click.x(), click.y());
                if (target == null || iconResolver.apply(target) == null) return true;
                navigationHandler.accept(target);
                return false;
            });
            ScreenEvents.afterRender(screen).register((current, context, mouseX, mouseY, delta) -> {
                if (nativeWorldmap) return;
                XaeroMapBridge.View view = XaeroMapBridge.view(current);
                if (!iconsVisible || view == null || client.world == null) return;
                String dimension = client.world.getRegistryKey().getValue().toString();
                for (ResourceSnapshot resource : visible(resources.get(), dimension)) {
                    int iconSize = iconSize(view.effScale());
                    float sx = view.screenX(centerX(resource.zone()));
                    float sy = view.screenY(centerZ(resource.zone()));
                    if (sx < -iconSize || sy < -iconSize
                            || sx > view.width() + iconSize || sy > view.height() + iconSize) continue;
                    int x = Math.round(sx) - iconSize / 2;
                    int y = Math.round(sy) - iconSize / 2;
                    Identifier icon = iconResolver.apply(resource);
                    if (icon == null) continue;
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y,
                            0f, 0f, iconSize, iconSize, 16, 16, 16, 16);
                    if (!resource.markerOnly()) {
                        int indicatorSize = iconSize <= 8 ? 4 : 5;
                        int indicatorX = x + iconSize - indicatorSize;
                        int indicatorY = y + iconSize - indicatorSize;
                        context.fill(indicatorX, indicatorY,
                                indicatorX + indicatorSize, indicatorY + indicatorSize, STATUS_BORDER);
                        context.fill(indicatorX + 1, indicatorY + 1,
                                indicatorX + indicatorSize - 1, indicatorY + indicatorSize - 1,
                                statusColor(resource));
                    }
                    if (!resource.markerOnly() && showsMinePickaxe(resource.type())) {
                        int pickaxeSize = minePickaxeSize(iconSize);
                        int pickaxeX = x + iconSize - pickaxeSize;
                        int pickaxeY = y + iconSize - pickaxeSize;
                        context.drawTexture(RenderPipelines.GUI_TEXTURED, MINE_PICKAXE,
                                pickaxeX, pickaxeY, 0f, 0f, pickaxeSize, pickaxeSize,
                                16, 16, 16, 16);
                    }
                }
                ResourceSnapshot hovered = resourceAt(resources.get(), dimension, view, mouseX, mouseY);
                if (hovered != null && iconResolver.apply(hovered) != null) {
                    context.drawTooltip(client.textRenderer, tooltip(hovered, client), mouseX, mouseY);
                }
            });
        });
    }

    static boolean toggle() {
        iconsVisible = !iconsVisible;
        return iconsVisible;
    }

    public static boolean iconsVisible() {
        return iconsVisible;
    }

    public static void useNativeWorldmap() {
        nativeWorldmap = true;
    }

    public static int iconSize(double effectiveScale) {
        if (effectiveScale <= 0.06) return 8;
        if (effectiveScale <= 0.18) return 12;
        return MAX_ICON_SIZE;
    }

    public static boolean showsMinePickaxe(String type) {
        return ResourcesModule.isMine(type);
    }

    public static Identifier minePickaxeTexture() {
        return MINE_PICKAXE;
    }

    static int minePickaxeSize(int iconSize) {
        if (iconSize <= 8) return 7;
        if (iconSize <= 12) return 9;
        return 11;
    }

    public static int statusColor(ResourceSnapshot resource) {
        if ("READY".equals(resource.status())) return STATUS_GREEN;
        if (!ResourcesModule.isMine(resource.type())) return STATUS_RED;
        if (resource.regenerationPercentage() >= 70) return STATUS_YELLOW;
        int secondsUntilAvailable = resource.secondsUntilNextStage();
        return secondsUntilAvailable >= 0 && secondsUntilAvailable <= 15 * 60
                ? STATUS_YELLOW : STATUS_RED;
    }

    private static Text buttonText() {
        return Text.literal("Ressourcen: "+(iconsVisible ? "An" : "Aus"));
    }

    static List<ResourceSnapshot> visible(List<ResourceSnapshot> resources, String dimension) {
        return resources.stream().filter(resource -> !"DISABLED".equals(resource.status())
                && resource.dimension().equals(dimension)).toList();
    }

    static ResourceSnapshot resourceAt(List<ResourceSnapshot> resources, String dimension,
                                       XaeroMapBridge.View view, double mouseX, double mouseY) {
        List<ResourceSnapshot> visible = visible(resources, dimension);
        int size = iconSize(view.effScale());
        for (int i = visible.size() - 1; i >= 0; i--) {
            ResourceSnapshot resource = visible.get(i);
            int x = Math.round(view.screenX(centerX(resource.zone()))) - size / 2;
            int y = Math.round(view.screenY(centerZ(resource.zone()))) - size / 2;
            if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
                return resource;
            }
        }
        return null;
    }

    static List<Text> tooltip(ResourceSnapshot resource, MinecraftClient client) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(resource.name()).formatted(Formatting.GOLD, Formatting.BOLD));
        String kind = detailLabel(resource);
        if (!kind.isBlank()) lines.add(Text.literal(kind).formatted(Formatting.GRAY));
        lines.add(Text.literal("Status: " + statusLabel(resource)).formatted(statusFormatting(resource)));
        if (!resource.markerOnly()) {
            lines.add(Text.literal("Zuletzt gefarmt: " + lastFarmedLabel(resource.lastFarmedAt()))
                    .formatted(Formatting.GRAY));
        }
        if (client.player != null) {
            double dx = client.player.getX() - centerX(resource.zone());
            double dz = client.player.getZ() - centerZ(resource.zone());
            lines.add(Text.literal("Entfernung: " + Math.round(Math.sqrt(dx * dx + dz * dz)) + " m")
                    .formatted(Formatting.GRAY));
        }
        lines.add(Text.literal("Linksklick: Navigation starten").formatted(Formatting.AQUA));
        return lines;
    }

    static String detailLabel(ResourceSnapshot resource) {
        String type = resource.typeLabel() == null ? "" : resource.typeLabel().trim();
        String material = resource.materialLabel() == null ? "" : resource.materialLabel().trim();
        if (type.isEmpty() && material.isEmpty()) return ResourceBookScreen.typeName(resource.type()).getString();
        if (type.isEmpty()) return material;
        if (material.isEmpty() || type.equalsIgnoreCase(material)) return type;
        return type + " · " + material;
    }

    static String statusLabel(ResourceSnapshot resource) {
        return ResourceMapStatus.label(resource);
    }

    static String lastFarmedLabel(String value) {
        if (value == null || value.isBlank()) return "Noch nie";
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
                    .format(FARMED_AT_FORMAT) + " Uhr";
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    private static Formatting statusFormatting(ResourceSnapshot resource) {
        if (resource.markerOnly()) return Formatting.AQUA;
        if ("READY".equals(resource.status())) return Formatting.GREEN;
        if ("REGENERATING".equals(resource.status())) return Formatting.YELLOW;
        return Formatting.RED;
    }

    public static double centerX(ResourceZone zone) {
        return zone.type() == ResourceZone.Type.POINT
                ? zone.x() : ((double)zone.minX() + zone.maxX()) / 2.0;
    }

    public static double centerZ(ResourceZone zone) {
        return zone.type() == ResourceZone.Type.POINT
                ? zone.z() : ((double)zone.minZ() + zone.maxZ()) / 2.0;
    }

    public static int centerY(ResourceZone zone) {
        return zone.type() == ResourceZone.Type.POINT
                ? zone.y() : (zone.minY() + zone.maxY()) / 2;
    }
}
