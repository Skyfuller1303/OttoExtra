package de.ottoextra.rpnames;

import com.mojang.authlib.GameProfile;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.mixin.HandledScreenAccessor;
import de.ottoextra.rpnames.ui.RpNamesPeopleBookScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The server opens its own container GUI when a player is shift + right-clicked. Its slot 10 holds
 * the player's head named "[title] [rp name]". A left click on that head opens the RP name editor
 * for the player instead of being sent to the server.
 */
final class ServerProfileHeadClick {

    private static final int HEAD_SLOT = 10;

    /** Title and RP name as shown on the server GUI's head item. */
    record HeadName(String title, String rpName) {
    }

    private ServerProfileHeadClick() {
    }

    /** Data parsed from the head's lore lines. Missing entries are empty strings. */
    record HeadLore(String state, String gender, String age, String description) {
    }

    /** Identity and head data of a recognised server player GUI. */
    private record Target(String account, String uuid, HeadName head, HeadLore lore) {
    }

    /** Stop polling for the head after this many ticks (the GUI content arrives right after opening). */
    private static final int MAX_IMPORT_TICKS = 200;

    private static final class ImportState {
        boolean done;
        int ticks;
    }

    private static final java.util.Map<Object, ImportState> IMPORT_STATES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof GenericContainerScreen container)) {
                return;
            }
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    return true;
                }
                return !handleClick(MinecraftClient.getInstance(), container);
            });
            // AFTER_INIT fires again whenever this screen instance is re-initialised, for example
            // when the people book closes and returns to it. The import state therefore lives
            // outside the listener, so a forgotten or edited profile is not overwritten again.
            ImportState state = IMPORT_STATES.computeIfAbsent(screen, k -> new ImportState());
            ScreenEvents.afterTick(screen).register(s -> {
                if (state.done || ++state.ticks > MAX_IMPORT_TICKS) {
                    return;
                }
                state.done = importFromGui(MinecraftClient.getInstance(), container);
            });
        });
    }

    /** Looks for the player head in the container part; null when this is not the player GUI. */
    private static Target resolveTarget(MinecraftClient client, GenericContainerScreen screen, Slot slot) {
        if (client.player == null || slot == null || slot.inventory == client.player.getInventory()
                || slot.id != HEAD_SLOT) {
            return null;
        }
        ItemStack stack = slot.getStack();
        if (!stack.isOf(Items.PLAYER_HEAD)) {
            return null;
        }
        HeadName head = parseHeadName(stack.getName());
        if (head == null || !matchesScreenTitle(head, screen.getTitle())) {
            return null;
        }
        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        GameProfile game = profile == null ? null : safeProfile(profile);
        if (game == null) {
            return null;
        }
        UUID uuid = game.id();
        String account = game.name();
        if ((account == null || account.isBlank()) && uuid != null
                && client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(uuid);
            account = entry == null ? null : entry.getProfile().name();
        }
        if (account == null || account.isBlank()) {
            return null;
        }
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        return new Target(account, uuid == null ? null : uuid.toString(), head,
                parseLore(lore == null ? List.of() : lore.lines()));
    }

    private static Slot headSlot(GenericContainerScreen screen) {
        var slots = screen.getScreenHandler().slots;
        return slots.size() > HEAD_SLOT ? slots.get(HEAD_SLOT) : null;
    }

    /** Imports the head data once its slot is filled. Returns true when the GUI was handled. */
    private static boolean importFromGui(MinecraftClient client, GenericContainerScreen screen) {
        try {
            if (!RpNamesServices.isActive() || RpNamesServices.store() == null) {
                return false;
            }
            Target target = resolveTarget(client, screen, headSlot(screen));
            if (target == null) {
                return false;
            }
            RpNamesServices.store().importServerGui(target.account(), target.uuid(),
                    target.head().rpName(), target.head().title(), target.lore().state());
            return true;
        } catch (Throwable t) {
            DebugLog.debug("[rpnames] Server-GUI-Import fehlgeschlagen: {}", t.toString());
            return true;
        }
    }

    /** Returns true when the click was consumed and the editor opened. */
    private static boolean handleClick(MinecraftClient client, GenericContainerScreen screen) {
        try {
            if (client.player == null || !RpNamesServices.isActive()
                    || RpNamesServices.store() == null || RpNamesModule.isCombatBossBarActive(client)) {
                return false;
            }
            Slot slot = ((HandledScreenAccessor) (Object) screen).ottoextra$focusedSlot();
            Target target = resolveTarget(client, screen, slot);
            if (target == null) {
                return false;
            }
            RpNamesServices.store().importServerGui(target.account(), target.uuid(),
                    target.head().rpName(), target.head().title(), target.lore().state());
            RpNamesPeopleBookScreen.openFor(screen, target.account(), target.uuid());
            return client.currentScreen != screen;
        } catch (Throwable t) {
            DebugLog.debug("[rpnames] Server-GUI-Kopfklick fehlgeschlagen: {}", t.toString());
            return false;
        }
    }

    private static GameProfile safeProfile(ProfileComponent profile) {
        try {
            return profile.getGameProfile();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Parses the lore lines "Stand: X", "Geschlecht: X", "Alter: X" and the description, which
     * starts after a "Beschreibung:" line and runs until the next blank line.
     */
    static HeadLore parseLore(List<Text> lines) {
        String state = "";
        String gender = "";
        String age = "";
        StringBuilder description = new StringBuilder();
        boolean inDescription = false;
        for (Text line : lines) {
            String text = line == null ? "" : line.getString().trim();
            if (inDescription) {
                if (text.isEmpty()) {
                    inDescription = false;
                } else {
                    if (description.length() > 0) {
                        description.append(' ');
                    }
                    description.append(text);
                }
                continue;
            }
            if (text.equalsIgnoreCase("Beschreibung:")) {
                inDescription = true;
                continue;
            }
            String value;
            if ((value = valueAfter(text, "Stand:")) != null) {
                state = value;
            } else if ((value = valueAfter(text, "Geschlecht:")) != null) {
                gender = value;
            } else if ((value = valueAfter(text, "Alter:")) != null) {
                age = value;
            }
        }
        return new HeadLore(state, gender, age, description.toString());
    }

    private static String valueAfter(String line, String prefix) {
        return line.regionMatches(true, 0, prefix, 0, prefix.length())
                ? line.substring(prefix.length()).trim() : null;
    }

    /**
     * Splits the head's custom name into title (first part) and RP name (last part). A single part
     * is the RP name without a title. Returns null when no name text is present.
     */
    static HeadName parseHeadName(Text name) {
        if (name == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        String root = name.copyContentOnly().getString().trim();
        if (!root.isEmpty()) {
            parts.add(root);
        }
        for (Text sibling : name.getSiblings()) {
            String s = sibling.getString().trim();
            if (!s.isEmpty()) {
                parts.add(s);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        String rpName = parts.get(parts.size() - 1);
        String title = parts.size() > 1 ? parts.get(0) : "";
        return new HeadName(title, rpName);
    }

    static boolean matchesScreenTitle(HeadName head, Text screenTitle) {
        if (head == null || screenTitle == null) {
            return false;
        }
        String title = screenTitle.getString().trim();
        return !title.isEmpty() && title.toLowerCase(Locale.ROOT)
                .equals(head.rpName().toLowerCase(Locale.ROOT));
    }
}
