package de.ottoextra.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatConfigTest {

    private static final Map<String, String> CHANNEL_DEFAULTS = Map.of(
            "sprechen", "#DFC8A7",
            "fluestern", "#768491",
            "murmeln", "#58666F",
            "rufen", "#D2BF6A",
            "bruellen", "#FCF47E",
            "offtopic", "#B4BEC6",
            "hilfe", "#B53764");

    @Test
    void channelMessageColorsHaveConcreteDefaults() {
        OttoExtraConfig config = new OttoExtraConfig();

        CHANNEL_DEFAULTS.forEach((channel, color) ->
                assertEquals(color, config.chat.channelColors(channel).messageColor));
    }

    @Test
    void repairMigratesMissingOrInvalidChannelMessageColors() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.chat.channelColors("sprechen").messageColor = "";
        config.chat.channelColors("fluestern").messageColor = null;
        config.chat.channelColors("murmeln").messageColor = "ungültig";
        config.chat.channelColors.remove("rufen");

        config.repair();

        assertEquals("#DFC8A7", config.chat.channelColors("sprechen").messageColor);
        assertEquals("#768491", config.chat.channelColors("fluestern").messageColor);
        assertEquals("#58666F", config.chat.channelColors("murmeln").messageColor);
        assertEquals("#D2BF6A", config.chat.channelColors("rufen").messageColor);
    }

    @Test
    void repairPreservesAndNormalizesCustomChannelColors() {
        OttoExtraConfig config = new OttoExtraConfig();
        var colors = config.chat.channelColors("sprechen");
        colors.labelColor = " 123abc ";
        colors.messageColor = "654def";

        config.repair();

        assertEquals("#123ABC", colors.labelColor);
        assertEquals("#654DEF", colors.messageColor);

        colors.labelColor = "";
        config.repair();
        assertEquals("", colors.labelColor);
    }

    @Test
    void oldSnapshotReceivesAllChannelMessageDefaults() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.restoreFrom("{\"chat\":{\"enabled\":false,\"channelColors\":{"
                + "\"sprechen\":{\"messageColor\":\"\"},"
                + "\"hilfe\":{\"messageColor\":null}}}}");

        CHANNEL_DEFAULTS.forEach((channel, color) ->
                assertEquals(color, config.chat.channelColors(channel).messageColor));
    }

    @Test
    void npcColorsHaveReadableDefaults() {
        OttoExtraConfig.Chat chat = new OttoExtraConfig().chat;

        assertEquals("#C7A87F", chat.npcNameColor);
        assertEquals("#DFC8A7", chat.npcMessageColor);
    }

    @Test
    void repairNormalizesOrRestoresNpcColors() {
        OttoExtraConfig config = new OttoExtraConfig();

        config.chat.npcNameColor = "123abc";
        config.chat.npcMessageColor = "456def";
        config.repair();
        assertEquals("#123ABC", config.chat.npcNameColor);
        assertEquals("#456DEF", config.chat.npcMessageColor);

        config.chat.npcNameColor = "ungültig";
        config.chat.npcMessageColor = "";
        config.repair();
        assertEquals("#C7A87F", config.chat.npcNameColor);
        assertEquals("#DFC8A7", config.chat.npcMessageColor);

        config.chat.npcNameColor = null;
        config.chat.npcMessageColor = null;
        config.repair();
        assertEquals("#C7A87F", config.chat.npcNameColor);
        assertEquals("#DFC8A7", config.chat.npcMessageColor);
    }

    @Test
    void oldSnapshotWithoutNpcColorsReceivesDefaults() {
        OttoExtraConfig config = new OttoExtraConfig();
        config.chat.npcNameColor = "#123456";
        config.chat.npcMessageColor = "#654321";

        config.restoreFrom("{\"chat\":{\"enabled\":false}}");

        assertEquals("#C7A87F", config.chat.npcNameColor);
        assertEquals("#DFC8A7", config.chat.npcMessageColor);
    }

    @Test
    void snapshotRestorePreservesCustomNpcColors() {
        OttoExtraConfig source = new OttoExtraConfig();
        source.chat.npcNameColor = "#654321";
        source.chat.npcMessageColor = "#123456";
        OttoExtraConfig restored = new OttoExtraConfig();

        restored.restoreFrom(source.snapshotJson());

        assertEquals("#654321", restored.chat.npcNameColor);
        assertEquals("#123456", restored.chat.npcMessageColor);
    }
}
