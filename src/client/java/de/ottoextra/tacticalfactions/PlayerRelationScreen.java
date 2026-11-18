package de.ottoextra.tacticalfactions;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.UUID;

final class PlayerRelationScreen extends Screen {
    private final Screen parent;
    private final UUID uuid;
    private final String account;

    PlayerRelationScreen(Screen parent, UUID uuid, String account) {
        super(Text.translatable("ottoextra_tacticalfactions.player.choose", account));
        this.parent = parent;
        this.uuid = uuid;
        this.account = account;
    }

    @Override
    protected void init() {
        int y = height / 2 - 48;
        for (Relation relation : Relation.values()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable(relation.translationKey()), button -> {
                TacticalFactions.politics().setPlayer(uuid, account, relation);
                close();
            }).dimensions(width / 2 - 90, y, 180, 20).build());
            y += 24;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 70, 0xFFFFFFFF);
    }

    @Override public void close() { if (client != null) client.setScreen(parent); }
}
