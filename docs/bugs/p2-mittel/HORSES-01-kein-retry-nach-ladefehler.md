# HORSES-01: Ein Ladefehler sperrt automatische Pferdedatenabfragen bis zum Reconnect

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Codepfad; kein Live-API-Test.

## Auslöser und Folge

Beim ersten automatischen Laden der Pferdedaten schlägt die Rollen- oder Datensatzabfrage vorübergehend fehl. Die Tierinfo beim Halten einer Leine bleibt danach ohne Daten, auch wenn die API wieder erreichbar ist. Voraussetzung: Für das Pferd existiert noch keine lokale Tierbeobachtung als Fallback.

## Codebeleg

- [HorseRegistry.java](../../../src/client/java/de/ottoextra/horses/HorseRegistry.java#L84): `requestDatabaseRecords()` setzt `databaseRefreshRequested=true` vor dem Request. Der Fehlerpfad protokolliert nur und liefert eine leere Liste; das Flag bleibt gesetzt. Weitere automatische Aufrufe kehren sofort zurück.
- [AnimalInfoService.java](../../../src/client/java/de/ottoextra/animalinfo/AnimalInfoService.java#L186): `showDatabaseHorseInfo()` nutzt genau diesen automatischen Ladepfad und blendet das Overlay ohne Daten aus.
- [HorseRegistry.java](../../../src/client/java/de/ottoextra/horses/HorseRegistry.java#L279): Erst `clearConnection()` setzt das Flag zurück. Ein explizites `refreshDatabaseRecords()` umgeht die Sperre ebenfalls.

## Reproduktion

1. Neue Verbindung, Pferd ohne lokale Beobachtung mit Leine anvisieren.
2. Erste API-Abfrage mit Timeout fehlschlagen lassen.
3. API wieder verfügbar machen und erneut anvisieren: kein weiterer automatischer Request.

## Fixansatz und Regressionstest

Laufende Anfrage und erfolgreich geladene Daten getrennt abbilden. Nach Fehler begrenzten Retry mit Wartezeit erlauben; das Zurücksetzen an die Verbindungsgeneration binden. Mit einer Fake-API prüfen: erster Request scheitert, ein späterer Versuch derselben Verbindung lädt erfolgreich.

## Umsetzung am 06.09.2026

Laufende Abfrage, erfolgreicher Datenstand und Wiederholungszeit sind getrennt. Fehler erlauben nach fünf Sekunden einen neuen automatischen Versuch. Verspätete Fehler alter Verbindungen verändern die neue Abfragesperre nicht.

Regression: [HorseRegistryLifecycleTest.java](../../../src/test/java/de/ottoextra/horses/HorseRegistryLifecycleTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
