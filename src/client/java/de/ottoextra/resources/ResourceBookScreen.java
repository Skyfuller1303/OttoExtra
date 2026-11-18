package de.ottoextra.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.ottoextra.resources.ResourceBook.BRASS;
import static de.ottoextra.resources.ResourceBook.INK;
import static de.ottoextra.resources.ResourceBook.INK_F;
import static de.ottoextra.resources.ResourceBook.INK_M;
import static de.ottoextra.resources.ResourceBook.OK_INK;
import static de.ottoextra.resources.ResourceBook.PA;
import static de.ottoextra.resources.ResourceBook.PA_D;
import static de.ottoextra.resources.ResourceBook.PA_L;
import static de.ottoextra.resources.ResourceBook.PA_S;
import static de.ottoextra.resources.ResourceBook.SOON_INK;
import static de.ottoextra.resources.ResourceBook.TERRA;

/**
 * The resource book: a left page with all lists and a right page that shows the entries of the
 * selected list, or a form (new list, edit list, share). Layout and colours follow the sketches
 * buch_*.png. Widgets are held in Minecraft-typed variables and are the addon's own classes.
 */
final class ResourceBookScreen extends Screen {
    /** What the right page shows. */
    enum View { LIST, NEW_LIST, EDIT_LIST, SHARE }

    private static final int LIST_ROW = 26;
    private static final int NEW_ROW = 18;
    private static final int ENTRY_ROW = 26;
    /** Space between a 16 px symbol and the text beside it. */
    static final int ICON_GAP = 4;
    /** Text column of the list rows on the left page: symbol at x + 8, 16 px, then the gap. */
    private static final int TEXT_X = 8 + 16 + ICON_GAP;
    private static final long CONFIRM_MILLIS = 3_000L;
    private static final String[][] STEPS = {
            {"ottoextra.resources.book.step1", "ottoextra.resources.book.step1_hint"},
            {"ottoextra.resources.book.step2", "ottoextra.resources.book.step2_hint"},
            {"ottoextra.resources.book.step3", "ottoextra.resources.book.step3_hint"},
            {"ottoextra.resources.book.step4", "ottoextra.resources.book.step4_hint"}};

    private final Screen parent;
    private final ResourcesModule module;
    private View view;

    private ResourceBook.Spread spread;
    private int leftScroll;
    private int entryScroll;
    private String codeText = "";

    // forms
    private String formName = "";
    private String formNote = "";
    private boolean formShare;
    private String formIcon = ListMeta.DEFAULT_ICON;
    private String formColor = ListMeta.DEFAULT_COLOR;
    private boolean formUseArms = true;
    private String formListId = "";
    private long deleteUntil;
    private long renewUntil;
    private String notice = "";

    // widgets (Minecraft types only)
    private ButtonWidget deleteButton;
    private ButtonWidget renewButton;

    // cached rows
    private List<ResourceSnapshot> source = List.of();
    private List<ResourceSnapshot> entries = List.of();
    private final Map<String, int[]> counts = new HashMap<>();

    ResourceBookScreen(Screen parent, ResourcesModule module, View view) {
        super(Text.translatable("ottoextra.resources.screen.title"));
        this.parent = parent;
        this.module = module;
        this.view = view;
    }

    // ---- shared static helpers -------------------------------------------------------

    static Text typeName(String type) {
        return Text.translatable("ottoextra.resources.typename." + type.toLowerCase(java.util.Locale.ROOT));
    }

    static int statusColor(ResourceSnapshot resource) {
        if ("READY".equals(resource.status())) return 0xFF9FC7A8;
        return ResourcesModule.isMine(resource.type()) ? 0xFFE4C477 : 0xFFE14949;
    }

    // ---- state ---------------------------------------------------------------------

    private ResourceList selected() {
        return module.activeList();
    }

    private boolean noLists() {
        return module.repository().lists().isEmpty();
    }

    private void show(View next) {
        view = next;
        notice = "";
        deleteUntil = 0L;
        renewUntil = 0L;
        clearAndInit();
    }

    private void select(ResourceList list) {
        module.setActive(list);
        entryScroll = 0;
        show(View.LIST);
    }

    // ---- layout --------------------------------------------------------------------

