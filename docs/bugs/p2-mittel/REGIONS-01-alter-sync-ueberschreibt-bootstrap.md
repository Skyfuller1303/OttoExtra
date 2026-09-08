# REGIONS-01: Alter Regions-Sync überschreibt nach Reconnect einen neueren Bootstrap

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Async- und Lifecycle-Reihenfolge statisch bestätigt; kein Live-API-Test.

## Auslöser und Folge

Eine Sync-Anfrage aus Verbindung A läuft weiter. Nach Disconnect und Reconnect wird bereits ein neuer Bootstrap angewendet. Trifft die alte Sync-Antwort erst danach ein, kann sie aktuelle Regions- und Fraktionsdaten wieder durch alte Datensätze ersetzen und den Sync-Cursor zurücksetzen. Der veraltete Stand wird zudem gespeichert.

## Codebeleg

- [RegionDataService.java:123](../../../src/client/java/de/ottoextra/regions/RegionDataService.java#L123): Disconnect erhöht Generationen und stoppt periodische Aufgaben, beendet aber die laufende Anfrage nicht.
- [RegionDataService.java:267](../../../src/client/java/de/ottoextra/regions/RegionDataService.java#L267): Der Sync-Callback übernimmt Antworten ohne Prüfung der ursprünglichen Verbindung.
- [RegionDataService.java:648](../../../src/client/java/de/ottoextra/regions/RegionDataService.java#L648): `applyEnvelope()` überschreibt Cursor und Indizes und speichert den Snapshot.
- [RegionsServices.java:36](../../../src/client/java/de/ottoextra/regions/RegionsServices.java#L36): Die Serviceinstanz bleibt über Disconnects erhalten.

## Reproduktion und Fixansatz

Mit steuerbaren Futures: alten Sync offenhalten, disconnecten, reconnecten, neuen Bootstrap mit höherem Cursor abschließen, dann alten Sync freigeben. Cursor und Daten dürfen sich danach nicht mehr ändern. Beim Syncstart die Verbindungsgeneration erfassen und vor Übernahme konsistent prüfen. Der reguläre Syncabstand von 20 Minuten macht eine automatische Korrektur nicht unmittelbar verfügbar.


## Umsetzung am 06.09.2026

Sync-Anfragen sind an die Bootstrap-/Verbindungsgeneration gebunden. Antwortprüfung und Übernahme sind gegenüber Join/Disconnect synchronisiert. Alte Antworten können weder Daten/Cursor ersetzen noch die Sperre eines neueren laufenden Syncs lösen. Der Test prüft auch den gespeicherten Snapshot.

Regression: [RegionSyncLifecycleTest.java](../../../src/test/java/de/ottoextra/regions/RegionSyncLifecycleTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
