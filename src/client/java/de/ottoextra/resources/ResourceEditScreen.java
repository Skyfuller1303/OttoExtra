package de.ottoextra.resources;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static de.ottoextra.resources.ResourceBook.BRASS;
import static de.ottoextra.resources.ResourceBook.INK;
import static de.ottoextra.resources.ResourceBook.INK_F;
import static de.ottoextra.resources.ResourceBook.INK_M;
import static de.ottoextra.resources.ResourceBook.OK_INK;
import static de.ottoextra.resources.ResourceBook.PA_D;
import static de.ottoextra.resources.ResourceBook.PA_L;
import static de.ottoextra.resources.ResourceBook.PA_S;
import static de.ottoextra.resources.ResourceBook.SOON_INK;
import static de.ottoextra.resources.ResourceBook.TERRA;
import static de.ottoextra.resources.ResourceBook.WARN_INK;

/**
 * Editor for one entry as a double page: the left page says what (name, kind, scope, Gefolge),
 * the right page where and how long (list, centre, radius, recovery, note). Used for new
 * entries, for editing and for completing a chat proposal. Layout and colours follow the sketch
 * buch_editor.png; widgets are held in Minecraft-typed variables and are the addon's own classes.
 */
final class ResourceEditScreen extends Screen {
    private static final int PAD = 10;
    /** Kind tiles are high enough for the 16 px item icons. */
    private static final int KIND_TILE = 18;
    private static final int KIND_STEP = 20;
    private static final int SCOPE_TILE = 13;
    private static final long CONFIRM_MILLIS = 3_000L;

    private record Chip(String id, int x, int y, int w, int h) { }

    private final Screen parent;
    private final ResourcesModule module;
    private final String editId;
    private final ResourceList editList;
    private final ZoneRecord record;
    private final boolean proposalMode;
    private final List<ResourceList> targets = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();
    private int listIndex;
    private boolean prepared;
    private boolean proposalDone;

    private String name = "";
    private String territory = "";
    private String note = "";
    private String minutes = "";
    private String centerText = "";
    private String base = "IRON";
    private boolean mine;
    private boolean customOn;
    private boolean territoryTouched;
    private String markerShape = MarkerSymbols.PIN;
    private String markerColor = MarkerSymbols.DEFAULT_COLOR;
    private ResourceZone zone;
    private int radius = 3;
    private String dimension = "";
    private String error = "";
    private long deleteUntil;

    private ResourceBook.Spread spread;
    private TextFieldWidget centerField;
    private TextFieldWidget territoryField;
    private TextFieldWidget minutesField;
    private ButtonWidget deleteButton;
    private RadiusSlider slider;

    /** Editor for an existing entry. */
    ResourceEditScreen(Screen parent, ResourcesModule module, String id) {
        super(Text.translatable("ottoextra.resources.edit.title"));
        this.parent = parent;
        this.module = module;
        this.editId = id;
        this.editList = module.repository().listOfZone(id);
        this.record = editList == null ? null : editList.zones.get(id);
        this.proposalMode = false;
        if (record != null) {
            name = record.name();
            territory = record.territory();
            note = record.note();
            territoryTouched = true;
            String known = ResourcePolicy.baseOf(record.type());
            base = known == null ? "IRON" : known;
            mine = ResourcePolicy.isMineType(record.type());
            zone = record.zone();
            dimension = record.dimension();
            if (record.customMinutes() != null) {
                customOn = true;
                minutes = String.valueOf(record.customMinutes());
            }
            if (zone.type() == ResourceZone.Type.POINT) {
                radius = zone.radius();
                centerText = centerText(zone);
            }
            String symbol = record.symbol();
            markerShape = MarkerSymbols.shape(symbol);
            markerColor = MarkerSymbols.color(symbol);
            prepared = true;
        }
    }

    private boolean marker() {
        return ResourcePolicy.MARKER.equals(base);
    }

    private ResourceEditScreen(Screen parent, ResourcesModule module, ResourceZone initial, boolean proposal) {
        super(Text.translatable("ottoextra.resources.create.title"));
        this.parent = parent;
        this.module = module;
        this.editId = null;
        this.editList = null;
        this.record = null;
        this.zone = initial;
        this.proposalMode = proposal;
    }

