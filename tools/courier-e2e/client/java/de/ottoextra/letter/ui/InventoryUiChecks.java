package de.ottoextra.letter.ui;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;

/** Regression for a Mouse Wheelie helper-name collision in the named dev runtime. */
final class InventoryUiChecks {
    private InventoryUiChecks() { }

    static void run(ClientGameTestContext ctx) {
        boolean wasOpen = ctx.computeOnClient(c -> c.player.getRecipeBook().isGuiOpen(c.player.playerScreenHandler.getCategory()));
        boolean wasCreative = ctx.computeOnClient(c -> c.player.getAbilities().creativeMode);
        try {
            ctx.runOnClient(c -> c.player.getAbilities().creativeMode = false);
            for (boolean open : new boolean[]{true, false, true}) {
                ctx.runOnClient(c -> {
                    c.setScreen(null);
                    c.player.getRecipeBook().setGuiOpen(c.player.playerScreenHandler.getCategory(), open);
                    c.setScreen(new InventoryScreen(c.player));
                    require(c.currentScreen instanceof InventoryScreen, "Inventory did not open");
                    var book = recipeBook((InventoryScreen) c.currentScreen);
                    require(book.isOpen() == open, "Recipe book visibility changed");
                    book.toggleOpen();
                    book.toggleOpen();
                });
                ctx.waitTicks(5);
            }
            ctx.takeScreenshot("inventory-recipebook-open");
            ctx.getInput().resizeWindow(960, 540); ctx.waitTicks(5);
            ctx.runOnClient(c -> require(c.currentScreen instanceof InventoryScreen, "Inventory closed after resize"));
            ctx.takeScreenshot("inventory-recipebook-compact");
            System.out.println("INVENTORY_SMOKE mousewheelie=" + FabricLoader.getInstance().isModLoaded("mousewheelie")
                    + " repeatedOpen=3 recipeBookToggles=6 resized=true");
        } finally {
            ctx.runOnClient(c -> {
                c.setScreen(null);
                c.player.getRecipeBook().setGuiOpen(c.player.playerScreenHandler.getCategory(), wasOpen);
                c.player.getAbilities().creativeMode = wasCreative;
            });
            ctx.getInput().resizeWindow(1280, 720); ctx.waitTick();
        }
    }

    private static RecipeBookWidget<?> recipeBook(InventoryScreen screen) {
        try {
            var field = RecipeBookScreen.class.getDeclaredField("recipeBook");
            field.setAccessible(true);
            return (RecipeBookWidget<?>) field.get(screen);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
