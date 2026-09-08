package de.ottoextra.animalinfo;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/** Öffentliche read-only Ereignisse des AnimalInfo-Moduls. */
public final class AnimalInfoEvents {

    public static final Event<Observed> OBSERVED = EventFactory.createArrayBacked(
            Observed.class,
            listeners -> observation -> {
                for (Observed listener : listeners) {
                    listener.onObserved(observation);
                }
            });

    private AnimalInfoEvents() {
    }

    @FunctionalInterface
    public interface Observed {
        void onObserved(AnimalInfoObservation observation);
    }
}
