package de.ottoextra.addon;

import de.ottoextra.config.settings.SettingsRegistry;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OttoExtraAddonHooksTest {
    @Test void nametagHooksRunInOrderAndIsolateExceptions() {
        OttoExtraAddon first = addon((context, current) -> current.withLines(
                Text.literal("title"), Text.literal("first"), null));
        OttoExtraAddon broken = addon((context, current) -> { throw new IllegalStateException("boom"); });
        OttoExtraAddon last = addon((context, current) -> current.withLines(
                current.title(), Text.literal(current.name().getString() + "-last"), Text.literal("account"))
                .withVisible(true, true));
        NametagContext context = new NametagContext(UUID.randomUUID(), "Account",
                Text.literal("Account"), true, true, false);

        NametagPresentation result = OttoExtraAddons.processNametag(List.of(first, broken, last),
                context, NametagPresentation.vanilla(false, context.vanillaName()));

        assertEquals("first-last", result.name().getString());
        assertEquals("title", result.title().getString());
        assertEquals("account", result.account().getString());
        assertTrue(result.visible());
        assertTrue(result.forceVisible());
    }

    @Test void hudLayoutElementsAreCollectedAndBrokenAddonsAreIsolated() {
        de.ottoextra.ui.hudlayout.HudLayoutElement element = new de.ottoextra.ui.hudlayout.HudLayoutElement() {
            public String id() { return "test"; }
            public Text label() { return Text.literal("Test"); }
            public Bounds bounds(int w, int h) { return null; }
            public void place(int x, int y, int w, int h, int sw, int sh) { }
            public void reset() { }
        };
        OttoExtraAddon good = new OttoExtraAddon() {
            public void registerSettings(SettingsRegistry registry) { }
            public List<de.ottoextra.ui.hudlayout.HudLayoutElement> hudLayoutElements() { return List.of(element); }
        };
        OttoExtraAddon broken = new OttoExtraAddon() {
            public void registerSettings(SettingsRegistry registry) { }
            public List<de.ottoextra.ui.hudlayout.HudLayoutElement> hudLayoutElements() {
                throw new IllegalStateException("boom");
            }
        };
        OttoExtraAddon plain = new OttoExtraAddon() {
            public void registerSettings(SettingsRegistry registry) { }
        };
        assertEquals(List.of(element), OttoExtraAddons.hudLayoutElements(List.of(broken, good, plain)));
    }

    @Test void addonCanProvideUsernameWhenBaseNametagModuleIsDisabled() {
        OttoExtraAddon fallback = addon((context, current) -> current.withLines(
                null, Text.literal(context.accountName()), null).withVisible(true, true));
        NametagContext context = new NametagContext(UUID.randomUUID(), "MinecraftName",
                Text.literal("MinecraftName"), true, false, false);
        NametagPresentation result = OttoExtraAddons.processNametag(List.of(fallback), context,
                NametagPresentation.vanilla(false, context.vanillaName()));
        assertEquals("MinecraftName", result.name().getString());
        assertTrue(result.forceVisible());
    }

    @Test void rpBookFieldsAreCombinedAndBrokenAddonDoesNotHideOthers() {
        OttoExtraAddon first = fields(List.of(new RpBookPlayerField(Text.literal("A"), Text.literal("1"), () -> { })));
        OttoExtraAddon broken = fields(null);
        OttoExtraAddon last = fields(List.of(new RpBookPlayerField(Text.literal("B"), Text.literal("2"), () -> { })));
        List<RpBookPlayerField> result = OttoExtraAddons.rpBookPlayerFields(List.of(first, broken, last),
                new RpBookPlayerContext(null, "Account", "RP", "Title", null));
        assertEquals(List.of("A", "B"), result.stream().map(f -> f.label().getString()).toList());
    }

    private static OttoExtraAddon addon(NametagHook hook) {
        return new OttoExtraAddon() {
            @Override public void registerSettings(SettingsRegistry registry) { }
            @Override public NametagPresentation processNametag(NametagContext context,
                                                                 NametagPresentation presentation) {
                return hook.apply(context, presentation);
            }
        };
    }

    private static OttoExtraAddon fields(List<RpBookPlayerField> fields) {
        return new OttoExtraAddon() {
            @Override public void registerSettings(SettingsRegistry registry) { }
            @Override public List<RpBookPlayerField> rpBookPlayerFields(RpBookPlayerContext context) {
                if (fields == null) throw new IllegalStateException("boom");
                return fields;
            }
        };
    }

    @FunctionalInterface private interface NametagHook {
        NametagPresentation apply(NametagContext context, NametagPresentation current);
    }
}
