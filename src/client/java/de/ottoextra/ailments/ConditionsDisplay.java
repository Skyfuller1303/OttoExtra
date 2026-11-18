package de.ottoextra.ailments;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/** Symbols next to name tags: one per kind, no tooltip. */
public final class ConditionsDisplay {

    public static final String SYMBOL_LEIDEN = "✚"; // heavy Greek cross
    public static final String SYMBOL_STATUS = "✦"; // black four pointed star

    private ConditionsDisplay() {
    }

    /** The name with a condition symbol appended, or the name itself if nothing is shared or shown. */
    public static Text decorate(UUID uuid, Text name) {
        try {
            var config = de.ottoextra.config.OttoExtraConfig.active();
            if (config.conditions != null && !config.conditions.showOthers) return name;
            boolean leiden = Conditions.has(uuid, Condition.KIND_LEIDEN);
            boolean status = Conditions.has(uuid, Condition.KIND_STATUS);
            if (!leiden && !status) return name;
            var out = name.copy();
            if (leiden) out.append(Text.literal(" " + SYMBOL_LEIDEN).formatted(Formatting.RED));
            if (status) out.append(Text.literal(" " + SYMBOL_STATUS).formatted(Formatting.AQUA));
            return out;
        } catch (RuntimeException e) {
            return name;
        }
    }
}
