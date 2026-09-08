package de.ottoextra.rpnames.tablist;

import com.mojang.authlib.GameProfile;
import de.ottoextra.config.OttoExtraConfig;
import de.ottoextra.rpnames.RpNamesServices;
import de.ottoextra.rpnames.model.LocalRpProfile;
import de.ottoextra.rpnames.model.RpNameSource;
import de.ottoextra.rpnames.store.LocalRpIdentityStore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.text.object.AtlasTextObjectContents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TablistNameFormatterTest {
    private static final GameProfile PLAYER = new GameProfile(
            UUID.fromString("025f608c-95a0-4100-b285-9e8a661f4574"), "ProxyPlayer");
    private final Map<Field, Object> savedServices = new LinkedHashMap<>();
    private OttoExtraConfig.RpNames config;
    private LocalRpIdentityStore store;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = LocalRpIdentityStore.class.getDeclaredConstructor(boolean.class);
        constructor.setAccessible(true);
        store = constructor.newInstance(false);
        config = new OttoExtraConfig.RpNames();
        config.enabled = true;
        config.tablistEnabled = true;
        config.tablistShowTitle = true;
        config.proactiveMeet = false;
        config.globalRpNameColor = "#123456";
        replaceService("store", store);
        replaceService("config", config);
        replaceService("catalog", null);
        replaceService("titles", null);
        replaceService("active", true);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (var entry : savedServices.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
        if (store != null) {
            store.shutdown();
        }
    }

    @Test
    void absentDisplayNamePreservesTeamCrestSuffixAndStyles() {
        knownPlayer();
        MutableText crest = crest("first").setStyle(Style.EMPTY.withInsertion("crest-hover"));
        Team team = team(crest.copy().append(" Ritter "));
        Text originalPrefix = team.getPrefix().copy();
        team.setSuffix(Text.literal(" [A]").formatted(Formatting.BOLD));
        team.setColor(Formatting.GOLD);

        Text result = TablistNameFormatter.format(PLAYER, null, team);

        assertEquals(List.of(crest.getContent()), objects(result));
        assertTrue(result.getString().endsWith(" Ritter Alrik [A]"));
        assertEquals(Formatting.GOLD.getColorValue(), result.getStyle().getColor().getRgb());
        assertTrue(hasStyledText(result, "Alrik", style ->
                style.getColor() != null && style.getColor().getRgb() == 0x123456));
        assertTrue(hasStyledText(result, " [A]", Style::isBold));
        assertTrue(hasStyledText(result, crest.getString(), style ->
                "crest-hover".equals(style.getInsertion())));
        assertEquals(originalPrefix, team.getPrefix(), "Server-Komponente darf nicht mutiert werden");
    }

    @Test
    void lateTeamUpdatesAndRejoinUseCurrentCrestWithoutCachingOldOne() {
        knownPlayer();
        assertTrue(objects(TablistNameFormatter.format(PLAYER, null, null)).isEmpty());
        Team first = team(Text.empty());
        assertTrue(objects(TablistNameFormatter.format(PLAYER, null, first)).isEmpty());

        MutableText firstCrest = crest("first");
        first.setPrefix(firstCrest.copy().append(" "));
        assertEquals(List.of(firstCrest.getContent()),
                objects(TablistNameFormatter.format(PLAYER, null, first)));

        MutableText nextCrest = crest("next");
        first.setPrefix(nextCrest.copy().append(" "));
        assertEquals(List.of(nextCrest.getContent()),
                objects(TablistNameFormatter.format(PLAYER, null, first)));

        Team rejoined = team(firstCrest.copy().append(" "));
        assertEquals(List.of(firstCrest.getContent()),
                objects(TablistNameFormatter.format(PLAYER, null, rejoined)));
        assertTrue(objects(TablistNameFormatter.format(PLAYER, null, null)).isEmpty());
    }

    @Test
    void explicitDisplayNameTakesPrecedenceAndDoesNotDuplicateTeamCrest() {
        knownPlayer();
        MutableText explicitCrest = crest("display");
        Text original = explicitCrest.copy().append(" Ritter ProxyPlayer");
        Text before = original.copy();

        Text result = TablistNameFormatter.format(PLAYER, original, team(crest("team")));

        assertEquals(List.of(explicitCrest.getContent()), objects(result));
        assertTrue(result.getString().endsWith(" Ritter Alrik"));
        assertEquals(before, original);
    }

    @Test
    void localTitleReplacementAndHiddenTitlesKeepStructuredCrest() {
        LocalRpProfile profile = knownPlayer();
        profile.title = "Freiherr";
        profile.rawTitleDisplay = true;
        MutableText crest = crest("translated");
        Team team = team(Text.translatableWithFallback("ottoextra.test.crest", "%s%s",
                crest, Text.literal(" Ritter ")));

        Text withTitle = TablistNameFormatter.format(PLAYER, null, team);
        assertEquals(List.of(crest.getContent()), objects(withTitle));
        assertTrue(withTitle.getString().endsWith(" Freiherr Alrik"), withTitle::getString);
        assertFalse(withTitle.getString().contains("Ritter"));

        config.tablistShowTitle = false;
        Text withoutTitle = TablistNameFormatter.format(PLAYER, null, team);
        assertEquals(List.of(crest.getContent()), objects(withoutTitle));
        assertTrue(withoutTitle.getString().endsWith(" Alrik"));
        assertFalse(withoutTitle.getString().contains("Freiherr"));
        assertFalse(withoutTitle.getString().contains("Ritter"));
    }

    @Test
    void unknownPlayerKeepsCrestWithTitlesShownOrHidden() {
        config.proactiveMeet = true;
        MutableText crest = crest("unknown");
        Team team = team(crest.copy().append(" Ritter "));

        for (boolean showTitles : new boolean[]{true, false}) {
            config.tablistShowTitle = showTitles;
            Text result = TablistNameFormatter.format(PLAYER, null, team);
            assertEquals(List.of(crest.getContent()), objects(result));
            assertTrue(result.getString().contains("Unbekannt"));
            assertEquals(showTitles, result.getString().contains("Ritter"));
        }
    }

    @Test
    void disabledFormattingLeavesVanillaTeamFallbackInControl() {
        Team team = team(crest("vanilla"));
        config.tablistEnabled = false;
        assertNull(TablistNameFormatter.format(PLAYER, null, team));

        config.tablistEnabled = true;
        LocalRpProfile profile = knownPlayer();
        profile.showInTablist = false;
        assertNull(TablistNameFormatter.format(PLAYER, null, team));

        profile.showInTablist = true;
        config.enabled = false;
        assertNull(TablistNameFormatter.format(PLAYER, null, team));
        assertNull(TablistNameFormatter.format(null, null, team));
    }

    private LocalRpProfile knownPlayer() {
        LocalRpProfile profile = store.ensureSeen(PLAYER.name(), PLAYER.id().toString(),
                RpNameSource.SEEN_TABLIST);
        profile.rpName = "Alrik";
        return profile;
    }

    private static Team team(Text prefix) {
        Team team = new Team(new Scoreboard(), "proxy-team");
        team.setPrefix(prefix);
        return team;
    }

    private static MutableText crest(String name) {
        return MutableText.of(new ObjectTextContent(new AtlasTextObjectContents(
                Identifier.of("ottoextra", "crests"), Identifier.of("ottoextra", name))));
    }

    private static List<ObjectTextContent> objects(Text text) {
        assertNotNull(text);
        List<ObjectTextContent> result = new ArrayList<>();
        visit(text, node -> {
            if (node.getContent() instanceof ObjectTextContent object) {
                result.add(object);
            }
        });
        return result;
    }

    private static boolean hasStyledText(Text text, String expected,
                                         java.util.function.Predicate<Style> style) {
        boolean[] found = {false};
        visit(text, node -> {
            if (MutableText.of(node.getContent()).getString().equals(expected)
                    && style.test(node.getStyle())) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static void visit(Text text, java.util.function.Consumer<Text> visitor) {
        visitor.accept(text);
        if (text.getContent() instanceof TranslatableTextContent translated) {
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Text nested) {
                    visit(nested, visitor);
                }
            }
        }
        for (Text sibling : text.getSiblings()) {
            visit(sibling, visitor);
        }
    }

    private void replaceService(String name, Object value) throws Exception {
        Field field = RpNamesServices.class.getDeclaredField(name);
        field.setAccessible(true);
        savedServices.put(field, field.get(null));
        field.set(null, value);
    }
}
