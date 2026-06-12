package de.ottoextra;

import de.ottoextra.config.OttoExtraConfig;

/**
 * Vertrag für ein OttoExtra-Feature-Modul (Karte, Regionen, RP-Namen, ...).
 *
 * <p>Module sind voneinander unabhängig und kapseln je eine Domäne. Sie werden
 * vom {@link OttoExtraClient} registriert und über den {@link OttoExtraContext}
 * mit Config und API versorgt. Lifecycle-Methoden sind optional (Default = no-op),
 * damit Stub-Module nur das implementieren, was sie brauchen.</p>
 *
 * <p>Fehler in einer Lifecycle-Methode dürfen den Client nicht abreissen — der
 * Aufrufer fängt {@link Throwable} pro Modul ab.</p>
 */
public interface OttoExtraModule {

    /** Stabiler Modul-Schlüssel (lowercase), z. B. {@code "map"}. */
    String id();

    /** Ob das Modul laut Config aktiv ist. Default: aktiv. */
    default boolean enabled(OttoExtraConfig config) {
        return true;
    }

    /** Einmalige Initialisierung beim Client-Start (Keybinds, HUD, Mixins-Hooks anmelden). */
    void onInitializeClient(OttoExtraContext context);

    /** Beitritt zu einem Ottonien-Server (nur aufgerufen, wenn Gate positiv). */
    default void onServerJoin(OttoExtraContext context) {
    }

    /** Verlassen eines Servers — flüchtigen Zustand zurücksetzen. */
    default void onDisconnect(OttoExtraContext context) {
    }

    /** Client fährt herunter — Ressourcen/Threads schliessen. */
    default void onClientStop(OttoExtraContext context) {
    }
}
