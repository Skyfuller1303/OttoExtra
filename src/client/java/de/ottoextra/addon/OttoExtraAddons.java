package de.ottoextra.addon;

import de.ottoextra.OttoExtra;
import de.ottoextra.config.settings.SettingsRegistry;
import de.ottoextra.logging.DebugLog;
import de.ottoextra.logging.FailureLogGate;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class OttoExtraAddons {

    private static final Map<OttoExtraAddon, FailureLogGate> CHAT_FAILURE_LOGS =
            new ConcurrentHashMap<>();

    private static List<OttoExtraAddon> addons;

    private OttoExtraAddons() {
    }

    private static synchronized List<OttoExtraAddon> addons() {
        if (addons == null) {
            List<OttoExtraAddon> found = new ArrayList<>();
            for (EntrypointContainer<OttoExtraAddon> container
                    : FabricLoader.getInstance().getEntrypointContainers("ottoextra", OttoExtraAddon.class)) {
                try {
                    found.add(container.getEntrypoint());
                    DebugLog.debug("OttoExtra-Addon geladen: {}",
                            container.getProvider().getMetadata().getId());
                } catch (Throwable t) {
                    OttoExtra.LOGGER.error("OttoExtra-Addon aus '{}' konnte nicht geladen werden",
                            container.getProvider().getMetadata().getId(), t);
                }
            }
            addons = found;
        }
        return addons;
    }

    public static void registerSettings(SettingsRegistry registry) {
        for (OttoExtraAddon addon : addons()) {
            try {
                addon.registerSettings(registry);
            } catch (Throwable t) {
                OttoExtra.LOGGER.error("Addon-Settings konnten nicht registriert werden", t);
            }
        }
    }

    public static void registerDefaultSettings(SettingsRegistry registry) {
        for (OttoExtraAddon addon : addons()) {
            try {
                addon.registerDefaultSettings(registry);
            } catch (Throwable t) {
                OttoExtra.LOGGER.error("Addon-Default-Settings konnten nicht registriert werden", t);
            }
        }
    }

    public static Text processChatMessage(Text message) {
        return processChatMessage(new ChatMessageContext(
                message, message,
                de.ottoextra.chat.ChatSpeakerClassifier.SpeakerKind.UNKNOWN,
                true));
    }

    public static Text processChatMessage(ChatMessageContext context) {
        Text original = context.original();
        Text current = context.displayed();
        for (OttoExtraAddon addon : addons()) {
            FailureLogGate failureLog = CHAT_FAILURE_LOGS.computeIfAbsent(
                    addon, ignored -> new FailureLogGate());
            try {
                Text changed = addon.processChatMessage(new ChatMessageContext(
                        original, current, context.speakerKind(), context.incoming()));
                if (changed != null) current = changed;
                if (failureLog.onSuccess()) {
                    DebugLog.debug("Addon-Chatverarbeitung wieder aktiv: {}",
                            addon.getClass().getName());
                }
            } catch (Throwable t) {
                if (failureLog.onFailure()) {
                    OttoExtra.LOGGER.warn("Addon-Chatverarbeitung fehlgeschlagen ({}): {}",
                            addon.getClass().getName(), t.toString());
                }
            }
        }
        return current;
    }

    public static boolean delaysIncomingChat(ChatMessageContext context) {
        for (OttoExtraAddon addon : addons()) {
            try {
                if (addon.delaysIncomingChat(context)) return true;
            } catch (Throwable t) {
                OttoExtra.LOGGER.warn("Addon-Chatverzoegerung fehlgeschlagen ({}): {}",
                        addon.getClass().getName(), t.toString());
            }
        }
        return false;
    }

    /** Verarbeitet Addons in derselben Reihenfolge wie der synchrone Hook. */
    public static CompletableFuture<Text> processChatMessageAsync(ChatMessageContext context) {
        CompletableFuture<Text> chain = CompletableFuture.completedFuture(context.displayed());
        for (OttoExtraAddon addon : addons()) {
            chain = chain.thenCompose(current -> processAsyncAddon(addon,
                    new ChatMessageContext(context.original(), current,
                            context.speakerKind(), context.incoming())));
        }
        return chain;
    }

    private static CompletableFuture<Text> processAsyncAddon(
            OttoExtraAddon addon, ChatMessageContext context) {
        FailureLogGate failureLog = CHAT_FAILURE_LOGS.computeIfAbsent(
                addon, ignored -> new FailureLogGate());
        try {
            CompletableFuture<Text> result = addon.processChatMessageAsync(context);
            if (result == null) return CompletableFuture.completedFuture(context.displayed());
            return result.handle((changed, error) -> {
                if (error != null) {
                    if (failureLog.onFailure()) {
                        OttoExtra.LOGGER.warn("Asynchrone Addon-Chatverarbeitung fehlgeschlagen ({}): {}",
                                addon.getClass().getName(), error.toString());
                    }
                    return context.displayed();
                }
                if (failureLog.onSuccess()) {
                    DebugLog.debug("Addon-Chatverarbeitung wieder aktiv: {}",
                            addon.getClass().getName());
                }
                return changed == null ? context.displayed() : changed;
            });
        } catch (Throwable t) {
            if (failureLog.onFailure()) {
                OttoExtra.LOGGER.warn("Asynchrone Addon-Chatverarbeitung fehlgeschlagen ({}): {}",
                        addon.getClass().getName(), t.toString());
            }
            return CompletableFuture.completedFuture(context.displayed());
        }
    }

    public static Text processDisplayText(String scope, Text text) {
        Text current = text;
        for (OttoExtraAddon addon : addons()) {
            try {
                Text changed = addon.processDisplayText(scope, current);
                if (changed != null) current = changed;
            } catch (Throwable t) {
                OttoExtra.LOGGER.warn("Addon-Anzeigeverarbeitung fehlgeschlagen ({}): {}",
                        addon.getClass().getName(), t.toString());
            }
        }
        return current;
    }

    public static NametagPresentation processNametag(NametagContext context,
                                                       NametagPresentation presentation) {
        return processNametag(addons(), context, presentation);
    }

    static NametagPresentation processNametag(List<OttoExtraAddon> chain, NametagContext context,
                                               NametagPresentation presentation) {
        NametagPresentation current = presentation;
        for (OttoExtraAddon addon : chain) {
            try {
                NametagPresentation changed = addon.processNametag(context, current);
                if (changed != null) current = changed;
            } catch (Throwable t) {
                OttoExtra.LOGGER.warn("Addon-Nametagverarbeitung fehlgeschlagen ({}): {}",
                        addon.getClass().getName(), t.toString());
            }
        }
        return current;
    }

    public static List<RpBookPlayerField> rpBookPlayerFields(RpBookPlayerContext context) {
        return rpBookPlayerFields(addons(), context);
    }

    static List<RpBookPlayerField> rpBookPlayerFields(List<OttoExtraAddon> chain,
                                                       RpBookPlayerContext context) {
        List<RpBookPlayerField> result = new ArrayList<>();
        for (OttoExtraAddon addon : chain) {
            try {
                List<RpBookPlayerField> fields = addon.rpBookPlayerFields(context);
                if (fields != null) fields.stream().filter(java.util.Objects::nonNull).forEach(result::add);
            } catch (Throwable t) {
                OttoExtra.LOGGER.warn("Addon-RP-Buchfelder fehlgeschlagen ({}): {}",
                        addon.getClass().getName(), t.toString());
            }
        }
        return List.copyOf(result);
    }
}
