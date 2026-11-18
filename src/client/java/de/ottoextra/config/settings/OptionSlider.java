package de.ottoextra.config.settings;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/** Slider for a {@link SettingsRegistry.Type#SLIDER} option; shows the value as a percentage. */
final class OptionSlider extends SliderWidget {
    private final SettingsRegistry.Option opt;

    OptionSlider(int x, int w, int h, SettingsRegistry.Option o) {
        super(x, 0, w, h, Text.empty(), normalized(o));
        this.opt = o;
        updateMessage();
    }

    private static double normalized(SettingsRegistry.Option o) {
        double v;
        try {
            v = Double.parseDouble(o.get.get().trim().replace(',', '.'));
        } catch (Exception e) {
            v = o.min;
        }
        double span = o.max - o.min;
        return span <= 0 ? 0 : Math.max(0, Math.min(1, (v - o.min) / span));
    }

    private int current() {
        return (int) Math.round(opt.min + value * (opt.max - opt.min));
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal(current() + "%"));
    }

    @Override
    protected void applyValue() {
        opt.set.accept(String.valueOf(current()));
    }
}
