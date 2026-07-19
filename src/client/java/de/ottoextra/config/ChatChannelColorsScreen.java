package de.ottoextra.config;

import de.ottoextra.chat.ChatChannelButton;
import de.ottoextra.chat.ChatChannelState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kompakte Farbverwaltung im dunklen Stil des RP-Personenbuchs. */
public final class ChatChannelColorsScreen extends Screen {
    private static final int COL_PANEL = 0xE0141418;
    private static final int COL_BORDER = 0xFF000000;
    private static final int COL_TITLE = 0xFFFFFFFF;
    private static final int COL_TEXT = 0xFFE0E0E0;
    private static final int COL_MUTED = 0xFF9A9A9A;
    private static final int ROW_H = 19;
    private static final int FIELD_W = 86;
    private static final ChatChannelState.ChatChannel[] CHANNELS = {
            ChatChannelState.ChatChannel.SPRECHEN,
            ChatChannelState.ChatChannel.FLUESTERN,
            ChatChannelState.ChatChannel.MURMELN,
            ChatChannelState.ChatChannel.RUFEN,
            ChatChannelState.ChatChannel.BRUELLEN,
            ChatChannelState.ChatChannel.OFFTOPIC,
            ChatChannelState.ChatChannel.HILFE
    };

    private final Screen parent;
    private final OttoExtraConfig config;
    private final List<TextFieldWidget> labelFields = new ArrayList<>();
    private final List<TextFieldWidget> messageFields = new ArrayList<>();
    private TextFieldWidget emoteColorField;
    private TextFieldWidget oocColorField;
    private ButtonWidget emoteItalicButton;
    private ButtonWidget oocItalicButton;

    public ChatChannelColorsScreen(Screen parent, OttoExtraConfig config) {
        super(Text.translatable("ottoextra.chatColors.title"));
        this.parent = parent;
        this.config = config;
    }

    private int panelW() { return Math.min(width - 20, 590); }
    private int panelH() { return Math.min(height - 12, 286); }
    private int panelX() { return (width - panelW()) / 2; }
    private int panelY() { return (height - panelH()) / 2; }
    private int rowTop() { return panelY() + 47; }
    private int labelFieldX() { return panelX() + panelW() - 2 * FIELD_W - 82; }
    private int messageFieldX() { return labelFieldX() + FIELD_W + 18; }
    private int resetX() { return messageFieldX() + FIELD_W + 18; }
    private int styleHeaderY() { return rowTop() + CHANNELS.length * ROW_H + 5; }
    private int styleRowTop() { return styleHeaderY() + 17; }

    @Override
    protected void init() {
        labelFields.clear();
        messageFields.clear();

        for (int index = 0; index < CHANNELS.length; index++) {
            ChatChannelState.ChatChannel channel = CHANNELS[index];
            OttoExtraConfig.ChannelColors colors = config.chat.channelColors(key(channel));
            int y = rowTop() + index * ROW_H;

            TextFieldWidget label = colorField(labelFieldX(), y, colors.labelColor,
                    originalHex(channel));
            label.setChangedListener(value -> colors.labelColor = normalized(value));
            labelFields.add(label);
            addDrawableChild(label);

            TextFieldWidget message = colorField(messageFieldX(), y, colors.messageColor,
                    Text.translatable("ottoextra.chatColors.server").getString());
            message.setChangedListener(value -> colors.messageColor = normalized(value));
            messageFields.add(message);
            addDrawableChild(message);

            final int row = index;
            addDrawableChild(ButtonWidget.builder(Text.literal("↺"), button -> resetChannel(row))
                    .dimensions(resetX(), y, 24, 14).build());
        }

        emoteColorField = colorField(labelFieldX(), styleRowTop(), config.chat.rpEmoteColor,
                "#C6C6C6");
        emoteColorField.setChangedListener(value -> {
            String color = normalized(value);
            config.chat.rpEmoteColor = color.isEmpty() ? "#C6C6C6" : color;
        });
        addDrawableChild(emoteColorField);
        emoteItalicButton = italicButton(messageFieldX(), styleRowTop(), true);
        addDrawableChild(emoteItalicButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("↺"), button -> resetStyle(true))
                .dimensions(resetX(), styleRowTop(), 24, 14).build());

