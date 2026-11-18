package de.ottoextra.tacticalfactions;

import de.ottoextra.tacticalfactions.theme.InkTextField;
import de.ottoextra.tacticalfactions.theme.OttonienTheme;
import de.ottoextra.tacticalfactions.theme.SlateButton;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Editor for a local group (fief or following), shown in the right leaf of the politics tab.
 * Holds the editing logic of the former CustomEntityEditorScreen; the host screen owns the widgets'
 * lifetime and forwards clicks, scrolling and rendering.
 */
final class GroupEditorPanel {
    private static final int MEMBER_ROW = 10;
    private static final int DROPDOWN_ROW = 11;
    private static final int DROPDOWN_ROWS = 7;
    private static final Relation[] STATUS = {Relation.INHERIT, Relation.ALLIED,
            Relation.NEUTRAL, Relation.HOSTILE};

    private final BookScreen host;
    private final TextRenderer font;
    private final Consumer<String> finished;
    private final CustomPoliticalEntity draft;
    private final boolean creating;
    private final Map<String, PoliticalStateService.MemberRef> members = new LinkedHashMap<>();
    private Relation relation;
    private Path pendingBanner;
    private boolean removeBanner;
    private boolean deleteArmed;
    private Text error = Text.empty();
    private Identifier bannerTexture;

    private List<PoliticalStateService.FactionView> parents = List.of();
    private boolean dropdownOpen;
    private int dropdownScroll;
    private int memberScroll;

    private InkTextField nameField;
    private InkTextField noteField;
    private InkTextField accountField;
    private SlateButton deleteButton;

    // Layout (valid after init()).
    private int x;
    private int right;
    private int fy;
    private int addY;

    /** {@code finished} receives the id to select afterwards, or null. */
    GroupEditorPanel(BookScreen host, TextRenderer font, CustomPoliticalEntity entity, boolean creating,
                     Consumer<String> finished) {
        this.host = host;
        this.font = font;
        this.finished = finished;
        this.draft = entity.copy();
        this.creating = creating;
        this.relation = TacticalFactions.config().factionRelations.getOrDefault(entity.id, Relation.INHERIT);
        for (PoliticalStateService.MemberRef member : TacticalFactions.politics().members(entity.id)) {
            members.put(memberKey(member), member);
        }
        this.parents = TacticalFactions.politics().availableParents(draft.id);
        this.bannerTexture = LocalBannerTextureService.instance()
                .bannerForCustom(draft.id, draft.bannerFile).orElse(null);
    }

    String entityId() { return draft.id; }

    // ---- widgets ----

    void init() {
        x = host.lfx + 10;
        right = host.lfx + host.lfw - 10;
        fy = host.lfy + 9 + 20;
        addY = host.lfy + host.lfh - 44;
        int fieldX = x + 51;
        nameField = host.leaf(new InkTextField(font, fieldX, fy - 2, right - fieldX, 12, Text.empty(), false));
        nameField.setMaxLength(80);
        nameField.setText(draft.name);
        nameField.setChangedListener(value -> error = Text.empty());
        noteField = host.leaf(new InkTextField(font, fieldX, fy + 11, right - fieldX, 12, Text.empty(), false));
        noteField.setMaxLength(256);
        noteField.setText(draft.description);

        accountField = host.leaf(new InkTextField(font, x, addY, right - 30 - x, 12,
                Text.translatable("ottoextra_tacticalfactions.editor.member.manual"), true));
        accountField.setMaxLength(40);
        TabHints.accounts(accountField);
        accountField.setSuggestion(Text.translatable("ottoextra_tacticalfactions.editor.member.manual").getString());
        accountField.setChangedListener(value -> accountField.setSuggestion(value.isEmpty()
                ? Text.translatable("ottoextra_tacticalfactions.editor.member.manual").getString() : ""));
        host.leaf(new SlateButton(right - 27, addY - 1, 17, 14, Text.literal("+"), b -> addMember()));

        int bw = (host.lfw - 26) / 2;
        host.leaf(new SlateButton(host.lfx + 10, host.lfy + host.lfh - 25, bw, 18,
                Text.translatable("gui.save"), b -> save()));
        deleteButton = host.leaf(new SlateButton(host.lfx + 16 + bw, host.lfy + host.lfh - 25, bw, 18,
                deleteText(), b -> deleteOrCancel()).danger());
    }

