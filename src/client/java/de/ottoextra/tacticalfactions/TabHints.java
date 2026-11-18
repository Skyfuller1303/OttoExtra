package de.ottoextra.tacticalfactions;

import net.minecraft.client.gui.widget.TextFieldWidget;

/**
 * Tab completion for name fields through OttoExtra's {@code TabCompletion}. Each call is guarded so
 * the fields keep working without completion on an OttoExtra that does not have it yet.
 */
final class TabHints {
    private TabHints() {
    }

    /** Minecraft account names (tab list and people book). */
    static void accounts(TextFieldWidget field) {
        try {
            de.ottoextra.ui.complete.TabCompletion.attach(field,
                    de.ottoextra.ui.complete.TabCompletionSources::accounts);
        } catch (Throwable ignored) {
            // Older OttoExtra: no completion.
        }
    }

    /** Gefolge and Lehen names from the region data. */
    static void gefolgeAndLehen(TextFieldWidget field) {
        try {
            de.ottoextra.ui.complete.TabCompletion.attach(field,
                    de.ottoextra.ui.complete.TabCompletionSources::gefolgeAndLehen);
        } catch (Throwable ignored) {
            // Older OttoExtra: no completion.
        }
    }
}