    @Override
    protected void init() {
        deleteButton = null;
        renewButton = null;
        spread = ResourceBook.spread(width, height, 430, 232);
        ResourceBook.Page left = spread.left();
        ResourceBook.Page right = spread.right();
        // Left page: join by code.
        int fy = codeFooterY();
        Text joinText = Text.translatable("ottoextra.resources.book.join");
        int joinW = ResourceBook.buttonWidth(textRenderer, joinText, 48);
        int joinX = left.x() + left.w() - 10 - joinW;
        TextFieldWidget codeField = new ResourceBook.Field(textRenderer, left.x() + 8, fy + 8,
                joinX - 4 - (left.x() + 8), Text.translatable("ottoextra.resources.book.code_hint"));
        codeField.setMaxLength(40);
        codeField.setText(codeText);
        codeField.setChangedListener(value -> codeText = value);
        addDrawableChild(codeField);
        addDrawableChild(new ResourceBook.Button(joinX, fy + 7, joinW, joinText, ResourceBook.Tone.SLATE,
                b -> join()));

        ResourceList list = selected();
        int rx = right.x() + 10;
        int rw = right.w() - 20;
        int edge = right.x() + right.w() - 8;
        int footerY = right.y() + right.h() - 22;
        if (noLists() && view == View.LIST) {
            Text create = Text.translatable("ottoextra.resources.book.new_list");
            int createW = ResourceBook.buttonWidth(textRenderer, create, 70);
            addDrawableChild(new ResourceBook.Button(edge - createW, footerY, createW, create,
                    ResourceBook.Tone.PRIMARY, b -> startNewList()));
            return;
        }
        switch (view) {
            case LIST -> {
                if (list != null && list.writable()) {
                    Text add = Text.translatable("ottoextra.resources.book.add_entry");
                    Text share = Text.translatable("ottoextra.resources.book.share");
                    int addW = ResourceBook.buttonWidth(textRenderer, add, 70);
                    int shareW = ResourceBook.buttonWidth(textRenderer, share, 40);
                    addDrawableChild(new ResourceBook.Button(edge - addW, right.y() + 4, addW, add,
                            ResourceBook.Tone.PRIMARY, b -> addEntry()));
                    addDrawableChild(new ResourceBook.Button(edge - addW - 3 - shareW, right.y() + 4, shareW, share,
                            ResourceBook.Tone.SLATE, b -> openShare()));
                }
            }
            case NEW_LIST -> {
                TextFieldWidget nameField = textField(rx, right.y() + 36, rw, ListMeta.NAME_MAX, formName,
                        "ottoextra.resources.book.name_hint", value -> formName = value);
                textField(rx, right.y() + 66, rw, ListMeta.NOTE_MAX, formNote,
                        "ottoextra.resources.book.note_hint", value -> formNote = value);
                Text shareNow = Text.translatable("ottoextra.resources.book.share_now");
                addDrawableChild(new ResourceBook.Toggle(rx, shareY(), 14 + textRenderer.getWidth(shareNow),
                        shareNow, formShare, INK, on -> formShare = on));
                Text create = Text.translatable("ottoextra.resources.book.create");
                Text cancel = Text.translatable("gui.cancel");
                int createW = ResourceBook.buttonWidth(textRenderer, create, 64);
                int cancelW = ResourceBook.buttonWidth(textRenderer, cancel, 60);
                addDrawableChild(new ResourceBook.Button(edge - createW, footerY, createW, create,
                        ResourceBook.Tone.PRIMARY, b -> createList()));
                addDrawableChild(new ResourceBook.Button(edge - createW - 3 - cancelW, footerY, cancelW, cancel,
                        ResourceBook.Tone.SLATE, b -> show(View.LIST)));
                setInitialFocus(nameField);
            }
            case EDIT_LIST -> {
                boolean locked = list == null || !list.writable();
                TextFieldWidget nameField = textField(rx, right.y() + 33, rw, ListMeta.NAME_MAX, formName,
                        "ottoextra.resources.book.name_hint", value -> formName = value);
                TextFieldWidget noteField = textField(rx, right.y() + 59, rw, ListMeta.NOTE_MAX, formNote,
                        "ottoextra.resources.book.note_hint", value -> formNote = value);
                nameField.setEditable(!locked);
                noteField.setEditable(!locked);
                Text remove = Text.translatable(list != null && list.shared() ? "ottoextra.resources.book.leave"
                        : "ottoextra.resources.book.delete");
                deleteButton = addDrawableChild(new ResourceBook.Button(rx, footerY,
                        ResourceBook.buttonWidth(textRenderer, Text.translatable("ottoextra.resources.book.leave"), 78),
                        remove, ResourceBook.Tone.DANGER, b -> removeList()));
                Text cancel = Text.translatable("gui.cancel");
                Text save = Text.translatable("ottoextra.resources.book.save");
                int saveW = ResourceBook.buttonWidth(textRenderer, save, 60);
                int cancelW = ResourceBook.buttonWidth(textRenderer, cancel, 60);
                if (!locked) {
                    addDrawableChild(new ResourceBook.Button(edge - saveW, footerY, saveW, save,
                            ResourceBook.Tone.PRIMARY, b -> saveList()));
                }
                addDrawableChild(new ResourceBook.Button(edge - (locked ? 0 : saveW + 3) - cancelW, footerY, cancelW,
                        cancel, ResourceBook.Tone.SLATE, b -> show(View.LIST)));
            }
            case SHARE -> {
                if (list == null || list.readCode == null) {
                    view = View.LIST;
                    clearAndInit();
                    return;
                }
                int y = right.y() + 40;
                if (list.writeCode != null) {
                    copyButtons(rx, rw, y, list.writeCode);
                    y += 40;
                }
                copyButtons(rx, rw, y, list.readCode);
                Text renew = Text.translatable("ottoextra.resources.book.renew");
                int renewW = ResourceBook.buttonWidth(textRenderer, renew, 82);
                renewButton = addDrawableChild(new ResourceBook.Button(rx, footerY, renewW, renew,
                        ResourceBook.Tone.DANGER, b -> renewCodes()));
                renewButton.visible = list.writeCode != null;
                Text done = Text.translatable("ottoextra.resources.book.done");
                int doneW = ResourceBook.buttonWidth(textRenderer, done, 50);
                addDrawableChild(new ResourceBook.Button(edge - doneW, footerY, doneW, done,
                        ResourceBook.Tone.PRIMARY, b -> show(View.LIST)));
            }
        }
    }

