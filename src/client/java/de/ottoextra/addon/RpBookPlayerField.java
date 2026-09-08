package de.ottoextra.addon;

import net.minecraft.text.Text;

/** A labelled, clickable row contributed to a selected RP player profile. */
public record RpBookPlayerField(Text label, Text value, Runnable action) {
    public RpBookPlayerField {
        if (label == null) label = Text.empty();
        if (value == null) value = Text.empty();
        if (action == null) action = () -> { };
    }
}
