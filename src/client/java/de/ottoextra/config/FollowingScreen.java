package de.ottoextra.config;

import de.ottoextra.api.model.FactionRecord;
import de.ottoextra.map.LehenPolygon;
import de.ottoextra.map.LehenPolygonStore;
import de.ottoextra.map.MapOverlayRenderer;
import de.ottoextra.map.PoliticalOverlay;
import de.ottoextra.regions.RegionsServices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Gefolge-Liste (Einstellungen → Karte → Basis), Vanilla-Dark-Stil. Drei-zeilige
 * Einträge: Wappen links, dann Gefolgename (groß), Titel/Rang, Lehensname.
 * Hierarchisch = Fraktions-Baum (Lehnsherren + alle Vasallen, eingerückt);
 * Flach = alle Lehen. Auswahl öffnet unten den Editor (Name + Gefolge-Farbe;
 * mit Lehen zusätzlich eigene Lehen-Farbe).
 */
public final class FollowingScreen extends Screen {

    private static final int COL_PANEL = 0xC8141418;
    private static final int COL_BORDER = 0xFF000000;
    private static final int COL_TITLE = 0xFFFFFFFF;
    private static final int COL_TEXT = 0xFFE0E0E0;
    private static final int COL_MUTED = 0xFF9A9A9A;
    private static final int COL_SELECTED = 0x40FFFFFF;
    private static final int ROW_H = 28;

    private static final Identifier EDIT_ICON = de.ottoextra.OttoExtra.id("textures/gui/edit.png");
    private static final Identifier RESET_ICON = de.ottoextra.OttoExtra.id("textures/gui/reset.png");
    private static final int RESET_SIZE = 13;

    private final Screen parent;
    private final OttoExtraConfig config;

    /**
     * Listenzeile. {@code nameKey} = umzubenennender Name (Fraktion bzw. Gefolge),
     * {@code colorKey} = Gefolge-Farb-Schlüssel (Wurzel-Gefolge), {@code regionId}
     * = Lehen-Key (Banner/Lehen-Farbe), {@code faction} = Wappen-Quelle (oder null).
     */
    private record Row(int depth, String nameKey, String factionName, String rootName,
                       String regionId, FactionRecord faction, String gefolge, String rank,
                       String lehen) {
    }

    private final List<Row> rows = new ArrayList<>();
    private TextFieldWidget searchField;
    private ButtonWidget hierarchyButton;
    private boolean hierarchy = true;
    private int scroll = 0;

    private Row editing;
    private TextFieldWidget nameField;
    private TextFieldWidget colorField;
    private TextFieldWidget lehenColorField;
    private ButtonWidget verbandButton;
    private ButtonWidget saveButton;
    private ButtonWidget discardButton;
    private boolean suppress;

