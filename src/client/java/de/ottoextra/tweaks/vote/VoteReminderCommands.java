package de.ottoextra.tweaks.vote;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class VoteReminderCommands {

    private VoteReminderCommands() {
    }

    public static void register(VoteReminderManager manager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("ottoextra")
                        .then(literal("vote")
                                .then(literal("test").executes(context -> {
                                    manager.showTest(MinecraftClient.getInstance());
                                    context.getSource().sendFeedback(
                                            Text.translatable("ottoextra.tweaks.vote.command.test"));
                                    return 1;
                                }))
                                .then(literal("reset").executes(context -> {
                                    manager.reset(MinecraftClient.getInstance());
                                    context.getSource().sendFeedback(
                                            Text.translatable("ottoextra.tweaks.vote.command.reset"));
                                    return 1;
                                })))));
    }
}