    private Text deleteText() {
        if (creating) return Text.translatable("gui.cancel");
        return Text.translatable(deleteArmed ? "ottoextra_tacticalfactions.editor.delete.confirm"
                : "ottoextra_tacticalfactions.editor.delete");
    }

    // ---- actions ----

    private void addMember() {
        String account = accountField.getText().trim();
        if (account.isBlank()) return;
        String uuid = null;
        String display = account;
        for (PoliticalStateService.PlayerChoice player : TacticalFactions.politics().players()) {
            if (player.account() != null && player.account().equalsIgnoreCase(account)) {
                uuid = player.uuid();
                account = player.account();
                display = player.displayName() != null ? player.displayName() : account;
                break;
            }
        }
        for (PoliticalStateService.MemberRef existing : members.values()) {
            if (existing.account() != null && existing.account().equalsIgnoreCase(account)) {
                accountField.setText("");
                return;
            }
        }
        PoliticalStateService.MemberRef member = new PoliticalStateService.MemberRef(uuid, account, display);
        members.putIfAbsent(memberKey(member), member);
        accountField.setText("");
        memberScroll = Integer.MAX_VALUE;
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            error = Text.translatable("ottoextra_tacticalfactions.editor.error.name");
            return;
        }
        draft.name = name;
        draft.description = noteField.getText().trim();
        String previousBanner = draft.bannerFile;
        if (pendingBanner != null) {
            try {
                draft.bannerFile = LocalBannerTextureService.instance().importPng(draft.id, pendingBanner);
            } catch (LocalBannerTextureService.BannerImportException exception) {
                error = Text.translatable(exception.translationKey());
                return;
            }
        } else if (removeBanner) {
            draft.bannerFile = null;
            LocalBannerTextureService.instance().delete(draft.id, previousBanner);
        }
        TacticalFactions.politics().upsertCustom(draft, relation, new ArrayList<>(members.values()));
        finished.accept(draft.id);
    }

    private void deleteOrCancel() {
        if (creating) {
            finished.accept(null);
            return;
        }
        if (!deleteArmed) {
            deleteArmed = true;
            deleteButton.setMessage(deleteText());
            return;
        }
        TacticalFactions.politics().deleteCustom(draft.id);
        finished.accept(null);
    }

    private void chooseBanner() {
        String chosen = TinyFileDialogs.tinyfd_openFileDialog(
                Text.translatable("ottoextra_tacticalfactions.editor.banner.choose").getString(), "", null,
                "PNG", false);
        if (chosen != null && !chosen.isBlank()) {
            pendingBanner = Path.of(chosen);
            removeBanner = false;
        }
    }

    // ---- hit boxes ----

    private BookScreen.Box bannerBox() { return new BookScreen.Box(host.lfx + host.lfw - 32, host.lfy + 6, 22, 22); }

    private BookScreen.Box parentBox() { return new BookScreen.Box(x + 54, fy + 39, right - (x + 54), 12); }

    private int typeChipWidth(PoliticalEntityType type) {
        return font.getWidth(typeLabel(type)) + 10;
    }

    private BookScreen.Box typeBox(PoliticalEntityType type) {
        int cx = x + 54;
        if (type == PoliticalEntityType.FOLLOWING) cx += typeChipWidth(PoliticalEntityType.FIEF) + 3;
        return new BookScreen.Box(cx, fy + 26, typeChipWidth(type), 11);
    }

    private int statusChipWidth() {
        int w = 0;
        for (Relation r : STATUS) w = Math.max(w, font.getWidth(shortLabel(r)));
        return w + 8;
    }

    private BookScreen.Box statusBox(int index) {
        int cw = statusChipWidth();
        return new BookScreen.Box(x + 54 + (index % 2) * (cw + 3), fy + 54 + (index / 2) * 13, cw, 11);
    }

    private int membersY() { return fy + 84; }

    private int memberRows() {
        return Math.max(1, (addY - 1 - (membersY() + 11)) / MEMBER_ROW);
    }

    private BookScreen.Box dropdownBox() {
        BookScreen.Box field = parentBox();
        int rows = Math.min(DROPDOWN_ROWS, parents.size() + 1);
        return new BookScreen.Box(field.x(), field.y() + field.h(), field.w(), rows * DROPDOWN_ROW + 2);
    }

    // ---- input ----

    boolean mouseClicked(double mx, double my, int button) {
        if (dropdownOpen) {
            BookScreen.Box box = dropdownBox();
            if (button == 0 && box.has(mx, my)) {
                int index = dropdownScroll + (int) ((my - box.y() - 1) / DROPDOWN_ROW);
                if (index == 0) {
                    draft.parentId = null;
                } else if (index - 1 < parents.size()) {
                    draft.parentId = parents.get(index - 1).id();
                }
                dropdownOpen = false;
                return true;
            }
            dropdownOpen = false;
            if (parentBox().has(mx, my)) return true;
        }
        if (bannerBox().has(mx, my)) {
            if (button == 1) {
                pendingBanner = null;
                removeBanner = true;
            } else if (button == 0) {
                chooseBanner();
            }
            return true;
        }
        if (button != 0) return false;
        for (PoliticalEntityType type : new PoliticalEntityType[]{PoliticalEntityType.FIEF,
                PoliticalEntityType.FOLLOWING}) {
            if (typeBox(type).has(mx, my)) {
                draft.type = type;
                return true;
            }
        }
        for (int i = 0; i < STATUS.length; i++) {
            if (statusBox(i).has(mx, my)) {
                relation = STATUS[i];
                return true;
            }
        }
        if (parentBox().has(mx, my)) {
            dropdownOpen = true;
            dropdownScroll = 0;
            return true;
        }
        int rows = memberRows();
        List<PoliticalStateService.MemberRef> list = new ArrayList<>(members.values());
        int first = Math.min(memberScroll, Math.max(0, list.size() - rows));
        for (int row = 0; row < rows && first + row < list.size(); row++) {
            int ry = membersY() + 11 + row * MEMBER_ROW;
            if (my >= ry && my < ry + MEMBER_ROW && mx >= right - 10 && mx < right) {
                members.remove(memberKey(list.get(first + row)));
                return true;
            }
        }
        return false;
    }

    boolean mouseScrolled(double mx, double my, double vertical) {
        int step = vertical > 0 ? -1 : 1;
        if (dropdownOpen && dropdownBox().has(mx, my)) {
            int max = Math.max(0, parents.size() + 1 - DROPDOWN_ROWS);
            dropdownScroll = Math.max(0, Math.min(max, dropdownScroll + step));
            return true;
        }
        int rows = memberRows();
        if (mx >= host.lfx && mx < host.lfx + host.lfw && my >= membersY() + 11 && my < addY) {
            memberScroll = Math.max(0, Math.min(Math.max(0, members.size() - rows), memberScroll + step));
            return true;
        }
        return false;
    }

    // ---- rendering ----

    void draw(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(font, Text.translatable(creating ? "ottoextra_tacticalfactions.editor.create"
                : "ottoextra_tacticalfactions.editor.edit").formatted(Formatting.BOLD),
                x, host.lfy + 9, OttonienTheme.INK, false);

        BookScreen.Box tile = bannerBox();
        OttonienTheme.tile(ctx, tile.x(), tile.y(), tile.w(), tile.h(), tile.has(mouseX, mouseY));
        boolean noBanner = removeBanner || draft.bannerFile == null && pendingBanner == null;
        BookArt.arms(ctx, noBanner ? null : bannerTexture, tile.x() + 3, tile.y() + 3, 16,
                mapColorOfParent(), draft.id);
        Text bannerLabel = Text.translatable("ottoextra_tacticalfactions.editor.banner");
        ctx.drawText(font, bannerLabel, tile.x() - font.getWidth(bannerLabel) - 3, tile.y() + 13,
                OttonienTheme.INK_MUTED, false);

        drawLabel(ctx, "ottoextra_tacticalfactions.editor.name", fy);
        drawLabel(ctx, "ottoextra_tacticalfactions.editor.description", fy + 13);
        drawLabel(ctx, "ottoextra_tacticalfactions.editor.type", fy + 27);
        drawLabel(ctx, "ottoextra_tacticalfactions.editor.parent", fy + 41);
        drawLabel(ctx, "ottoextra_tacticalfactions.editor.relation", fy + 55);

        for (PoliticalEntityType type : new PoliticalEntityType[]{PoliticalEntityType.FIEF,
                PoliticalEntityType.FOLLOWING}) {
            BookScreen.Box box = typeBox(type);
            BookArt.chip(ctx, font, box.x(), box.y(), box.w(), typeLabel(type), BookArt.LOCAL_TAG,
                    draft.type == type, false);
        }

        BookScreen.Box parent = parentBox();
        BookArt.line(ctx, parent.x(), parent.x() + parent.w(), parent.y() + parent.h() - 1);
        ctx.drawText(font, BookArt.ellipsize(font, parentName(), parent.w() - 14), parent.x() + 2, fy + 41,
                OttonienTheme.INK, false);
        BookArt.triangleDown(ctx, parent.x() + parent.w() - 8, parent.y() + 4, OttonienTheme.PICTO);

        for (int i = 0; i < STATUS.length; i++) {
            BookScreen.Box box = statusBox(i);
            BookArt.chip(ctx, font, box.x(), box.y(), box.w(), shortLabel(STATUS[i]),
                    BookArt.relationColor(STATUS[i]), relation == STATUS[i], true);
        }

        BookArt.line(ctx, x, right, membersY() - 2);
        ctx.drawText(font, Text.translatable("ottoextra_tacticalfactions.editor.members", members.size()),
                x, membersY() + 1, OttonienTheme.INK_MUTED, false);
        Text status = error.getString().isEmpty() ? statusLine() : error;
        if (!status.getString().isEmpty()) {
            String clipped = BookArt.ellipsize(font, status.getString(), 100);
            ctx.drawText(font, clipped, right - font.getWidth(clipped), membersY() + 1,
                    error.getString().isEmpty() ? OttonienTheme.INK_MUTED : OttonienTheme.WAX_TEXT, false);
        }
        drawMembers(ctx, mouseX, mouseY);
    }

    private Text statusLine() {
        if (pendingBanner != null) return Text.literal(pendingBanner.getFileName().toString());
        return Text.empty();
    }

    private void drawMembers(DrawContext ctx, int mouseX, int mouseY) {
        List<PoliticalStateService.MemberRef> list = new ArrayList<>(members.values());
        int rows = memberRows();
        int first = Math.min(memberScroll, Math.max(0, list.size() - rows));
        memberScroll = first;
        for (int row = 0; row < rows && first + row < list.size(); row++) {
            PoliticalStateService.MemberRef member = list.get(first + row);
            int ry = membersY() + 11 + row * MEMBER_ROW;
            BookArt.head(ctx, BookArt.skinFor(member.uuid(), member.account()), x, ry, 9);
            String name = member.displayName() != null && !member.displayName().isBlank()
                    ? member.displayName() : member.account();
            if (member.account() != null && !member.account().equalsIgnoreCase(name)) {
                name = name + " (" + member.account() + ")";
            }
            ctx.drawText(font, BookArt.ellipsize(font, name, right - x - 12 - 14), x + 12, ry + 1,
                    OttonienTheme.INK, false);
            boolean hover = mouseX >= right - 10 && mouseX < right && mouseY >= ry && mouseY < ry + MEMBER_ROW;
            BookArt.cross(ctx, right - 8, ry + 2, hover ? OttonienTheme.WAX_TEXT : OttonienTheme.PICTO);
        }
    }

    /** Drawn after the widgets so the dropdown covers the fields below it. */
    void drawOver(DrawContext ctx, int mouseX, int mouseY) {
        BookScreen.Box tile = bannerBox();
        if (!dropdownOpen) {
            if (tile.has(mouseX, mouseY)) {
                ctx.drawTooltip(font, Text.translatable("ottoextra_tacticalfactions.editor.banner.tip"),
                        mouseX, mouseY);
            }
            return;
        }
        BookScreen.Box box = dropdownBox();
        ctx.fill(box.x(), box.y(), box.x() + box.w(), box.y() + box.h(), 0xFF000000);
        ctx.fill(box.x() + 1, box.y() + 1, box.x() + box.w() - 1, box.y() + box.h() - 1, OttonienTheme.FIELD_DARK);
        int rows = Math.min(DROPDOWN_ROWS, parents.size() + 1);
        for (int row = 0; row < rows; row++) {
            int index = dropdownScroll + row;
            int ry = box.y() + 1 + row * DROPDOWN_ROW;
            boolean hover = mouseX >= box.x() && mouseX < box.x() + box.w()
                    && mouseY >= ry && mouseY < ry + DROPDOWN_ROW;
            if (hover) ctx.fill(box.x() + 1, ry, box.x() + box.w() - 1, ry + DROPDOWN_ROW, 0xFF323D4A);
            String label = index == 0
                    ? Text.translatable("ottoextra_tacticalfactions.editor.parent.none").getString()
                    : parents.get(index - 1).name();
            boolean current = index == 0 ? draft.parentId == null
                    : parents.get(index - 1).id().equals(draft.parentId);
            ctx.drawText(font, BookArt.ellipsize(font, label, box.w() - 8), box.x() + 3, ry + 2,
                    current ? OttonienTheme.GOLD_HOVER : OttonienTheme.LABEL, false);
        }
    }

    // ---- helpers ----

    private void drawLabel(DrawContext ctx, String key, int y) {
        ctx.drawText(font, Text.translatable(key), x, y, OttonienTheme.INK_MUTED, false);
    }

    private String parentName() {
        if (draft.parentId == null) {
            return Text.translatable("ottoextra_tacticalfactions.editor.parent.none").getString();
        }
        return parents.stream().filter(view -> view.id().equals(draft.parentId)).findFirst()
                .map(PoliticalStateService.FactionView::name).orElse(draft.parentId);
    }

    private int mapColorOfParent() {
        if (draft.parentId != null) {
            for (PoliticalStateService.FactionView view : parents) {
                if (view.id().equals(draft.parentId)) return view.mapColor();
            }
        }
        return 0xFF777777;
    }

    private static Text typeLabel(PoliticalEntityType type) {
        return Text.translatable("ottoextra_tacticalfactions.editor.type." + type.name().toLowerCase(Locale.ROOT));
    }

    private static Text shortLabel(Relation relation) {
        return Text.translatable(BookArt.shortRelationKey(relation));
    }

    private static String memberKey(PoliticalStateService.MemberRef member) {
        if (member.uuid() != null && !member.uuid().isBlank()) return "u:" + member.uuid();
        return "a:" + (member.account() == null ? "" : member.account().trim().toLowerCase(Locale.ROOT));
    }
}