    private TextFieldWidget textField(int x, int y, int w, int max, String value, String hintKey,
                                      java.util.function.Consumer<String> onChange) {
        TextFieldWidget field = new ResourceBook.Field(textRenderer, x, y, w, Text.translatable(hintKey));
        field.setMaxLength(max);
        field.setText(value);
        field.setChangedListener(onChange);
        addDrawableChild(field);
        return field;
    }

    /** "Kopieren" and "Link" next to a code box that starts at {@code y} (box top). */
    private void copyButtons(int x, int w, int y, String code) {
        Text copy = Text.translatable("ottoextra.resources.book.copy");
        Text link = Text.translatable("ottoextra.resources.book.link");
        int copyW = ResourceBook.buttonWidth(textRenderer, copy, 50);
        int linkW = ResourceBook.buttonWidth(textRenderer, link, 35);
        int linkX = x + w - linkW;
        int copyX = linkX - 3 - copyW;
        addDrawableChild(new ResourceBook.Button(copyX, y + 1, copyW, copy, ResourceBook.Tone.SLATE,
                b -> client.keyboard.setClipboard(ResourceCodes.display(code))));
        addDrawableChild(new ResourceBook.Button(linkX, y + 1, linkW, link, ResourceBook.Tone.SLATE,
                b -> client.keyboard.setClipboard(ResourceShareApi.joinUrl(module.config().shareBaseUrl,
                        ResourceCodes.normalize(code)))));
    }

    /** Width of the code box: the page minus the two buttons beside it. */
    private int codeBoxWidth(int w) {
        Text copy = Text.translatable("ottoextra.resources.book.copy");
        Text link = Text.translatable("ottoextra.resources.book.link");
        return w - ResourceBook.buttonWidth(textRenderer, copy, 50) - ResourceBook.buttonWidth(textRenderer, link, 35)
                - 3 - 4;
    }

    // ---- actions -------------------------------------------------------------------

    private void join() {
        module.join(MinecraftClient.getInstance(), codeText);
        codeText = "";
        ResourceList list = selected();
        if (list != null) select(list);
        else clearAndInit();
    }

    private void startNewList() {
        formName = module.currentTerritory();
        formNote = "";
        formShare = false;
        formIcon = ListMeta.DEFAULT_ICON;
        formColor = ListMeta.DEFAULT_COLOR;
        formUseArms = true;
        show(View.NEW_LIST);
    }

    private void createList() {
        String listName = formName.isBlank() ? Text.translatable("ottoextra.resources.list.default").getString()
                : formName;
        ResourceList list = module.repository().createList(listName);
        module.repository().updateMeta(list, list.name, formNote, formIcon, formColor,
                System.currentTimeMillis());
        module.repository().setUseArms(list, formUseArms);
        module.setActive(list);
        module.invalidate();
        if (formShare && module.sharingReady(MinecraftClient.getInstance())) {
            module.repository().share(list);
            module.sync().syncSoon(list.localId);
            show(View.SHARE);
        } else {
            select(list);
        }
    }

    private void openEditList() {
        ResourceList list = selected();
        if (list == null) return;
        formName = list.name;
        formNote = list.note;
        formIcon = ListMeta.canonicalIcon(list.icon);
        formColor = list.color;
        formUseArms = list.useArms;
        formListId = list.localId;
        show(View.EDIT_LIST);
    }

    private void saveList() {
        ResourceList list = module.repository().list(formListId);
        if (list == null) { show(View.LIST); return; }
        if (formName.isBlank() || !module.repository().updateMeta(list, formName, formNote, formIcon, formColor,
                System.currentTimeMillis())) {
            notice = Text.translatable("ottoextra.resources.edit.invalid").getString();
            return;
        }
        module.repository().setUseArms(list, formUseArms);
        if (list.shared()) module.sync().syncSoon(list.localId);
        module.invalidate();
        show(View.LIST);
    }

    private void removeList() {
        ResourceList list = module.repository().list(formListId);
        if (list == null) { show(View.LIST); return; }
        long now = System.currentTimeMillis();
        if (now < deleteUntil) {
            module.removeList(MinecraftClient.getInstance(), list);
            ResourceList next = selected();
            if (next != null) select(next);
            else show(View.LIST);
        } else {
            deleteUntil = now + CONFIRM_MILLIS;
        }
    }

    private void openShare() {
        ResourceList list = selected();
        if (list == null) return;
        if (!list.shared()) {
            if (!module.sharingReady(MinecraftClient.getInstance())) return;
            module.repository().share(list);
            module.sync().syncSoon(list.localId);
        }
        show(View.SHARE);
    }

    private void renewCodes() {
        ResourceList list = selected();
        if (list == null || list.writeCode == null) return;
        long now = System.currentTimeMillis();
        if (now < renewUntil) {
            if (module.sharingReady(MinecraftClient.getInstance())) {
                module.repository().renewCode(list, now);
                module.sync().syncSoon(list.localId);
            }
            show(View.SHARE);
        } else {
            renewUntil = now + CONFIRM_MILLIS;
        }
    }

    private void addEntry() {
        client.setScreen(ResourceEditScreen.forCreate(this, module, null));
    }

    // ---- data ----------------------------------------------------------------------

