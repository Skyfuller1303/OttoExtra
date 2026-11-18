package de.ottoextra.tacticalfactions;

import com.mojang.authlib.GameProfile;
import de.ottoextra.tacticalfactions.theme.InkCheckbox;
import de.ottoextra.tacticalfactions.theme.InkTextField;
import de.ottoextra.tacticalfactions.theme.OttonienTheme;
import de.ottoextra.tacticalfactions.theme.SlateButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Trupp tab: several named combat squads. The only live state shown is whether a member is
 * present in the client's tab list; nothing else about a player is read or displayed.
 */
final class SquadScreen extends BookScreen {
    private static final int CARD_PITCH = 36;
    private static final int CARD_H = 32;
    private static final int MEMBER_ROW = 12;

    private final Set<String> onlineKeys = new HashSet<>();
    private String selectedId;
    private Squad draft;
    private boolean creating;
    private boolean dissolveArmed;
    private Text error = Text.empty();
    private int listScroll;
    private int memberScroll;

    private InkTextField nameField;
    private InkTextField accountField;
    private SlateButton dissolveButton;
    private int memberTop;
    private int memberBottom;
    private int addY;
    private int ffY = -1;
    private SlateButton cornerButton;

    SquadScreen(Screen parent) {
        super(parent, Tab.TRUPP, Text.translatable("ottoextra_tacticalfactions.book.title"));
        refreshOnline();
    }

    // ---- data ----

    private List<Squad> squads() {
        return TacticalFactions.config().squads;
    }

