package de.ottoextra.addon;

import de.ottoextra.config.settings.SettingsRegistry;

/**
 * Erweiterungspunkt für optionale OttoExtra-Addons (eigene Mod-JARs).
 *
 * <p>Ein Addon deklariert in seiner {@code fabric.mod.json} einen Entrypoint
 * unter dem Schlüssel {@code "ottoextra"}, der diese Schnittstelle
 * implementiert. OttoExtra ruft die Hooks auf, wenn das Settings-GUI
 * aufgebaut wird — Addons erscheinen so als eigenes Modul in der Sidebar.</p>
 *
 * <p>Addons verwalten ihre Config selbst (eigene Datei, eigenes Speichern);
 * OttoExtra persistiert nur die eigene {@code ottoextra.json}.</p>
 */
public interface OttoExtraAddon {

    /**
     * Registriert die Settings-Module des Addons in der Live-Registry.
     * Setter sollen direkt auf die Addon-Config schreiben und selbst speichern.
     */
    void registerSettings(SettingsRegistry registry);

    /**
     * Registriert dieselben Module gebunden an eine frische Default-Config,
     * damit das GUI "Standardwert"-Vergleiche anzeigen kann. Optional.
     */
    default void registerDefaultSettings(SettingsRegistry registry) {
    }
}