        oocColorField = colorField(labelFieldX(), styleRowTop() + ROW_H,
                config.chat.rpOocColor, "#B4BEC6");
        oocColorField.setChangedListener(value -> {
            String color = normalized(value);
            config.chat.rpOocColor = color.isEmpty() ? "#B4BEC6" : color;
        });
        addDrawableChild(oocColorField);
        oocItalicButton = italicButton(messageFieldX(), styleRowTop() + ROW_H, false);
        addDrawableChild(oocItalicButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("↺"), button -> resetStyle(false))
                .dimensions(resetX(), styleRowTop() + ROW_H, 24, 14).build());

        int footerY = panelY() + panelH() - 24;
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ottoextra.chatColors.resetAll"), button -> resetChannels())
                .dimensions(width / 2 - 154, footerY, 150, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 + 4, footerY, 150, 18).build());
    }

    private TextFieldWidget colorField(int x, int y, String value, String suggestion) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, FIELD_W, 14, Text.empty());
        field.setMaxLength(7);
        field.setText(value == null ? "" : value);
        field.setSuggestion(field.getText().isEmpty() ? suggestion : "");
        return field;
    }

    private ButtonWidget italicButton(int x, int y, boolean emote) {
        return ButtonWidget.builder(italicLabel(emote), button -> {
            if (emote) config.chat.rpEmoteItalic = !config.chat.rpEmoteItalic;
            else config.chat.rpOocItalic = !config.chat.rpOocItalic;
            button.setMessage(italicLabel(emote));
        }).dimensions(x, y, FIELD_W, 14).build();
    }

    private Text italicLabel(boolean emote) {
        boolean enabled = emote ? config.chat.rpEmoteItalic : config.chat.rpOocItalic;
        return Text.translatable("ottoextra.chatColors.italic",
                Text.translatable(enabled ? "ottoextra.settings.on" : "ottoextra.settings.off"));
    }

    private void resetChannel(int index) {
        ChatChannelState.ChatChannel channel = CHANNELS[index];
        OttoExtraConfig.ChannelColors colors = config.chat.channelColors(key(channel));
        colors.labelColor = "";
        colors.messageColor = "";
        setFieldDefault(labelFields.get(index), originalHex(channel));
        setFieldDefault(messageFields.get(index),
                Text.translatable("ottoextra.chatColors.server").getString());
    }

    private void resetChannels() {
        for (int index = 0; index < CHANNELS.length; index++) resetChannel(index);
    }

    private void resetStyle(boolean emote) {
        if (emote) {
            config.chat.rpEmoteColor = "#C6C6C6";
            config.chat.rpEmoteItalic = true;
            emoteColorField.setText(config.chat.rpEmoteColor);
            emoteItalicButton.setMessage(italicLabel(true));
        } else {
            config.chat.rpOocColor = "#B4BEC6";
            config.chat.rpOocItalic = false;
            oocColorField.setText(config.chat.rpOocColor);
            oocItalicButton.setMessage(italicLabel(false));
        }
    }

    private static void setFieldDefault(TextFieldWidget field, String suggestion) {
        field.setText("");
        field.setSuggestion(suggestion);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Bewusst ohne renderBackground(): ein zweiter Blur im selben Frame
        // crasht Minecraft 1.21.11 in mehreren Fabric-Modpacks.
        context.fill(0, 0, width, height, 0xD0101010);
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();
        context.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, COL_BORDER);
        context.fill(px, py, px + pw, py + ph, COL_PANEL);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, py + 8, COL_TITLE);

        context.drawText(textRenderer, Text.translatable("ottoextra.chatColors.channel"),
                px + 28, py + 29, COL_MUTED, false);
        context.drawText(textRenderer, Text.translatable("ottoextra.chatColors.label"),
                labelFieldX(), py + 29, COL_MUTED, false);
        context.drawText(textRenderer, Text.translatable("ottoextra.chatColors.message"),
                messageFieldX(), py + 29, COL_MUTED, false);

        for (int index = 0; index < CHANNELS.length; index++) {
            ChatChannelState.ChatChannel channel = CHANNELS[index];
            int y = rowTop() + index * ROW_H;
            int original = ChatChannelButton.originalColor(channel);
            if (index % 2 == 0) context.fill(px + 6, y - 2, px + pw - 6, y + 16, 0x24FFFFFF);
            context.fill(px + 9, y + 1, px + 21, y + 13, COL_BORDER);
            context.fill(px + 10, y + 2, px + 20, y + 12, original);
            context.drawText(textRenderer, Text.literal(channel.label), px + 28, y + 3,
                    configuredPreview(labelFields.get(index).getText(), original), false);
            drawSwatch(context, labelFields.get(index), original);
            drawSwatch(context, messageFields.get(index), original);
        }

        context.drawText(textRenderer, Text.translatable("ottoextra.chatColors.rpFormatting"),
                px + 9, styleHeaderY(), COL_TITLE, false);
        drawStyleLabel(context, "*Emotes*", emoteColorField, config.chat.rpEmoteItalic,
                0xFFC6C6C6, styleRowTop());
        drawStyleLabel(context, "(OOC)", oocColorField, config.chat.rpOocItalic,
                0xFFB4BEC6, styleRowTop() + ROW_H);
        drawSwatch(context, emoteColorField, 0xFFC6C6C6);
        drawSwatch(context, oocColorField, 0xFFB4BEC6);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStyleLabel(DrawContext context, String value, TextFieldWidget field,
                                boolean italic, int fallback, int y) {
        int color = configuredPreview(field.getText(), fallback);
        context.drawText(textRenderer, Text.literal(value).setStyle(
                Style.EMPTY.withColor(color).withItalic(italic)),
                panelX() + 28, y + 3, color, false);
    }

    private static void drawSwatch(DrawContext context, TextFieldWidget field, int fallback) {
        int color = configuredPreview(field.getText(), fallback);
        int x = field.getX() + field.getWidth() + 2;
        int y = field.getY() + 2;
        context.fill(x - 1, y - 1, x + 11, y + 11, COL_BORDER);
        context.fill(x, y, x + 10, y + 10, color);
    }

    private static int configuredPreview(String raw, int fallback) {
        String normalized = normalized(raw);
        if (normalized.isEmpty()) return fallback;
        try {
            return 0xFF000000 | Integer.parseInt(normalized.substring(1), 16);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String normalized(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String value = raw.trim().replace("#", "");
        return value.matches("[0-9a-fA-F]{6}")
                ? "#" + value.toUpperCase(Locale.ROOT) : "";
    }

    private static String originalHex(ChatChannelState.ChatChannel channel) {
        return String.format(Locale.ROOT, "#%06X", ChatChannelButton.originalColor(channel) & 0xFFFFFF);
    }

    private static String key(ChatChannelState.ChatChannel channel) {
        return switch (channel) {
            case SPRECHEN -> "sprechen";
            case FLUESTERN -> "fluestern";
            case MURMELN -> "murmeln";
            case RUFEN -> "rufen";
            case BRUELLEN -> "bruellen";
            case OFFTOPIC -> "offtopic";
            case HILFE -> "hilfe";
        };
    }

    @Override
    public void close() {
        config.save();
        de.ottoextra.rpnames.chat.ChatHistoryRefresh.request();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