    /** Editor for a new entry; {@code initial} is a Litematica cuboid, or null for a point. */
    static ResourceEditScreen forCreate(Screen parent, ResourcesModule module, ResourceZone initial) {
        return new ResourceEditScreen(parent, module, initial, false);
    }

    /** Editor for a new point at a world map position; y is the surface or the player's y. */
    static ResourceEditScreen forCreateAt(Screen parent, ResourcesModule module, int x, int z) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        Integer surface = ResourceSurface.yAt(minecraft.world, x, z);
        int y = surface != null ? surface : minecraft.player == null ? 64 : minecraft.player.getBlockY();
        return new ResourceEditScreen(parent, module, ResourceZone.point(x, y, z, 3), false);
    }

    /** Editor prefilled from the pending chat proposal; saving completes it. */
    static ResourceEditScreen forProposal(Screen parent, ResourcesModule module,
                                          ResourceProposals.Proposal proposal) {
        ResourceEditScreen screen = new ResourceEditScreen(parent, module, proposal.zone(), true);
        screen.name = proposal.name();
        String known = ResourcePolicy.baseOf(proposal.type());
        screen.base = known == null ? "IRON" : known;
        screen.mine = ResourcePolicy.isMineType(proposal.type());
        screen.dimension = proposal.dimension();
        return screen;
    }

    private boolean creating() {
        return editId == null;
    }

    private boolean readOnly() {
        return !creating() && (editList == null || !editList.writable());
    }

    private ResourceList currentList() {
        if (!creating()) return editList;
        return listIndex < targets.size() ? targets.get(listIndex) : null;
    }

    /** Defaults for a new entry: player position, detected Gefolge, active list. */
    private void prepare() {
        prepared = true;
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (dimension.isEmpty()) {
            dimension = minecraft.world == null ? ""
                    : minecraft.world.getRegistryKey().getValue().toString();
        }
        BlockPos here = minecraft.player == null ? BlockPos.ORIGIN : minecraft.player.getBlockPos();
        if (zone == null) zone = ResourceZone.point(here.getX(), here.getY(), here.getZ(), 3);
        if (zone.type() == ResourceZone.Type.POINT) {
            radius = zone.radius();
            centerText = centerText(zone);
        }
        territory = ResourceTerritories.nameAt(dimension, ResourceMapOverlay.centerX(zone),
                ResourceMapOverlay.centerZ(zone));
        targets.clear();
        for (ResourceList list : module.repository().lists()) if (list.writable()) targets.add(list);
        ResourceList active = module.activeList();
        listIndex = active != null && targets.contains(active) ? targets.indexOf(active) : targets.size();
        if (zone.type() == ResourceZone.Type.CUBOID || !proposalMode) mine = true;
    }

    // ---- pure helpers ------------------------------------------------------------------

    static String centerText(ResourceZone point) {
        return point.x() + " " + point.y() + " " + point.z();
    }

    /** Parses "x y z" (spaces or commas); null when malformed or outside the protocol limits. */
    static int[] parseCenter(String text) {
        if (text == null) return null;
        String[] parts = text.trim().split("[\\s,]+");
        if (parts.length != 3) return null;
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            if (Math.abs((long) x) > 30_000_000L || Math.abs((long) z) > 30_000_000L
                    || Math.abs((long) y) > 2048L) return null;
            return new int[] {x, y, z};
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /** "2:00" for 120 minutes. */
    static String hoursText(int minutes) {
        return (minutes / 60) + ":" + (minutes % 60 < 10 ? "0" : "") + (minutes % 60);
    }

    // ---- layout ------------------------------------------------------------------------

    @Override
    protected void init() {
        if (!creating() && record == null) {
            close();
            return;
        }
        if (!prepared) prepare();
        chips.clear();
        boolean locked = readOnly();
        spread = ResourceBook.spread(width, height, 430, 232);
        ResourceBook.Page left = spread.left();
        ResourceBook.Page right = spread.right();

        // Left page
        int lx = left.x() + PAD;
        int lw = left.w() - 2 * PAD;
        TextFieldWidget nameField = field(lx, left.y() + 33, lw, ResourceWire.NAME_MAX, name,
                "ottoextra.resources.edit.name_hint", value -> name = value);
        int tileW = (lw - 4) / 2;
        for (int index = 0; index < ResourcePolicy.BASES.size(); index++) {
            chips.add(new Chip("base:" + ResourcePolicy.BASES.get(index), lx + (index % 2) * (tileW + 4),
                    kindTop() + (index / 2) * KIND_STEP, tileW, KIND_TILE));
        }
        if (ResourcePolicy.hasScope(base)) {
            chips.add(new Chip("scope:deposit", lx, scopeTop(), tileW, SCOPE_TILE));
            chips.add(new Chip("scope:mine", lx + tileW + 4, scopeTop(), tileW, SCOPE_TILE));
        }
        territoryField = field(lx, territoryTop() + 7, lw, ResourceWire.TERRITORY_MAX, territory,
                "ottoextra.resources.edit.territory_hint", value -> {
                    territory = value;
                    territoryTouched = true;
                });
        completeGefolge(territoryField);

        // Right page
        int rx = right.x() + PAD;
        int rw = right.w() - 2 * PAD;
        int buttonRight = right.x() + right.w() - PAD;
        if (zone.type() == ResourceZone.Type.POINT) {
            Text mineText = Text.translatable("ottoextra.resources.edit.my_position");
            int buttonW = ResourceBook.buttonWidth(textRenderer, mineText, 82);
            centerField = field(rx, right.y() + 33, Math.min(150, rw - buttonW - 8), 40, centerText,
                    "ottoextra.resources.edit.center_hint", value -> {
                        centerText = value;
                        pushOutline();
                    });
            ClickableWidget position = addDrawableChild(new ResourceBook.Button(buttonRight - buttonW,
                    right.y() + 31, buttonW, mineText, ResourceBook.Tone.SLATE, b -> useMyPosition()));
            position.active = !locked;
            slider = new RadiusSlider(rx, right.y() + 63, Math.min(170, rw - 80), radius);
            slider.active = !locked;
            addDrawableChild(slider);
        } else {
            Text convert = Text.translatable("ottoextra.resources.edit.to_point");
            int buttonW = ResourceBook.buttonWidth(textRenderer, convert, 82);
            ClickableWidget toPoint = addDrawableChild(new ResourceBook.Button(buttonRight - buttonW,
                    right.y() + 31, buttonW, convert, ResourceBook.Tone.SLATE, b -> {
                        zone = zone.asPoint();
                        radius = zone.radius();
                        centerText = centerText(zone);
                        clearAndInit();
                    }));
            toPoint.active = !locked;
        }
        if (!marker()) {
            Text customText = Text.translatable("ottoextra.resources.edit.custom");
            ClickableWidget custom = new ResourceBook.Toggle(rx, right.y() + 121, 14 + textRenderer.getWidth(customText),
                    customText, customOn, INK_M, on -> {
                        customOn = on;
                        minutesField.setEditable(on && !locked);
                    });
            custom.active = !locked;
            addDrawableChild(custom);
            int minutesX = rx + 14 + textRenderer.getWidth(customText) + 8;
            minutesField = field(minutesX, right.y() + 120, 70, 5, minutes, "ottoextra.resources.edit.minutes",
                    value -> minutes = value);
            minutesField.setEditable(customOn && !locked);
        } else {
            minutesField = null;
        }
        field(rx, right.y() + 149, rw, ResourceWire.NOTE_MAX, note, "ottoextra.resources.edit.note_hint",
                value -> note = value);

        // Footer
        int fy = right.y() + right.h() - 22;
        if (!creating() && !locked) {
            Text text = Text.translatable("ottoextra.resources.edit.delete");
            deleteButton = addDrawableChild(new ResourceBook.Button(rx, fy, ResourceBook.buttonWidth(textRenderer,
                    text, 60), text, ResourceBook.Tone.DANGER, b -> delete()));
        } else {
            deleteButton = null;
        }
        Text saveText = Text.translatable("ottoextra.resources.edit.save");
        Text cancelText = Text.translatable("gui.cancel");
        int saveW = ResourceBook.buttonWidth(textRenderer, saveText, 60);
        int cancelW = ResourceBook.buttonWidth(textRenderer, cancelText, 60);
        int edge = right.x() + right.w() - 8;
        ButtonWidget save = addDrawableChild(new ResourceBook.Button(edge - saveW, fy, saveW, saveText,
                ResourceBook.Tone.PRIMARY, b -> save()));
        save.visible = !locked;
        addDrawableChild(new ResourceBook.Button(edge - (locked ? 0 : saveW + 3) - cancelW, fy, cancelW, cancelText,
                ResourceBook.Tone.SLATE, b -> close()));
        if (creating()) setInitialFocus(nameField);
        pushOutline();
    }

    /** Top of the first kind tile on the left page. */
    private int kindTop() { return spread.left().y() + 62; }

    /** Top of the scope tiles; only used for iron, coal and salt. */
    private int scopeTop() { return kindTop() + 4 * KIND_STEP + 3 + 8; }

    /** Top of the Gefolge caption; the field sits 7 px below. */
    private int territoryTop() {
        return ResourcePolicy.hasScope(base) ? scopeTop() + 21 : kindTop() + 4 * KIND_STEP + 3;
    }

    /** Tab completion of Gefolge names; does nothing with an OttoExtra that has no TabCompletion yet. */
    private static void completeGefolge(TextFieldWidget field) {
        try {
            de.ottoextra.ui.complete.TabCompletion.attach(field,
                    de.ottoextra.ui.complete.TabCompletionSources::gefolge, false);
        } catch (Throwable ignored) {
            // Older OttoExtra: the field works without completion.
        }
    }

    private TextFieldWidget field(int x, int y, int w, int max, String value, String hintKey,
                                  Consumer<String> onChange) {
        TextFieldWidget field = new ResourceBook.Field(textRenderer, x, y, w, Text.translatable(hintKey));
        field.setMaxLength(max);
        field.setText(value);
        field.setChangedListener(onChange);
        field.setEditable(!readOnly());
        addDrawableChild(field);
        return field;
    }

    private void useMyPosition() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player == null) return;
        BlockPos here = minecraft.player.getBlockPos();
        centerText = here.getX() + " " + here.getY() + " " + here.getZ();
        centerField.setText(centerText);
        if (!territoryTouched) {
            territory = ResourceTerritories.nameAt(dimension, here.getX(), here.getZ());
            territoryField.setText(territory);
            territoryTouched = false;
        }
    }

    /** Tells the module which outline to show with particles in the world. */
    private void pushOutline() {
        ResourceZone outline = null;
        if (zone != null && zone.type() == ResourceZone.Type.POINT) {
            int[] center = parseCenter(centerText);
            if (center != null) outline = ResourceZone.point(center[0], center[1], center[2], radius);
        } else if (zone != null) {
            outline = zone;
        }
        module.setEditorOutline(outline, dimension);
    }

    // ---- actions -----------------------------------------------------------------------

    private void delete() {
        long now = System.currentTimeMillis();
        if (now < deleteUntil) {
            module.deleteEntry(editId);
            close();
        } else {
            deleteUntil = now + CONFIRM_MILLIS;
        }
    }

    private void save() {
        if (readOnly()) return;
        String trimmed = name.trim();
        Integer custom = null;
        if (customOn && !marker()) {
            try {
                custom = Integer.parseInt(minutes.trim());
            } catch (NumberFormatException failure) {
                custom = 0;
            }
            if (custom < 1 || custom > ResourcePolicy.MAX_CUSTOM_MINUTES) {
                fail("ottoextra.resources.edit.invalid_minutes");
                return;
            }
        }
        if (trimmed.isEmpty()) {
            fail("ottoextra.resources.edit.invalid_name");
            return;
        }
        ResourceZone result = zone;
        if (zone.type() == ResourceZone.Type.POINT) {
            int[] center = parseCenter(centerText);
            if (center == null) {
                fail("ottoextra.resources.edit.invalid_center");
                return;
            }
            result = ResourceZone.point(center[0], center[1], center[2], radius);
        }
        String type = ResourcePolicy.compose(base, mine);
        if (creating()) {
            ResourceList list = currentList();
            int created = module.createEntry(MinecraftClient.getInstance(), list, territory, trimmed,
                    type, result, territory.trim(), custom, note.trim(),
                    marker() ? MarkerSymbols.key(markerShape, markerColor) : null);
            if (created > 0) {
                proposalSaved();
                close();
            } else {
                fail("ottoextra.resources.edit.invalid");
            }
            return;
        }
        ZoneRecord changed = new ZoneRecord(editId, trimmed, type, record.dimension(), territory.trim(),
                result, custom, note.trim(), record.updatedAt(), false,
                marker() ? MarkerSymbols.key(markerShape, markerColor) : null);
        if (ResourceWire.problem(changed) != null || !module.updateEntry(changed)) {
            fail("ottoextra.resources.edit.invalid");
            return;
        }
        close();
    }

    private void proposalSaved() {
        if (proposalMode && !proposalDone) {
            proposalDone = true;
            module.finishProposalEdit(MinecraftClient.getInstance(), true);
        }
    }

    private void fail(String key) {
        error = Text.translatable(key).getString();
    }

    // ---- input -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && !readOnly()) {
            for (Chip chip : chips) {
                if (click.x() >= chip.x() && click.x() < chip.x() + chip.w() && click.y() >= chip.y()
                        && click.y() < chip.y() + chip.h()) {
                    if (chip.id().startsWith("base:")) base = chip.id().substring(5);
                    else mine = chip.id().equals("scope:mine");
                    clearAndInit();
                    return true;
                }
            }
            if (marker() && pickMarkerSymbol(click.x(), click.y())) return true;
            if (creating() && !proposalMode && hitListSwitch(click.x(), click.y())) {
                listIndex = (listIndex + 1) % (targets.size() + 1);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    /** Top of the marker symbol picker, between the rule at +90 and the note label at +142. */
    private int markerY() { return spread.right().y() + 101; }
    private int markerX() { return spread.right().x() + PAD; }

    private boolean pickMarkerSymbol(double x, double y) {
        String shape = SymbolPicker.shapeAt(markerX(), markerY(), x, y);
        if (shape != null) {
            markerShape = shape;
            return true;
        }
        String color = SymbolPicker.colorAt(markerX(), markerY(), x, y);
        if (color != null) {
            markerColor = color;
            return true;
        }
        return false;
    }

    private boolean hitListSwitch(double x, double y) {
        ResourceBook.Page right = spread.right();
        Text text = Text.translatable("ottoextra.resources.edit.other_list");
        int w = textRenderer.getWidth(text);
        return y >= right.y() + 5 && y < right.y() + 17 && x >= right.x() + right.w() - PAD - w
                && x < right.x() + right.w() - PAD;
    }

    // ---- drawing -----------------------------------------------------------------------

    private void text(DrawContext context, Text text, int x, int y, int color) {
        ResourceBook.text(context, textRenderer, text, x, y, color);
    }

    private void small(DrawContext context, String key, int x, int y, int color) {
        ResourceBook.small(context, textRenderer, Text.translatable(key), x, y, color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ResourceBook.drawSpread(context, width, height, spread);
        ResourceBook.Page left = spread.left();
        ResourceBook.Page right = spread.right();
        int lx = left.x() + PAD;
        int lw = left.w() - 2 * PAD;
        int rx = right.x() + PAD;
        int rw = right.w() - 2 * PAD;
        text(context, Text.literal(clip(title.getString(), lw)), lx, left.y() + 7, TERRA);
        ResourceBook.rule(context, lx, lx + lw, left.y() + 20, PA_D);
        small(context, "ottoextra.resources.book.name", lx, left.y() + 26, INK_M);
        small(context, "ottoextra.resources.edit.kind", lx, kindTop() - 8, INK_M);
        if (ResourcePolicy.hasScope(base)) small(context, "ottoextra.resources.edit.scope", lx, scopeTop() - 8, INK_M);
        String territoryLabel = Text.translatable("ottoextra.resources.edit.territory").getString();
        ResourceBook.small(context, textRenderer, territoryLabel, lx, territoryTop(), INK_M);
        int detectedX = lx + ResourceBook.smallWidth(textRenderer, territoryLabel) + 3;
        ResourceBook.small(context, textRenderer, ResourceBook.clipSmall(textRenderer,
                Text.translatable("ottoextra.resources.edit.territory_detected").getString(), lx + lw - detectedX),
                detectedX, territoryTop(), INK_F);
        drawChips(context, mouseX, mouseY);

        drawListLine(context, rx, rw);
        ResourceBook.rule(context, rx, rx + rw + 2, right.y() + 20, PA_D);
        if (zone.type() == ResourceZone.Type.POINT) {
            small(context, "ottoextra.resources.edit.center", rx, right.y() + 26, INK_M);
            small(context, "ottoextra.resources.edit.radius", rx, right.y() + 54, INK_M);
            if (module.config().showOutlineParticles) {
                small(context, "ottoextra.resources.edit.particle_hint", rx, right.y() + 78, INK_F);
            }
        } else {
            small(context, "ottoextra.resources.edit.place", rx, right.y() + 26, INK_M);
            drawCuboidInfo(context, rx, rw);
        }
        ResourceBook.rule(context, rx, rx + rw + 2, right.y() + 90, PA_S);
        if (marker()) {
            drawMarkerPicker(context);
        } else {
            small(context, "ottoextra.resources.edit.recovery_title", rx, right.y() + 96, INK_M);
            drawRecoveryLine(context, rx, right.y() + 105);
        }
        small(context, "ottoextra.resources.edit.note", rx, right.y() + 142, INK_M);
        drawFooterText(context, rx, rw);
        super.render(context, mouseX, mouseY, delta);
        if (slider != null) {
            text(context, Text.translatable("ottoextra.resources.edit.radius_value", radius),
                    slider.getX() + slider.getWidth() + 8, slider.getY() + 2, INK);
        }
    }

    private void drawListLine(DrawContext context, int rx, int rw) {
        ResourceBook.Page right = spread.right();
        ResourceList list = currentList();
        ResourceListIcons.drawList(context, list, rx, right.y() + 3, 16);
        String listName = list == null ? Text.translatable("ottoextra.resources.screen.new_list").getString() : list.name;
        int otherW = 0;
        if (creating() && !proposalMode) {
            String other = Text.translatable("ottoextra.resources.edit.other_list").getString();
            otherW = ResourceBook.smallWidth(textRenderer, other);
            ResourceBook.smallRight(context, textRenderer, other, rx + rw, right.y() + 9, BRASS);
        }
        int textX = rx + 16 + ResourceBookScreen.ICON_GAP;
        text(context, Text.literal(clip(listName, rx + rw - otherW - 6 - textX)), textX, right.y() + 7, INK);
    }

    private void drawChips(DrawContext context, int mouseX, int mouseY) {
        boolean locked = readOnly();
        for (Chip chip : chips) {
            boolean kind = chip.id().startsWith("base:");
            boolean on = kind ? chip.id().equals("base:" + this.base)
                    : chip.id().equals(mine ? "scope:mine" : "scope:deposit");
            boolean hover = !locked && mouseX >= chip.x() && mouseX < chip.x() + chip.w() && mouseY >= chip.y()
                    && mouseY < chip.y() + chip.h();
            ResourceBook.chip(context, chip.x(), chip.y(), chip.w(), chip.h(), on, hover);
            int textX = chip.x() + 4;
            Text label;
            if (kind) {
                String tile = chip.id().substring(5);
                ResourceIcons.draw(context, ResourcePolicy.compose(tile, true), chip.x() + 1, chip.y() + 1);
                textX = chip.x() + 1 + 16 + ResourceBookScreen.ICON_GAP;
                label = Text.translatable("ottoextra.resources.base." + tile.toLowerCase(java.util.Locale.ROOT));
            } else {
                label = Text.translatable(chip.id().equals("scope:mine") ? "ottoextra.resources.edit.mine"
                        : "ottoextra.resources.edit.deposit");
            }
            text(context, Text.literal(clip(label.getString(), chip.x() + chip.w() - 1 - textX)), textX,
                    chip.y() + (chip.h() - 8) / 2 + 1, on ? INK : INK_M);
        }
    }

    /** Predefined shapes and colours of a marker: the same picker as for list symbols. */
    private void drawMarkerPicker(DrawContext context) {
        SymbolPicker.draw(context, textRenderer, markerX(), markerY(), markerShape, markerColor);
    }

    private void drawRecoveryLine(DrawContext context, int x, int y) {
        String type = ResourcePolicy.compose(base, mine);
        Integer custom = null;
        if (customOn) {
            try {
                int value = Integer.parseInt(minutes.trim());
                if (value >= 1 && value <= ResourcePolicy.MAX_CUSTOM_MINUTES) custom = value;
            } catch (NumberFormatException ignored) {
                // Falls back to the default while the field is not a valid number.
            }
        }
        List<ResourcePolicy.Stage> stages = ResourcePolicy.stages(type, custom);
        if (stages.isEmpty()) {
            text(context, Text.translatable("ottoextra.resources.edit.no_recovery"), x, y, INK_M);
            return;
        }
        int cursor = x;
        for (int index = 0; index < stages.size(); index++) {
            ResourcePolicy.Stage stage = stages.get(index);
            boolean last = stage.percentage() >= 100;
            Text part = last ? Text.translatable("ottoextra.resources.edit.stage_full", hoursText(stage.afterMinutes()))
                    : Text.translatable("ottoextra.resources.edit.stage_partial", hoursText(stage.afterMinutes()),
                    stage.percentage());
            text(context, part, cursor, y, last ? OK_INK : WARN_INK);
            cursor += textRenderer.getWidth(part);
            if (index < stages.size() - 1) {
                text(context, Text.literal(","), cursor, y, INK_M);
                cursor += textRenderer.getWidth(", ");
            }
        }
    }

    private void drawCuboidInfo(DrawContext context, int rx, int rw) {
        ResourceBook.Page right = spread.right();
        int boxW = rw - 100;
        int y = right.y() + 33;
        ResourceBook.rect(context, rx, y, rx + boxW, y + 34, PA_D);
        ResourceBook.rect(context, rx + 1, y + 1, rx + boxW - 1, y + 33, PA_L);
        text(context, Text.literal(clip(Text.translatable("ottoextra.resources.edit.cuboid", zone.sizeX(),
                zone.sizeZ(), zone.sizeY()).getString(), boxW - 8)), rx + 4, y + 5, INK);
        text(context, Text.literal(clip(Text.translatable("ottoextra.resources.edit.cuboid_center",
                centerText(zone.asPoint())).getString(), boxW - 8)), rx + 4, y + 18, INK_M);
    }

    private String clip(String text, int maxWidth) {
        return ResourceBook.clip(textRenderer, text, maxWidth);
    }

    private void drawFooterText(DrawContext context, int rx, int rw) {
        ResourceBook.Page right = spread.right();
        int hintY = right.y() + right.h() - 34;
        Text message = null;
        int color = INK_F;
        if (!error.isEmpty()) {
            message = Text.literal(error);
            color = SOON_INK;
        } else if (deleteButton != null && System.currentTimeMillis() < deleteUntil) {
            message = Text.translatable("ottoextra.resources.edit.delete_confirm");
            color = SOON_INK;
        } else if (readOnly()) {
            message = Text.translatable("ottoextra.resources.readonly");
            color = SOON_INK;
        } else if (currentList() != null && currentList().shared()) {
            message = Text.translatable("ottoextra.resources.edit.visible_to_all");
        }
        if (message == null) return;
        ResourceBook.small(context, textRenderer, ResourceBook.clipSmall(textRenderer, message.getString(), rw),
                rx, hintY, color);
    }

    /** Radius 1..64 as a parchment track with a brass handle; the value is drawn next to it. */
    private final class RadiusSlider extends SliderWidget {
        RadiusSlider(int x, int y, int w, int radiusValue) {
            super(x, y, w, 12, Text.empty(), (radiusValue - 1) / 63.0);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int handle = getX() + (int) Math.round(value * (getWidth() - 5));
            ResourceBook.rect(context, getX(), getY() + 5, getX() + getWidth(), getY() + 7, PA_D);
            ResourceBook.rect(context, getX(), getY() + 5, handle + 2, getY() + 7, INK_M);
            int brass = isHovered() || isFocused() ? ResourceBook.shade(BRASS, 1.3) : BRASS;
            ResourceBook.rect(context, handle, getY() + 1, handle + 5, getY() + 11, brass);
            ResourceBook.rect(context, handle + 1, getY() + 2, handle + 4, getY() + 3, ResourceBook.shade(brass, 1.4));
        }

        @Override protected void updateMessage() { setMessage(Text.empty()); }

        @Override
        protected void applyValue() {
            radius = 1 + (int) Math.round(value * 63);
            pushOutline();
        }
    }

    @Override
    public void removed() {
        module.setEditorOutline(null, "");
    }

    @Override
    public void close() {
        if (proposalMode && !proposalDone) {
            proposalDone = true;
            module.finishProposalEdit(MinecraftClient.getInstance(), false);
        }
        if (client != null) client.setScreen(parent);
    }
}
