package de.ottoextra.verify;

import de.ottoextra.OttoExtra;
import de.ottoextra.OttoExtraContext;
import de.ottoextra.OttoExtraModule;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AccountLinkModule implements OttoExtraModule {

    private final AtomicBoolean statusInFlight = new AtomicBoolean();
    private AccountLinkConfig config;
    private de.ottoextra.config.OttoExtraConfig rootConfig;
    private AccountLinkApi api;
    private de.ottoextra.api.OttoExtraApiClient profileApi;
    private long nextStatusCheck;
    private long startRequest;
    private boolean starting;

    @Override
    public String id() {
        return "verify";
    }

    @Override
    public void onInitializeClient(OttoExtraContext context) {
        config = AccountLinkConfig.load();
        rootConfig = context.config();
        api = new AccountLinkApi(config, rootConfig);
        profileApi = context.api();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("verify")
                                .executes(command -> {
                                    start(MinecraftClient.getInstance());
                                    return 1;
                                })
                                .then(ClientCommandManager.literal("status").executes(command -> {
                                    showStatus(MinecraftClient.getInstance());
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("unlink").executes(command -> {
                                    unlink(MinecraftClient.getInstance());
                                    return 1;
                                })))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("ottoextra")
                        .then(ClientCommandManager.literal("account").executes(command -> {
                            openOverview(MinecraftClient.getInstance());
                            return 1;
                        }))));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void start(MinecraftClient client) {
        var permit = de.ottoextra.privacy.ConsentPolicy.capture(rootConfig,
                de.ottoextra.privacy.ConsentPolicy.Purpose.ONLINE_SERVICES);
        if (!permit.valid()) {
            feedback(client, "ottoextra.consent.online.required", Formatting.YELLOW);
            return;
        }
        if (starting) return;
        if (client.player == null || client.player.getGameProfile() == null) {
            feedback(client, "ottoextra.verify.no_player", Formatting.RED);
            return;
        }
        if (config.hasDeviceToken() && !config.linkPending) {
            feedback(client, "ottoextra.verify.already_linked", Formatting.GREEN);
            return;
        }
        if (config.linkPending && config.authorizationUrl != null
                && !config.authorizationUrl.isBlank()) {
            showLink(client, config.authorizationUrl);
            return;
        }

        String uuid = client.player.getGameProfile().id().toString();
        String name = client.player.getGameProfile().name();
        long request = ++startRequest;
        starting = true;
        feedback(client, "ottoextra.verify.requesting", Formatting.GRAY);
        api.start(uuid, name).whenComplete((result, error) -> client.execute(() -> {
            if (request != startRequest) return;
            starting = false;
            if (!permit.valid()) return;
            if (error != null) {
                networkError(client, "ottoextra.verify.failed", error);
                return;
            }
            permit.commit(() -> {
                config.deviceToken = result.deviceToken();
                config.linkPending = true;
                config.minecraftUuid = uuid;
                config.minecraftName = name;
                config.authorizationUrl = result.authorizationUrl();
                config.save();
                nextStatusCheck = 0L;
                showLink(client, result.authorizationUrl());
            });
        }));
    }

    private void tick(MinecraftClient client) {
        var permit = de.ottoextra.privacy.ConsentPolicy.capture(rootConfig,
                de.ottoextra.privacy.ConsentPolicy.Purpose.ONLINE_SERVICES);
        if (!permit.valid()) return;
        if (config == null || !config.linkPending || !config.hasDeviceToken()
                || System.currentTimeMillis() < nextStatusCheck
                || !statusInFlight.compareAndSet(false, true)) {
            return;
        }
        String requestedToken = config.deviceToken;
        api.status().whenComplete((status, error) -> client.execute(() -> {
            statusInFlight.set(false);
            if (!permit.valid()) return;
            if (!Objects.equals(requestedToken, config.deviceToken)) return;
            nextStatusCheck = System.currentTimeMillis() + config.pollIntervalSeconds * 1_000L;
            if (error != null) {
                if (isUnauthorized(error)) {
                    config.clear();
                    feedback(client, "ottoextra.verify.expired", Formatting.RED);
                } else {
                    OttoExtra.LOGGER.debug("Verify-Status fehlgeschlagen", rootCause(error));
                }
                return;
            }
            if ("active".equals(status)) {
                permit.commit(() -> {
                    config.linkPending = false;
                    config.authorizationUrl = "";
                    config.save();
                    feedback(client, "ottoextra.verify.done", Formatting.GREEN);
                });
            }
        }));
    }

    private void showStatus(MinecraftClient client) {
        if (!config.hasDeviceToken()) {
            feedback(client, "ottoextra.verify.status.unlinked", Formatting.YELLOW);
        } else if (config.linkPending) {
            feedback(client, "ottoextra.verify.status.pending", Formatting.YELLOW);
        } else {
            feedback(client, "ottoextra.verify.status.linked", Formatting.GREEN);
        }
    }

    private void openOverview(MinecraftClient client) {
        if (client.player == null || client.player.getGameProfile() == null) {
            feedback(client, "ottoextra.verify.no_player", Formatting.RED);
            return;
        }
        client.setScreen(new AccountOverviewScreen(
                client.currentScreen,
                config,
                api,
                profileApi,
                client.player.getGameProfile().id(),
                client.player.getGameProfile().name()));
    }

    private void unlink(MinecraftClient client) {
        startRequest++;
        starting = false;
        if (!config.hasDeviceToken()) {
            feedback(client, "ottoextra.verify.status.unlinked", Formatting.GRAY);
            return;
        }
        String requestedToken = config.deviceToken;
        api.unlink().whenComplete((ignored, error) -> client.execute(() -> {
            if (!Objects.equals(requestedToken, config.deviceToken)) return;
            if (error != null && !isUnauthorized(error)) {
                networkError(client, "ottoextra.verify.unlink_failed", error);
                return;
            }
            config.clear();
            feedback(client, "ottoextra.verify.unlinked", Formatting.GREEN);
        }));
    }

    private static void showLink(MinecraftClient client, String url) {
        MutableText link = Text.literal(url).styled(style -> style
                .withColor(Formatting.AQUA)
                .withUnderline(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                .withHoverEvent(new HoverEvent.ShowText(
                        Text.translatable("ottoextra.verify.open"))));
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("ottoextra.verify.click")
                    .append(" ").append(link), false);
        }
    }

    private static void networkError(MinecraftClient client, String key, Throwable error) {
        String message = rootCause(error).getMessage();
        if (message == null || message.isBlank()) {
            message = rootCause(error).getClass().getSimpleName();
        }
        message = message.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").trim();
        if (message.length() > 160) {
            message = message.substring(0, 160);
        }
        if (client.player != null) {
            client.player.sendMessage(Text.translatable(key, message).formatted(Formatting.RED), false);
        }
    }

    private static void feedback(MinecraftClient client, String key, Formatting formatting) {
        if (client.player != null) {
            client.player.sendMessage(Text.translatable(key).formatted(formatting), false);
        }
    }

    private static boolean isUnauthorized(Throwable error) {
        Throwable cause = rootCause(error);
        return cause instanceof AccountLinkApi.ApiException apiError
                && (apiError.status() == 401 || apiError.status() == 403);
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
