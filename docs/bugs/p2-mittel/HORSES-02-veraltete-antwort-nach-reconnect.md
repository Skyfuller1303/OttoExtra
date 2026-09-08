# HORSES-02: Alte Pferdenamen-Antwort überschreibt Daten einer neuen Verbindung

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Codepfad; Timingfall nicht im Spiel ausgeführt.

## Auslöser und Folge

Eine Pferdenamenabfrage läuft während eines Disconnects weiter. Nach einem schnellen Reconnect trifft ihre Antwort später als die neue Abfrage ein. Die alte Antwort ersetzt dann die bereits aktuellen Namen. Ohne erneutes manuelles Laden bleiben veraltete Namen in der Verbindung sichtbar.

## Codebeleg

- [HorseRegistryModule.java](../../../src/client/java/de/ottoextra/horses/HorseRegistryModule.java#L93): Der Join lädt Namen; Disconnect leert den Verbindungszustand.
- [HorseRegistry.java](../../../src/client/java/de/ottoextra/horses/HorseRegistry.java#L203): `refresh()` prüft das Server-Gate nur vor dem Request. Der `thenApply`-Callback leert und befüllt `NAMES` ohne Prüfung von `connectionGeneration` oder aktuellem Server.
- [HorseRegistry.java](../../../src/client/java/de/ottoextra/horses/HorseRegistry.java#L279): `clearConnection()` erhöht bereits eine Generation. Der Namens-Callback verwendet sie nicht.

## Reproduktion

1. Namensabfrage A beim Join zurückhalten.
2. Disconnect und Reconnect; neue Abfrage B mit einem inzwischen geänderten Pferdenamen abschließen.
3. Danach A mit dem alten Namen abschließen: `NAMES` enthält wieder den alten Stand.

## Fixansatz und Regressionstest

Verbindungsgeneration beim Request erfassen und beim Anwenden unter gemeinsamer Synchronisierung prüfen. Auch innerhalb einer Verbindung sollten überholte Refresh-Antworten keinen neueren Stand ersetzen. Zwei steuerbare Futures in umgekehrter Reihenfolge abschließen; nach Disconnect darf A keinerlei Cacheänderung mehr auslösen.

## Umsetzung am 06.09.2026

Namensantworten müssen zur aktuellen Verbindung und zur neuesten Refresh-Anfrage passen. Prüfung und Cacheaustausch teilen die Lifecycle-Synchronisierung. Auch überholte Antworten innerhalb derselben Verbindung werden verworfen.

Regression: [HorseRegistryLifecycleTest.java](../../../src/test/java/de/ottoextra/horses/HorseRegistryLifecycleTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