    private void refresh() {
        List<ResourceSnapshot> all = module.snapshots();
        if (all == source) return;
        source = all;
        counts.clear();
        for (ResourceSnapshot snapshot : all) {
            int[] count = counts.computeIfAbsent(snapshot.listId(), id -> new int[2]);
            count[0]++;
            if (!snapshot.markerOnly() && "READY".equals(snapshot.status())) count[1]++;
        }
        ResourceList list = selected();
        String id = list == null ? "" : list.localId;
        // POIs (markers) are listed too, after the resources, alphabetically.
        entries = all.stream().filter(s -> s.listId().equals(id))
                .sorted(Comparator.<ResourceSnapshot>comparingInt(s -> s.markerOnly() ? 1 : 0)
                        .thenComparingInt(s -> "READY".equals(s.status()) ? 0 : 1)
                        .thenComparingInt(ResourceSnapshot::secondsUntilFull)
                        .thenComparing(ResourceSnapshot::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    // ---- geometry helpers ----------------------------------------------------------

    /** Top of the "code from someone else" caption on the left page. */
    private int codeFooterY() { return spread.left().y() + spread.left().h() - 34; }
    private int rowsTop() { return spread.left().y() + 21; }
    private int rowsBottom() { return codeFooterY() - 7; }
    private int entriesTop() { return spread.right().y() + 23; }
    private int entriesBottom() { return spread.right().y() + spread.right().h() - 16; }
    private int visibleEntries() { return Math.max(1, (entriesBottom() - entriesTop()) / ENTRY_ROW); }

    /** Index of the left row at a y position: 0..lists-1 for lists, lists for "+ Neue Liste", else -1. */
    private int leftRowAt(double mouseX, double mouseY) {
        ResourceBook.Page left = spread.left();
        if (mouseX < left.x() + 5 || mouseX >= left.x() + left.w() - 8 || mouseY < rowsTop() || mouseY >= rowsBottom()) {
            return -1;
        }
        int count = module.repository().lists().size();
        int y = rowsTop() + (count == 0 ? 14 : 0);
        for (int index = leftScroll; index <= count; index++) {
            int h = index == count ? NEW_ROW : LIST_ROW;
            if (y + h > rowsBottom()) return -1;
            if (mouseY >= y && mouseY < y + h) return index;
            y += h;
        }
        return -1;
    }

    private int entryAt(double mouseX, double mouseY) {
        ResourceBook.Page right = spread.right();
        if (view != View.LIST || mouseX < right.x() + 6 || mouseX >= right.x() + right.w() - 6
                || mouseY < entriesTop() || mouseY >= entriesBottom()) return -1;
        int index = (int) ((mouseY - entriesTop()) / ENTRY_ROW);
        return index < Math.min(visibleEntries(), entries.size() - entryScroll) ? index : -1;
    }

    // ---- input ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double x = click.x();
        double y = click.y();
        ResourceBook.Page left = spread.left();
        // "Statistik" link in the left header.
        String stats = Text.translatable("ottoextra.resources.stats.button").getString();
        int statsW = ResourceBook.smallWidth(textRenderer, stats);
        if (click.button() == 0 && y >= left.y() + 5 && y < left.y() + 17 && x >= left.x() + left.w() - 10 - statsW
                && x < left.x() + left.w() - 10) {
            module.openStatistics(this);
            return true;
        }
        int row = leftRowAt(x, y);
        if (click.button() == 0 && row >= 0) {
            List<ResourceList> lists = module.repository().lists();
            if (row == lists.size()) startNewList();
            else select(lists.get(row));
            return true;
        }
        ResourceList list = selected();
        ResourceBook.Page right = spread.right();
        if (view == View.LIST && list != null && click.button() == 0 && y >= right.y() + 4 && y < right.y() + 18
                && x >= right.x() + 10 && x < right.x() + 10 + textRenderer.getWidth(list.name)) {
            openEditList();
            return true;
        }
        if (view == View.EDIT_LIST && click.button() == 0 && list != null) {
            if (pickSymbol(x, y, list.writable() ? null : list)) return true;
        }
        if (view == View.NEW_LIST && click.button() == 0 && pickSymbol(x, y, null)) return true;
        if (view == View.EDIT_LIST && click.button() == 0 && list != null && list.writable() && list.writeCode != null
                && hitCodesLink(x, y)) {
            show(View.SHARE);
            return true;
        }
        int entry = entryAt(x, y);
        if (entry >= 0) {
            ResourceSnapshot snapshot = entries.get(entryScroll + entry);
            if (!snapshot.writable()) return true;
            if (click.button() == 0) {
                if (doubled) client.setScreen(new ResourceEditScreen(this, module, snapshot.id()));
                else module.startEntry(snapshot.id());
            } else if (click.button() == 1) {
                module.resetEntry(snapshot.id());
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private int symbolX() { return spread.right().x() + 10; }
    /** Top of the symbol rows: below the faction line on a new list, below the note when editing. */
    private int symbolY() { return spread.right().y() + (view == View.NEW_LIST ? 128 : 88); }
    private int shareY() { return symbolY() + SymbolPicker.HEIGHT + 6; }

    /** Coat of arms of the Gefolge named like the list being typed, or null; checked every frame. */
    private net.minecraft.util.Identifier formArms() {
        return ResourceListIcons.coatOfArms(formName);
    }

    /** Arms are chosen and a Gefolge with arms matches the current name. */
    private boolean armsChosen() {
        return formUseArms && formArms() != null;
    }

    /**
     * Click in the symbol picker. The arms tile chooses the coat of arms, a shape chooses that symbol
     * and switches the arms off; colours only apply to symbols. On a read-only list ({@code readOnly}
     * not null) the shared symbol cannot change, so only the local arms choice is applied at once.
     */
    private boolean pickSymbol(double x, double y, ResourceList readOnly) {
        if (formArms() != null && SymbolPicker.armsAt(symbolX(), symbolY(), x, y)) {
            setFormUseArms(true, readOnly);
            return true;
        }
        String shape = SymbolPicker.shapeAt(symbolX(), symbolY(), x, y);
        if (shape != null) {
            if (readOnly == null) formIcon = shape;
            setFormUseArms(false, readOnly);
            return true;
        }
        String color = SymbolPicker.colorAt(symbolX(), symbolY(), x, y);
        if (color != null) {
            if (readOnly == null && !armsChosen()) formColor = color;
            return true;
        }
        return false;
    }

    private void setFormUseArms(boolean useArms, ResourceList readOnly) {
        formUseArms = useArms;
        if (readOnly != null) {
            module.repository().setUseArms(readOnly, useArms);
            module.invalidate();
        }
    }

    private int accessY() { return symbolY() + SymbolPicker.HEIGHT + 14; }

    private boolean hitCodesLink(double x, double y) {
        Text link = Text.translatable("ottoextra.resources.book.codes_show");
        int start = spread.right().x() + 14 + textRenderer.getWidth(accessText(selected()));
        return y >= accessY() + 8 && y < accessY() + 19 && x >= start && x < start + textRenderer.getWidth(link);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double amount = verticalAmount != 0 ? verticalAmount : horizontalAmount;
        int entry = entryAt(mouseX, mouseY);
        boolean shift = key(GLFW.GLFW_KEY_LEFT_SHIFT) || key(GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean control = key(GLFW.GLFW_KEY_LEFT_CONTROL) || key(GLFW.GLFW_KEY_RIGHT_CONTROL);
        if ((shift || control) && entry >= 0) {
            ResourceSnapshot snapshot = entries.get(entryScroll + entry);
            if (snapshot.writable() && amount != 0) {
                module.shiftEntry(snapshot.id(), (long) Math.signum(amount) * (shift ? 5 : 1) * 60_000L);
            }
            return true;
        }
        ResourceBook.Page right = spread.right();
        if (view == View.LIST && mouseX >= right.x() && mouseX < right.x() + right.w()) {
            entryScroll = Math.clamp(entryScroll - (long) Math.signum(amount), 0,
                    Math.max(0, entries.size() - visibleEntries()));
            return true;
        }
        if (mouseX < spread.left().x() + spread.left().w()) {
            leftScroll = Math.clamp(leftScroll - (long) Math.signum(amount), 0,
                    Math.max(0, module.repository().lists().size()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean key(int code) {
        return InputUtil.isKeyPressed(client.getWindow(), code);
    }

    // ---- drawing -------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refresh();
        ResourceBook.drawSpread(context, width, height, spread);
        drawLeft(context, mouseX, mouseY);
        drawRight(context, mouseX, mouseY);
        if (deleteButton != null) {
            ResourceList list = selected();
            deleteButton.setMessage(Text.translatable(System.currentTimeMillis() < deleteUntil
                    ? "ottoextra.resources.book.sure"
                    : list != null && list.shared() ? "ottoextra.resources.book.leave"
                            : "ottoextra.resources.book.delete"));
        }
        if (renewButton != null) {
            renewButton.setMessage(Text.translatable(System.currentTimeMillis() < renewUntil
                    ? "ottoextra.resources.book.sure" : "ottoextra.resources.book.renew"));
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void text(DrawContext context, String text, int x, int y, int color) {
        ResourceBook.text(context, textRenderer, text, x, y, color);
    }

    private void text(DrawContext context, Text text, int x, int y, int color) {
        ResourceBook.text(context, textRenderer, text, x, y, color);
    }

    private void small(DrawContext context, String text, int x, int y, int color) {
        ResourceBook.small(context, textRenderer, text, x, y, color);
    }

    private void small(DrawContext context, Text text, int x, int y, int color) {
        ResourceBook.small(context, textRenderer, text, x, y, color);
    }

    private String clip(String text, int maxWidth) {
        return ResourceBook.clip(textRenderer, text, maxWidth);
    }

    private void drawLeft(DrawContext context, int mouseX, int mouseY) {
        ResourceBook.Page left = spread.left();
        int x = left.x();
        int w = left.w();
        text(context, Text.translatable("ottoextra.resources.book.lists"), x + 8, left.y() + 7, TERRA);
        String stats = Text.translatable("ottoextra.resources.stats.button").getString();
        ResourceBook.smallRight(context, textRenderer, stats, x + w - 10, left.y() + 9, BRASS);
        ResourceBook.rule(context, x + 8, x + w - 10, left.y() + 18, PA_D);
        List<ResourceList> lists = module.repository().lists();
        ResourceList active = selected();
        int hovered = leftRowAt(mouseX, mouseY);
        int top = rowsTop();
        if (lists.isEmpty()) {
            small(context, Text.translatable("ottoextra.resources.book.no_lists"), x + 11, top + 4, INK_F);
            top += 14;
        }
        for (int index = leftScroll; index <= lists.size(); index++) {
            int h = index == lists.size() ? NEW_ROW : LIST_ROW;
            if (top + h > rowsBottom()) break;
            int cy = top + 2;
            boolean hover = hovered == index;
            if (index == lists.size()) {
                text(context, Text.translatable("ottoextra.resources.book.new_list"), x + 11, cy + 2,
                        hover || view == View.NEW_LIST ? INK : BRASS);
            } else {
                ResourceList list = lists.get(index);
                boolean on = list == active && view != View.NEW_LIST;
                if (on || hover) ResourceBook.rect(context, x + 5, cy - 2, x + w - 8, cy + 21, PA_L);
                if (on) ResourceBook.rect(context, x + 5, cy - 2, x + 7, cy + 21, TERRA);
                ResourceListIcons.drawList(context, list, x + 8, cy - 1, 16);
                int[] count = counts.getOrDefault(list.localId, new int[2]);
                String ready = Text.translatable("ottoextra.resources.book.ready", count[1]).getString();
                int readyW = ResourceBook.smallWidth(textRenderer, ready);
                small(context, ready, x + w - 12 - readyW, cy + 1, count[1] > 0 ? OK_INK : INK_F);
                text(context, clip(list.name, w - 12 - readyW - 4 - TEXT_X), x + TEXT_X, cy, INK);
                String caption = Text.translatable("ottoextra.resources.book.list_caption", count[0],
                        Text.translatable(list.shared() ? list.writable() ? "ottoextra.resources.lists.shared"
                                : "ottoextra.resources.lists.read_only" : "ottoextra.resources.lists.local").getString())
                        .getString();
                small(context, ResourceBook.clipSmall(textRenderer, caption, w - TEXT_X - 10), x + TEXT_X, cy + 11,
                        INK_M);
            }
            top += h;
        }
        int fy = codeFooterY();
        ResourceBook.rule(context, x + 8, x + w - 10, fy - 5, PA_D);
        small(context, Text.translatable("ottoextra.resources.book.code_label"), x + 8, fy, INK_M);
    }

    private void drawRight(DrawContext context, int mouseX, int mouseY) {
        ResourceBook.Page right = spread.right();
        int x = right.x() + 10;
        int w = right.w() - 20;
        ResourceList list = selected();
        if (noLists() && view == View.LIST) {
            drawHowTo(context, x, w);
            return;
        }
        switch (view) {
            case LIST -> drawEntries(context, mouseX, mouseY, list, x, w);
            case NEW_LIST -> drawNewList(context, x, w);
            case EDIT_LIST -> drawEditList(context, x, w, list);
            case SHARE -> drawShare(context, x, w, list);
        }
    }

    /** Header of the right page: terracotta title, optional caption, rule below. */
    private void header(DrawContext context, int x, int w, Text title, String caption, int titleMax) {
        ResourceBook.Page right = spread.right();
        String shown = clip(title.getString(), titleMax);
        text(context, shown, x, right.y() + 7, TERRA);
        if (caption != null && !caption.isEmpty()) {
            int cx = x + 2 + textRenderer.getWidth(shown);
            small(context, ResourceBook.clipSmall(textRenderer, caption, Math.max(0, x + titleMax - cx)), cx,
                    right.y() + 9, INK_M);
        }
        ResourceBook.rule(context, x, x + w + 2, right.y() + 20, PA_D);
    }

    private void drawHowTo(DrawContext context, int x, int w) {
        ResourceBook.Page right = spread.right();
        header(context, x, w, Text.translatable("ottoextra.resources.book.howto_title"), null, w);
        int cy = right.y() + 28;
        for (int index = 0; index < STEPS.length; index++) {
            ResourceBook.rect(context, x, cy, x + 12, cy + 12, TERRA);
            text(context, String.valueOf(index + 1), x + 4, cy + 2, PA_L);
            text(context, clip(Text.translatable(STEPS[index][0]).getString(), w - 18), x + 18, cy, INK);
            small(context, ResourceBook.clipSmall(textRenderer, Text.translatable(STEPS[index][1]).getString(),
                    w - 18), x + 18, cy + 11, INK_M);
            cy += 30;
        }
        ResourceBook.smallWrapped(context, textRenderer, Text.translatable("ottoextra.resources.book.howto_code"),
                x, cy, w, 8, INK_F);
    }

    private void drawEntries(DrawContext context, int mouseX, int mouseY, ResourceList list, int x, int w) {
        ResourceBook.Page right = spread.right();
        if (list == null) return;
        int buttonsW = 0;
        if (list.writable()) {
            buttonsW = ResourceBook.buttonWidth(textRenderer, Text.translatable("ottoextra.resources.book.add_entry"), 70)
                    + 3 + ResourceBook.buttonWidth(textRenderer, Text.translatable("ottoextra.resources.book.share"), 40)
                    + 4;
        }
        header(context, x, w, Text.literal(list.name),
                Text.translatable("ottoextra.resources.book.count", entries.size()).getString(), w + 2 - buttonsW);
        int px = right.x();
        int pw = right.w();
        if (entries.isEmpty()) {
            ResourceBook.smallWrapped(context, textRenderer, Text.translatable(list.writable()
                    ? "ottoextra.resources.book.no_entries" : "ottoextra.resources.book.no_entries_read"),
                    x, entriesTop() + 6, w, 8, INK_M);
            return;
        }
        MinecraftClient minecraft = MinecraftClient.getInstance();
        String dimension = minecraft.world == null ? "" : minecraft.world.getRegistryKey().getValue().toString();
        double playerX = minecraft.player == null ? 0 : minecraft.player.getX();
        double playerZ = minecraft.player == null ? 0 : minecraft.player.getZ();
        int hovered = entryAt(mouseX, mouseY);
        int count = Math.min(visibleEntries(), entries.size() - entryScroll);
        int barX0 = px + pw - 62;
        int barX1 = px + pw - 10;
        for (int index = 0; index < count; index++) {
            ResourceSnapshot row = entries.get(entryScroll + index);
            int cy = entriesTop() + index * ENTRY_ROW + 2;
            if (index == hovered) ResourceBook.rect(context, px + 6, cy - 2, px + pw - 6, cy + 23, PA_L);
            if (row.markerOnly()) {
                drawPoiRow(context, row, px, pw, cy, dimension, playerX, playerZ);
                continue;
            }
            ResourceIcons.draw(context, row.type(), px + 7, cy);
            int ink = ResourceBook.timeInk(row);
            String time = ResourceBook.timeText(row);
            int timeW = textRenderer.getWidth(time);
            text(context, time, px + pw - 10 - timeW, cy, ink);
            int textX = px + 7 + 16 + ICON_GAP;
            text(context, clip(row.name(), px + pw - 10 - timeW - 6 - textX), textX, cy, INK);
            String distance = row.dimension().equals(dimension)
                    ? Text.translatable("ottoextra.resources.book.distance",
                            Math.round(Math.sqrt(ResourcesModule.distanceSquared(row, playerX, playerZ)))).getString()
                    : Text.translatable("ottoextra.resources.book.other_dimension").getString();
            small(context, ResourceBook.clipSmall(textRenderer, caption(row) + ", " + distance, barX0 - 4 - textX),
                    textX, cy + 11, INK_M);
            ResourceBook.rect(context, barX0, cy + 13, barX1, cy + 16, PA_D);
            ResourceBook.rect(context, barX0, cy + 13,
                    barX0 + (int) Math.round((barX1 - barX0) * ResourceBook.progress(row)), cy + 16, ink);
            ResourceBook.rule(context, px + 10, px + pw - 8, cy + 23, PA_S);
        }
        int more = entries.size() - entryScroll - count;
        if (more > 0) {
            small(context, Text.translatable(more == 1 ? "ottoextra.resources.book.more_one"
                    : "ottoextra.resources.book.more", more), x, right.y() + right.h() - 12, INK_F);
        }
    }

    /** A POI row: its own marker symbol, name, "POI" plus distance; no timer and no bar. */
    private void drawPoiRow(DrawContext context, ResourceSnapshot row, int px, int pw, int cy,
                            String dimension, double playerX, double playerZ) {
        net.minecraft.util.Identifier icon = ResourceIcons.iconFor(row);
        if (icon != null) {
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, icon, px + 7, cy,
                    0f, 0f, 16, 16, 16, 16);
        } else {
            ResourceIcons.draw(context, row.type(), px + 7, cy);
        }
        int textX = px + 7 + 16 + ICON_GAP;
        text(context, clip(row.name(), px + pw - 10 - textX), textX, cy, INK);
        String distance = row.dimension().equals(dimension)
                ? Text.translatable("ottoextra.resources.book.distance",
                        Math.round(Math.sqrt(ResourcesModule.distanceSquared(row, playerX, playerZ)))).getString()
                : Text.translatable("ottoextra.resources.book.other_dimension").getString();
        small(context, ResourceBook.clipSmall(textRenderer,
                Text.translatable("ottoextra.resources.book.poi").getString() + ", " + distance,
                px + pw - 10 - textX), textX, cy + 11, INK_M);
        ResourceBook.rule(context, px + 10, px + pw - 8, cy + 23, PA_S);
    }

    /** "Eisen, Mine" / "Torf"; a mine that can already be worked gets ", abbaubar". */
    private String caption(ResourceSnapshot row) {
        String base = ResourcePolicy.baseOf(row.type());
        if (base == null) return row.type();
        String kind = Text.translatable("ottoextra.resources.base." + base.toLowerCase(java.util.Locale.ROOT)).getString();
        if (!ResourcePolicy.hasScope(base)) return kind;
        boolean mine = ResourcePolicy.isMineType(row.type());
        String text = kind + ", " + Text.translatable(mine ? "ottoextra.resources.edit.mine"
                : "ottoextra.resources.edit.deposit").getString();
        if (mine && !"READY".equals(row.status())
                && row.regenerationPercentage() >= ResourcePolicy.MINE_HARVEST_PERCENTAGE) {
            text += ", " + Text.translatable("ottoextra.resources.book.harvestable").getString();
        }
        return text;
    }

    private void drawNewList(DrawContext context, int x, int w) {
        ResourceBook.Page right = spread.right();
        header(context, x, w, Text.translatable("ottoextra.resources.book.new_list_title"), null, w);
        int cy = right.y() + 28;
        small(context, Text.translatable("ottoextra.resources.book.name"), x, cy, INK_M);
        cy += 30;
        small(context, Text.translatable("ottoextra.resources.book.note"), x, cy, INK_M);
        cy += 30;
        small(context, Text.translatable("ottoextra.resources.book.faction"), x, cy, INK_M);
        String faction = module.currentTerritory();
        String shown = faction.isEmpty() ? Text.translatable("ottoextra.resources.edit.no_faction").getString() : faction;
        text(context, clip(shown, w - 60), x, cy + 10, faction.isEmpty() ? INK_F : INK);
        if (!faction.isEmpty()) {
            int sx = x + 2 + textRenderer.getWidth(clip(shown, w - 60));
            small(context, ResourceBook.clipSmall(textRenderer,
                    Text.translatable("ottoextra.resources.book.standing").getString(), x + w - sx), sx, cy + 12, INK_F);
        }
        SymbolPicker.draw(context, textRenderer, symbolX(), symbolY(), formIcon, formColor, formArms(), formUseArms);
        small(context, ResourceBook.clipSmall(textRenderer,
                Text.translatable("ottoextra.resources.book.share_now_hint").getString(), w - 14), x + 14,
                shareY() + 13, INK_M);
    }

    private Text accessText(ResourceList list) {
        if (list == null || !list.shared()) return Text.translatable("ottoextra.resources.book.access_local");
        return Text.translatable(list.writable() ? "ottoextra.resources.book.access_write"
                : "ottoextra.resources.book.access_read");
    }

    private void drawEditList(DrawContext context, int x, int w, ResourceList list) {
        ResourceBook.Page right = spread.right();
        boolean locked = list == null || !list.writable();
        header(context, x, w, Text.translatable("ottoextra.resources.book.edit_title"), null, w);
        small(context, Text.translatable("ottoextra.resources.book.name"), x, right.y() + 26, INK_M);
        small(context, Text.translatable("ottoextra.resources.edit.note"), x, right.y() + 52, INK_M);
        int sy = symbolY();
        SymbolPicker.draw(context, textRenderer, symbolX(), sy, formIcon, formColor, formArms(), formUseArms);
        int ay = accessY();
        small(context, Text.translatable("ottoextra.resources.book.access"), x, ay, INK_M);
        Text access = accessText(list);
        text(context, access, x, ay + 9, INK);
        if (!locked && list.writeCode != null) {
            text(context, Text.translatable("ottoextra.resources.book.codes_show"),
                    x + 4 + textRenderer.getWidth(access), ay + 9, BRASS);
        }
        if (list != null && list.shared()) {
            ResourceBook.smallWrapped(context, textRenderer, Text.translatable("ottoextra.resources.book.meta_hint"),
                    x, ay + 23, w, 8, INK_F);
        }
        if (!notice.isEmpty()) small(context, notice, x, right.y() + right.h() - 34, TERRA);
    }

    private void drawShare(DrawContext context, int x, int w, ResourceList list) {
        ResourceBook.Page right = spread.right();
        header(context, x, w, Text.translatable("ottoextra.resources.book.share_title"),
                list == null ? null : list.name, w);
        if (list == null || list.readCode == null) return;
        int boxW = codeBoxWidth(w);
        int cy = right.y() + 28;
        if (list.writeCode != null) {
            codeBox(context, x, cy, boxW, Text.translatable("ottoextra.resources.book.for_group"),
                    Text.translatable("ottoextra.resources.book.for_group_hint"), list.writeCode);
            cy += 40;
        }
        codeBox(context, x, cy, boxW, Text.translatable("ottoextra.resources.book.for_others"),
                Text.translatable("ottoextra.resources.book.for_others_hint"), list.readCode);
        cy += 40;
        int top = cy;
        int bottom = ResourceBook.smallWrapped(context, textRenderer,
                Text.translatable("ottoextra.resources.book.password_warning"), x + 7, cy + 1, w - 7, 8, SOON_INK);
        bottom = ResourceBook.smallWrapped(context, textRenderer,
                Text.translatable("ottoextra.resources.book.links_hint"), x + 7, bottom, w - 7, 8, INK_M);
        ResourceBook.rect(context, x, top, x + 2, bottom - 2, SOON_INK);
        if (list.writeCode != null) {
            int renewW = ResourceBook.buttonWidth(textRenderer, Text.translatable("ottoextra.resources.book.renew"), 82);
            int doneW = ResourceBook.buttonWidth(textRenderer, Text.translatable("ottoextra.resources.book.done"), 50);
            int hx = x + renewW + 4;
            small(context, ResourceBook.clipSmall(textRenderer,
                    Text.translatable("ottoextra.resources.book.renew_hint").getString(), x + w - doneW - 4 - hx),
                    hx, right.y() + right.h() - 18, INK_F);
        }
    }

    /** Title with caption and a light box with the code; the buttons sit right of the box. */
    private void codeBox(DrawContext context, int x, int y, int boxW, Text title, Text hint, String code) {
        text(context, title, x, y, INK);
        small(context, hint, x + 4 + textRenderer.getWidth(title), y + 2, INK_M);
        int by = y + 12;
        ResourceBook.rect(context, x, by, x + boxW, by + 16, PA_D);
        ResourceBook.rect(context, x + 1, by + 1, x + boxW - 1, by + 15, PA_L);
        text(context, clip(ResourceCodes.display(code), boxW - 10), x + 5, by + 4, INK);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
