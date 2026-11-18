package de.ottoextra.ui.complete;

import de.ottoextra.ui.theme.InkTextField;
import de.ottoextra.ui.theme.OttonienTheme;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tab completion for any text field. A screen attaches a candidate supplier to a field in its
 * {@code init()}; while that field is focused, Tab completes to the first match, further Tabs cycle
 * (Shift+Tab backwards) and Esc restores what was typed. A shadowless suggestion line under the
 * field (above it near the screen bottom) shows up to {@link TabCompletionModel#MAX_SHOWN} matches.
 * With no match Tab is left alone, so the normal focus change still works.
 *
 * <p>Addons call the static {@link #attach} methods; they only take Minecraft and JDK types, so no
 * inherited Minecraft member is reached through an OttoExtra type. Wrap the call in a
 * {@code try/catch (Throwable)} to keep working with an older OttoExtra without this class.
 */
public final class TabCompletion {
    private static final long CANDIDATE_REFRESH_MS = 1000L;
    private static final int MAX_ATTACHMENTS = 256;
    private static final int LINE_H = 11;
    private static final String SEPARATOR = "  ";

    private static final class Attachment {
        final TextFieldWidget field;
        final Supplier<List<String>> source;
        final boolean slate;
        final Screen owner;
        final TabCompletionModel model = new TabCompletionModel();
        List<String> cached = List.of();
        long cachedAt = Long.MIN_VALUE;

        Attachment(TextFieldWidget field, Supplier<List<String>> source, boolean slate, Screen owner) {
            this.field = field;
            this.source = source;
            this.slate = slate;
            this.owner = owner;
        }

        List<String> candidates() {
            long now = System.currentTimeMillis();
            if (now - cachedAt > CANDIDATE_REFRESH_MS || now < cachedAt) {
                cachedAt = now;
                try {
                    List<String> fresh = source.get();
                    cached = fresh == null ? List.of() : List.copyOf(fresh.stream().filter(s -> s != null).toList());
                } catch (Throwable ignored) {
                    cached = List.of();
                }
            }
            return cached;
        }
    }

    /** Attachments of open screens; dropped when their screen is removed. Render thread only. */
    private static final List<Attachment> ATTACHED = new ArrayList<>();
    private static boolean registered;

    private TabCompletion() {
    }

    /**
     * Attaches completion to {@code field}. The suggestion line uses the slate look for
     * {@link InkTextField}s in slate mode and for vanilla fields, otherwise the parchment look.
     */
    public static void attach(TextFieldWidget field, Supplier<List<String>> candidates) {
        boolean slate = !(field instanceof InkTextField ink) || ink.isSlate();
        attach(field, candidates, slate);
    }

    /**
     * Attaches completion to {@code field} with the given look ({@code slate}: dark line with light
     * text, else parchment with ink). Call it from the screen's {@code init()}; attaching the same
     * field again replaces the earlier supplier. Null arguments are ignored.
     */
    public static void attach(TextFieldWidget field, Supplier<List<String>> candidates, boolean slate) {
        if (field == null || candidates == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Screen owner = client == null ? null : client.currentScreen;
        ATTACHED.removeIf(a -> a.field == field);
        if (ATTACHED.size() >= MAX_ATTACHMENTS) {
            ATTACHED.remove(0);
        }
        ATTACHED.add(new Attachment(field, candidates, slate, owner));
    }

    /** Hooks key handling and drawing into every screen; called once from the client initializer. */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            ATTACHED.removeIf(a -> a.owner != null && a.owner != screen && a.owner != client.currentScreen);
            ScreenKeyboardEvents.allowKeyPress(screen).register(TabCompletion::allowKey);
            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, delta) -> render(s, context));
            ScreenEvents.remove(screen).register(s -> ATTACHED.removeIf(a -> a.owner == s));
        });
    }

    private static Attachment focused(Screen screen) {
        for (int i = ATTACHED.size() - 1; i >= 0; i--) {
            Attachment a = ATTACHED.get(i);
            if ((a.owner == null || a.owner == screen) && a.field.isVisible() && a.field.isFocused()
                    && a.field.isActive()) {
                return a;
            }
        }
        return null;
    }

    /** Returns false to swallow the key (handled here), true to let the screen handle it. */
    private static boolean allowKey(Screen screen, KeyInput input) {
        try {
            Attachment a = focused(screen);
            if (a == null) {
                return true;
            }
            String text = a.field.getText();
            if (input.isTab()) {
                String next = a.model.cycle(text, a.candidates(), input.hasShift());
                if (next == null) {
                    return true;
                }
                a.field.setText(next);
                a.field.setCursorToEnd(false);
                a.model.applied(a.field.getText());
                return false;
            }
            if (input.isEscape()) {
                String typed = a.model.cancel(text);
                if (typed != null) {
                    a.field.setText(typed);
                    a.field.setCursorToEnd(false);
                    return false;
                }
            }
        } catch (Throwable ignored) {
            // Never break a screen's keyboard handling.
        }
        return true;
    }

    private static void render(Screen screen, DrawContext context) {
        try {
            Attachment a = focused(screen);
            if (a == null) {
                return;
            }
            TabCompletionModel.Shown shown = a.model.shown(a.field.getText(), a.candidates());
            if (shown.isEmpty()) {
                return;
            }
            draw(context, MinecraftClient.getInstance().textRenderer, a, shown, screen.width, screen.height);
        } catch (Throwable ignored) {
            // Drawing the hint must never break the screen.
        }
    }

    private static void draw(DrawContext context, TextRenderer font, Attachment a, TabCompletionModel.Shown shown,
                             int screenW, int screenH) {
        TextFieldWidget f = a.field;
        int left = f.getX();
        int right = f.getX() + f.getWidth();
        if (f instanceof InkTextField ink) {
            left -= ink.boxInsetLeft();
            right += 3;
        }
        String lead = "Tab";
        int maxW = Math.max(Math.min(right - left, screenW), 160);
        List<String> items = new ArrayList<>(shown.items());
        int selectedIndex = shown.selected();
        // Too wide: drop matches from the end farther away from the selected one.
        while (items.size() > 1 && lineWidth(font, lead, items, shown.total()) > maxW) {
            boolean dropFirst = selectedIndex >= 0 && selectedIndex > (items.size() - 1) / 2;
            if (dropFirst) {
                items.remove(0);
                selectedIndex--;
            } else {
                items.remove(items.size() - 1);
            }
        }
        String more = shown.total() > items.size() ? "+" + (shown.total() - items.size()) : "";
        int width = lineWidth(font, lead, items, shown.total());
        int x = Math.max(0, Math.min(left, screenW - width));
        int y = f.getY() + f.getHeight() + 2;
        if (y + LINE_H > screenH) {
            y = f.getY() - LINE_H - 2;
        }
        int bg = a.slate ? OttonienTheme.FIELD_DARK : OttonienTheme.PA_L;
        int border = a.slate ? 0xFF000000 : OttonienTheme.PA_S;
        int text = a.slate ? OttonienTheme.LABEL_MUTED : OttonienTheme.INK_MUTED;
        int selText = a.slate ? OttonienTheme.LABEL : OttonienTheme.INK;
        int selBg = a.slate ? OttonienTheme.SLATE_TAB : OttonienTheme.PA_S;
        int hint = a.slate ? OttonienTheme.FIELD_HINT : OttonienTheme.INK_MUTED;
        OttonienTheme.rect(context, x, y, x + width, y + LINE_H, border);
        OttonienTheme.rect(context, x + 1, y + 1, x + width - 1, y + LINE_H - 1, bg);
        int tx = x + 4;
        int ty = y + 2;
        context.drawText(font, lead, tx, ty, hint, false);
        tx += font.getWidth(lead);
        for (int i = 0; i < items.size(); i++) {
            tx += font.getWidth(SEPARATOR);
            String item = items.get(i);
            int iw = font.getWidth(item);
            boolean selected = i == selectedIndex;
            if (selected) {
                OttonienTheme.rect(context, tx - 2, y + 1, tx + iw + 2, y + LINE_H - 1, selBg);
            }
            context.drawText(font, item, tx, ty, selected ? selText : text, false);
            tx += iw;
        }
        if (!more.isEmpty()) {
            tx += font.getWidth(SEPARATOR);
            context.drawText(font, more, tx, ty, hint, false);
        }
    }

    private static int lineWidth(TextRenderer font, String lead, List<String> items, int total) {
        int width = 4 + font.getWidth(lead) + 4;
        for (String item : items) {
            width += font.getWidth(SEPARATOR) + font.getWidth(item);
        }
        if (total > items.size()) {
            width += font.getWidth(SEPARATOR + "+" + (total - items.size()));
        }
        return width;
    }
}