    private void refreshOnline() {
        onlineKeys.clear();
        if (client == null || client.getNetworkHandler() == null) return;
        // Presence only: who is listed in the tab list. No other player data is read here.
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            GameProfile profile = entry.getProfile();
            if (profile == null) continue;
            if (profile.name() != null) onlineKeys.add(profile.name().toLowerCase(Locale.ROOT));
            if (profile.id() != null) onlineKeys.add(profile.id().toString().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    void tickContent() {
        refreshOnline();
    }

    private Squad shownSquad(int index) {
        List<Squad> list = squads();
        if (index < list.size()) return list.get(index);
        return creating ? draft : null;
    }

    private int cardCount() {
        return squads().size() + (creating ? 1 : 0);
    }

    // ---- layout ----

    private int listTop() { return pageY() + 22; }
    private int listBottom() { return pageY() + pageH() - 30; }

    private int maxListScroll() {
        return Math.max(0, cardCount() * CARD_PITCH - (listBottom() - listTop()));
    }

    private boolean ffAvailable() { return BuildVariant.friendlyFireProtectionAvailable(); }

    @Override
    void initContent() {
        page(new SlateButton(pageX() + 9, pageY() + pageH() - 26, pageW() - 18, 18,
                Text.literal("+ ").append(Text.translatable("ottoextra_tacticalfactions.squad.new")),
                b -> startNew()));
        listScroll = Math.min(listScroll, maxListScroll());
        nameField = null;
        accountField = null;
        dissolveButton = null;
        ffY = -1;
        cornerButton = null;
        if (draft == null) {
            initHudSettings();
            return;
        }

        int x = lfx + 10;
        int y = lfy + 9;
        int right = lfx + lfw - 10;
        nameField = leaf(new InkTextField(textRenderer, x + 39, y - 1, right - 20 - (x + 39), 12,
                Text.translatable("ottoextra_tacticalfactions.squad.name"), false));
        nameField.setMaxLength(32);
        nameField.setText(draft.name);
        nameField.setChangedListener(value -> {
            draft.name = value;
            error = Text.empty();
        });

        int boxes = (ffAvailable() ? 3 : 2) + 2;
        int cb0 = lfy + lfh - 25 - 6 - 13 * boxes;
        addY = cb0 - 5 - 3 - 14;
        memberTop = y + 28;
        memberBottom = addY - 2;
        accountField = leaf(new InkTextField(textRenderer, x, addY, right - 30 - x, 12,
                Text.translatable("ottoextra_tacticalfactions.squad.account"), true));
        accountField.setMaxLength(16);
        TabHints.accounts(accountField);
        accountField.setSuggestion(Text.translatable("ottoextra_tacticalfactions.squad.account").getString());
        accountField.setChangedListener(value -> {
            accountField.setSuggestion(value.isEmpty()
                    ? Text.translatable("ottoextra_tacticalfactions.squad.account").getString() : "");
            error = Text.empty();
        });
        leaf(new SlateButton(right - 27, addY - 1, 17, 14, Text.literal("+"), b -> addMember()));

        int cy = cb0;
        leaf(InkCheckbox.builder(Text.translatable("ottoextra_tacticalfactions.squad.allied"), textRenderer)
                .pos(x, cy).checked(draft.alwaysAllied)
                .callback((box, checked) -> draft.alwaysAllied = checked).build());
        cy += 13;
        leaf(InkCheckbox.builder(Text.translatable("ottoextra_tacticalfactions.squad.tablist"), textRenderer)
                .pos(x, cy).checked(draft.tablistTop)
                .callback((box, checked) -> draft.tablistTop = checked).build());
        cy += 13;
        if (ffAvailable()) {
            InkCheckbox ff = leaf(InkCheckbox.builder(
                            Text.translatable("ottoextra_tacticalfactions.squad.friendly_fire"), textRenderer)
                    .pos(x, cy).checked(draft.friendlyFireProtection)
                    .callback((box, checked) -> draft.friendlyFireProtection = checked).build());
            ff.setTooltip(Tooltip.of(Text.translatable("ottoextra_tacticalfactions.option.friendlyFire.warning")));
            ffY = cy;
            cy += 13;
        }
        leaf(InkCheckbox.builder(Text.translatable("ottoextra_tacticalfactions.squad_hud.show_squad"), textRenderer)
                .pos(x, cy).checked(draft.showInHud)
                .callback((box, checked) -> draft.showInHud = checked).build());
        cy += 13;
        leaf(hudBox("ottoextra_tacticalfactions.squad_hud.enable", x, cy, TacticalFactions.config().squadHudEnabled,
                value -> TacticalFactions.config().squadHudEnabled = value));

        int bw = (lfw - 26) / 2;
        leaf(new SlateButton(lfx + 10, lfy + lfh - 25, bw, 18,
                Text.translatable("ottoextra_tacticalfactions.squad.save"), b -> save()));
        dissolveButton = leaf(new SlateButton(lfx + 16 + bw, lfy + lfh - 25, bw, 18, dissolveText(),
                b -> dissolve()).danger());
    }

    private InkCheckbox hudBox(String key, int x, int y, boolean checked,
                               java.util.function.Consumer<Boolean> setter) {
        return InkCheckbox.builder(Text.translatable(key), textRenderer).pos(x, y).checked(checked)
                .callback((box, value) -> {
                    setter.accept(value);
                    TacticalFactions.config().save();
                    SquadActivityHud.invalidate();
                }).build();
    }

    /** HUD options shown in the leaf while no squad is being edited. */
    private void initHudSettings() {
        TacticalConfig config = TacticalFactions.config();
        int x = lfx + 10;
        int y = lfy + 10 + 30;
        leaf(hudBox("ottoextra_tacticalfactions.squad_hud.enable", x, y, config.squadHudEnabled,
                value -> config.squadHudEnabled = value));
        leaf(hudBox("ottoextra_tacticalfactions.squad_hud.members", x, y + 14, config.squadHudMembers,
                value -> config.squadHudMembers = value));
        leaf(hudBox("ottoextra_tacticalfactions.squad_hud.only_online", x, y + 28, config.squadHudOnlyOnline,
                value -> config.squadHudOnlyOnline = value));
        cornerButton = leaf(new SlateButton(x, y + 46, lfw - 20, 16, cornerText(), b -> {
            config.squadHudCorner = config.squadHudCorner.next();
            config.save();
            SquadActivityHud.invalidate();
            b.setMessage(cornerText());
        }));
        leaf(new SlateButton(x, y + 64, lfw - 20, 16, maxText(), b -> {
            int[] steps = {0, 3, 5, 8, 10, 15, 20};
            int next = steps[0];
            for (int i = steps.length - 1; i >= 0; i--) {
                if (steps[i] > config.squadHudMaxMembers) next = steps[i];
            }
            config.squadHudMaxMembers = next;
            config.save();
            SquadActivityHud.invalidate();
            b.setMessage(maxText());
        }));
        leaf(new SlateButton(x, y + 82, lfw - 20, 16,
                Text.translatable("ottoextra_tacticalfactions.squad_hud.position"),
                b -> openLayoutEditor()));
    }

    /** The shared layout editor lives in OttoExtra; fail soft if the installed OttoExtra is too old. */
    private void openLayoutEditor() {
        try {
            client.setScreen(de.ottoextra.ui.hudlayout.HudLayoutEditorScreen.create(this));
        } catch (Throwable ignored) {
            // OttoExtra without the HUD layout editor.
        }
    }

    private Text maxText() {
        int max = TacticalFactions.config().squadHudMaxMembers;
        return Text.translatable("ottoextra_tacticalfactions.squad_hud.max",
                max == 0 ? Text.translatable("ottoextra_tacticalfactions.squad_hud.max.all")
                        : Text.literal(String.valueOf(max)));
    }

    private Text cornerText() {
        return Text.translatable("ottoextra_tacticalfactions.squad_hud.corner",
                Text.translatable(TacticalFactions.config().squadHudCorner.translationKey()));
    }

    private Text dissolveText() {
        return Text.translatable(dissolveArmed ? "ottoextra_tacticalfactions.squad.dissolve.confirm"
                : creating ? "ottoextra_tacticalfactions.squad.discard"
                : "ottoextra_tacticalfactions.squad.dissolve");
    }

    // ---- actions ----

    private void select(Squad squad) {
        selectedId = squad.id;
        draft = squad.copy();
        creating = false;
        dissolveArmed = false;
        error = Text.empty();
        memberScroll = 0;
        clearAndInit();
        showLeafIfNarrow();
    }

    private void startNew() {
        TacticalConfig config = TacticalFactions.config();
        String color = TacticalConfig.SQUAD_COLORS[config.squads.size() % TacticalConfig.SQUAD_COLORS.length];
        draft = Squad.create("", color);
        draft.name = Text.translatable("ottoextra_tacticalfactions.squad.default_name",
                config.squads.size() + 1).getString();
        selectedId = draft.id;
        creating = true;
        dissolveArmed = false;
        error = Text.empty();
        memberScroll = 0;
        clearAndInit();
        listScroll = maxListScroll();
        showLeafIfNarrow();
    }

    private void addMember() {
        if (draft == null || accountField == null) return;
        String raw = accountField.getText();
        if (!TacticalConfig.validAccountName(raw)) {
            error = Text.translatable("ottoextra_tacticalfactions.squad.error.account");
            return;
        }
        String account = TacticalConfig.cleanAccountName(raw);
        for (SquadMember member : draft.members) {
            if (member.account.equalsIgnoreCase(account)) {
                error = Text.translatable("ottoextra_tacticalfactions.squad.error.duplicate");
                return;
            }
        }
        draft.members.add(new SquadMember(account, knownUuid(account), draft.members.isEmpty()));
        accountField.setText("");
        error = Text.empty();
        memberScroll = Integer.MAX_VALUE;
    }

    /** UUID of a player already known from the tab list or the political snapshot, else null. */
    private String knownUuid(String account) {
        ClientPlayNetworkHandler handler = client != null ? client.getNetworkHandler() : null;
        if (handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(account);
            if (entry != null && entry.getProfile() != null && entry.getProfile().id() != null) {
                return entry.getProfile().id().toString();
            }
        }
        for (PoliticalStateService.PlayerChoice player : TacticalFactions.politics().players()) {
            if (player.account() != null && player.account().equalsIgnoreCase(account)
                    && player.uuid() != null) {
                try {
                    return java.util.UUID.fromString(player.uuid()).toString();
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private void save() {
        if (draft == null || nameField == null) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            error = Text.translatable("ottoextra_tacticalfactions.squad.error.name");
            return;
        }
        draft.name = name;
        TacticalConfig config = TacticalFactions.config();
        int index = -1;
        for (int i = 0; i < config.squads.size(); i++) {
            if (config.squads.get(i).id.equals(draft.id)) index = i;
        }
        Squad stored = draft.copy();
        if (index >= 0) config.squads.set(index, stored); else config.squads.add(stored);
        config.repair();
        config.save();
        Squad saved = config.findSquad(draft.id);
        creating = false;
        dissolveArmed = false;
        draft = saved != null ? saved.copy() : null;
        selectedId = saved != null ? saved.id : null;
        error = Text.empty();
        clearAndInit();
    }

    private void dissolve() {
        if (draft == null) return;
        if (!creating && !dissolveArmed) {
            dissolveArmed = true;
            if (dissolveButton != null) dissolveButton.setMessage(dissolveText());
            return;
        }
        TacticalConfig config = TacticalFactions.config();
        if (!creating) {
            config.squads.removeIf(squad -> squad.id.equals(draft.id));
            config.save();
        }
        draft = null;
        selectedId = null;
        creating = false;
        dissolveArmed = false;
        listScroll = Math.min(listScroll, maxListScroll());
        clearAndInit();
    }

    private void move(int index, int direction) {
        List<Squad> list = squads();
        int target = index + direction;
        if (index < 0 || index >= list.size() || target < 0 || target >= list.size()) return;
        java.util.Collections.swap(list, index, target);
        TacticalFactions.config().save();
    }

    private void cycleColor() {
        if (draft == null) return;
        String[] palette = TacticalConfig.SQUAD_COLORS;
        int next = 0;
        for (int i = 0; i < palette.length; i++) {
            if (palette[i].equalsIgnoreCase(draft.color)) next = (i + 1) % palette.length;
        }
        draft.color = palette[next];
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() == 0) {
            if (pageVisible() && mx >= pageX() && mx < pageX() + pageW() && my >= listTop() && my < listBottom()) {
                int index = (int) ((my - listTop() + listScroll) / CARD_PITCH);
                int cardY = listTop() + index * CARD_PITCH - listScroll;
                if (index >= 0 && index < cardCount() && my - cardY < CARD_H) {
                    Squad squad = shownSquad(index);
                    int right = pageX() + pageW() - 6;
                    if (squad != null && !(creating && index == squads().size())) {
                        if (mx >= right - 28 && mx < right - 19 && my - cardY >= 19) {
                            move(index, -1);
                            return true;
                        }
                        if (mx >= right - 16 && mx < right - 7 && my - cardY >= 19) {
                            move(index, 1);
                            return true;
                        }
                    }
                    if (squad != null && !(creating && index == squads().size())) select(squad);
                    return true;
                }
            }
            if (leafVisible() && draft != null && leafClick(mx, my)) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean leafClick(double mx, double my) {
        int x = lfx + 10;
        int right = lfx + lfw - 10;
        int y = lfy + 9;
        if (mx >= right - 18 && mx < right - 5 && my >= y - 1 && my < y + 12) {
            cycleColor();
            return true;
        }
        int rows = Math.max(1, (memberBottom - memberTop) / MEMBER_ROW);
        int first = Math.min(memberScroll, Math.max(0, draft.members.size() - rows));
        for (int row = 0; row < rows && first + row < draft.members.size(); row++) {
            int cy = memberTop + row * MEMBER_ROW;
            if (my < cy || my >= cy + MEMBER_ROW) continue;
            int index = first + row;
            if (mx >= right - 9 && mx < right) {
                draft.members.remove(index);
                return true;
            }
            if (mx >= x && mx < x + 10) {
                boolean makeLeader = !draft.members.get(index).leader;
                for (SquadMember member : draft.members) member.leader = false;
                draft.members.get(index).leader = makeLeader;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int step = vertical > 0 ? -1 : 1;
        if (pageVisible() && mouseX >= pageX() && mouseX < pageX() + pageW()
                && mouseY >= listTop() && mouseY < listBottom()) {
            listScroll = Math.max(0, Math.min(maxListScroll(), listScroll + step * 18));
            return true;
        }
        if (leafVisible() && draft != null && mouseX >= lfx && mouseX < lfx + lfw
                && mouseY >= memberTop && mouseY < memberBottom) {
            int rows = Math.max(1, (memberBottom - memberTop) / MEMBER_ROW);
            memberScroll = Math.max(0, Math.min(Math.max(0, draft.members.size() - rows), memberScroll + step));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    // ---- rendering ----

    @Override
    void drawPage(DrawContext ctx, int mouseX, int mouseY) {
        int lx = pageX();
        int lw = pageW();
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.squad.title")
                .formatted(Formatting.BOLD), lx + 10, pageY() + 8, OttonienTheme.INK, false);
        int top = listTop();
        ctx.enableScissor(lx, top, lx + lw, listBottom());
        for (int i = 0; i < cardCount(); i++) {
            int y = top + i * CARD_PITCH - listScroll;
            if (y + CARD_H < top || y > listBottom()) continue;
            Squad squad = shownSquad(i);
            if (squad != null) drawCard(ctx, squad, i, lx, lw, y, mouseX, mouseY);
        }
        ctx.disableScissor();
        if (cardCount() == 0) {
            drawWrapped(ctx, Text.translatable("ottoextra_tacticalfactions.squad.empty"),
                    lx + 12, top + 6, lw - 24, OttonienTheme.INK_MUTED);
        }
        if (maxListScroll() > 0) {
            int trackH = listBottom() - top;
            int barH = Math.max(10, trackH * trackH / (trackH + maxListScroll()));
            int barY = top + (trackH - barH) * listScroll / maxListScroll();
            ctx.fill(lx + lw - 5, barY, lx + lw - 3, barY + barH, 0xCC7A6248);
        }
    }

    private void drawCard(DrawContext ctx, Squad squad, int index, int lx, int lw, int y,
                          int mouseX, int mouseY) {
        boolean selected = squad.id.equals(selectedId);
        ctx.fill(lx + 6, y, lx + lw - 6, y + CARD_H, selected ? OttonienTheme.PA_D : OttonienTheme.PA_S);
        ctx.fill(lx + 7, y + 1, lx + lw - 7, y + CARD_H - 1, selected ? OttonienTheme.PA_L : OttonienTheme.PA);
        ctx.fill(lx + 7, y + 1, lx + 10, y + CARD_H - 1, BookArt.parseHex(squad.color));
        if (selected) ctx.fill(lx + 10, y + 1, lx + lw - 7, y + 2, OttonienTheme.GOLD_HOVER);

        int online = squad.onlineCount(onlineKeys);
        int total = squad.members.size();
        Text tagText = Text.translatable("ottoextra_tacticalfactions.squad.online", online, total);
        int tagW = BookArt.tag(ctx, textRenderer, lx + lw - 10, y + 4, tagText,
                online > 0 ? OttonienTheme.GREEN : OttonienTheme.NEUTRAL_TAN);
        Text name = Text.literal(BookArt.ellipsize(textRenderer, squad.name, lw - 30 - tagW - 4))
                .formatted(Formatting.BOLD);
        ctx.drawText(textRenderer, name, lx + 15, y + 4, OttonienTheme.INK, false);

        int maxHeads = Math.max(1, (lw - 30 - 30) / 12);
        for (int k = 0; k < Math.min(maxHeads, total); k++) {
            SquadMember member = squad.members.get(k);
            int hx = lx + 15 + k * 12;
            boolean memberOnline = member.isOnline(onlineKeys);
            ctx.fill(hx - 1, y + 16, hx + 10, y + 27, OttonienTheme.PA_D);
            BookArt.head(ctx, BookArt.skinFor(member.uuid, member.account), hx, y + 17, 9);
            if (!memberOnline) ctx.fill(hx, y + 17, hx + 9, y + 26, 0x8CC9AA81);
            else ctx.fill(hx + 7, y + 24, hx + 10, y + 27, BookArt.ONLINE_LIGHT);
            if (member.leader) BookArt.crown(ctx, hx, y + 16);
        }
        if (total > maxHeads) {
            ctx.drawText(textRenderer, "+" + (total - maxHeads), lx + 15 + maxHeads * 12 + 1, y + 19,
                    OttonienTheme.INK_MUTED, false);
        }
        boolean saved = !(creating && squad == draft);
        if (saved) {
            int right = lx + lw - 6;
            int rowEnd = squads().size() - 1;
            boolean hoverUp = mouseX >= right - 28 && mouseX < right - 19 && mouseY >= y + 19 && mouseY < y + CARD_H;
            boolean hoverDown = mouseX >= right - 16 && mouseX < right - 7 && mouseY >= y + 19 && mouseY < y + CARD_H;
            BookArt.triangleUp(ctx, right - 27, y + 21, index > 0
                    ? hoverUp ? OttonienTheme.GOLD_HOVER : OttonienTheme.PICTO : OttonienTheme.PA_S);
            BookArt.triangleDown(ctx, right - 15, y + 21, index < rowEnd
                    ? hoverDown ? OttonienTheme.GOLD_HOVER : OttonienTheme.PICTO : OttonienTheme.PA_S);
        }
    }

    @Override
    void drawLeaf(DrawContext ctx, int mouseX, int mouseY) {
        if (draft == null) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.squad_hud.heading")
                    .formatted(Formatting.BOLD), lfx + 10, lfy + 10, OttonienTheme.INK, false);
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.squad_hud.hint"),
                    lfx + 10, lfy + 21, OttonienTheme.INK_MUTED, false);
            drawWrapped(ctx, Text.translatable("ottoextra_tacticalfactions.squad.select"),
                    lfx + 12, lfy + 10 + 30 + 106, lfw - 24, OttonienTheme.INK_MUTED);
            return;
        }
        int x = lfx + 10;
        int y = lfy + 9;
        int right = lfx + lfw - 10;
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.squad.name"),
                x, y + 1, OttonienTheme.INK_MUTED, false);
        ctx.fill(right - 19, y - 2, right - 4, y + 13, 0xFF0E0E10);
        ctx.fill(right - 18, y - 1, right - 5, y + 12, BookArt.parseHex(draft.color));

        int cy = y + 17;
        ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.squad.members"),
                x, cy, OttonienTheme.INK_MUTED, false);
        Text count = Text.translatable("ottoextra_tacticalfactions.squad.online",
                draft.onlineCount(onlineKeys), draft.members.size());
        ctx.drawText(textRenderer, count, right - textRenderer.getWidth(count), cy,
                OttonienTheme.INK_MUTED, false);

        int rows = Math.max(1, (memberBottom - memberTop) / MEMBER_ROW);
        int first = Math.min(memberScroll, Math.max(0, draft.members.size() - rows));
        memberScroll = first;
        for (int row = 0; row < rows && first + row < draft.members.size(); row++) {
            SquadMember member = draft.members.get(first + row);
            int my = memberTop + row * MEMBER_ROW;
            boolean memberOnline = member.isOnline(onlineKeys);
            BookArt.head(ctx, BookArt.skinFor(member.uuid, member.account), x, my, 9);
            if (member.leader) BookArt.crown(ctx, x, my - 1);
            BookArt.onlineDot(ctx, x + 12, my + 2, memberOnline);
            String shown = TacticalFactions.politics().tablistDisplayName(
                    parseUuid(member.uuid), member.account);
            ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, shown, right - (x + 20) - 14),
                    x + 20, my + 1, memberOnline ? OttonienTheme.INK : OttonienTheme.INK_MUTED, false);
            boolean hover = mouseX >= right - 9 && mouseX < right && mouseY >= my && mouseY < my + MEMBER_ROW;
            BookArt.cross(ctx, right - 7, my + 2, hover ? OttonienTheme.WAX_RED : OttonienTheme.PICTO);
        }
        if (draft.members.isEmpty()) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra_tacticalfactions.squad.no_members"),
                    x, memberTop + 1, OttonienTheme.INK_MUTED, false);
        } else if (draft.members.size() > rows) {
            ctx.drawText(textRenderer, "…", right - 4, memberBottom - 8, OttonienTheme.INK_MUTED, false);
        }
        BookArt.line(ctx, x, right, addY + 17);
        if (error.getString().length() > 0) {
            ctx.drawText(textRenderer, BookArt.ellipsize(textRenderer, error.getString(), lfw - 20),
                    x, lfy + lfh - 25 - 11, OttonienTheme.WAX_TEXT, false);
        }
        if (ffAvailable()) drawFfWarningMark(ctx);
    }

    private void drawFfWarningMark(DrawContext ctx) {
        if (ffY < 0) return;
        int cb = ffY;
        int labelEnd = lfx + 10 + 13 + textRenderer.getWidth(
                Text.translatable("ottoextra_tacticalfactions.squad.friendly_fire"));
        OttonienTheme.pictoWarning(ctx, labelEnd + 6, cb + 2, OttonienTheme.WARN_OCHRE);
    }

    private static java.util.UUID parseUuid(String value) {
        if (value == null) return null;
        try {
            return java.util.UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void drawWrapped(DrawContext ctx, Text text, int x, int y, int width, int color) {
        int ty = y;
        for (var line : textRenderer.wrapLines(text, width)) {
            ctx.drawText(textRenderer, line, x, ty, color, false);
            ty += 10;
        }
    }
}