    public FollowingScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.following.title"));
        this.parent = parent;
        this.config = config;
    }

    private int panelW() {
        return Math.min(width - 16, 460);
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    private int listTop() {
        return 56;
    }

    private int editorTop() {
        return height - 86;
    }

    private int listBottom() {
        return editorTop() - 4;
    }

    @Override
    protected void init() {
        int px = panelX();
        int pw = panelW();
        searchField = new TextFieldWidget(textRenderer, px + 6, 30, pw - 12 - 84, 16,
                Text.translatable("ottoextra.rpbook.search"));
        searchField.setSuggestion(Text.translatable("ottoextra.rpbook.search").getString());
        searchField.setChangedListener(s -> {
            searchField.setSuggestion(s.isEmpty()
                    ? Text.translatable("ottoextra.rpbook.search").getString() : "");
            scroll = 0;
            rebuildRows();
        });
        addDrawableChild(searchField);

        hierarchyButton = ButtonWidget.builder(hierarchyLabel(), b -> {
            hierarchy = !hierarchy;
            b.setMessage(hierarchyLabel());
            scroll = 0;
            rebuildRows();
        }).dimensions(px + pw - 80, 30, 74, 16).build();
        addDrawableChild(hierarchyButton);

        int fx = px + 78;
        nameField = new TextFieldWidget(textRenderer, fx, editorTop() + 8, pw - 78 - 28, 16,
                Text.empty());
        nameField.setMaxLength(48);
        // Kein Live-Apply mehr: Änderungen werden erst mit „Speichern" übernommen.
        addDrawableChild(nameField);
        colorField = new TextFieldWidget(textRenderer, fx, editorTop() + 28, 70, 16, Text.empty());
        colorField.setMaxLength(7);
        addDrawableChild(colorField);
        lehenColorField = new TextFieldWidget(textRenderer, fx, editorTop() + 48, 70, 16, Text.empty());
        lehenColorField.setMaxLength(7);
        addDrawableChild(lehenColorField);

        verbandButton = ButtonWidget.builder(
                        Text.translatable("ottoextra.following.applyVerband"), b -> applyToVerband())
                .dimensions(px + pw - 160, editorTop() + 47, 154, 16).build();
        addDrawableChild(verbandButton);

        // Editor-Aktionen: Speichern übernimmt Name + Farben, Verwerfen lädt den
        // gespeicherten Stand neu. Nur bei ausgewähltem Gefolge sichtbar.
        saveButton = ButtonWidget.builder(Text.translatable("ottoextra.following.save"),
                        b -> saveEdits())
                .dimensions(width / 2 - 154, height - 22, 100, 18).build();
        addDrawableChild(saveButton);
        discardButton = ButtonWidget.builder(Text.translatable("ottoextra.following.discard"),
                        b -> discardEdits())
                .dimensions(width / 2 - 50, height - 22, 100, 18).build();
        addDrawableChild(discardButton);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 + 54, height - 22, 100, 18).build());

        selectRow(null);
        rebuildRows();
    }

    private Text hierarchyLabel() {
        return Text.translatable(hierarchy
                ? "ottoextra.following.hierarchy.on" : "ottoextra.following.hierarchy.off");
    }

    // ---- Daten ---------------------------------------------------------------

    private void rebuildRows() {
        rows.clear();
        PoliticalOverlay.groupTintOverview();
        String q = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        if (hierarchy) {
            buildFactionTree(q);
        } else {
            buildLehenList(q);
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    /** Hierarchie: Fraktions-Baum über lord_name; alle Vasallen eingerückt. */
    private void buildFactionTree(String q) {
        var data = RegionsServices.data();
        if (data == null) {
            return;
        }
        List<FactionRecord> all = data.allFactions();
        Map<String, FactionRecord> byName = new HashMap<>();
        for (FactionRecord f : all) {
            if (f.name() != null && !f.name().isBlank()) {
                byName.putIfAbsent(normalize(f.name()), f);
            }
        }
        Map<String, List<FactionRecord>> kids = new HashMap<>();
        List<FactionRecord> roots = new ArrayList<>();
        for (FactionRecord f : all) {
            if (f.name() == null || f.name().isBlank()) {
                continue;
            }
            String lord = f.lord_name();
            FactionRecord parent = (lord != null && !lord.isBlank())
                    ? byName.get(normalize(lord)) : null;
            if (parent != null && !normalize(parent.name()).equals(normalize(f.name()))) {
                kids.computeIfAbsent(normalize(lord), k -> new ArrayList<>()).add(f);
            } else {
                roots.add(f);
            }
        }
        roots.sort((a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));
        kids.values().forEach(l -> l.sort((a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b))));
        Set<String> visited = new HashSet<>();
        for (FactionRecord root : roots) {
            addFactionRow(root, 0, root.name(), kids, visited, q);
        }
    }

    private void addFactionRow(FactionRecord f, int depth, String rootName,
                               Map<String, List<FactionRecord>> kids,
                               Set<String> visited, String q) {
        String norm = normalize(f.name());
        if (!visited.add(norm)) {
            return; // Zyklusschutz
        }
        String gefolge = PoliticalOverlay.displayNameFor(f.name());
        String rank = f.rank_name() == null ? "" : f.rank_name();
        String regionId = f.region_id();
        String lehen = f.region_name() != null && !f.region_name().isBlank()
                ? f.region_name() : (regionId != null ? lehenName(regionId) : "");
        if (matches(q, gefolge, lehen, rank)) {
            rows.add(new Row(depth, f.name(), f.name(), rootName, regionId, f, gefolge, rank, lehen));
        }
        for (FactionRecord child : kids.getOrDefault(norm, List.of())) {
            addFactionRow(child, depth + 1, rootName, kids, visited, q);
        }
    }

    /** Flach: alle Lehen einzeln. */
    private void buildLehenList(String q) {
        Map<String, List<String>> byGroup = lehenByGroup();
        var data = RegionsServices.data();
        for (Map.Entry<String, List<String>> e : byGroup.entrySet()) {
            String group = e.getKey();
            String gefolge = PoliticalOverlay.displayNameFor(group);
            for (String k : e.getValue()) {
                String ln = lehenName(k);
                String rank = rankFor(k);
                FactionRecord fac = data != null ? data.factionForRegion(k).orElse(null) : null;
                String facName = fac != null && fac.name() != null ? fac.name() : group;
                if (matches(q, gefolge, ln, rank)) {
                    rows.add(new Row(0, group, facName, group, k, fac, gefolge, rank, ln));
                }
            }
        }
    }

    private Map<String, List<String>> lehenByGroup() {
        Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> seen = new HashSet<>();
        for (LehenPolygon p : LehenPolygonStore.polygons()) {
            if (!seen.add(p.key())) {
                continue;
            }
            String grp = PoliticalOverlay.groupDisplayName(p.key());
            if (grp == null || grp.isBlank()) {
                continue;
            }
            map.computeIfAbsent(grp, k -> new ArrayList<>()).add(p.key());
        }
        map.values().forEach(l -> l.sort((a, b) ->
                lehenName(a).compareToIgnoreCase(lehenName(b))));
        return map;
    }

    private static String nameOf(FactionRecord f) {
        return f.name() == null ? "" : f.name();
    }

    private String lehenName(String key) {
        var d = RegionsServices.data();
        if (d != null && key != null) {
            var r = d.regionByName(key).orElse(null);
            if (r != null && r.name() != null && !r.name().isBlank()) {
                return r.name();
            }
        }
        return key == null ? "" : key;
    }

    private String rankFor(String key) {
        var d = RegionsServices.data();
        if (d != null) {
            var f = d.factionForRegion(key).orElse(null);
            if (f != null && f.rank_name() != null) {
                return f.rank_name();
            }
        }
        return "";
    }

    private boolean matches(String q, String... fields) {
        if (q.isEmpty()) {
            return true;
        }
        for (String f : fields) {
            if (f != null && f.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    // ---- Auswahl / Editor ----------------------------------------------------

    private void selectRow(Row row) {
        editing = row;
        suppress = true;
        boolean any = row != null;
        boolean hasLehen = row != null && row.regionId() != null && !row.regionId().isBlank();
        if (nameField != null) {
            nameField.visible = any;
            nameField.setEditable(any);
            nameField.setText(any ? config.map.groupNameOverrides
                    .getOrDefault(normalize(row.nameKey()), "") : "");
        }
        if (colorField != null) {
            colorField.visible = any;
            colorField.setEditable(any);
            colorField.setText(any ? config.map.factionColors.getOrDefault(row.factionName(), "") : "");
        }
        if (verbandButton != null) {
            verbandButton.visible = any;
            verbandButton.active = any;
        }
        if (saveButton != null) {
            saveButton.visible = any;
            saveButton.active = any;
        }
        if (discardButton != null) {
            discardButton.visible = any;
            discardButton.active = any;
        }
        if (lehenColorField != null) {
            lehenColorField.visible = hasLehen;
            lehenColorField.setEditable(hasLehen);
            lehenColorField.setText(hasLehen
                    ? config.map.lehenColors.getOrDefault(row.regionId(), "") : "");
        }
        suppress = false;
    }

    /** Speichern: Name + Gefolge-Farbe + Lehen-Farbe aus den Feldern übernehmen. */
    private void saveEdits() {
        if (editing == null) {
            return;
        }
        applyName(nameField.getText());
        applyColor(colorField.getText(), false);
        if (lehenColorField.visible) {
            applyColor(lehenColorField.getText(), true);
        }
    }

    /** Verwerfen: Felder aus dem gespeicherten Stand neu laden. */
    private void discardEdits() {
        selectRow(editing);
    }

    private void applyName(String raw) {
        if (editing == null) {
            return;
        }
        String key = normalize(editing.nameKey());
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            config.map.groupNameOverrides.remove(key);
        } else {
            config.map.groupNameOverrides.put(key, v);
        }
        config.save();
        PoliticalOverlay.setGroupNameOverrides(config.map.groupNameOverrides);
    }

    private void applyColor(String raw, boolean lehenColor) {
        if (editing == null) {
            return;
        }
        String s = raw == null ? "" : raw.trim().replace("#", "");
        boolean valid = s.length() == 6 && s.matches("[0-9a-fA-F]{6}");
        String hex = "#" + s.toUpperCase(Locale.ROOT);
        if (lehenColor) {
            String id = editing.regionId();
            if (id == null || id.isBlank()) {
                return;
            }
            if (s.isEmpty()) {
                config.map.lehenColors.remove(id);
            } else if (valid) {
                config.map.lehenColors.put(id, hex);
            } else {
                return;
            }
            PoliticalOverlay.setUserLehenColors(config.map.lehenColors);
        } else {
            if (s.isEmpty()) {
                config.map.factionColors.remove(editing.factionName());
            } else if (valid) {
                config.map.factionColors.put(editing.factionName(), hex);
            } else {
                return;
            }
            PoliticalOverlay.setUserFactionColors(config.map.factionColors);
        }
        config.save();
    }

    /** Aktuelle Gefolge-Farbe auf das ganze Verband (Lehnsherr + alle Vasallen) anwenden. */
    private void applyToVerband() {
        if (editing == null) {
            return;
        }
        String color = config.map.factionColors.get(editing.factionName());
        if (color == null || color.isBlank()) {
            color = PoliticalOverlay.jsonDefaultColor(editing.rootName());
        }
        if (color == null) {
            return;
        }
        for (String fac : factionsInVerband(editing.rootName())) {
            config.map.factionColors.put(fac, color);
        }
        config.save();
        PoliticalOverlay.setUserFactionColors(config.map.factionColors);
    }

    /** Alle Fraktionsnamen eines Verbands (Wurzel == rootName). */
    private List<String> factionsInVerband(String rootName) {
        List<String> out = new ArrayList<>();
        var data = RegionsServices.data();
        if (data == null || rootName == null) {
            return out;
        }
        Map<String, FactionRecord> byName = new HashMap<>();
        for (FactionRecord f : data.allFactions()) {
            if (f.name() != null && !f.name().isBlank()) {
                byName.putIfAbsent(normalize(f.name()), f);
            }
        }
        String rootNorm = normalize(rootName);
        for (FactionRecord f : data.allFactions()) {
            if (f.name() == null || f.name().isBlank()) {
                continue;
            }
            FactionRecord cur = f;
            String root = normalize(f.name());
            for (int d = 0; d < 8; d++) {
                String lord = cur.lord_name();
                if (lord == null || lord.isBlank()) {
                    break;
                }
                String lk = normalize(lord);
                if (lk.equals(root)) {
                    break;
                }
                root = lk;
                FactionRecord lf = byName.get(lk);
                if (lf == null) {
                    break;
                }
                cur = lf;
            }
            if (root.equals(rootNorm)) {
                out.add(f.name());
            }
        }
        return out;
    }

    private static String normalize(String name) {
        return de.ottoextra.regions.RegionNameKeys.normalize(name);
    }

    // ---- Maus ----------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseY >= listTop() && mouseY <= listBottom()) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(vertical), maxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        // Reset-Icons neben den Editor-Feldern: Override löschen (= Default).
        if (editing != null && click.button() == 0) {
            if (nameField.visible && resetHit(nameField, mx, my)) {
                nameField.setText("");
                return true;
            }
            if (colorField.visible && resetHit(colorField, mx, my)) {
                // Reset = ausgelieferte JSON-Standardfarbe (persistiert via Listener).
                String def = PoliticalOverlay.jsonDefaultColor(editing.rootName());
                colorField.setText(def != null ? def : "");
                return true;
            }
            if (lehenColorField.visible && resetHit(lehenColorField, mx, my)) {
                lehenColorField.setText("");
                return true;
            }
        }
        if (click.button() == 0 && mx >= panelX() && mx <= panelX() + panelW()
                && my >= listTop() && my < listBottom()) {
            int idx = scroll + (int) ((my - listTop()) / ROW_H);
            if (idx >= 0 && idx < rows.size()) {
                selectRow(rows.get(idx));
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    // ---- Render --------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int px = panelX();
        int pw = panelW();
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 12, COL_TITLE);
        ctx.fill(px - 2, listTop() - 2, px + pw + 2, listBottom() + 2, COL_PANEL);

        int rowsVisible = visibleRows();
        for (int r = 0; r < rowsVisible; r++) {
            int idx = scroll + r;
            if (idx >= rows.size()) {
                break;
            }
            Row row = rows.get(idx);
            int y = listTop() + r * ROW_H;
            if (row.equals(editing)) {
                ctx.fill(px, y, px + pw, y + ROW_H - 1, COL_SELECTED);
            }
            int indent = row.depth() * 12;
            ctx.fill(px + 2 + indent, y + 2, px + 5 + indent, y + ROW_H - 3,
                    0xFF000000 | (swatchFor(row) & 0xFFFFFF));
            int bx = px + 8 + indent;
            Identifier banner = bannerFor(row);
            if (banner != null) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, banner, bx, y + 2, 0f, 0f,
                        24, 24, 24, 24);
            }
            int textX = bx + 28;
            int maxTextW = pw - (textX - px) - 22;
            ctx.drawText(textRenderer, textRenderer.trimToWidth(row.gefolge(), maxTextW),
                    textX, y + 2, COL_TITLE, false);
            if (!row.rank().isBlank()) {
                drawSmall(ctx, row.rank(), textX, y + 12, COL_MUTED, maxTextW);
            }
            if (!row.lehen().isBlank()) {
                drawSmall(ctx, row.lehen(), textX, y + 20, COL_TEXT, maxTextW);
            }
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, EDIT_ICON, px + pw - 20, y + 7, 0f, 0f,
                    14, 14, 16, 16, 16, 16);
        }

        if (maxScroll() > 0) {
            int trackH = listBottom() - listTop();
            int barH = Math.max(12, trackH * rowsVisible / Math.max(rowsVisible, rows.size()));
            int barY = listTop() + (trackH - barH) * scroll / Math.max(1, maxScroll());
            ctx.fill(px + pw - 3, barY, px + pw - 1, barY + barH, 0xCC808080);
        }

        ctx.fill(px - 2, editorTop() - 3, px + pw + 2, editorTop() - 2, COL_BORDER);
        if (editing == null) {
            ctx.drawText(textRenderer, Text.translatable("ottoextra.following.editHint"),
                    px + 6, editorTop() + 12, COL_MUTED, false);
        } else {
            boolean hasLehen = editing.regionId() != null && !editing.regionId().isBlank();
            ctx.drawText(textRenderer,
                    Text.translatable("ottoextra.following.editGroup", editing.gefolge()),
                    px + 6, editorTop() - 2, COL_TITLE, false);
            ctx.drawText(textRenderer, Text.translatable("ottoextra.following.name"),
                    px + 6, editorTop() + 12, COL_TEXT, false);
            ctx.drawText(textRenderer, Text.translatable("ottoextra.following.color"),
                    px + 6, editorTop() + 32, COL_TEXT, false);
            if (hasLehen) {
                ctx.drawText(textRenderer, Text.translatable("ottoextra.following.lehenColor"),
                        px + 6, editorTop() + 52, COL_TEXT, false);
            }
            // Reset-Icons + Live-Farbvorschau neben den Feldern
            if (nameField.visible) {
                drawReset(ctx, nameField);
            }
            if (colorField.visible) {
                drawReset(ctx, colorField);
                drawColorPreview(ctx, colorField);
            }
            if (lehenColorField.visible) {
                drawReset(ctx, lehenColorField);
                drawColorPreview(ctx, lehenColorField);
            }
        }
    }

    private int resetX(TextFieldWidget f) {
        return f.getX() + f.getWidth() + 3;
    }

    private int resetY(TextFieldWidget f) {
        return f.getY() + (f.getHeight() - RESET_SIZE) / 2;
    }

    private boolean resetHit(TextFieldWidget f, double mx, double my) {
        int ix = resetX(f);
        int iy = resetY(f);
        return mx >= ix && mx <= ix + RESET_SIZE && my >= iy && my <= iy + RESET_SIZE;
    }

    private void drawReset(DrawContext ctx, TextFieldWidget f) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, RESET_ICON, resetX(f), resetY(f), 0f, 0f,
                RESET_SIZE, RESET_SIZE, 16, 16, 16, 16);
    }

    /** Live-Vorschau der aktuell getippten Farbe rechts neben dem Reset-Icon. */
    private void drawColorPreview(DrawContext ctx, TextFieldWidget f) {
        int x = resetX(f) + RESET_SIZE + 4;
        int y = f.getY() + (f.getHeight() - 14) / 2;
        Integer rgb = parseLive(f.getText());
        ctx.fill(x - 1, y - 1, x + 15, y + 15, COL_BORDER);
        ctx.fill(x, y, x + 14, y + 14, rgb != null ? 0xFF000000 | rgb : 0xFF555555);
    }

    private static Integer parseLive(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replace("#", "");
        if (s.length() == 6 && s.matches("[0-9a-fA-F]{6}")) {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        }
        return null;
    }

    private void drawSmall(DrawContext ctx, String text, int x, int y, int color, int maxW) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(0.8f, 0.8f);
        ctx.drawText(textRenderer, textRenderer.trimToWidth(text, (int) (maxW / 0.8f)),
                0, 0, color, false);
        m.popMatrix();
    }

    private Identifier bannerFor(Row row) {
        try {
            if (row.faction() != null) {
                Identifier id = RegionsServices.banners().bannerFor(row.faction()).orElse(null);
                if (id != null) {
                    return id;
                }
            }
            return row.regionId() != null ? MapOverlayRenderer.bannerForKey(row.regionId()) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private int swatchFor(Row row) {
        if (row.regionId() != null && !row.regionId().isBlank()) {
            return PoliticalOverlay.fillTintFor(row.regionId());
        }
        Integer t = PoliticalOverlay.groupTintOverview().get(row.rootName());
        return t == null ? 0xFF888888 : t;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
