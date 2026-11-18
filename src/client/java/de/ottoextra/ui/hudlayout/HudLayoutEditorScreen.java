package de.ottoextra.ui.hudlayout;

import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.ui.hudlayout.HudSnap.Rect;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared layout editor for the HUD elements of OttoExtra, the Xaero minimap and addons. Drag to
 * move, mouse wheel or the corner handle to scale (where supported), arrow keys to nudge. Boxes
 * snap to screen edges, centre lines and each other; Alt disables snapping.
 */
public class HudLayoutEditorScreen extends Screen {
    private static final int SNAP = 4;
    private static final int HANDLE = 6;
    private static final int MARGIN = 4;

    private final Screen parent;
    private final OttoExtraConfig config;
    private final List<HudLayoutElement> elements;
    private HudLayoutElement selected;
    private HudLayoutElement active;
    private boolean scaling;
    private double grabX;
    private double grabY;
    private int dragX;
    private int dragY;
    private int scaleStartW;
    private float scaleStart;
    private List<Integer> guideX = List.of();
    private List<Integer> guideY = List.of();

    /** Opens the right variant: a Xaero screen (minimap stays visible and live) when Xaero is loaded. */
    public static Screen create(Screen parent) {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            try {
                return XaeroVariant.create(parent);
            } catch (Throwable ignored) {
                // Xaero classes unavailable: plain editor.
            }
        }
        return new HudLayoutEditorScreen(parent);
    }

    private static final class XaeroVariant {
        static Screen create(Screen parent) {
            return new de.ottoextra.map.xaero.XaeroHudLayoutEditorScreen(parent);
        }
    }

    protected Screen parentScreen() {
        return parent;
    }

    public HudLayoutEditorScreen(Screen parent) {
        super(Text.translatable("ottoextra.hudlayout.title"));
        this.parent = parent;
        this.config = OttoExtraConfig.active();
        this.elements = HudLayoutRegistry.elements(config);
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.hudlayout.reset"), b -> reset())
                .dimensions(width / 2 - 80, height - 26, 76, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 + 4, height - 26, 76, 18).build());
    }

    // ---- geometry ----

    private HudLayoutElement.Bounds rawBounds(HudLayoutElement e) {
        try {
            return e.bounds(width, height);
        } catch (Throwable t) {
            return null;
        }
    }

    private Rect rect(HudLayoutElement e) {
        HudLayoutElement.Bounds b = rawBounds(e);
        if (b == null) {
            return null;
        }
        if (e == active) {
            return new Rect(dragX, dragY, b.w(), b.h());
        }
        // Where the element is really drawn: HudFlow may have moved it away from the wanted spot.
        int[] res = e.flowId() == null ? null : HudFlow.resolved(e.flowId());
        if (res != null) {
            return new Rect(res[0], res[1], b.w(), b.h());
        }
        return new Rect(b.x(), b.y(), b.w(), b.h());
    }

    /** The wanted (stored) rectangle, which differs from {@link #rect} only if HudFlow moved it. */
    private Rect wantedRect(HudLayoutElement e) {
        HudLayoutElement.Bounds b = rawBounds(e);
        return b == null ? null : new Rect(b.x(), b.y(), b.w(), b.h());
    }

    /** True if the game HUD itself draws this element behind the editor, so no copy is drawn. */
    private boolean isLive(HudLayoutElement e) {
        return e.rendersLive() || e.flowId() != null && HudFlow.resolved(e.flowId()) != null;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // No blur or dimming: the in-game HUD (including the Xaero minimap) stays visible.
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private List<Rect> othersOf(HudLayoutElement moving) {
        List<Rect> result = new ArrayList<>();
        for (HudLayoutElement e : elements) {
            Rect r = e == moving ? null : rect(e);
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }

    private HudLayoutElement hit(double mx, double my) {
        List<HudLayoutElement> order = new ArrayList<>(elements);
        java.util.Collections.reverse(order);
        if (selected != null && order.remove(selected)) {
            order.add(0, selected);
        }
        for (HudLayoutElement e : order) {
            Rect r = rect(e);
            if (r != null && e.movable() && mx >= r.x() && mx < r.right() && my >= r.y() && my < r.bottom()) {
                return e;
            }
        }
        return null;
    }

    private HudLayoutElement handleHit(double mx, double my) {
        for (HudLayoutElement e : elements) {
            Rect r = rect(e);
            if (r != null && e.scalable() && mx >= r.right() - 1 && mx < r.right() + HANDLE - 1
                    && my >= r.bottom() - 1 && my < r.bottom() + HANDLE - 1) {
                return e;
            }
        }
        return null;
    }

    private void safe(Runnable action) {
        try {
            action.run();
        } catch (Throwable ignored) {
            // A misbehaving element must never break the editor.
        }
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && !super.mouseClicked(click, doubled)) {
            HudLayoutElement handle = handleHit(click.x(), click.y());
            if (handle != null) {
                Rect r = rect(handle);
                selected = handle;
                active = handle;
                scaling = true;
                dragX = r.x();
                dragY = r.y();
                scaleStart = handle.scale();
                scaleStartW = Math.max(1, r.w());
                HudFlow.pin(handle.flowId());
                return true;
            }
            HudLayoutElement e = hit(click.x(), click.y());
            if (e != null) {
                Rect r = rect(e);
                selected = e;
                active = e;
                scaling = false;
                grabX = click.x() - r.x();
                grabY = click.y() - r.y();
                dragX = r.x();
                dragY = r.y();
                HudFlow.pin(e.flowId());
                return true;
            }
            return false;
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean altHeld() {
        if (client == null) {
            return false;
        }
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (active == null) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        if (scaling) {
            HudLayoutElement e = active;
            safe(() -> e.setScale(HudSnap.stepScale(scaleStart * (float) (click.x() - dragX) / scaleStartW)));
            guideX = List.of();
            guideY = List.of();
            return true;
        }
        Rect r = rect(active);
        if (r == null) {
            return true;
        }
        HudSnap.Result snap = HudSnap.snap(new Rect((int) Math.round(click.x() - grabX),
                        (int) Math.round(click.y() - grabY), r.w(), r.h()), othersOf(active), width, height,
                MARGIN, SNAP, altHeld());
        dragX = Math.max(0, Math.min(Math.max(0, width - r.w()), snap.x()));
        dragY = Math.max(0, Math.min(Math.max(0, height - r.h()), snap.y()));
        guideX = snap.guideX();
        guideY = snap.guideY();
        // Apply live (without saving) so the real HUD, including the Xaero minimap, follows the mouse.
        HudLayoutElement moving = active;
        safe(() -> moving.place(dragX, dragY, r.w(), r.h(), width, height));
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (active != null) {
            HudLayoutElement e = active;
            HudLayoutElement.Bounds b = rawBounds(e);
            if (b != null) {
                safe(() -> e.place(dragX, dragY, b.w(), b.h(), width, height));
            }
            safe(e::save);
            active = null;
            scaling = false;
            HudFlow.pin(null);
            guideX = List.of();
            guideY = List.of();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        HudLayoutElement e = hit(mouseX, mouseY);
        if (e == null || !e.scalable()) {
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        selected = e;
        Rect before = wantedRect(e);
        safe(() -> e.setScale(HudSnap.stepScale(e.scale() + (vertical > 0 ? 0.1f : -0.1f))));
        HudLayoutElement.Bounds after = rawBounds(e);
        if (before != null && after != null) {
            safe(() -> e.place(before.x(), before.y(), after.w(), after.h(), width, height));
        }
        safe(e::save);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int step = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
        int dx = 0;
        int dy = 0;
        switch (input.key()) {
            case GLFW.GLFW_KEY_LEFT -> dx = -step;
            case GLFW.GLFW_KEY_RIGHT -> dx = step;
            case GLFW.GLFW_KEY_UP -> dy = -step;
            case GLFW.GLFW_KEY_DOWN -> dy = step;
            default -> {
                return super.keyPressed(input);
            }
        }
        if (selected != null && selected.movable()) {
            Rect r = wantedRect(selected);
            if (r != null) {
                HudLayoutElement e = selected;
                int nx = Math.max(0, Math.min(Math.max(0, width - r.w()), r.x() + dx));
                int ny = Math.max(0, Math.min(Math.max(0, height - r.h()), r.y() + dy));
                safe(() -> e.place(nx, ny, r.w(), r.h(), width, height));
                safe(e::save);
            }
        }
        return true;
    }

    @Override
    public void removed() {
        HudFlow.pin(null);
        super.removed();
    }

    private void reset() {
        if (selected != null) {
            HudLayoutElement e = selected;
            safe(e::reset);
            safe(e::save);
        }
        active = null;
        scaling = false;
    }

    // ---- rendering ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.fill(width / 2, 0, width / 2 + 1, height, 0x30FFFFFF);
        ctx.fill(0, height / 2, width, height / 2 + 1, 0x30FFFFFF);
        TextRenderer font = textRenderer;
        ctx.drawCenteredTextWithShadow(font, title, width / 2, 7, 0xFFF4F7FA);
        ctx.drawCenteredTextWithShadow(font, Text.translatable("ottoextra.hudlayout.hint"), width / 2, 20,
                0xFFB8C0C8);
        if (selected == null) {
            for (HudLayoutElement e : elements) {
                if (e.movable() && rawBounds(e) != null) {
                    selected = e;
                    break;
                }
            }
        }
        for (HudLayoutElement e : elements) {
            Rect r = rect(e);
            if (r == null) {
                continue;
            }
            if (!isLive(e)) {
                boolean drew = false;
                try {
                    drew = e.drawPreview(ctx, font, r.x(), r.y());
                } catch (Throwable ignored) {
                    // fall back to the plain box
                }
                if (!drew) {
                    ctx.fill(r.x(), r.y(), r.right(), r.bottom(), e.movable() ? 0x40808080 : 0x205A6A80);
                }
            }
            drawFlowGhost(ctx, e, r);
            drawFrame(ctx, font, e, r);
        }
        for (int gx : guideX) {
            ctx.fill(gx, 0, gx + 1, height, 0xAAFFD479);
        }
        for (int gy : guideY) {
            ctx.fill(0, gy, width, gy + 1, 0xAAFFD479);
        }
    }

    /** Faint dashed outline of the wanted spot if HudFlow moved the element somewhere else. */
    private void drawFlowGhost(DrawContext ctx, HudLayoutElement e, Rect drawn) {
        Rect wanted = e.flowId() == null || active == e ? null : wantedRect(e);
        if (wanted == null || wanted.x() == drawn.x() && wanted.y() == drawn.y()) {
            return;
        }
        int color = 0x70FFD479;
        for (int x = wanted.x(); x < wanted.right(); x += 6) {
            int end = Math.min(x + 3, wanted.right());
            ctx.fill(x, wanted.y(), end, wanted.y() + 1, color);
            ctx.fill(x, wanted.bottom() - 1, end, wanted.bottom(), color);
        }
        for (int y = wanted.y(); y < wanted.bottom(); y += 6) {
            int end = Math.min(y + 3, wanted.bottom());
            ctx.fill(wanted.x(), y, wanted.x() + 1, end, color);
            ctx.fill(wanted.right() - 1, y, wanted.right(), end, color);
        }
    }

    private void drawFrame(DrawContext ctx, TextRenderer font, HudLayoutElement e, Rect r) {
        boolean sel = e == selected;
        int color = !e.movable() ? 0xFF5A6A80 : active == e ? 0xFFFFD479 : sel ? 0xFFE6C8A9 : 0xFF796A55;
        ctx.fill(r.x() - 1, r.y() - 1, r.right() + 1, r.y(), color);
        ctx.fill(r.x() - 1, r.bottom(), r.right() + 1, r.bottom() + 1, color);
        ctx.fill(r.x() - 1, r.y(), r.x(), r.bottom(), color);
        ctx.fill(r.right(), r.y(), r.right() + 1, r.bottom(), color);
        // The label sits inside the top edge of its own box, never between boxes.
        String label = e.label().getString();
        if (e.scalable()) {
            label += " – " + Math.round(e.scale() * 100f) + " %";
        }
        int labelW = Math.min(r.w(), font.getWidth(label) + 6);
        ctx.fill(r.x(), r.y(), r.x() + labelW, r.y() + 10, 0xC0000000);
        ctx.drawText(font, font.trimToWidth(label, Math.max(0, labelW - 4)), r.x() + 3, r.y() + 1,
                sel ? 0xFFFFD479 : 0xFFD8DEE6, false);
        if (e.scalable()) {
            ctx.fill(r.right() - 1, r.bottom() - 1, r.right() + HANDLE - 1, r.bottom() + HANDLE - 1,
                    sel ? 0xFFFFD479 : 0xFF796A55);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
