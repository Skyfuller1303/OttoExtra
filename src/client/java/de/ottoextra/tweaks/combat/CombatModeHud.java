package de.ottoextra.tweaks.combat;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

/** Small, passive actionbar mirror next to the main-hand hotbar. */
public final class CombatModeHud {
    private static final Identifier SLOT = Identifier.ofVanilla("hud/hotbar_offhand_right");
    private static CombatModeHud active;

    private final OttoExtraContext context;
    private final CombatModeState modes = new CombatModeState();
    private final Map<CombatMode, Identifier> modeIcons = new EnumMap<>(CombatMode.class);
    private final Map<WeaponFamily, Identifier> familyIcons = new EnumMap<>(WeaponFamily.class);
    private PlayerEntity sessionPlayer;
    private ItemStack sampledStack = ItemStack.EMPTY;
    private WeaponFamily heldFamily;

    private CombatModeHud(OttoExtraContext context) {
        this.context = context;
        for (CombatMode mode : CombatMode.values()) modeIcons.put(mode, icon(mode.icon()));
        for (WeaponFamily family : WeaponFamily.values()) familyIcons.put(family, icon(family.icon()));
    }

    public static void register(OttoExtraContext context) {
        CombatModeHud feature = new CombatModeHud(context);
        active = feature;
        ClientTickEvents.END_CLIENT_TICK.register(feature::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> feature.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> feature.reset());
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                OttoExtra.id("combat_mode"), feature::render);
    }

    /** Called for both vanilla actionbar packet forms, after main-thread dispatch. */
    public static void onActionBar(Text message) {
        CombatModeHud feature = active;
        if (feature == null || message == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!feature.available(client)) return;
        feature.sampleWeapon(client);
        feature.modes.observe(feature.heldFamily, message.getString());
    }

    private boolean available(MinecraftClient client) {
        return context.config().tweaks.combatModeHud && context.isOnOttonien()
                && client.player != null && client.world != null
                && client.player.isAlive() && !client.player.isSpectator();
    }

    private void tick(MinecraftClient client) {
        if (!available(client)) {
            reset();
            return;
        }
        sampleWeapon(client);
    }

    private void sampleWeapon(MinecraftClient client) {
        if (sessionPlayer != client.player) {
            reset();
            sessionPlayer = client.player;
        }
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty()) {
            sampledStack = ItemStack.EMPTY;
            heldFamily = null;
            return;
        }
        if (!ItemStack.areItemsAndComponentsEqual(stack, sampledStack)) {
            sampledStack = stack.copy();
            heldFamily = WeaponClassifier.classify(stack);
        }
    }

    private void render(DrawContext draw, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!available(client) || client.options.hudHidden || client.currentScreen != null
                || client.getCameraEntity() != client.player) return;
        sampleWeapon(client);
        if (heldFamily == null) return;

        CombatMode mode = modes.mode(heldFamily).orElse(null);
        Identifier texture = mode == null ? familyIcons.get(heldFamily) : modeIcons.get(mode);
        boolean leftHanded = client.player.getMainArm() == Arm.LEFT;
        boolean occupied = (leftHanded && !client.player.getOffHandStack().isEmpty())
                || (!leftHanded && client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR);
        int x = iconX(draw.getScaledWindowWidth(), occupied);
        int y = draw.getScaledWindowHeight() - 19;
        draw.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT, x - 10, y - 4, 29, 24);
        draw.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y,
                0.0f, 0.0f, 16, 16, 16, 16);
        if (mode == null) {
            draw.drawTextWithShadow(client.textRenderer, "?", x + 12, y - 2, 0xFFFFD479);
        }
    }

    /** Mirrors vanilla's offhand icon, with room for an occupied right-side slot. */
    static int iconX(int width, boolean rightSideOccupied) {
        return width / 2 + 101 + (rightSideOccupied ? 29 : 0);
    }

    private static Identifier icon(String name) {
        return OttoExtra.id("textures/gui/combat/" + name + ".png");
    }

    private void reset() {
        modes.clear();
        sessionPlayer = null;
        sampledStack = ItemStack.EMPTY;
        heldFamily = null;
    }
}
