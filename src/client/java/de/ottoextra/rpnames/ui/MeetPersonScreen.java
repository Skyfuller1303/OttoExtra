package de.ottoextra.rpnames.ui;

import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class MeetPersonScreen extends Screen {

    private static final net.minecraft.util.Identifier EDIT_ICON =
            de.ottoextra.OttoExtra.id("textures/gui/edit.png");

    private final Screen parent;
    private final String account;
    private final String uuid;
    private final boolean hasApiPrefill;
    private final String apiPrefillName;
    private final String apiPrefillTitle;

    private boolean editing;
    private String prefillName = "";
    private String prefillTitle = "";

    private TextFieldWidget nameField;
    private TextFieldWidget titleField;
    private ButtonWidget editButton;
    private net.minecraft.client.network.AbstractClientPlayerEntity entity;

    public MeetPersonScreen(Screen parent, String account, String uuid) {
        this(parent, account, uuid, false, null, null);
    }

    /**
     * Öffnet das Kennenlernfenster mit den gerade gezielt von der API
     * abgefragten Daten. Auch leere Werte sind dabei absichtlich verbindlich,
     * damit kein älterer lokaler Vorschlag eingeblendet wird.
     */
    public MeetPersonScreen(Screen parent, String account, String uuid,
                            String apiRpName, String apiTitle) {
        this(parent, account, uuid, true, apiRpName, apiTitle);
    }

    private MeetPersonScreen(Screen parent, String account, String uuid,
                             boolean hasApiPrefill, String apiRpName, String apiTitle) {
        super(Text.translatable("ottoextra.meet.title"));
        this.parent = parent;
        this.account = account;
        this.uuid = uuid;
        this.hasApiPrefill = hasApiPrefill;
        this.apiPrefillName = apiRpName;
        this.apiPrefillTitle = apiTitle;
    }

    private int boxH() {
        return Math.min(150, height / 3);
    }

    private int contentHeight() {
        int header = 22;
        int below = editing ? (8 + 18 + 24 + 18 + 26 + 20) : (16 + 22);
        return header + boxH() + below;
    }

    private int top() {
        return Math.max(16, (height - contentHeight()) / 2);
    }

    private int boxTop() {
        return top() + 22;
    }

    private int boxBottom() {
        return boxTop() + boxH();
    }

    @Override
    protected void init() {
        loadPrefill();
        int cx = width / 2;
        int y = boxBottom() + 16;
        if (editing) {
            y += 8;
            titleField = new TextFieldWidget(textRenderer, cx - 90, y, 180, 18,
                    Text.translatable("ottoextra.rpbook.titleField"));
            titleField.setMaxLength(48);
            titleField.setText(prefillTitle);
            addDrawableChild(titleField);
            y += 24;
            nameField = new TextFieldWidget(textRenderer, cx - 90, y, 180, 18,
                    Text.translatable("ottoextra.rpbook.rpname"));
            nameField.setMaxLength(48);
            nameField.setText(prefillName);
            addDrawableChild(nameField);
            y += 26;
            addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.rpbook.save"),
                    b -> save(nameField.getText().trim(), titleField.getText().trim()))
                    .dimensions(cx - 90, y, 88, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"),
                    b -> toggleEditing()).dimensions(cx + 2, y, 88, 20).build());
        } else {

            addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.meet.yes"),
                    b -> save(prefillName, prefillTitle)).dimensions(cx - 110, y, 130, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("ottoextra.meet.no"),
                    b -> close()).dimensions(cx + 24, y, 60, 22).build());
            editButton = ButtonWidget.builder(Text.empty(), b -> toggleEditing())
                    .dimensions(cx + 88, y, 22, 22)
                    .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                            Text.translatable("ottoextra.meet.edit")))
                    .build();
            addDrawableChild(editButton);
        }
        ensureEntity();
    }

    private void toggleEditing() {
        editing = !editing;
        clearAndInit();
    }

    private void loadPrefill() {
        if (hasApiPrefill) {
            String name = apiPrefillName == null ? "" : apiPrefillName.trim();
            String title = apiPrefillTitle == null ? "" : apiPrefillTitle.trim();
            if (name.isBlank()) {
                title = "";
            }
            prefillName = name;
            prefillTitle = RpNamesServices.canonicalTitle(title);
            return;
        }

        RpNamesServices.MeetSuggestion s = RpNamesServices.meetSuggestion(account);
        var p = RpNamesServices.store() != null
                ? RpNamesServices.store().findByName(account).orElse(null) : null;
        String name = s != null && s.rpName() != null && !s.rpName().isBlank() ? s.rpName()
                : (p != null && p.hasRpName() ? p.rpName : "");

        String title = s != null && s.title() != null && !s.title().isBlank() ? s.title()
                : (p != null && p.title != null ? p.title : "");

        if (name.isBlank()) {
            title = "";
        }
        prefillName = name;

        prefillTitle = RpNamesServices.canonicalTitle(title);
    }

    private String liveName() {
        if (editing && nameField != null) {
            return nameField.getText();
        }
        return prefillName;
    }

    private String liveTitle() {
        if (editing && titleField != null) {
            return titleField.getText();
        }
        return prefillTitle;
    }

    private String headerName() {
        String n = liveName();
        return n != null && !n.isBlank() ? n : account;
    }

    private void save(String rp, String title) {
        LocalRpIdentityStore store = RpNamesServices.store();
        if (store == null || account == null || account.isBlank()) {
            close();
            return;
        }
        final String rpName = rp == null ? "" : rp.trim();
        final String t = title == null ? "" : title.trim();
        store.ensureSeen(account, uuid, de.ottoextra.rpnames.model.RpNameSource.MANUAL_EDIT);

        store.updateManual(account, p -> {
            if (!rpName.isEmpty()) {
                p.rpName = rpName;
            }
            p.title = t;
        }, true);

        close();
    }

    private void ensureEntity() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return;
            }
            com.mojang.authlib.GameProfile profile = null;
            var nh = client.getNetworkHandler();
            if (nh != null) {
                net.minecraft.client.network.PlayerListEntry e = uuid != null
                        ? nh.getPlayerListEntry(java.util.UUID.fromString(uuid)) : null;
                if (e == null) {
                    e = nh.getPlayerListEntry(account);
                }
                if (e != null) {
                    profile = e.getProfile();
                }
            }
            if (profile == null) {
                java.util.UUID id = uuid != null ? java.util.UUID.fromString(uuid)
                        : java.util.UUID.nameUUIDFromBytes(
                                ("OfflinePlayer:" + account).getBytes(
                                        java.nio.charset.StandardCharsets.UTF_8));
                profile = new com.mojang.authlib.GameProfile(id, account);
            }
            net.minecraft.client.network.OtherClientPlayerEntity dummy =
                    new net.minecraft.client.network.OtherClientPlayerEntity(client.world, profile);
            dummy.getDataTracker().set(
                    de.ottoextra.mixin.PlayerCustomizationAccessor.ottoextra$customization(),
                    (byte) 0x7F);
            entity = dummy;
        } catch (Throwable t) {
            entity = null;
        }
    }

    private void drawScaledCentered(DrawContext ctx, String s, int cx, int y, int color, int maxW) {
        if (s == null || s.isEmpty()) {
            return;
        }
        int w = textRenderer.getWidth(s);
        float scale = w > maxW ? (float) maxW / w : 1.0f;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, y);
        m.scale(scale, scale);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(s), 0, 0, color);
        m.popMatrix();
    }

    private static int argb(String hex, int def) {
        net.minecraft.text.TextColor c =
                de.ottoextra.rpnames.chat.ChatNameRewriter.parseColor(hex);
        return c != null ? (0xFF000000 | c.getRgb()) : def;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private int titleColorArgb(String title) {
        var catalog = RpNamesServices.catalog();
        var p = RpNamesServices.store() != null
                ? RpNamesServices.store().findByName(account).orElse(null) : null;
        String catalogColor = catalog != null
                ? catalog.titleColor(title).orElse(null) : null;
        String groupColor = RpNamesServices.titles() != null
                ? RpNamesServices.titles().find(title).map(r -> r.group().titleColor).orElse(null)
                : null;
        String fallback = catalog != null ? catalog.fallbackTitleColor() : "#a17f5f";

        String pers = p != null ? p.colors.nametagTitleColor : null;
        String hex = RpNamesServices.titleOverridesColor(title)
                ? firstNonBlank(catalogColor, firstNonBlank(pers, firstNonBlank(groupColor, fallback)))
                : firstNonBlank(pers, firstNonBlank(catalogColor, firstNonBlank(groupColor, fallback)));
        return argb(hex, 0xFFD2BF6A);
    }

    private int nameColorArgb() {
        var p = RpNamesServices.store() != null
                ? RpNamesServices.store().findByName(account).orElse(null) : null;
        String hex = RpNamesServices.rpNameColor(
                p != null ? p.colors.nametagNameColor : null, liveTitle());
        return argb(hex, 0xFFC7A87F);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("ottoextra.meet.know", headerName()), width / 2, top() + 5,
                0xFFFFFFFF);

        int cx = width / 2;
        int top = boxTop();
        int bottom = boxBottom();
        ctx.fill(cx - 50, top, cx + 50, bottom, 0x50000000);

        int maxW = 96;
        int labelY = top + 3;
        String exTitle = liveTitle();
        String exName = liveName();
        boolean nameKnown = exName != null && !exName.isBlank();

        if (nameKnown && exTitle != null && !exTitle.isBlank()) {
            drawScaledCentered(ctx, exTitle, cx, labelY, titleColorArgb(exTitle), maxW);
            labelY += 10;
        }
        drawScaledCentered(ctx, headerName(), cx, labelY, nameColorArgb(), maxW);
        if (entity != null) {
            int size = (int) ((bottom - top) * 0.34f);
            float sway = (float) Math.sin(System.currentTimeMillis() / 1400.0) * 25f;
            try {
                net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                        ctx, cx - 50, top + 22, cx + 50, bottom - 4, size, 0.0f,
                        mouseX - sway, mouseY, entity);
            } catch (Throwable ignored) {

            }
        }
        if (editing) {
            ctx.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.rpbook.titleField"),
                    cx - 90, boxBottom() + 14, 0xFFB0B0B0);
            ctx.drawTextWithShadow(textRenderer, Text.translatable("ottoextra.rpbook.rpname"),
                    cx - 90, boxBottom() + 38, 0xFFB0B0B0);
        } else if (editButton != null) {

            int ix = editButton.getX() + (editButton.getWidth() - 16) / 2;
            int iy = editButton.getY() + (editButton.getHeight() - 16) / 2;
            ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, EDIT_ICON,
                    ix, iy, 0f, 0f, 16, 16, 16, 16);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
