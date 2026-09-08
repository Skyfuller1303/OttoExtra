package de.ottoextra.animalinfo;

import de.ottoextra.config.OttoExtraConfig;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/** Actual renderer/layout checks; this class belongs only to the isolated test source set. */
public final class AnimalInfoUiChecks {
    private AnimalInfoUiChecks() { }

    public static void run(ClientGameTestContext ctx) {
        OttoExtraConfig settings = new OttoExtraConfig();
        ctx.runOnClient(c -> {
            for (String type : OttoExtraConfig.AnimalInfo.ANIMAL_TYPES) {
                var layout = settings.animalInfo.layout(type);
                for (float scale : new float[]{0.5F, 1.0F, 2.0F}) {
                    layout.genderIconScale = scale;
                    var parts = AnimalInfoOverlay.editorParts(settings.animalInfo, type);
                    var gender = part(parts, AnimalInfoOverlay.Part.GENDER_ICON);
                    require(gender.width() == Math.round(8 * scale)
                            && gender.height() == Math.round(8 * scale), "Gender icon must use an 8px display box");
                    int width = AnimalInfoOverlay.editorWidth(settings.animalInfo, type);
                    int height = AnimalInfoOverlay.editorHeight(settings.animalInfo, type);
                    for (var item : parts) {
                        require(item.x() >= 0 && item.y() >= 0 && item.x() + item.width() <= width
                                && item.y() + item.height() <= height, "Part clipped: " + type + "/" + item.part());
                    }
                }
                layout.genderIconScale = 1.0F;
                var parts = AnimalInfoOverlay.editorParts(settings.animalInfo, type);
                var title = part(parts, AnimalInfoOverlay.Part.TITLE);
                var gender = part(parts, AnimalInfoOverlay.Part.GENDER_ICON);
                require(title.x() + title.width() + 2 <= gender.x(), "Default title overlaps gender: " + type);
                if (parts.stream().anyMatch(item -> item.part() == AnimalInfoOverlay.Part.JUMP_HEIGHT)) {
                    var jump = part(parts, AnimalInfoOverlay.Part.JUMP_HEIGHT);
                    var speed = part(parts, AnimalInfoOverlay.Part.SPEED);
                    require(jump.x() + jump.width() <= speed.x(), "Default horse values overlap: " + type);
                }
            }
            var cow = settings.animalInfo.layout("cow");
            int clippedTitleWidth = part(AnimalInfoOverlay.editorParts(settings.animalInfo, "cow"),
                    AnimalInfoOverlay.Part.TITLE).width();
            cow.showGenderIcon = false;
            cow.showGenderText = false;
            int fullTitleWidth = part(AnimalInfoOverlay.editorParts(settings.animalInfo, "cow"),
                    AnimalInfoOverlay.Part.TITLE).width();
            require(fullTitleWidth > clippedTitleWidth, "Hidden gender must free space for the title");
            cow.showGenderIcon = true;
            cow.showGenderText = true;
            cow.genderIconY = 45;
            cow.genderTextY = 45;
            require(part(AnimalInfoOverlay.editorParts(settings.animalInfo, "cow"),
                    AnimalInfoOverlay.Part.TITLE).width() == fullTitleWidth,
                    "Gender in another row must not shorten the title");
            AnimalInfoOverlay.resetLayout(settings.animalInfo, "cow");
            cow = settings.animalInfo.layout("cow");
            cow.titleScale = 1.5F;
            var scaledTitle = part(AnimalInfoOverlay.editorParts(settings.animalInfo, "cow"), AnimalInfoOverlay.Part.TITLE);
            require(scaledTitle.x() + scaledTitle.width() + 2 <= cow.genderIconX,
                    "Scaled title overlaps gender");
            AnimalInfoOverlay.resetLayout(settings.animalInfo, "cow");
            var horse = settings.animalInfo.layout("horse");
            AnimalInfoOverlay.setPartVisible(horse, AnimalInfoOverlay.Part.GENDER_ICON, false);
            require(AnimalInfoOverlay.editorParts(settings.animalInfo, "horse").stream()
                    .noneMatch(item -> item.part() == AnimalInfoOverlay.Part.GENDER_ICON), "Hidden icon still occupies space");
            AnimalInfoOverlay.setPartVisible(horse, AnimalInfoOverlay.Part.GENDER_ICON, true);
            AnimalInfoOverlay.setPartPosition(horse, AnimalInfoOverlay.Part.GENDER_ICON, 220, 45);
            AnimalInfoOverlay.setPartScale(horse, AnimalInfoOverlay.Part.GENDER_ICON, 1.5F);
            var moved = part(AnimalInfoOverlay.editorParts(settings.animalInfo, "horse"), AnimalInfoOverlay.Part.GENDER_ICON);
            require(moved.x() == 220 && moved.y() == 45 && moved.width() == 12, "Moved/scaled icon has stale bounds");
            AnimalInfoOverlay.resetLayout(settings.animalInfo, "horse");
        });
        try {
            ctx.getInput().resizeWindow(1920, 1080); ctx.waitTick();
            for (int offset = 0; offset < OttoExtraConfig.AnimalInfo.ANIMAL_TYPES.size(); offset += 6) {
                List<String> page = OttoExtraConfig.AnimalInfo.ANIMAL_TYPES.subList(offset,
                        Math.min(offset + 6, OttoExtraConfig.AnimalInfo.ANIMAL_TYPES.size()));
                ctx.runOnClient(c -> { c.getToastManager().clear(); c.setScreen(new Gallery(settings, page)); });
                ctx.waitTick(); ctx.takeScreenshot("animal-toast-icons-types-" + offset);
            }
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
            ctx.runOnClient(c -> c.setScreen(new Gallery(settings,
                    List.of("horse", "sheep", "cow", "donkey", "pig", "wolf"))));
            ctx.waitTick(); ctx.takeScreenshot("animal-toast-icons-compact");
            ctx.runOnClient(c -> c.setScreen(new AnimalInfoEditScreen(null, settings)));
            ctx.waitTick(); ctx.takeScreenshot("animal-toast-layout-editor");
        } finally {
            ctx.runOnClient(c -> c.setScreen(null));
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
        }
    }

    private static AnimalInfoOverlay.PartBounds part(List<AnimalInfoOverlay.PartBounds> parts,
                                                     AnimalInfoOverlay.Part expected) {
        return parts.stream().filter(item -> item.part() == expected).findFirst().orElseThrow();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Gallery extends Screen {
        private final OttoExtraConfig settings;
        private final List<String> types;

        Gallery(OttoExtraConfig settings, List<String> types) {
            super(Text.literal("Tierinfo: 16px Texturen, kompakte Anzeige"));
            this.settings = settings;
            this.types = types;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(0, 0, width, height, 0xFF10151B);
            context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, 0xFFFFFFFF);
            for (int i = 0; i < types.size(); i++) {
                int x = 8 + (i % 2) * (width / 2);
                int y = 32 + (i / 2) * 50;
                AnimalInfoOverlay.renderEditorPreview(context, x, y, settings.animalInfo, types.get(i));
            }
        }
    }
}
